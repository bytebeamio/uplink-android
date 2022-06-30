package io.bytebeam.uplink.common;

import android.os.Parcel;
import android.os.Parcelable;
import lombok.Value;
import org.json.JSONArray;
import org.json.JSONObject;

@Value
public class ActionResponse implements Parcelable {
    String id;
    int sequence;
    long timestamp;
    String status;
    int progress;
    String[] errors;

    public static final Creator<ActionResponse> CREATOR = new Creator<ActionResponse>() {
        @Override
        public ActionResponse createFromParcel(Parcel in) {
            return new ActionResponse(
                    in.readString(),
                    in.readInt(),
                    in.readLong(),
                    in.readString(),
                    in.readInt(),
                    in.createStringArray()
            );
        }

        @Override
        public ActionResponse[] newArray(int size) {
            return new ActionResponse[size];
        }
    };

    public static ActionResponse success(String id) {
        return new ActionResponse(id, 0, System.currentTimeMillis(), "Completed", 100, new String[]{});
    }

    public static ActionResponse failure(String id, String... errors) {
        return new ActionResponse(id, 0, System.currentTimeMillis(), "Completed", 100, errors);
    }

    public UplinkPayload toPayload() {
        JSONObject payload = new JSONObject();
        try {
            payload.put("id", id);
            payload.put("status", status);
            payload.put("progress`", progress);
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

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeInt(sequence);
        dest.writeLong(timestamp);
        dest.writeString(status);
        dest.writeInt(progress);
        dest.writeStringArray(errors);
    }
}
