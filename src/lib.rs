use std::ops::Deref;
use std::sync::Arc;
use jni::JNIEnv;
use jni::objects::{JClass, JObject, JString, JValue};
use jni_sys::{jlong, jobject, jstring};
use uplink::{Config, Uplink};
use uplink::config::initalize_config;
use crate::bridge::AndroidBridge;

mod bridge;

pub struct UplinkAndroidContext {
    uplink: Uplink,
}

lazy_static::lazy_static! {
    pub static ref RUNTIME: tokio::runtime::Runtime =
        tokio::runtime::Runtime::new().expect("Can't start Tokio runtime");
}

#[no_mangle]
pub extern "C" fn Java_io_bytebeam_uplink_NativeApi_createUplink(
    env: JNIEnv,
    _: JClass,
    auth_config: JString,
    uplink_config: JString,
    action_callback: JObject,
) -> jlong {
    let action_callback = env.new_global_ref(action_callback).unwrap();
    env.call_method(
        &action_callback,
        "invoke",
        "(Lio/bytebeam/uplink/UplinkAction;)Ljava/lang/Void;",
        &[JValue::Void]
    ).unwrap();

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
        uplink.action_status(),
        Box::new(move |action| {
            // create java object from Action
            let env = jvm.attach_current_thread().unwrap();
            let ua_class = env.find_class("io/bytebeam/uplink/UplinkAction").unwrap();
            let uplink_action = env.new_object(
                ua_class,
                "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V",
                &[
                    JValue::Object(*env.new_string(action.action_id).unwrap())
                ]
            ).unwrap();
            env.call_method(
                &action_callback,
                "invoke",
                "(Lio/bytebeam/uplink/UplinkAction;)Ljava/lang/Void;",
                &[JValue::Object(uplink_action)]
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