package io.bytebeam.uplink.types;

import android.os.Parcel;
import android.os.Parcelable;
import lombok.Value;

@Value
public class ActionResponse implements Parcelable {
    String id;
    int sequence;
    long timestamp;
    String state;
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
        return new ActionResponse(id, 0, System.currentTimeMillis(), "Completed", 100, new String[] {});
    }

    public static ActionResponse failure(String id, String... errors) {
        return new ActionResponse(id, 0, System.currentTimeMillis(), "Completed", 100, errors);
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
        dest.writeString(state);
        dest.writeInt(progress);
        dest.writeStringArray(errors);
    }
}
