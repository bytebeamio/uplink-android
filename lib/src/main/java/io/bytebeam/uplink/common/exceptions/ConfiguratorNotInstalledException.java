package io.bytebeam.uplink.common.exceptions;

/**
 * Thrown if the configurator app is not installed on this device
 */
public class ConfiguratorNotInstalledException extends Exception {
    public ConfiguratorNotInstalledException() {
        super("Configurator app is not installed on this device");
    }
}
