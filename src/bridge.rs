use std::sync::Arc;
use flume::Receiver;
use anyhow::Error;
use log::error;
use uplink::{Action, Stream};
use crate::logcat;
use crate::logcat::{LogcatConfig, LogcatInstance, LogLevel};

pub struct AndroidBridge {
    uplink_config: Arc<uplink::Config>,
    actions_rx: Receiver<Action>,
    current_action: Option<String>,
    action_sink: Box<dyn Fn(Action) -> ()>,
    logcat: LogcatInstance,
}

unsafe impl Send for AndroidBridge {}

impl AndroidBridge {
    pub fn new(
        uplink_config: Arc<uplink::Config>,
        actions_rx: Receiver<Action>,
        action_sink: Box<dyn Fn(Action) -> ()>,
    ) -> AndroidBridge {
        AndroidBridge {
            uplink_config,
            actions_rx,
            current_action: None,
            action_sink,
            logcat: LogcatInstance::new(
                uplink_config.as_ref(),
                &LogcatConfig {
                    tags: vec!["*".to_string()],
                    min_level: LogLevel::Verbose,
                },
            ),
        }
    }

    pub async fn start(&mut self) -> Result<(), Error> {
        loop {
            if let Err(e) = self.collect().await {
                error!("Bridge failed. Error = {:?}", e);
            }
        }
    }

    pub async fn collect(
        &mut self,
    ) -> Result<(), Error> {
        loop {
            let action = self.actions_rx.recv_async().await?;
            self.current_action = Some(action.action_id.to_owned());

            match action.name.as_str() {
                "configure_logging" => {
                    match serde_json::from_str::<LogcatConfig>(action.payload.as_str()) {
                        Ok(logcat_config) => {
                            self.logcat = LogcatInstance::new(self.uplink_config.as_ref(), &logcat_config)
                        }
                        Err(e) => {
                            error!("couldn't parse logcat config payload:\n{}\n{}", action.payload, e)
                        }
                    }
                }
                _ => (self.action_sink)(action),
            }
        }
    }
}
