use std::io::Write;
use std::process::Command;
use std::thread::sleep;
use std::time::Duration;
use regex::Regex;
use time::{OffsetDateTime, PrimitiveDateTime, UtcOffset};

lazy_static::lazy_static! {
    static ref UPLINK_MODE_REGEX: Regex = Regex::new("^.+Switching to (.+) mode!!$").unwrap();
}

fn uplink_running() -> bool {
    let output = Command::new("pgrep")
        .arg("-x")
        .arg("uplink")
        .output()
        .expect("failed to execute process");
    return output.status.success();
}

fn uplink_mode() -> Option<String> {
    let logs = format!(
        "{}\n{}",
        std::fs::read_to_string("/data/local/uplink/out.log.1").unwrap_or(String::new()),
        std::fs::read_to_string("/data/local/uplink/out.log").unwrap_or(String::new())
    );
    for line in logs.lines().rev() {
        if let Some(mode) = UPLINK_MODE_REGEX.captures(line)
            .and_then(|matches| matches.get(1)) {
            return Some(mode.as_str().to_string());
        }
    }
    return None;
}

fn internet_working() -> bool {
    let output = Command::new("ping")
        .arg("-c")
        .arg("1")
        .arg("google.com")
        .output()
        .expect("failed to execute process");
    return output.status.success();
}

fn main() {
    let tomorrow_3am: PrimitiveDateTime = OffsetDateTime::now_utc()
        .date()
        .next_day().unwrap()
        .with_hms(22, 15, 0)
        .unwrap();

    let current_exe_path = std::env::current_exe().unwrap();
    let uplink_module_dir = current_exe_path
        .parent()
        .and_then(|d| d.parent())
        .unwrap_or_else(|| panic!("uplink module not installed properly"));

    let restart = || {
        Command::new(uplink_module_dir.join("bin").join("daemonize"))
            .arg(uplink_module_dir.join("service.sh"))
            .output()
            .unwrap();
    };

    println!("waiting till: {tomorrow_3am}");
    let mut tics = 0;
    loop {
        let _ = std::io::stdout().flush();
        let _ = std::io::stderr().flush();
        sleep(Duration::from_secs(1));
        tics += 1;
        let now = OffsetDateTime::now_utc();

        if now > tomorrow_3am.assume_offset(UtcOffset::UTC) {
            println!("{now}: restarting uplink");
            restart()
        } else if !uplink_running() {
            println!("{now}: uplink crashed, restarting");
            restart()
        }

        let ev_time = 5 * 60;
        if tics % ev_time == ev_time / 2 {
            if internet_working() {
                if let Some(mode) = uplink_mode() {
                    if mode == "slow eventloop".to_string()  {
                        println!("{now}: uplink stuck in slow eventloop mode, restarting");
                        restart();
                    }
                }
            }
        }
    }
}