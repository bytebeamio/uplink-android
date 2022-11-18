package io.bytebeam.uplink.common;

import lombok.Value;
import org.json.JSONArray;
import org.json.JSONObject;

@Value
public class ActionResponse {
    String id;
    int sequence;
    long timestamp;
    String state;
    int progress;
    String[] errors;

    public static ActionResponse success(String id) {
        return new ActionResponse(id, 0, System.currentTimeMillis(), "Completed", 100, new String[]{});
    }

    public static ActionResponse failure(String id, String... errors) {
        return new ActionResponse(id, 0, System.currentTimeMillis(), "Completed", 100, errors);
    }

    public UplinkPayload toPayload() {
        JSONObject payload = new JSONObject();
        try {
            payload.put("action_id", id);
            payload.put("state", state);
            payload.put("progress", progress);
            JSONArray errorsJson = new JSONArray();
            for (String error : errors) {
                errorsJson.put(error);
            }
            payload.put("errors", errorsJson);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return new UplinkPayload(
                "action_status",
                sequence,
                timestamp,
                payload
        );
    }
}
