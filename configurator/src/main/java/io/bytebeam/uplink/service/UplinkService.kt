package io.bytebeam.uplink.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.*
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import io.bytebeam.uplink.common.Constants
import io.bytebeam.uplink.common.UplinkAction
import io.bytebeam.uplink.common.UplinkPayload
import io.bytebeam.uplink.configurator.MainActivity
import io.bytebeam.uplink.configurator.R

class UplinkService : Service() {
    var subscribers: MutableList<Messenger> = ArrayList()
    var uplink: Long = 0
    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val prefs = applicationContext.getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE)
        val authConfig = prefs.getString(Constants.PREFS_AUTH_CONFIG_KEY, null)
        if (authConfig == null) {
            Log.d(TAG, "device config not found")
            Toast.makeText(this, "device config not found", Toast.LENGTH_LONG).show()
            stopSelf()
            return START_NOT_STICKY
        }
        uplink = NativeApi.createUplink(
            authConfig, String.format(
                """
                    [persistence]
                    path = "%s/uplink"
                    
                    [streams.battery_stream]
                    topic = "/tenants/{tenant_id}/devices/{device_id}/events/battery_level"
                    buf_size = 1
                    
                    """.trimIndent(),
                applicationContext.filesDir.absolutePath
            ),
            true
        ) { uplinkAction: UplinkAction -> uplinkSubscription(uplinkAction) }
        Log.d(TAG, "uplink native context initialized")
        makeForeground()
        return START_REDELIVER_INTENT
    }

    private fun makeForeground() {
        val NOTIFICATION_CHANNEL_ID = "io.bytebeam.uplink.sevice"
        val channelName = "Uplink service status"

        val nm = (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
        nm.createNotificationChannel(
            NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                channelName,
                NotificationManager.IMPORTANCE_MAX
            )
        )

        val startAppIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            Intent.FLAG_ACTIVITY_NEW_TASK
        )
        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setOngoing(true)
            .setChannelId(NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Uplink service")
            .setContentText("Service is running and ready to be used")
            .setPriority(NotificationManager.IMPORTANCE_MAX)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(startAppIntent)
            .build()

        startForeground(1, notification)
        nm.notify(1, notification)
    }

    override fun onBind(intent: Intent): IBinder? {
        return if (uplink == 0L) {
            Log.d(TAG, "uplink service not running")
            null
        } else {
            Log.d(TAG, "returning messenger")
            Messenger(Handler(Looper.myLooper()!!) { message: Message -> handleMessage(message) }).binder
        }
    }

    override fun onDestroy() {
        Handler(Looper.myLooper()!!).postDelayed({ end() }, 200)
    }

    private fun end() {
        Log.d(TAG, "shutting down uplink service and process")
        stopForeground(true)
        subscribers.clear()
        if (uplink != 0L) {
            NativeApi.destroyUplink(uplink)
            uplink = 0
        }
        // forcefully kill the service process to allow the cleanup of the native resources
        System.exit(0)
    }

    private fun handleMessage(message: Message): Boolean {
        if (uplink == 0L) {
            Log.e(TAG, "messenger of an unbound service is being used, ignoring")
            return true
        }
        when (message.what) {
            Constants.SEND_DATA -> {
                val b = message.data
                b.classLoader = UplinkPayload::class.java.classLoader
                val payload = b.getParcelable<UplinkPayload>(Constants.DATA_KEY)
                Log.d(TAG, String.format("Submitting payload: %s", payload.toString()))
                NativeApi.sendData(uplink, payload)
            }
            Constants.SUBSCRIBE -> {
                Log.d(TAG, "adding a subscriber")
                subscribers.add(message.replyTo)
            }
            Constants.STOP_SERVICE -> {
                val sudoPass = applicationContext.getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE).getString(
                    Constants.PREFS_SERVICE_SUDO_PASS_KEY, null
                )
                val pass = message.data.getString(Constants.DATA_KEY, null)
                if (pass != null && pass == sudoPass) {
                    Log.d(TAG, "stopping service")
                    end()
                } else {
                    Log.e(TAG, String.format("privileged operation key mismatch: %s is not valid", pass))
                }
            }
            else -> throw IllegalArgumentException()
        }
        return true
    }

    private fun uplinkSubscription(uplinkAction: UplinkAction) {
        if (uplink == 0L) {
            Log.e(TAG, "Action delivered to an unbound service, ignoring")
            return
        }
        Log.d(TAG, String.format("Broadcasting action: %s", uplinkAction.toString()))
        for (subscriber in subscribers) {
            val m = Message()
            val b = Bundle()
            b.putParcelable(Constants.DATA_KEY, uplinkAction)
            m.data = b
            try {
                subscriber.send(m)
            } catch (e: RemoteException) {
                throw RuntimeException(e)
            }
        }
    }

    companion object {
        const val TAG = "UplinkService"
    }
}