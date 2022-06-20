package io.bytebeam.uplink

import android.app.Service
import android.content.Intent
import android.os.IBinder

class UplinkService: Service() {
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}