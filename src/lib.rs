use std::collections::HashMap;
use std::sync::Arc;
use jni::JNIEnv;
use jni::objects::{GlobalRef, JClass, JObject, JString, JValue};
use jni_sys::{jlong, jobject};
use log::{error, Level};
use uplink::{ActionResponse, Config, Payload, Stream, Uplink};
use uplink::config::initalize_config;
use crate::bridge::AndroidBridge;
use crate::jni_helpers::{action_response_to_payload, FromJava};

mod bridge;
mod jni_helpers;

pub struct UplinkAndroidContext {
    config: Arc<Config>,
    uplink: Uplink,
    java_api: GlobalRef,
    bridge_partitions: HashMap<String, Stream<Payload>>,
}

impl UplinkAndroidContext {
    pub fn push_payload(&mut self, payload: Payload) {
        let partition = match self.bridge_partitions.get_mut(&payload.stream) {
            Some(partition) => partition,
            None => {
                if self.bridge_partitions.keys().len() > 20 {
                    error!("Failed to create {:?} stream. More than max 20 streams", payload.stream);
                    return;
                }

                self.bridge_partitions.insert(
                    payload.stream.clone(),
                    Stream::dynamic(&payload.stream, &self.config.project_id, &self.config.device_id, self.uplink.bridge_data_tx()),
                ).unwrap();
                self.bridge_partitions.get_mut(&payload.stream).unwrap()
            }
        };

        if let Err(e) = partition.push(payload) {
            error!("Failed to send data. Error = {:?}", e.to_string());
        }
    }
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

    let java_api = env.get_static_field(
        "io/bytebeam/uplink/JavaApi",
        "INSTANCE",
        "Lio/bytebeam/uplink/JavaApi;",
    )
        .and_then(|f| f.l())
        .and_then(|l| env.new_global_ref(l))
        .unwrap();

    let action_callback = env.new_global_ref(action_callback).unwrap();

    let config = Arc::new(initalize_config(
        String::from(env.get_string(auth_config).unwrap()).as_str(),
        String::from(env.get_string(uplink_config).unwrap()).as_str(),
    ).unwrap());

    let mut uplink = Uplink::new(config.clone()).unwrap();
    RUNTIME.block_on(uplink.spawn()).unwrap();

    let jvm = env.get_java_vm().unwrap();
    let mut bridge = {
        let java_api = java_api.clone();
        AndroidBridge::new(
            config.clone(),
            uplink.bridge_action_rx(),
            Box::new(move |action| {
                let env = jvm.attach_current_thread().unwrap();
                let uplink_action = env.call_method(
                    java_api.as_obj(),
                    "createUplinkAction",
                    "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Lio/bytebeam/uplink/types/UplinkAction;",
                    &[
                        JValue::Object(*env.new_string(&action.action_id).unwrap()),
                        JValue::Object(*env.new_string(&action.kind).unwrap()),
                        JValue::Object(*env.new_string(&action.name).unwrap()),
                        JValue::Object(*env.new_string(&action.payload).unwrap()),
                    ],
                ).unwrap();
                env.call_method(
                    &action_callback,
                    "processAction",
                    "(Lio/bytebeam/uplink/types/UplinkAction;)V",
                    &[JValue::Object(uplink_action.l().unwrap())],
                ).unwrap();
            }),
        )
    };
    RUNTIME.spawn(async move {
        bridge.start().await.unwrap();
    });

    let mut bridge_partitions = HashMap::new();
    for (stream, config) in config.streams.clone() {
        bridge_partitions.insert(
            stream.clone(),
            Stream::new(stream, config.topic, config.buf_size, uplink.bridge_data_tx()),
        );
    }

    Box::into_raw(Box::new(UplinkAndroidContext {
        config,
        uplink,
        java_api,
        bridge_partitions,
    })) as _
}

#[no_mangle]
pub unsafe extern "C" fn Java_io_bytebeam_uplink_NativeApi_destroyUplink(
    _: JNIEnv,
    _: JClass,
    context: jlong,
) {
    Box::from_raw(context as *mut UplinkAndroidContext);
}

#[no_mangle]
pub unsafe extern "C" fn Java_io_bytebeam_uplink_NativeApi_sendData(
    env: JNIEnv,
    _: JClass,
    context: jlong,
    payload: jobject,
) {
    let context = &mut *(context as *mut UplinkAndroidContext);
    let payload = Payload::from_java(env, payload);

    context.push_payload(payload);
}

#[no_mangle]
pub unsafe extern "C" fn Java_io_bytebeam_uplink_NativeApi_crash(
    _: JNIEnv,
    _: JClass,
) {
    panic!("Crash requested");
}
