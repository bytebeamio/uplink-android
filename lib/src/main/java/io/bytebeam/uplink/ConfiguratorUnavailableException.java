package io.bytebeam.uplink;

public class ConfiguratorUnavailableException extends Exception {
    public ConfiguratorUnavailableException() {
        super("Configurator app is not installed on this device");
    }
}
