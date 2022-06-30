package io.bytebeam.uplink.common;

import android.os.Parcel;
import android.os.Parcelable;
import lombok.Value;
import org.json.JSONException;
import org.json.JSONObject;

@Value
public class UplinkPayload implements Parcelable {
    String stream;
    int sequence;
    long timestamp;
    String payload;

    public UplinkPayload(String stream, int sequence, long timestamp, JSONObject payload) {
        this.stream = stream;
        this.sequence = sequence;
        this.timestamp = timestamp;
        this.payload = payload.toString();
    }

    public static final Creator<UplinkPayload> CREATOR = new Creator<UplinkPayload>() {
        @Override
        public UplinkPayload createFromParcel(Parcel in) {
            try {
                return new UplinkPayload(
                        in.readString(),
                        in.readInt(),
                        in.readLong(),
                        new JSONObject(in.readString())
                );
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public UplinkPayload[] newArray(int size) {
            return new UplinkPayload[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(stream);
        dest.writeInt(sequence);
        dest.writeLong(timestamp);
        dest.writeString(payload);
    }
}
