package io.bytebeam.uplink

object NativeApi {
    init {
        System.loadLibrary("uplink_android")
    }
    external fun createUplink(authConfig: String, uplinkConfig: String, actionCallback: java.util.function.Function<Void, UplinkAction>): Long
    external fun destroyUplink(ref: Long)

    external fun sendData(that: Long, payload: UplinkPayload)
    external fun respond(that: Long, response: UplinkActionResponse)
}