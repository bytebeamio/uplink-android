use flume::Receiver;
use anyhow::Error;
use log::error;
use uplink::{Action, Payload, Stream};
use crate::logcat::{LogcatConfig, LogcatInstance, LogLevel};
use crate::LOGCAT_TAG;

pub struct AndroidBridge {
    log_stream_constructor: Box<dyn Fn() -> Stream<Payload>>,
    actions_rx: Receiver<Action>,
    current_action: Option<String>,
    action_sink: Box<dyn Fn(Action) -> ()>,
    logcat: LogcatInstance,
}

unsafe impl Send for AndroidBridge {}

impl AndroidBridge {
    pub fn new(
        log_stream_constructor: Box<dyn Fn() -> Stream<Payload>>,
        actions_rx: Receiver<Action>,
        action_sink: Box<dyn Fn(Action) -> ()>,
    ) -> AndroidBridge {
        let log_stream = log_stream_constructor();
        AndroidBridge {
            log_stream_constructor,
            actions_rx,
            current_action: None,
            action_sink,
            logcat: LogcatInstance::new(
                log_stream,
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

            log::info!("received action: {:?}", action);
            match action.name.as_str() {
                "configure_logcat" => {
                    match serde_json::from_str::<LogcatConfig>(action.payload.as_str()) {
                        Ok(mut logcat_config) => {
                            logcat_config.tags = logcat_config.tags.into_iter()
                                .filter(|tag| !tag.is_empty() && tag.as_str() != LOGCAT_TAG)
                                .collect();
                            log::info!("restarting logcat");
                            self.logcat = LogcatInstance::new((self.log_stream_constructor)(), &logcat_config)
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
