package io.bytebeam.uplink.common;

public class Constants {
    public static String TAG = "UplinkService";
    public static String DATA_KEY = "parcelData";

    // Incoming messenger events
    public static final int SEND_DATA = 0;
    public static final int SUBSCRIBE = 2;
    /// for testing, will trigger a segmentation fault
    public static final int CRASH = 3;
}
