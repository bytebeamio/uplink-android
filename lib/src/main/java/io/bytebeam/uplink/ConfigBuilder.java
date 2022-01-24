package io.bytebeam.uplink;

import org.json.JSONObject;

public class ConfigBuilder {
    private JSONObject config;

    public ConfigBuilder(String base) throws Exception {
        config = new JSONObject(base);
    }

    public ConfigBuilder setOta(boolean enabled, String path) throws Exception {
        JSONObject ota = new JSONObject();
        ota.put("enabled", enabled);
        ota.put("path", path);
        config.put("ota", ota);

        return this;
    }

    public ConfigBuilder setStats(boolean enabled, String[] processNames, int  updatePeriod) throws Exception {
        JSONObject stats = new JSONObject();
        stats.put("enabled", enabled);
        stats.put("process_names", processNames);
        stats.put("update_period", updatePeriod);
        config.put("stats", stats);

        return this;
    }

    public ConfigBuilder setPersistence(String path, long maxFileSize, int  maxFileCount) throws Exception {
        JSONObject persistence = new JSONObject();
        persistence.put("path",  path);
        persistence.put("max_file_size", maxFileSize);
        persistence.put("max_file_count", maxFileCount);
        config.put("persistence", persistence);

        return this;
    }

    public UplinkConfig build() throws Exception {
        return new UplinkConfig(config.toString());
    }
}