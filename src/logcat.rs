use std::io::{BufRead, BufReader};
use std::ops::Deref;
use std::process::{Command, ExitStatus, Stdio};
use std::sync::{Arc, Mutex};
use std::time::{Duration, SystemTime, UNIX_EPOCH};
use anyhow::Context;

use log::debug;
use serde::{Deserialize, Serialize};
use uplink::{Payload, Stream};

#[derive(Debug, Deserialize)]
pub struct LogcatConfig {
    pub tags: Vec<String>,
    pub min_level: LogLevel,
}

#[derive(Debug, Serialize, Deserialize, PartialEq, Eq, PartialOrd, Ord)]
pub enum LogLevel {
    Verbose = 0,
    Debug = 1,
    Info = 2,
    Warn = 3,
    Error = 4,
    Assert = 5,
    Fatal = 6,
}

#[derive(Debug, Serialize)]
struct LogEntry {
    level: LogLevel,
    tag: String,
    msg: String,
}

impl LogEntry {
    fn from_string(log: &str) -> Option<Self> {
        let tokens: Vec<&str> = log.split(' ').collect();

        let level = match *(tokens.get(4)?) {
            "F" => LogLevel::Fatal,
            "A" => LogLevel::Assert,
            "E" => LogLevel::Error,
            "W" => LogLevel::Warn,
            "I" => LogLevel::Info,
            "D" => LogLevel::Debug,
            _ => LogLevel::Verbose,
        };

        if level < config.min_level {
            return None;
        }

        let tag = tokens.get(5)?.to_string();

        Some(Self {
            level,
            tag,
            msg: log.to_string(),
        })
    }

    fn to_payload(&self, sequence: u32) -> anyhow::Result<Payload> {
        let payload = serde_json::to_value(self)?;
        let timestamp = SystemTime::now()
            .duration_since(UNIX_EPOCH)
            .unwrap_or(Duration::from_secs(0))
            .as_millis() as u64;

        Ok(Payload {
            stream: "logs".to_string(),
            sequence,
            timestamp,
            payload,
        })
    }
}

/// Starts a logcat instance that reports to the logs stream for a given
/// device+project id, that logcat instance is killed when this object
/// is dropped
pub struct LogcatInstance {
    kill_switch: Arc<Mutex<bool>>,
}

impl LogcatInstance {
    pub fn new(uplink_config: &uplink::Config, logcat_config: &LogcatConfig) -> Self {
        let kill_switch = Arc::new(Mutex::new(true));

        {
            /// Use a separate context for logging thread
            let kill_switch = kill_switch.clone();
            let mut log_stream = Stream::dynamic_with_size(
                "logs",
                &uplink_config.project_id,
                &uplink_config.device_id,
                1,
                uplink.bridge_data_tx().clone(),
            );

            std::thread::spawn(move || {
                let mut log_index = 1;
                match Command::new("logcat")
                    .args(["-v", "threadtime"])
                    .stdout(Stdio::piped())
                    .spawn() {
                    Ok(mut logcat) => {
                        let stdout = logcat
                            .stdout
                            .take()
                            .unwrap();
                        let mut buf_stdout = BufReader::new(stdout);
                        loop {
                            if kill_switch.lock() == false {
                                logcat.kill();
                                break;
                            } else {
                                let mut next_line = String::new();
                                match buf_stdout.read_line(&mut next_line) {
                                    Ok(bc) => {
                                        if bc == 0 {
                                            break;
                                        }
                                        let next_line = next_line.trim();
                                        log_stream.push(
                                            LogEntry::from_string(next_line)
                                                .unwrap_or(LogEntry {
                                                    level: LogLevel::Error,
                                                    tag: "LOGGER".to_string(),
                                                    msg: format!("Log line in unknown format: {}", next_line),
                                                })
                                                .to_payload(log_index),
                                        ).unwrap();
                                        log_index += 1;
                                    }
                                    Err(e) => {
                                        log_stream.push(
                                            LogEntry {
                                                level: LogLevel::Error,
                                                tag: "LOGGER".to_string(),
                                                msg: e.to_string(),
                                            }.to_payload(log_index),
                                        ).unwrap();
                                        log_index += 1;
                                        break;
                                    }
                                };
                            }
                        }
                    }
                    Err(_) => {
                        log_stream.push(
                            LogEntry {
                                level: LogLevel::Error,
                                tag: "LOGGER".to_string(),
                                msg: "Failed to start logcat".to_string(),
                            }.to_payload(log_index),
                        ).unwrap();
                        log_index += 1;
                    }
                };
            });
        }
        Self {
            kill_switch,
        }
    }
}

pub fn log_to_stream(entry: LogEntry, log_index: u32) {
    log_stream.push(
        entry.to_payload(log_index)
    );
}

impl Drop for LogcatInstance {
    fn drop(&mut self) {
        *self.kill_switch.get_mut() = false;
    }
}