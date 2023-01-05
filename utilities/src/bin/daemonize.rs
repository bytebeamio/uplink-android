use std::process::Command;

fn main() {
    let args: Vec<String> = std::env::args().collect();
    let command = args.get(1).unwrap_or_else(|| panic!("no command provided"));
    let args = args.get(2..).unwrap_or(&[]);
    Command::new(command)
        .args(args)
        .spawn().unwrap();
}