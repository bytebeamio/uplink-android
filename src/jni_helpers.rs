use jni::errors::Error;
use jni::JNIEnv;
use jni::objects::{JObject, JValue};
use jni_sys::jobject;
use uplink::{ActionResponse, Payload};

pub fn create_ua_object(env: JNIEnv) -> JObject {
    let uplink_action = env.new_object(
        "io/bytebeam/uplink/UplinkAction",
        "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V",
        &[
            JValue::Object(*env.new_string("").unwrap()),
            JValue::Object(*env.new_string("").unwrap()),
            JValue::Object(*env.new_string("").unwrap()),
            JValue::Object(*env.new_string("").unwrap()),
        ]
    ).unwrap();
    uplink_action
}

pub fn call_string_getter(env: JNIEnv, obj: jobject, name: &str) -> Result<String, Error> {
    env.call_method(obj, name, "()Ljava/lang/String;", &[])
        .and_then(|s| s.l())
        .and_then(|l| env.get_string(l as _))
        .map(|js| js.into())
}

pub fn call_int_getter(env: JNIEnv, obj: jobject, name: &str) -> Result<u32, Error> {
    env.call_method(obj, name, "()Ljava/lang/Integer;", &[])
        .and_then(|s| s.i())
        .map(|i| i as _)
}

pub fn call_long_getter(env: JNIEnv, obj: jobject, name: &str) -> Result<u64, Error> {
    env.call_method(obj, name, "()Ljava/lang/Long;", &[])
        .and_then(|s| s.j())
        .map(|j| j as _)
}

pub trait FromJava<T> {
    fn from_java(env: JNIEnv, obj: jobject) -> T;
}

impl FromJava<Self> for Payload {
    fn from_java(env: JNIEnv, obj: jobject) -> Self {
        let payload_s = call_string_getter(env, obj, "getPayload").unwrap();
        let payload = serde_json::from_str(payload_s.as_str()).unwrap();
        Payload {
            stream: call_string_getter(env, obj, "getStream").unwrap(),
            sequence: call_int_getter(env, obj, "getSequence").unwrap(),
            timestamp: call_long_getter(env, obj, "getStream").unwrap(),
            payload,
        }
    }
}

impl FromJava<Self> for ActionResponse {
    fn from_java(env: JNIEnv, obj: jobject) -> Self {
        let errors_j = env.call_method(obj, "getErrors", "()[Ljava/lang/String;", &[])
            .and_then(|o| o.l())
            .unwrap();
        let capacity = env.get_array_length(errors_j as _).unwrap();
        let mut errors = Vec::with_capacity(capacity as _);
        for eidx in 0..capacity {
            errors.push(
                env.get_object_array_element(errors_j as _, eidx)
                    .and_then(|el| env.get_string(el as _))
                    .map(|js| js.into())
                    .unwrap()
            );
        }
        ActionResponse {
            id: call_string_getter(env, obj, "getId").unwrap(),
            sequence: call_int_getter(env, obj, "getSequence").unwrap(),
            timestamp: call_long_getter(env, obj, "getTimestamp").unwrap(),
            state: call_string_getter(env, obj, "getState").unwrap(),
            progress: call_int_getter(env, obj, "getProgress").unwrap() as _,
            errors,
        }
    }
}
