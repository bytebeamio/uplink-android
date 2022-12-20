package io.bytebeam.uplink.common;

import android.util.Log;
import lombok.Value;
import org.json.JSONException;
import org.json.JSONObject;

import static io.bytebeam.uplink.Uplink.TAG;

@Value
public class UplinkPayload {
    String stream;
    int sequence;
    long timestamp;
    JSONObject payload;

    public String toJson() {
        try {
            JSONObject result = new JSONObject(payload.toString());
            result.put("stream", stream);
            result.put("sequence", sequence);
            result.put("timestamp", timestamp);
            return result.toString();
        } catch (JSONException e) {
            Log.e(TAG, String.format("unexpected error: %s", e));
            return "{}";
        }
    }
}
