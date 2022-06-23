package io.bytebeam.uplink.types;

import lombok.Value;

@Value
public class UplinkAction {
    String id;
    String kind;
    String name;
    String payload;
}
