package io.bytebeam.uplink;

public enum UplinkServiceState {
    /**
     * The client is trying to connect to the service
     */
    UNINITIALIZED,
    /**
     * The client is ready to be used
     */
    CONNECTED,
    /**
     * No device configuration selected in configurator app
     */
    SERVICE_NOT_CONFIGURED,
    /**
     * The service has stopped
     */
    SERVICE_STOPPED,
    /**
     * The client has disconnected from the service
     */
    FINISHED
}
