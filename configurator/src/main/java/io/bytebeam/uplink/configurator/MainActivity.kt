package io.bytebeam.uplink.configurator

import android.app.ActivityManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.ShareActionProvider
import android.widget.TextView
import android.widget.Toast
import io.bytebeam.uplink.common.Constants
import io.bytebeam.uplink.configurator.R
import io.bytebeam.uplink.service.UplinkService
import org.json.JSONObject
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

const val PICK_AUTH_CONFIG = 1

class MainActivity : AppCompatActivity() {
    lateinit var statusView: TextView
    lateinit var statusUpdater: ExecutorService
    lateinit var selectBtn: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusView = findViewById(R.id.status_view)
        statusUpdater = Executors.newSingleThreadExecutor().also {
            it.execute {
                while (true) {
                    var status = "processing"
                    if (!serviceRunning()) {
                        status = "0 clients connected"
                    } else {
                        val count = getSharedPreferences(UplinkService.PREFS_NAME, Context.MODE_PRIVATE).getInt(UplinkService.PREFS_CLIENTS_COUNT_KEY, 0)
                        status = "$count client${if (count == 1) { "" } else { "s" }} connected"
                    }
                    runOnUiThread {
                        statusView.text = status
                    }
                    Thread.sleep(3000)
                }
            }
        }
        selectBtn = findViewById(R.id.select_config_btn)
        selectBtn.setOnClickListener {
            selectFile()
        }
    }

    override fun onDestroy() {
        statusUpdater.shutdownNow()
        super.onDestroy()
    }

    fun selectFile() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/json"
        }

        startActivityForResult(intent, PICK_AUTH_CONFIG)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            PICK_AUTH_CONFIG -> {
                if (resultCode == RESULT_OK) {
                    val uri = data?.data
                    if (uri != null) {
                        val inputStream = contentResolver.openInputStream(uri)
                        val jsonString = inputStream?.bufferedReader()?.use { it.readText() }
                        if (jsonString == null) {
                            Toast.makeText(this, "Could not read file", Toast.LENGTH_SHORT).show()
                            return
                        }
                        try {
                            val json = JSONObject(jsonString)
                            // TODO: verify properties
                        } catch (e: Exception) {
                            Toast.makeText(this, "Invalid JSON", Toast.LENGTH_SHORT).show()
                            return
                        }
                        getSharedPreferences(UplinkService.PREFS_NAME, Context.MODE_PRIVATE).edit().let {
                            it.putString(UplinkService.PREFS_AUTH_CONFIG_NAME_KEY, uri.toString())
                            it.putString(UplinkService.PREFS_AUTH_CONFIG_KEY, jsonString)
                            it.apply()
                        }
                        runOnUiThread {
                            selectBtn.text = uri.toString()
                            statusView.text = "restarting service"
                        }
                    }
                }
            }
        }
    }

    fun serviceRunning(): Boolean {
        val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (info in am.getRunningServices(Int.MAX_VALUE)) {
            if (info.service.equals(ComponentName(Constants.CONFIGURATOR_APP_ID, Constants.UPLINK_SERVICE_ID))) {
                return true
            }
        }
        return false
    }
}