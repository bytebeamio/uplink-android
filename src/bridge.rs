use std::collections::HashMap;
use std::sync::Arc;
use std::time::Duration;
use flume::{Receiver, Sender};
use anyhow::Error;
use jni::{JavaVM, JNIEnv};
use jni::objects::{JClass, JObject};
use jni_sys::jobject;
use log::error;
use tokio::select;
use tokio::time::Instant;
use uplink::{Action, ActionResponse, Config, Package, Stream};

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
            select! {
                action = self.actions_rx.recv_async() => {
                    let action = action?;
                    self.current_action = Some(action.action_id.to_owned());

                    action_timeout.as_mut().reset(Instant::now() + Duration::from_secs(10));

                    (self.action_sink)(action);
                }
            }
        }
    }
}
