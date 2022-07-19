use std::io::{BufRead, BufReader};
use std::process::{Command, Stdio};
use std::sync::{Arc, Mutex};
use std::time::{Duration, SystemTime, UNIX_EPOCH};

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

impl LogLevel {
    pub fn from_str(s: &str) -> Option<LogLevel> {
        match s {
            "V" => Some(LogLevel::Verbose),
            "D" => Some(LogLevel::Debug),
            "I" => Some(LogLevel::Info),
            "W" => Some(LogLevel::Warn),
            "E" => Some(LogLevel::Error),
            "A" => Some(LogLevel::Assert),
            "F" => Some(LogLevel::Fatal),
            _ => None,
        }
    }

    pub fn to_str(&self) -> &'static str {
        match self {
            LogLevel::Verbose => "V",
            LogLevel::Debug => "D",
            LogLevel::Info => "I",
            LogLevel::Warn => "W",
            LogLevel::Error => "E",
            LogLevel::Assert => "A",
            LogLevel::Fatal => "F",
        }
    }
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

        let level = LogLevel::from_str(tokens.get(4)?)?;

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
    pub fn new(mut log_stream: Stream<Payload>, logcat_config: &LogcatConfig) -> Self {
        let kill_switch = Arc::new(Mutex::new(true));

        let mut filter_spec = vec!["*:S".to_string()];
        for tag in &logcat_config.tags {
            filter_spec.push(format!("{}:{}", tag, logcat_config.min_level.to_str()));
        }
        {
            let kill_switch = kill_switch.clone();

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
                            if *kill_switch.lock().unwrap() == false {
                                logcat.kill().ok();
                                break;
                            } else {
                                let mut next_line = String::new();
                                match buf_stdout.read_line(&mut next_line) {
                                    Ok(bc) => {
                                        if bc == 0 {
                                            break;
                                        }
                                        let next_line = next_line.trim();
                                        if let Some(entry) = LogEntry::from_string(next_line) {
                                            log_stream.push(entry.to_payload(log_index).unwrap()).unwrap();
                                            log_index += 1;
                                        } else {
                                            log::error!("log line in unknown format: {}", next_line);
                                        }
                                    }
                                    Err(e) => {
                                        log::error!("error while reading logcat output: {}", e);
                                        break;
                                    }
                                };
                            }
                        }
                    }
                    Err(e) => {
                        log::error!("failed to start logcat: {}", e);
                    }
                };
            });
        }
        Self {
            kill_switch,
        }
    }
}

impl Drop for LogcatInstance {
    fn drop(&mut self) {
        *self.kill_switch.lock().unwrap() = false;
    }
}