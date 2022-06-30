package io.bytebeam.uplink.common;

public interface ActionSubscriber {
    void processAction(UplinkAction action);
}