package io.bytebeam.uplink;

import org.json.JSONObject;

public class ConfigBuilder {
    private UplinkConfig config;

    public ConfigBuilder(String base) throws Exception {
        config = new UplinkConfig(base);
    }

    public ConfigBuilder setOta(boolean enabled, String path) throws Exception {
        config.set_ota(enabled, path);
        return this;
    }

    public ConfigBuilder setStats(boolean enabled, String[] processNames, int  updatePeriod) throws Exception {
        config.set_stats(enabled, updatePeriod);
        for (String app: processNames) {
            config.add_to_stats(app);
        }
        return this;
    }

    public ConfigBuilder setPersistence(String path, long maxFileSize, int  maxFileCount) throws Exception {
        config.set_persistence(path, maxFileSize, maxFileCount);
        return this;
    }

    public UplinkConfig build() throws Exception {
        return config;
    }
}