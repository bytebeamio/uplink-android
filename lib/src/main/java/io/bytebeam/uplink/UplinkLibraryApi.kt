package io.bytebeam.uplink

object NativeApi {
    init {
        System.loadLibrary("uplink_android")
    }
    external fun createUplink(
        authConfig: String,
        uplinkConfig: String,
        actionCallback: ActionSubscriber,
    ): Long
    external fun destroyUplink(ref: Long)
    external fun sendData(that: Long, payload: UplinkPayload)
    external fun respond(that: Long, response: UplinkActionResponse)
}

object JavaApi {
    fun createUplinkAction(
        id: String,
        kind: String,
        name: String,
        payload: String,
    ) : UplinkAction {
        return UplinkAction(
            id,
            kind,
            name,
            payload
        )
    }
}