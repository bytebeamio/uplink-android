use std::io::{BufRead, BufReader};
use std::process::{Command, ExitStatus};
use std::time::{Duration, SystemTime, UNIX_EPOCH};

use uplink::{Payload, Stream};

#[derive(Debug, serde::Serialize)]
enum LogLevel {
    Debug,
    Info,
    Warning,
    Error,
}

#[derive(Debug, serde::Serialize)]
struct Log {
    level: LogLevel,
    tag: String,
    msg: String,
}

impl Log {
    fn from_string(log: String) -> Option<Self> {
        let tokens: Vec<&str> = log.split(' ').collect();

        let level = match tokens.get(4)? {
            &"I" => LogLevel::Info,
            &"D" => LogLevel::Debug,
            &"W" => LogLevel::Warning,
            &"E" => LogLevel::Error,
            _ => return None,
        };
        let tag = tokens.get(5)?.to_string();

        Some(Self {
            level,
            tag,
            msg: log,
        })
    }

    fn to_payload(self, sequence: u32) -> Option<Payload> {
        let payload = serde_json::to_value(self).ok()?;
        let timestamp = SystemTime::now()
            .duration_since(UNIX_EPOCH)
            .unwrap_or(Duration::from_secs(0))
            .as_millis() as u64;

        Some(Payload {
            stream: "logs".to_string(),
            sequence,
            timestamp,
            payload,
        })
    }
}

pub fn relay_logs(mut log_stream: Stream<Payload>) -> Result<ExitStatus, String> {
    let mut logcat = Command::new("logcat")
        .args(["-v", "threadtime"])
        .stdout(std::process::Stdio::piped())
        .spawn()
        .map_err(|e| e.to_string())?;
    let stdout = logcat.stdout.as_mut().ok_or("stdout missing".to_string())?;
    let stdout_reader = BufReader::new(stdout);

    for (sequence, line) in stdout_reader.lines().enumerate() {
        let log = line.map_err(|e| e.to_string())?;
        if let Some(data) = Log::from_string(log)
            .and_then(|log| log.to_payload(sequence as u32)) {
            log_stream.push(data);
        }
    }

    logcat.wait().map_err(|e| e.to_string())
}
