use std::collections::HashMap;
use std::sync::Arc;
use std::time;
use std::time::Duration;
use flume::Receiver;
use anyhow::Error;
use log::error;
use tokio::select;
use tokio::time::Instant;
use uplink::{Action, ActionResponse, Config, Stream};

pub struct AndroidBridge {
    config: Arc<Config>,
    actions_rx: Receiver<Action>,
    current_action: Option<String>,
    action_status: Stream<ActionResponse>,
    action_sink: Box<dyn Fn(Action) -> ()>,
}

impl AndroidBridge {
    pub fn new(
        config: Arc<Config>,
        actions_rx: Receiver<Action>,
        action_status: Stream<ActionResponse>,
        action_sink: Box<dyn Fn(Action) -> ()>,
    ) -> AndroidBridge {
        AndroidBridge { config, actions_rx, current_action: None, action_status, action_sink }
    }

    pub async fn start(&mut self) -> Result<(), Error> {
        let mut action_status = self.action_status.clone();

        loop {
            if let Err(e) = self.collect().await {
                error!("Bridge failed. Error = {:?}", e);
            }
        }
    }

    pub async fn collect(
        &mut self,
    ) -> Result<(), Error> {
        let mut bridge_partitions = HashMap::new();
        for (stream, config) in self.config.streams.clone() {
            bridge_partitions.insert(
                stream.clone(),
                Stream::new(stream, config.topic, config.buf_size, self.data_tx.clone()),
            );
        }

        let mut action_status = self.action_status.clone();
        let action_timeout = time::sleep(Duration::from_secs(10));

        tokio::pin!(action_timeout);
        loop {
            select! {
                action = self.actions_rx.recv_async() => {
                    let action = action?;
                    self.current_action = Some(action.action_id.to_owned());

                    action_timeout.as_mut().reset(Instant::now() + Duration::from_secs(10));

                    self.action_sink.call(action);
                }

                _ = &mut action_timeout, if self.current_action.is_some() => {
                    let action = self.current_action.take().unwrap();
                    error!("Timeout waiting for action response. Action ID = {}", action);

                    // Send failure response to cloud
                    let status = ActionResponse::failure(&action, "Action timed out");
                    if let Err(e) = action_status.fill(status).await {
                        error!("Failed to fill. Error = {:?}", e);
                    }
                }
            }
        }
    }
}
