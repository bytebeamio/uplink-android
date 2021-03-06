package io.bytebeam.uplink.service;

import io.bytebeam.uplink.common.ActionSubscriber;
import io.bytebeam.uplink.common.UplinkPayload;

public class NativeApi {
    static {
        System.loadLibrary("uplink_android");
    }

    public static native long createUplink(
            String authConfig,
            String uplinkConfig,
            boolean enableLogging,
            ActionSubscriber actionCallback
    );

    public static native void destroyUplink(long uplink);

    public static native void sendData(long uplink, UplinkPayload payload);

    public static native void crash();
}
