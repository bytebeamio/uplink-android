package io.bytebeam.uplink;

public interface UplinkStateCallback {
    /**
     * Called when the uplink instance is connected to the service and
     * ready to be used
     */
    void onUplinkReady();
}
