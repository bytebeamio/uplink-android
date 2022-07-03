package io.bytebeam.uplink.common.exceptions;

/**
 * Thrown if the configurator app is not installed on this device
 */
public class ConfiguratorUnavailableException extends Exception {
    public ConfiguratorUnavailableException() {
        super("Configurator app is not installed on this device");
    }
}
