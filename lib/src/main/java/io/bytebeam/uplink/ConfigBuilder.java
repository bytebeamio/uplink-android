package io.bytebeam.uplink;

import io.bytebeam.uplink.generated.UplinkConfig;

public class ConfigBuilder {
    private UplinkConfig config;

    public ConfigBuilder(String base) throws Exception {
        config = new UplinkConfig(base);
    }

    public ConfigBuilder setOta(boolean enabled, String path) {
        config.setOta(enabled, path);
        return this;
    }

    public ConfigBuilder setStats(boolean enabled, String[] processNames, int  updatePeriod) {
        config.setStats(enabled, updatePeriod);
        for (String app: processNames) {
            config.addToStats(app);
        }
        return this;
    }

    public ConfigBuilder setPersistence(String path, long maxFileSize, int  maxFileCount) {
        config.setPersistence(path, maxFileSize, maxFileCount);
        return this;
    }

    public UplinkConfig build() {
        return config;
    }
}