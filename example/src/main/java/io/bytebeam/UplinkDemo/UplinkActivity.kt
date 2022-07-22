package io.bytebeam.UplinkDemo

import android.os.BatteryManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import io.bytebeam.uplink.common.exceptions.ConfiguratorNotInstalledException
import io.bytebeam.uplink.Uplink
import io.bytebeam.uplink.UplinkStateCallback
import io.bytebeam.uplink.UplinkServiceState
import io.bytebeam.uplink.common.ActionSubscriber
import io.bytebeam.uplink.common.ActionResponse
import io.bytebeam.uplink.common.UplinkAction
import io.bytebeam.uplink.common.UplinkPayload
import io.bytebeam.uplink.common.exceptions.UplinkServiceNotRunningException
import org.json.JSONObject
import java.util.concurrent.Executors

class UplinkActivity : AppCompatActivity(), UplinkStateCallback, ActionSubscriber {
    val logs = ArrayDeque<String>()
    lateinit var logView: TextView

    var uplink: Uplink? = null;
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_uplink)

        logView = findViewById(R.id.logView)

        if (uplink == null) {
            initUplink()
        }
    }

    private fun initUplink() {
        log("connecting to uplink service")
        try {
            uplink = Uplink(this, this)
        } catch (e: ConfiguratorNotInstalledException) {
            Toast.makeText(this, "configurator app is not installed on this device", Toast.LENGTH_LONG).show()
        } catch (e: UplinkServiceNotRunningException) {
            log("service not running")
            Handler(Looper.getMainLooper()).postDelayed(this::initUplink, 1000)
        }
    }

    override fun onDestroy() {
        uplink?.dispose()
        super.onDestroy()
    }

    override fun onUplinkReady() {
        uplink?.subscribe(this)
        Executors.newSingleThreadExecutor().execute {
            var idx = 1
            // This loop will run as long as uplink client is connected
            while (uplink?.state == UplinkServiceState.CONNECTED) {
                val service = getSystemService(BATTERY_SERVICE) as BatteryManager
                uplink?.sendData(
                    UplinkPayload(
                        "test",
                        idx++,
                        System.currentTimeMillis(),
                        JSONObject().apply {
                            put("add", service.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY))
                            log("Sending battery data: $this")
                        }
                    )
                )
                Thread.sleep(100)
            }

            log("uplink client disconnected")
            // If the service goes down (configuration change), wait for a while and try to reconnect
            Thread.sleep(5000)
            initUplink()
        }
    }

    override fun processAction(action: UplinkAction) {
        log("Received action: $action")
        Executors.newSingleThreadExecutor().execute {
            for (i in 1..10) {
                while (uplink?.state != UplinkServiceState.CONNECTED) {
                    log("processAction: waiting for service to become available")
                    Thread.sleep(1000)
                }
                uplink?.respondToAction(
                    ActionResponse(
                        action.id,
                        i,
                        System.currentTimeMillis(),
                        if (i != 10) {
                            "Running"
                        } else {
                            "Completed"
                        },
                        i * 10,
                        arrayOf()
                    )
                )
                log("sending response: $i")
                Thread.sleep(1000)
            }
        }
    }

    private fun log(line: String) {
        Log.d(TAG, line)
        runOnUiThread {
            logs.addFirst(line)
            if (logs.size > 250) {
                logs.removeLast()
            }
            logView.text = logs.joinToString("\n")
        }
    }
}