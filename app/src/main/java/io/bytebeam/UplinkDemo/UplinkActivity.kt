package io.bytebeam.UplinkDemo

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import io.bytebeam.uplink.UplinkReadyCallback
import io.bytebeam.uplink.Uplink
import io.bytebeam.uplink.service.ActionSubscriber
import io.bytebeam.uplink.types.ActionResponse
import io.bytebeam.uplink.types.UplinkAction
import io.bytebeam.uplink.types.UplinkPayload
import java.util.concurrent.Executors

class UplinkActivity : AppCompatActivity(), UplinkReadyCallback, ActionSubscriber {
    val logs = mutableListOf<String>()
    lateinit var logView: TextView

    var uplink: Uplink? = null;
    var dataIndex = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_uplink)

        logView = findViewById(R.id.logView)
        findViewById<Button>(R.id.sendBtn).setOnClickListener {
            uplink?.sendData(
                UplinkPayload(
                    "square_stream",
                    dataIndex++,
                    System.currentTimeMillis(),
                    (dataIndex * dataIndex).toString()
                )
            )
        }
        log("<<END>>")
    }

    override fun onStart() {
        super.onStart()
        uplink = Uplink(
            this,
            resources.getRawTextFile(R.raw.local_device),
            """
                [persistence]
                path = "${applicationInfo.dataDir}/uplink"
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
    }

    override fun processAction(action: UplinkAction) {
        log("Recieved action: $action")
        Executors.newSingleThreadExecutor().execute {
            for (i in 1..10) {
                log("sending response: $i")
                uplink?.respondToAction(
                    ActionResponse(
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