package io.bytebeam.uplink;

import io.bytebeam.uplink.types.UplinkAction;

public interface ActionSubscriber {
    void processAction(UplinkAction action);
}