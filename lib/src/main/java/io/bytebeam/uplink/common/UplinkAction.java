package io.bytebeam.uplink.common;

import lombok.Value;

@Value
public class UplinkAction {
    String action_id;
    String kind;
    String name;
    String payload;
}
