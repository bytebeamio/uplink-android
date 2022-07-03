package io.bytebeam.uplink;

public interface UplinkReadyCallback {
    /**
     * Called when the uplink instance is connected to the service and
     * ready to be used
     */
    void onUplinkReady();

    /**
     * Called if the user hasn't initialized the uplink service using
     * the configurator app
     */
    void onServiceNotConfigured();
}
