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
     * The service has stopped
     */
    SERVICE_STOPPED,
    /**
     * Dispose method has been called
     */
    DISPOSED
}
