use std::sync::Arc;
use jni::JNIEnv;
use jni::objects::{JClass, JObject, JString};
use jni_sys::{jlong, jobject, jstring};
use uplink::{Config, Uplink};
use uplink::config::initalize_config;
use crate::bridge::AndroidBridge;

mod lib3;
mod bridge;

pub struct UplinkAndroidContext {
    uplink: Uplink,
    bridge: AndroidBridge,
}

lazy_static::lazy_static! {
    pub static ref RUNTIME: runtime::Runtime =
        runtime::Runtime::new().expect("Can't start Tokio runtime");
}

#[no_mangle]
pub extern "C" fn Java_io_bytebeam_uplink_NativeApi_createUplink(
    env: JNIEnv,
    _: JClass,
    auth_config: JString,
    uplink_config: JString,
    action_callback: JObject,
) -> jlong {
    let config = Arc::new(initalize_config(
        env.get_string(auth_config).unwrap().into().as_str(),
        env.get_string(uplink_config).unwrap().into().as_str(),
    ).unwrap());

    let mut uplink = Uplink::new(config.clone()).unwrap();
    uplink.spawn().await.unwrap();

    let mut bridge = AndroidBridge::new(
        config.clone(),
        uplink.bridge_action_rx(),
        uplink.action_status(),
        Box::new(|action| {
            // create java object from Action

        }),
    );

    Box::into_raw(Box::new(UplinkAndroidContext {
        uplink,
        bridge,
    })) as _
}

#[no_mangle]
pub unsafe extern "C" fn Java_io_bytebeam_uplink_NativeApi_destroyUplink(
    env: JNIEnv,
    _: JClass,
    context: jlong,
) {
    Box::from_raw(context as *mut UplinkAndroidContext);
}