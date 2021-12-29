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

use jni_sys::*;

pub struct Trial {}

impl Trial {
    pub fn new() -> Trial {
        Trial {}
    }

    pub fn hello(&self) -> String {
        "Hello!".to_string()
    }
}

include!(concat!(env!("OUT_DIR"), "/java_glue.rs"));
