package io.bytebeam.UplinkDemo

import android.os.BatteryManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import io.bytebeam.uplink.common.UplinkReadyCallback
import io.bytebeam.uplink.Uplink
import io.bytebeam.uplink.UplinkServiceState
import io.bytebeam.uplink.common.ActionSubscriber
import io.bytebeam.uplink.common.ActionResponse
import io.bytebeam.uplink.common.UplinkAction
import io.bytebeam.uplink.common.UplinkPayload
import org.json.JSONObject
import java.util.concurrent.Executors

class UplinkActivity : AppCompatActivity(), io.bytebeam.uplink.common.UplinkReadyCallback,
    io.bytebeam.uplink.common.ActionSubscriber {
    val logs = mutableListOf<String>()
    lateinit var logView: TextView

    var uplink: Uplink? = null;
    var dataIndex = 1
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_uplink)

        logView = findViewById(R.id.logView)
    }

    override fun onStart() {
        super.onStart()
        uplink = Uplink(
            this,
            resources.getRawTextFile(R.raw.local_device),
            """
                [persistence]
                path = "${applicationInfo.dataDir}/uplink"
                
                [streams.battery_stream]
                topic = "/tenants/{tenant_id}/devices/{device_id}/events/battery_level"
                buf_size = 1
            """.trimIndent(),
            true,
            this
        )
    }

    override fun onStop() {
        uplink?.dispose()
        super.onStop()
    }

    override fun onUplinkReady() {
        uplink?.subscribe(this)
        Executors.newSingleThreadExecutor().execute {
            var idx = 1
            while (uplink?.state == UplinkServiceState.CONNECTED) {
                val service = getSystemService(BATTERY_SERVICE) as BatteryManager
                uplink?.sendData(
                    io.bytebeam.uplink.common.UplinkPayload(
                        "battery_stream",
                        idx++,
                        System.currentTimeMillis(),
                        JSONObject().apply {
                            put("level", service.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY))
                            put("status", service.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS).let {
                                when (it) {
                                    BatteryManager.BATTERY_STATUS_CHARGING -> "charging"
                                    BatteryManager.BATTERY_STATUS_DISCHARGING -> "discharging"
                                    BatteryManager.BATTERY_STATUS_FULL -> "full"
                                    BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "not charging"
                                    BatteryManager.BATTERY_STATUS_UNKNOWN -> "unknown"
                                    else -> "unknown"
                                }
                            })
                            log("Sending battery data: $this")
                        }
                    )
                )
                Thread.sleep(5000)
            }
        }
    }

    override fun processAction(action: io.bytebeam.uplink.common.UplinkAction) {
        log("Received action: $action")
        Executors.newSingleThreadExecutor().execute {
            for (i in 1..10) {
                log("sending response: $i")
                uplink?.respondToAction(
                    io.bytebeam.uplink.common.ActionResponse(
                        action.id,
                        i,
                        System.currentTimeMillis(),
                        if (i == 10) {
                            "done"
                        } else {
                            "processing"
                        },
                        i * 10,
                        arrayOf()
                    )
                )
                Thread.sleep(1000)
            }
        }
    }

    private fun log(line: String) {
        Log.e(TAG, line)
        logs.add(0, line)
        runOnUiThread {
            logView.text = logs.joinToString("\n")
        }
    }
}