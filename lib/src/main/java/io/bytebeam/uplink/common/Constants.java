package io.bytebeam.uplink.common;

public class Constants {
    public static String DATA_KEY = "parcelData";
    // Incoming messenger events
    public static final int SEND_DATA = 0;
    public static final int SUBSCRIBE = 2;
    public static final int STOP_SERVICE = 3;

    public static final String CONFIGURATOR_APP_ID = "io.bytebeam.uplink.configurator";
    public static final String UPLINK_SERVICE_ID = "io.bytebeam.uplink.service.UplinkService";

    public static final String PREFS_NAME = "configuration";
    public static final String PREFS_AUTH_CONFIG_KEY = "auth_config";
    public static final String PREFS_AUTH_CONFIG_NAME_KEY = "auth_config_name";
    public static final String PREFS_SERVICE_SUDO_PASS_KEY = "app_sudo_pass";
    public static String genPassKey() {
        return java.util.UUID.randomUUID().toString();
    }
}
