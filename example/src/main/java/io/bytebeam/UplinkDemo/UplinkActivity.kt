package io.bytebeam.UplinkDemo

import android.app.Activity
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
    val mainThread = Handler(Looper.getMainLooper())
    lateinit var logView: TextView

    lateinit var uplinkInterface: UplinkInterface;
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_uplink)

        logView = findViewById(R.id.logView)

        uplinkInterface = UplinkInterface(this)
        connectToUplink()
        Executors.newSingleThreadExecutor().execute {
            var idx = 1
            while (true) {
                try {
                    uplinkInterface.sendData(
                        UplinkPayload(
                            "test_stream_12",
                            idx++,
                            System.currentTimeMillis(),
                            JSONObject().apply {
                                put("a", idx % 2)
                                put("b", idx % 3)
                                put("c", idx % 4)
                            }
                        ).also {
                            log("Pushing: ${it.payload}")
                        }
                    )
                } catch (e: IOException) {
                    log("connection closed, stopping battery status thread")
                    break
                }
                Thread.sleep(1000)
            }
        }
    }

    override fun onDestroy() {
        uplinkInterface.dispose()
        super.onDestroy()
    }

    override fun processAction(action: UplinkAction) {
        log("Received action: $action")
        Executors.newSingleThreadExecutor().execute {
            for (i in 1..10) {
                log("sending response: $i")
                try {
                    uplinkInterface.sendData(
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
                        ).toPayload()
                    )
                } catch (e: IOException) {
                    log("connection closed, aborting action responses")
                    break
                }
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

    private fun connectToUplink() {
        if (!uplinkInterface.connected()) {
            log("disconnected, trying connection...")
            if (uplinkInterface.connect(8031)) {
                log("connected to uplink.")
            }
        }
        mainThread.postDelayed(this::connectToUplink, 1000)
    }
}

val TAG = "UplinkActivity"

class UplinkInterface(
    private val context: UplinkActivity,
) : ActionSubscriber {
    private var uplink: Uplink? = null

    fun connect(port: Int): Boolean {
        if (uplink != null) {
            dispose()
        }
        return try {
            uplink = Uplink(
                ConnectionConfig().withHost("localhost").withPort(port), this
            )
            true
        } catch (e: IOException) {
            false
        }
    }

    fun dispose() {
        Log.e(TAG, "disposing uplink")
        uplink?.dispose()
        uplink = null
    }

    fun sendData(payload: UplinkPayload) {
        try {
            uplink?.sendData(payload)
        } catch (e: Throwable) {
            Log.w(TAG, "Uplink.sendData error: ", e)
        }
    }

    fun connected(): Boolean {
        return uplink?.connected() == true
    }

    override fun processAction(action: UplinkAction) {
        context.processAction(action)
    }
}
