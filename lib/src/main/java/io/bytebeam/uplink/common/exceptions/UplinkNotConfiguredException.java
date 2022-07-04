package io.bytebeam.uplink.common.exceptions;

/**
 * Thrown if the service is not configured properly using the configurator app
 */
public class UplinkNotConfiguredException extends RuntimeException {
    public UplinkNotConfiguredException() {
        super("attempt to use an uplink instance when the service is not configured properly");
    }
}
