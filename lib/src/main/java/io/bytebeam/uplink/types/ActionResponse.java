package io.bytebeam.uplink.types;

import lombok.Value;

@Value
public class ActionResponse {
    String id;
    int sequence;
    long timestamp;
    String state;
    int progress;
    String[] errors;
}
