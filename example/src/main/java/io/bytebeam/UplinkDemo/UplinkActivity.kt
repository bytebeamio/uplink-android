package io.bytebeam.UplinkDemo

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import io.bytebeam.uplink.ConnectionConfig
import io.bytebeam.uplink.Uplink
import io.bytebeam.uplink.common.ActionSubscriber
import io.bytebeam.uplink.common.ActionResponse
import io.bytebeam.uplink.common.UplinkAction
import io.bytebeam.uplink.common.UplinkPayload
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.Executors

class UplinkActivity : AppCompatActivity(), ActionSubscriber {
    val logs = ArrayDeque<String>()
    lateinit var logView: TextView

    lateinit var uplink: Uplink;
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_uplink)

        logView = findViewById(R.id.logView)

        try {
            uplink = Uplink(
                ConnectionConfig()
                    .withHost("192.168.1.6")
                    .withPort(5555),
                this
            )
        } catch (e: IOException) {
            log("uplink refused to connect")
            return
        }
        Executors.newSingleThreadExecutor().execute {
            var idx = 1
            var count = 0
            var last = System.currentTimeMillis()
            while (uplink.connected()) {
                try {
                    uplink.sendData(
                        UplinkPayload(
                            "device_shadow",
                            idx++,
                            System.currentTimeMillis(),
                            JSONObject().apply {
                                put("a", true)
                                put("b", true)
                                put("c", true)
                            }
                        )
                    )
                } catch (e: IOException) {
                    log("connection closed, stopping battery status thread")
                    break
                }
                val now = System.currentTimeMillis()
                count++
                if (now - last > 1000) {
                    log("$idx: $count messages/s")
                    last = now
                    count = 0
                }
            }
        }
    }

    override fun onDestroy() {
        uplink.dispose()
        super.onDestroy()
    }

    override fun processAction(action: UplinkAction) {
        log("Received action: $action")
        Executors.newSingleThreadExecutor().execute {
            for (i in 1..10) {
                log("sending response: $i")
                try {
                    uplink.respondToAction(
                        ActionResponse(
                            action.action_id,
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
                } catch (e: IOException) {
                    log("connection closed, aborting action responses")
                    break
                }
                Thread.sleep(100)
            }
        }
    }

    private fun log(line: String) {
        Log.d("UPLINK_EXAMPLE", line)
        runOnUiThread {
            logs.addFirst(line)
            if (logs.size > 250) {
                logs.removeLast()
            }
            logView.text = logs.joinToString("\n")
        }
    }
}