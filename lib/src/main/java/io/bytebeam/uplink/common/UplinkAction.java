package io.bytebeam.uplink.common;

import android.os.Parcel;
import android.os.Parcelable;
import lombok.Value;

@Value
public class UplinkAction implements Parcelable {
    String id;
    String kind;
    String name;
    String payload;

    public static final Creator<UplinkAction> CREATOR = new Creator<UplinkAction>() {
        @Override
        public UplinkAction createFromParcel(Parcel in) {
            return new UplinkAction(
                    in.readString(),
                    in.readString(),
                    in.readString(),
                    in.readString()
            );
        }

        @Override
        public UplinkAction[] newArray(int size) {
            return new UplinkAction[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(kind);
        dest.writeString(name);
        dest.writeString(payload);
    }
}
