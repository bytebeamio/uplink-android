use jni::errors::Error;
use jni::JNIEnv;
use jni::objects::JString;
use jni_sys::jobject;
use uplink::Payload;

pub fn call_string_getter(env: JNIEnv, obj: jobject, name: &str) -> Result<String, Error> {
    env.call_method(obj, name, "()Ljava/lang/String;", &[])
        .and_then(|s| s.l())
        .and_then(|l| env.get_string(JString::from(l)))
        .map(|js| js.into())
}

pub fn call_int_getter(env: JNIEnv, obj: jobject, name: &str) -> Result<u32, Error> {
    env.call_method(obj, name, "()I", &[])
        .and_then(|s| s.i())
        .map(|i| i as _)
}

pub fn call_long_getter(env: JNIEnv, obj: jobject, name: &str) -> Result<u64, Error> {
    env.call_method(obj, name, "()J", &[])
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
            timestamp: call_long_getter(env, obj, "getTimestamp").unwrap(),
            payload,
        }
    }
}

