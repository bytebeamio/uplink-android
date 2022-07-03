package io.bytebeam.uplink.configurator

import android.app.ActivityManager
import android.app.AlertDialog
import android.content.*
import android.os.Bundle
import android.os.IBinder
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import io.bytebeam.uplink.common.Constants
import io.bytebeam.uplink.service.UplinkService
import org.json.JSONObject

const val PICK_AUTH_CONFIG = 1
const val TAG = "MainActivity"

class MainActivity : AppCompatActivity() {
    lateinit var statusView: TextView
    lateinit var selectBtn: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusView = findViewById(R.id.status_view)
        selectBtn = findViewById(R.id.select_config_btn)
        selectBtn.setOnClickListener {
            val prefs = getSharedPreferences(UplinkService.PREFS_NAME, Context.MODE_PRIVATE)
            if (prefs.contains(UplinkService.PREFS_AUTH_CONFIG_NAME_KEY)) {
                AlertDialog.Builder(this)
                    .setTitle("Remove device config")
                    .setMessage("This operation will restart the uplink service, the connected clients will have to reconnect") // Specifying a listener allows you to take an action before dismissing the dialog.
                    .setPositiveButton(android.R.string.ok) { dialog, which ->
                        prefs.edit().also {
                            it.remove(UplinkService.PREFS_AUTH_CONFIG_NAME_KEY)
                            it.remove(UplinkService.PREFS_AUTH_CONFIG_KEY)
                            it.apply()
                        }

                        if (serviceRunning()) {
                            Intent().also {
                                it.component = ComponentName(Constants.CONFIGURATOR_APP_ID, Constants.UPLINK_SERVICE_ID)
                                it.putExtra(UplinkService.KILL_COMMAND_TAG, true)
                                bindService(
                                    it,
                                    object : ServiceConnection {
                                        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {}
                                        override fun onServiceDisconnected(name: ComponentName?) {}
                                    },
                                    0
                                )
                            }
                        }

                        updateUI()
                    }
                    .setNegativeButton(android.R.string.cancel, null)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show()
            } else {
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "application/json"
                }

                startActivityForResult(intent, PICK_AUTH_CONFIG)
            }
        }

        updateUI()
    }

    private fun updateUI() {
        val prefs = getSharedPreferences(UplinkService.PREFS_NAME, Context.MODE_PRIVATE)
        val configName = prefs.getString(UplinkService.PREFS_AUTH_CONFIG_NAME_KEY, null)
        runOnUiThread {
            when (configName) {
                null -> {
                    statusView.text = "No device config selected"
                    selectBtn.text = "Select device config"
                    selectBtn.setBackgroundColor(0xFFCCCCCC.toInt())
                }
                else -> {
                    statusView.text = "Service ready for $configName"
                    selectBtn.text = "Remove device config"
                    selectBtn.setBackgroundColor(0xFFFF3300.toInt())
                }
            }
        }
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
                        val configName = uri.lastPathSegment ?: "device.json"
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
                            it.putString(UplinkService.PREFS_AUTH_CONFIG_NAME_KEY, configName)
                            it.putString(UplinkService.PREFS_AUTH_CONFIG_KEY, jsonString)
                            it.apply()
                        }
                        updateUI()
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