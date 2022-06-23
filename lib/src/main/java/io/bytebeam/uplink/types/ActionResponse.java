package io.bytebeam.uplink.types;

import lombok.Value;

@Value
public class ActionResponse {
    String id;
    int sequence;
    long timestamp;
    String state;
    int progress;
    String[] errors;

    public static ActionResponse success(String id) {
        return new ActionResponse(id, 0, System.currentTimeMillis(), "Completed", 100, new String[] {});
    }

    public static ActionResponse failure(String id, String... errors) {
        return new ActionResponse(id, 0, System.currentTimeMillis(), "Completed", 100, errors);
    }
}
