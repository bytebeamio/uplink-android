package io.bytebeam.UplinkDemo

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import io.bytebeam.uplink.Uplink

const val TAG = "==APP=="

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<TextView>(R.id.versionView).text = BuildConfig.VERSION_NAME
        findViewById<Button>(R.id.start_btn).setOnClickListener {
            if (!Uplink.configuratorAvailable(this)) {
                Toast.makeText(this, "configurator app is not installed on this device", Toast.LENGTH_LONG).show()
            } else if (!Uplink.serviceRunning(this)) {
                Toast.makeText(this, "You need to start the uplink service using the configurator app", Toast.LENGTH_LONG).show()
            } else {
                Intent(this, UplinkActivity::class.java).also {
                    startActivity(it)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        findViewById<TextView>(R.id.statusView).text = "configuratorAvailable: ${Uplink.configuratorAvailable(this)}\nserviceRunning: ${Uplink.serviceRunning(this)}";
    }
}