use std::alloc::System;
use std::cmp::PartialEq;
use std::fs::File;
use std::io::{BufRead, Write};
use std::process::Command;
use std::thread::sleep;
use std::time::{Duration, SystemTime};
use regex::{Captures, Regex};
use time::{OffsetDateTime, PrimitiveDateTime, Time};

const TOMORROW_3AM: PrimitiveDateTime = OffsetDateTime::now_utc().date()
    .next_day().unwrap()
    .with_hms(22, 15, 0)
    .unwrap();

const UPLINK_MODE_REGEX: Regex = Regex::new("^.+Switching to (.+) mode!!$").unwrap();

fn uplink_running() -> bool {
    let output = Command::new("pgrep")
        .arg("-x")
        .arg("uplink")
        .output()
        .expect("failed to execute process");
    return output.status.success();
}

fn uplink_mode() -> Option<String> {
    let file = File::open("/var/log/uplink.log").unwrap();
    for line in std::io::BufReader::new(file).lines().rev() {
        match line {
            Err(_) => break,
            Ok(line) => {
                if let Some(mode) = UPLINK_MODE_REGEX.captures(line.as_str())
                    .and_then(|matches| matches.get(1)) {
                    return Some(mode.as_str().to_string())
                }
            }
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
    let current_exe_path = std::env::current_exe().unwrap();
    let uplink_module_dir = current_exe_path
        .parent()
        .and_then(|d| d.parent())
        .unwrap_or_else(|| panic!("uplink module not installed properly"));
    let restart = || {
        Command::new(format!(uplink_module_dir.join("bin").join("daemonize")))
            .arg(uplink_module_dir.join("service.sh"))
            .output()
            .unwrap();
    };

    println!("waiting till: {TOMORROW_3AM}");
    let mut tics = 0;
    loop {
        let _ = std::io::stdout().flush();
        let _ = std::io::stderr().flush();
        sleep(Duration::from_secs(1));
        tics += 1;
        let now = OffsetDateTime::now_utc();

        if now > TOMORROW_3AM {
            println!("{now}: restarting uplink");
            restart()
        } else if !uplink_running() {
            println!("{now}: uplink crashed, restarting");
            restart
        }

        let ev_time = 5 * 60;
        if tics % ev_time == ev_time / 2 {
            if internet_working() {
                let curr_mode = uplink_mode();
                if curr_mode == "slow eventloop" {
                    println!("{now}: uplink stuck in slow eventloop mode, restarting");
                    restart();
                }
            }
        }
    }
}