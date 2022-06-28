use std::io::{BufRead, BufReader};
use std::process::{Command, ExitStatus, Stdio};
use std::time::{Duration, SystemTime, UNIX_EPOCH};

use log::debug;
use serde::{Deserialize, Serialize};
use uplink::{Payload, Stream};

#[derive(Debug, Deserialize)]
pub struct LogConfig {
    pub name: String,
    pub topic: String,
    pub tag: String,
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
struct Log {
    level: LogLevel,
    tag: String,
    msg: String,
}

impl Log {
    fn from_string(log: String, config: &LogConfig) -> Option<Self> {
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

        let tag = match tokens.get(5)? {
            s if s == &config.tag => s.to_string(),
            _ => return None,
        };

        Some(Self {
            level,
            tag,
            msg: log,
        })
    }

    fn to_payload(&self, sequence: u32) -> Result<Payload, String> {
        let payload = serde_json::to_value(self).map_err(|e| e.to_string())?;
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

pub fn relay_logs(
    mut log_stream: Stream<Payload>,
    log_config: LogConfig,
) -> Result<ExitStatus, String> {
    let mut logcat = Command::new("logcat")
        .args(["-v", "threadtime"])
        .stdout(Stdio::piped())
        .spawn()
        .map_err(|e| e.to_string())?;
    let stdout = logcat
        .stdout
        .as_mut()
        .ok_or_else(|| "stdout missing".to_string())?;
    let stdout_reader = BufReader::new(stdout);

    debug!("Collector setup to relay logs");

    for (sequence, line) in stdout_reader.lines().enumerate() {
        let log = line.map_err(|e| e.to_string())?;
        if let Some(log) = Log::from_string(log, &log_config) {
            let data = log.to_payload(sequence as u32)?;
            log_stream.push(data).map_err(|e| e.to_string())?;
        }
    }

    logcat.wait().map_err(|e| e.to_string())
}
