package io.bytebeam.uplink.configurator

import android.app.ActivityManager
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.os.Message
import android.os.Messenger
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import io.bytebeam.uplink.common.Constants.*
import org.json.JSONObject

const val PICK_AUTH_CONFIG = 1
const val TAG = "MainActivity"
const val PREFS_SERVICE_RUNNING_KEY = "serviceState"

enum class ServiceState {
    STOPPING,
    STOPPED,
    STARTING,
    STARTED,
}

class MainActivity : AppCompatActivity(), ServiceConnection {
    lateinit var statusView: TextView
    lateinit var selectBtn: Button

    private lateinit var _serviceState: ServiceState
    var serviceState: ServiceState
        get() = _serviceState
        set(newState) {
            _serviceState = newState
            getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().let {
                it.putBoolean(PREFS_SERVICE_RUNNING_KEY, _serviceState == ServiceState.STARTED)
                it.apply()
            }
            updateUI()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusView = findViewById(R.id.status_view)
        selectBtn = findViewById(R.id.select_config_btn)

        serviceState = if (serviceRunning()) {
            ServiceState.STARTED
        } else {
            ServiceState.STOPPED
        }

        applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).let {
            if (!it.contains(PREFS_SERVICE_SUDO_PASS_KEY)) {
                it.edit().putString(PREFS_SERVICE_SUDO_PASS_KEY, genPassKey()).apply()
            }
        }

        selectBtn.setOnClickListener {
            if (serviceRunning()) {
                serviceState = ServiceState.STOPPING
                AlertDialog.Builder(this)
                    .setTitle("Remove device config")
                    .setMessage("This operation will restart the uplink service, the connected clients will have to reconnect")
                    .setPositiveButton(android.R.string.ok) { dialog, which ->
                        applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().also {
                            it.remove(PREFS_AUTH_CONFIG_NAME_KEY)
                            it.remove(PREFS_AUTH_CONFIG_KEY)
                            it.apply()
                        }

                        Log.d(TAG, "stopping service")
                        Intent().also {
                            it.component = ComponentName(CONFIGURATOR_APP_ID, UPLINK_SERVICE_ID)
                            bindService(
                                it,
                                this,
                                Context.BIND_NOT_FOREGROUND
                            )
                        }
                    }
                    .setNegativeButton(android.R.string.cancel) { _, _ ->
                        serviceState = ServiceState.STARTED
                    }
                    .setOnDismissListener {
                        when (serviceState) {
                            ServiceState.STARTED -> {}
                            ServiceState.STOPPED -> {}
                            ServiceState.STOPPING -> {
                                serviceState = ServiceState.STARTED
                            }
                            ServiceState.STARTING -> throw IllegalStateException()
                        }
                    }
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show()
            } else {
                serviceState = ServiceState.STARTING
                startActivityForResult(
                    Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                        addCategory(Intent.CATEGORY_OPENABLE)
                        type = "application/json"
                    },
                    PICK_AUTH_CONFIG
                )
            }
        }
    }

    private fun updateUI() {
        val prefs = applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val configName = prefs.getString(PREFS_AUTH_CONFIG_NAME_KEY, null)
        runOnUiThread {
            when (serviceState) {
                ServiceState.STOPPING -> {
                    statusView.text = "Stopping service"
                    selectBtn.isEnabled = false
                }
                ServiceState.STOPPED -> {
                    statusView.text = "Service stopped"
                    selectBtn.text = "Select device config"
                    selectBtn.isEnabled = true
                    selectBtn.setBackgroundColor(0xFF0022CC.toInt())
                }
                ServiceState.STARTING -> {
                    statusView.text = "Starting service"
                    selectBtn.isEnabled = false
                }
                ServiceState.STARTED -> {
                    statusView.text = "Service running for $configName"
                    selectBtn.text = "Stop service"
                    selectBtn.isEnabled = true
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
                        applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().let {
                            it.putString(PREFS_AUTH_CONFIG_NAME_KEY, configName)
                            it.putString(PREFS_AUTH_CONFIG_KEY, jsonString)
                            it.apply()
                        }
                        startService(Intent().also {
                            it.component = ComponentName(CONFIGURATOR_APP_ID, UPLINK_SERVICE_ID)
                        })
                        serviceState = ServiceState.STARTED
                    }
                } else {
                    serviceState = ServiceState.STOPPED
                }
            }
        }
    }

    fun serviceRunning(): Boolean {
        val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (info in am.getRunningServices(Int.MAX_VALUE)) {
            if (info.service.equals(ComponentName(CONFIGURATOR_APP_ID, UPLINK_SERVICE_ID))) {
                return true
            }
        }
        return false
    }

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        val sudoPass = applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(PREFS_SERVICE_SUDO_PASS_KEY, "")
        val messenger = Messenger(service)
        messenger.send(
            Message().also {
                it.what = STOP_SERVICE
                it.data = Bundle().also {
                    it.putString(DATA_KEY, sudoPass)
                }
            }
        )
        serviceState = ServiceState.STOPPED
        unbindService(this)
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        Log.d(TAG, "onServiceDisconnected")
        unbindService(this)
    }

    override fun onBindingDied(name: ComponentName?) {
        Log.d(TAG, "onBindingDied")
        unbindService(this)
    }

    override fun onNullBinding(name: ComponentName?) {
        unbindService(this)
    }
}