package io.bytebeam.uplink.service;

import io.bytebeam.uplink.types.UplinkAction;

public class JavaApi {
    private JavaApi() {}
    public static JavaApi INSTANCE = new JavaApi();

    public UplinkAction createUplinkAction(
            String stream,
            String kind,
            String name,
            String payload
    ) {
        return new UplinkAction(stream, kind, name, payload);
    }
}
