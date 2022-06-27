package io.bytebeam.UplinkDemo

import android.content.res.Resources
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.annotation.RawRes
import androidx.appcompat.app.AppCompatActivity
import io.bytebeam.uplink.service.ActionSubscriber
import io.bytebeam.uplink.ServiceReadyCallback
import io.bytebeam.uplink.Uplink
import io.bytebeam.uplink.service.UplinkTerminatedException
import io.bytebeam.uplink.types.ActionResponse
import io.bytebeam.uplink.types.UplinkAction
import java.util.concurrent.Executors

fun Resources.getRawTextFile(@RawRes id: Int) =
    openRawResource(id).bufferedReader().use { it.readText() }

const val TAG = "==APP=="

class MainActivity : AppCompatActivity(), ActionSubscriber, ServiceReadyCallback {
    var uplink: Uplink? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.start_btn).setOnClickListener {
            try {
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
            } catch (e: UplinkTerminatedException) {
                Log.e(TAG, "$e")
            }
        }

        findViewById<Button>(R.id.stop_btn).setOnClickListener {
            try {
                uplink?.dispose()
                uplink = null
            } catch (e: UplinkTerminatedException) {
                Log.e(TAG, "$e")
            }
        }
    }

    override fun processAction(action: UplinkAction) {
        Log.e(TAG, "received action: $action")
        val executor = Executors.newSingleThreadExecutor()
        executor.execute {
            for (i in 1..10) {
                Log.e(TAG, "sending response: $i")
                uplink?.respondToAction(
                    ActionResponse(
                        action.id,
                        i + 1,
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

    override fun uplinkReady() {
        uplink?.subscribe(this);
    }

}