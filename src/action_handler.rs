use flume::{Receiver, Sender};
use log::error;
use uplink::{Action, ActionResponse, Package, Stream};

use crate::{logcat::LogConfig, UplinkAction};

pub trait ActionCallback {
    fn recvd_action(&self, action: UplinkAction);
}

#[derive(thiserror::Error, Debug)]
pub enum Error {
    #[error("Error receiving Action: {0}")]
    Recv(#[from] flume::RecvError),
    #[error("Error forwarding Action: {0}")]
    Send(#[from] flume::SendError<Action>),
    #[error("Error extracting LogConfig: {0}")]
    Json(#[from] serde_json::Error),
}

pub struct ActionHandler {
    bridge_rx: Receiver<Action>,
    actions_tx: Sender<Action>,
    data_tx: Sender<Box<dyn Package>>,
    action_status: Stream<ActionResponse>,
    log_enabled: bool,
}

impl ActionHandler {
    pub fn new(
        bridge_rx: Receiver<Action>,
        data_tx: Sender<Box<dyn Package>>,
        action_status: Stream<ActionResponse>,
        log_enabled: bool,
    ) -> (Self, Receiver<Action>) {
        let (actions_tx, actions_rx) = flume::bounded(10);
        (
            Self {
                bridge_rx,
                actions_tx,
                data_tx,
                action_status,
                log_enabled,
            },
            actions_rx,
        )
    }

    pub fn start(mut self) -> Result<(), Error> {
        loop {
            let action = self.bridge_rx.recv()?;
            let id = action.action_id.to_owned();

            if let Err(e) = self.select(action) {
                let resp = ActionResponse::failure(&id, e.to_string());
                self.action_status.push(resp).unwrap();
            }
        }
    }

    fn select(&self, action: Action) -> Result<(), Error> {
        match action.kind.as_str() {
            "log_collector" if self.log_enabled => {
                let log_config = serde_json::from_str(&action.payload)?;
                self.spawn_log_relay(log_config);
            }
            _ => {
                self.actions_tx.send(action)?;
            }
        }

        Ok(())
    }

    pub fn spawn_log_relay(&self, log_config: LogConfig) {
        let log_stream = Stream::new(
            &log_config.name,
            &log_config.topic,
            80,
            self.data_tx.clone(),
        );

        std::thread::spawn(move || {
            if let Err(e) = crate::logcat::relay_logs(log_stream, log_config) {
                error!("Error while relaying logs: {}", e);
            }
        });
    }
}
