#![allow(
    clippy::enum_variant_names,
    clippy::unused_unit,
    clippy::let_and_return,
    clippy::not_unsafe_ptr_arg_deref,
    clippy::cast_lossless,
    clippy::blacklisted_name,
    clippy::too_many_arguments,
    clippy::trivially_copy_pass_by_ref,
    clippy::let_unit_value,
    clippy::clone_on_copy
)]

use std::collections::HashMap;
use std::sync::Arc;

use figment::providers::{Data, Json, Toml};
use figment::Figment;
use flume::{Receiver, Sender};
use log::{error, info};

use uplink::config::{Config, Ota, Persistence, Stats};
use uplink::{Action, ActionResponse, Package, Payload, Stream};

const DEFAULT_CONFIG: &'static str = r#"
    bridge_port = 5555
    max_packet_size = 102400
    max_inflight = 100

    # Whitelist of binaries which uplink can spawn as a process
    # This makes sure that user is protected against random actions
    # triggered from cloud.
    actions = ["tunshell"]

    [streams.metrics]
    topic = "/tenants/{tenant_id}/devices/{device_id}/events/metrics/jsonarray"
    buf_size = 10

    # Action status stream from status messages from bridge
    [streams.action_status]
    topic = "/tenants/{tenant_id}/devices/{device_id}/action/status"
    buf_size = 1

    [ota]
    enabled = false
    path = "/var/tmp/ota-file"

    [stats]
    enabled = false
    process_names = ["uplink"]
    update_period = 5
"#;

pub trait ActionCallback {
    fn recvd_action(&self, action: UplinkAction);
}

struct CB(pub Box<dyn ActionCallback>);

unsafe impl Send for CB {}

pub struct UplinkConfig {
    inner: Config,
}

impl UplinkConfig {
    pub fn new(config: String) -> Result<UplinkConfig, String> {
        Ok(UplinkConfig {
            inner: Figment::new()
                .merge(Data::<Toml>::string(&DEFAULT_CONFIG))
                .merge(Data::<Json>::string(&config))
                .extract()
                .map_err(|e| e.to_string())?,
        })
    }

    pub fn set_ota(&mut self, enabled: bool, path: String) {
        self.inner.ota = Ota { enabled, path };
    }

    pub fn set_stats(&mut self, enabled: bool, update_period: u64) {
        self.inner.stats = Stats {
            enabled,
            process_names: vec![],
            update_period,
            stream_size: None,
        };
    }

    // TODO: Add a method to insert `StreamConfig`s into inner.streams

    pub fn add_to_stats(&mut self, app: String) {
        self.inner.stats.process_names.push(app);
    }

    pub fn set_persistence(&mut self, path: String, max_file_size: usize, max_file_count: usize) {
        let persistence = Persistence {
            path,
            max_file_size,
            max_file_count,
        };
        self.inner.persistence = Some(persistence);
    }
}

pub struct UplinkPayload {
    inner: Payload,
}

impl UplinkPayload {
    pub fn new(
        stream: String,
        timestamp: u64,
        sequence: u32,
        data: String,
    ) -> Result<UplinkPayload, String> {
        Ok(UplinkPayload {
            inner: Payload {
                stream,
                timestamp,
                sequence,
                payload: serde_json::from_str(&data).map_err(|e| e.to_string())?,
            },
        })
    }
}

pub struct UplinkAction {
    inner: Action,
}

impl UplinkAction {
    pub fn get_id(&self) -> &str {
        &self.inner.action_id
    }

    pub fn get_payload(&self) -> &str {
        &self.inner.payload
    }

    pub fn get_name(&self) -> &str {
        &self.inner.name
    }
}

pub struct Uplink {
    config: Arc<Config>,
    action_stream: Stream<ActionResponse>,
    streams: HashMap<String, Stream<Payload>>,
    bridge_rx: Receiver<Action>,
    data_tx: Sender<Box<dyn Package>>,
}

impl Uplink {
    pub fn new(config: UplinkConfig) -> Result<Uplink, String> {
        #[cfg(target_os = "android")]
        android_logger::init_once(
            android_logger::Config::default()
                .with_min_level(log::Level::Debug)
                .with_tag("uplink"),
        );
        log_panics::init();
        info!("init log system - done");

        let mut config = config.inner;

        if let Some(persistence) = &config.persistence {
            std::fs::create_dir_all(&persistence.path).map_err(|e| e.to_string())?;
        }
        let tenant_id = config.project_id.trim();
        let device_id = config.device_id.trim();
        for config in config.streams.values_mut() {
            let topic = str::replace(&config.topic, "{tenant_id}", tenant_id);
            config.topic = topic;

            let topic = str::replace(&config.topic, "{device_id}", device_id);
            config.topic = topic;
        }

        info!("Config: {:#?}", config);
        let config = Arc::new(config);

        let mut uplink = uplink::Uplink::new(config.clone()).map_err(|e| e.to_string())?;
        uplink.spawn().map_err(|e| e.to_string())?;

        let mut streams = HashMap::new();

        for (stream, cfg) in config.streams.iter() {
            streams.insert(
                stream.to_owned(),
                Stream::new(
                    stream.to_owned(),
                    cfg.topic.to_owned(),
                    cfg.buf_size,
                    uplink.bridge_data_tx(),
                ),
            );
        }

        Ok(Uplink {
            config,
            action_stream: uplink.action_status(),
            bridge_rx: uplink.bridge_action_rx(),
            streams,
            data_tx: uplink.bridge_data_tx(),
        })
    }

    pub fn send(&mut self, payload: UplinkPayload) -> Result<(), String> {
        let data = payload.inner;
        match self.streams.get_mut(&data.stream) {
            Some(x) => x.push(data).map_err(|e| e.to_string()),
            _ => {
                self.streams.insert(
                    data.stream.to_owned(),
                    Stream::dynamic(
                        &data.stream,
                        &self.config.project_id,
                        &self.config.device_id,
                        self.data_tx.clone(),
                    ),
                );

                self.streams
                    .get_mut(&data.stream)
                    .unwrap()
                    .push(data)
                    .map_err(|e| e.to_string())
            }
        }
    }

    pub fn respond(&mut self, response: ActionResponse) -> Result<(), String> {
        self.action_stream.push(response).map_err(|e| e.to_string())
    }

    pub fn subscribe(&mut self, cb: Box<dyn ActionCallback>) -> Result<(), String> {
        let cb = CB(cb);
        let bridge_rx = self.bridge_rx.clone();
        std::thread::spawn(move || {
            if let Err(e) = subscriber(cb, bridge_rx) {
                error!("Error while handling callback: {}", e);
            }
        });

        Ok(())
    }
}

fn subscriber(cb: CB, bridge_rx: Receiver<Action>) -> Result<(), String> {
    loop {
        cb.0.recvd_action(UplinkAction {
            inner: bridge_rx.recv().map_err(|e| e.to_string())?,
        });
    }
}

include!(concat!(env!("OUT_DIR"), "/java_glue.rs"));
