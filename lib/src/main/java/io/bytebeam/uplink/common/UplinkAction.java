package io.bytebeam.uplink.common;

import android.os.Parcel;
import android.os.Parcelable;
import lombok.Value;

@Value
public class UplinkAction {
    String id;
    String kind;
    String name;
    String payload;
}
