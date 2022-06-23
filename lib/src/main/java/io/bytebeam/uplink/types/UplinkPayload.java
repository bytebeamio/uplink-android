package io.bytebeam.uplink.types;

import lombok.Value;

@Value
public class UplinkPayload {
    String stream;
    int sequence;
    long timestamp;
    String payload;
}
