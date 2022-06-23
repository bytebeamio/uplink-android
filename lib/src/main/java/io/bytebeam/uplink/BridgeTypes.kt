package io.bytebeam.uplink

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class UplinkAction(
    val id: String,
    val kind: String,
    val name: String,
    val payload: String,
): Parcelable

@Parcelize
data class UplinkActionResponse(
    val id: String,
    val sequence: Int,
    val timestamp: Long,
    val state: String,
    val progress: Int,
    val errors: Array<String>,
): Parcelable

@Parcelize
data class  UplinkPayload(
    val stream: String,
    val sequence: Int,
    val timestamp: Long,
    // json payload
    val payload: String,
): Parcelable