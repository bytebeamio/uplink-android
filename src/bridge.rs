use std::sync::Arc;
use flume::Receiver;
use anyhow::Error;
use log::error;
use uplink::{Action, Config};

pub struct AndroidBridge {
    config: Arc<Config>,
    actions_rx: Receiver<Action>,
    current_action: Option<String>,
    action_sink: Box<dyn Fn(Action) -> ()>,
}

unsafe impl Send for AndroidBridge {}

impl AndroidBridge {
    pub fn new(
        config: Arc<Config>,
        actions_rx: Receiver<Action>,
        action_sink: Box<dyn Fn(Action) -> ()>,
    ) -> AndroidBridge {
        AndroidBridge { config, actions_rx, current_action: None, action_sink }
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

            (self.action_sink)(action);
        }
    }
}
