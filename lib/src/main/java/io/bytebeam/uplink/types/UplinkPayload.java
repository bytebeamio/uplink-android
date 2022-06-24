package io.bytebeam.uplink.types;

import android.os.Parcel;
import android.os.Parcelable;
import lombok.Value;

@Value
public class UplinkPayload implements Parcelable {
    String stream;
    int sequence;
    long timestamp;
    String payload;

    public static final Creator<UplinkPayload> CREATOR = new Creator<UplinkPayload>() {
        @Override
        public UplinkPayload createFromParcel(Parcel in) {
            return new UplinkPayload(
                    in.readString(),
                    in.readInt(),
                    in.readLong(),
                    in.readString()
            );
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
