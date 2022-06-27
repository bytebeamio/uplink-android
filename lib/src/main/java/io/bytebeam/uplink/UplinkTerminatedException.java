package io.bytebeam.uplink;

/**
 * Thrown when the client notices that the remote
 * process has crashed. The user needs to create a new instance of
 * {Uplink} and use it
 */
public class UplinkTerminatedException extends Exception {
    public UplinkTerminatedException() {
        super("uplink service has terminated, a new instance needs to be created");
    }
}
