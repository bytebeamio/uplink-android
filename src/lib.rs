use std::ops::Deref;
use std::sync::Arc;
use jni::JNIEnv;
use jni::objects::{JClass, JObject, JString, JValue};
use jni_sys::{jlong, jobject, jstring};
use log::{error, Level};
use uplink::{Config, Uplink};
use uplink::config::initalize_config;
use crate::bridge::AndroidBridge;

mod bridge;
mod jni_helpers;

pub struct UplinkAndroidContext {
    uplink: Uplink,
}

lazy_static::lazy_static! {
    pub static ref RUNTIME: tokio::runtime::Runtime =
        tokio::runtime::Runtime::new().expect("Can't start Tokio runtime");
}

macro_rules! strace {
    ($expr:expr) => ({
        let result = $expr;
        error!("{:?}", result);
        result
    })
}

#[no_mangle]
pub extern "C" fn Java_io_bytebeam_uplink_NativeApi_createUplink(
    env: JNIEnv,
    _: JClass,
    auth_config: JString,
    uplink_config: JString,
    action_callback: JObject,
) -> jlong {
    android_logger::init_once(
        android_logger::Config::default()
            .with_tag("NDK_MOD")
            .with_min_level(Level::Trace),
    );
    log_panics::init();

    let java_api = env.new_global_ref(env.get_static_field(
        "io/bytebeam/uplink/JavaApi",
        "INSTANCE",
        "Lio/bytebeam/uplink/JavaApi;"
    ).unwrap().l().unwrap()).unwrap();

    let action_callback = env.new_global_ref(action_callback).unwrap();

    let config = Arc::new(initalize_config(
        String::from(env.get_string(auth_config).unwrap()).as_str(),
        String::from(env.get_string(uplink_config).unwrap()).as_str(),
    ).unwrap());

    let mut uplink = Uplink::new(config.clone()).unwrap();
    RUNTIME.block_on(uplink.spawn()).unwrap();

    let jvm = env.get_java_vm().unwrap();
    let mut bridge = AndroidBridge::new(
        config.clone(),
        uplink.bridge_action_rx(),
        Box::new(move |action| {
            // create java object from Action
            let env = jvm.attach_current_thread().unwrap();
            let uplink_action = env.call_method(
                java_api.as_obj(),
                    "createUplinkAction",
                "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Lio/bytebeam/uplink/UplinkAction;",
                &[
                    JValue::Object(*env.new_string(&action.action_id).unwrap()),
                    JValue::Object(*env.new_string(&action.kind).unwrap()),
                    JValue::Object(*env.new_string(&action.name).unwrap()),
                    JValue::Object(*env.new_string(&action.payload).unwrap()),
                ]
            ).unwrap();
            env.call_method(
                &action_callback,
                "processAction",
                "(Lio/bytebeam/uplink/UplinkAction;)Ljava/lang/Void;",
                &[JValue::Object(uplink_action.l().unwrap())]
            ).unwrap();
        }),
    );
    RUNTIME.spawn(async move {
        bridge.start().await.unwrap();
    });

    Box::into_raw(Box::new(UplinkAndroidContext {
        uplink,
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

#[no_mangle]
pub extern "C" fn Java_io_bytebeam_uplink_NativeApi_sendData(
    env: JNIEnv,
    _: JClass,
) {}

#[no_mangle]
pub extern "C" fn Java_io_bytebeam_uplink_NativeApi_respond(
    env: JNIEnv,
    _: JClass,
) {}
