package io.bytebeam.uplink.service;

import io.bytebeam.uplink.types.UplinkAction;

public interface ActionSubscriber {
    void processAction(UplinkAction action);
}