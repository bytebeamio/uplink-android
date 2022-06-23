use std::ops::Deref;
use jni::errors::Error;
use jni::JNIEnv;
use jni::objects::{JObject, JString, JValue};
use jni_sys::{jarray, jobject};
use uplink::{ActionResponse, Payload};

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

impl FromJava<Self> for ActionResponse {
    fn from_java(env: JNIEnv, obj: jobject) -> Self {
        let errors_j = env.call_method(obj, "getErrors", "()[Ljava/lang/String;", &[])
            .and_then(|o| o.l())
            .unwrap();
        let errors_ja = *errors_j as jarray;
        let capacity = env.get_array_length(errors_ja).unwrap();
        let mut errors = Vec::with_capacity(capacity as _);
        for eidx in 0..capacity {
            errors.push(
                env.get_object_array_element(errors_ja, eidx)
                    .and_then(|el| env.get_string(JString::from(el)))
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

#[derive(Debug, serde::Serialize, serde::Deserialize)]
pub struct ActionResponsePayload {
    pub id: String,
    pub status: String,
    pub progress: u8,
    pub errors: Vec<String>,
}

impl ActionResponsePayload {
    pub fn from_action_response(response: &ActionResponse) -> Self {
        ActionResponsePayload {
            id: response.id.clone(),
            status: response.state.clone(),
            progress: response.progress,
            errors: response.errors.clone(),
        }
    }
}

pub fn action_response_to_payload(response: ActionResponse) -> Payload {
    Payload {
        stream: "action_status".to_owned(),
        sequence: response.sequence,
        timestamp: response.timestamp,
        payload: serde_json::to_value(ActionResponsePayload::from_action_response(&response)).unwrap(),
    }
}