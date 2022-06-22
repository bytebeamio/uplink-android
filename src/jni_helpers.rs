use jni::JNIEnv;
use jni::objects::{JObject, JValue};

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