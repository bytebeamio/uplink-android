package io.bytebeam.uplink.configurator

import android.app.ActivityManager
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import io.bytebeam.uplink.common.Constants.*
import org.json.JSONObject
import java.nio.file.Files
import java.nio.file.Paths
import java.util.concurrent.Executors

const val PICK_AUTH_CONFIG = 1
const val TAG = "MainActivity"
const val PREFS_SERVICE_RUNNING_KEY = "serviceState"

enum class ServiceState {
    STOPPED,
    RUNNING,
    WORKING,
}

class MainActivity : AppCompatActivity(), ServiceConnection {
    lateinit var statusView: TextView
    lateinit var selectBtn: Button
    lateinit var handler: Handler
    var messenger: Messenger? = null

    private lateinit var _serviceState: ServiceState
    var serviceState: ServiceState
        get() = _serviceState
        set(newState) {
            _serviceState = newState
            getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().let {
                it.putBoolean(PREFS_SERVICE_RUNNING_KEY, _serviceState == ServiceState.RUNNING)
                it.commit()
            }
            updateUI()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (ourArchitecture is UnknownArchitecture) {
            Toast.makeText(this, "This cpu architecture (${ourArchitecture.name}) is not supported", Toast.LENGTH_LONG).show()
            finish()
            return
        } else {
            val exePath = Paths.get(dataDir.absolutePath + "/exe")
            if (!Files.exists(exePath) || !Files.isExecutable(exePath)) {
                Log.i(TAG, "copying executable")
                assets.open("executables/${ourArchitecture.assetId}/uplink").use {
                    Files.copy(it, exePath)
                }
                Runtime.getRuntime().exec(arrayOf("chmod", "+x", exePath.toString())).let {
                    it.waitFor()
                    if (it.exitValue() != 0) {
                        Log.e(TAG, "executable chmod failed")
                        Files.delete(exePath)
                        Toast.makeText(this, "failed to setup uplink", Toast.LENGTH_LONG).show()
                        finish()
                        return
                    } else {
                        Log.i(TAG, "executable ready")
                    }
                }
            }
        }

        try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "cat", "/sys/kernel/boot_params/version"))
            process.waitFor()
            if (process.exitValue() != 0) {
                Log.e(TAG, "process failed, status: ${process.exitValue()}\noutput:\n${process.errorStream.bufferedReader().readText()}")
                throw Exception("Unable to read boot params")
            }
            val version = process.inputStream.bufferedReader().readLine()
            if (version == null) {
                Log.e(TAG, "file read failed")
                throw Exception("Unable to read boot params")
            }
            Log.i(TAG, "sysfs test passed")
        } catch (e: Exception) {
            Log.e(TAG, "unable to access sysfs", e)
            Toast.makeText(this, "This app requires root privileges", Toast.LENGTH_LONG).show()
            finish()
            return
        }
h
        findViewById<TextView>(R.id.version_view).text = BuildConfig.VERSION_NAME
        statusView = findViewById(R.id.status_view)
        selectBtn = findViewById(R.id.select_config_btn)

        applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).let {
            if (!it.contains(PREFS_SERVICE_SUDO_PASS_KEY)) {
                it.edit().putString(PREFS_SERVICE_SUDO_PASS_KEY, genPassKey()).commit()
            }
        }

        if (serviceRunning()) {
            serviceState = ServiceState.WORKING
            connectToService()
        } else {
            serviceState = ServiceState.STOPPED
        }

        selectBtn.setOnClickListener {
            when (serviceState) {
                ServiceState.RUNNING -> {
                    serviceState = ServiceState.WORKING
                    AlertDialog.Builder(this)
                        .setTitle("Stop Service")
                        .setMessage("The connected clients will have to reconnect")
                        .setPositiveButton(android.R.string.ok) { _, _ ->
                            Log.d(TAG, "stopping service")
                            sendStopCommand()
                        }
                        .setNegativeButton(android.R.string.cancel) { _, _ ->
                            serviceState = ServiceState.RUNNING
                        }
                        .setOnCancelListener {
                            serviceState = ServiceState.RUNNING
                        }
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .show()
                }
                ServiceState.STOPPED -> {
                    serviceState = ServiceState.WORKING
                    @Suppress("DEPRECATION")
                    startActivityForResult(
                        Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                            addCategory(Intent.CATEGORY_OPENABLE)
                            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                            type = "application/json"
                        },
                        PICK_AUTH_CONFIG
                    )

                }
                ServiceState.WORKING -> {}
            }
        }

        handler = Handler(Looper.getMainLooper())
    }

    private fun updateUI() {
        val prefs = applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val configName = prefs.getString(PREFS_AUTH_CONFIG_NAME_KEY, null)
        runOnUiThread {
            when (serviceState) {
                ServiceState.STOPPED -> {
                    statusView.text = "Service stopped"
                    selectBtn.text = "Select device config"
                    selectBtn.isEnabled = true
                    selectBtn.setBackgroundColor(0xFF0022CC.toInt())
                }
                ServiceState.RUNNING -> {
                    statusView.text = "Service running for $configName"
                    selectBtn.text = "Stop service"
                    selectBtn.isEnabled = true
                    selectBtn.setBackgroundColor(0xFFFF3300.toInt())
                }
                ServiceState.WORKING -> {
                    statusView.text = ""
                    selectBtn.text = "Working"
                    selectBtn.isEnabled = false
                    selectBtn.setBackgroundColor(0xFF332255.toInt())
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        @Suppress("DEPRECATION")
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            PICK_AUTH_CONFIG -> {
                if (resultCode == RESULT_OK) {
                    val uri = data?.data
                    if (uri != null) {
                        val inputStream = contentResolver.openInputStream(uri)
                        val jsonString = inputStream?.bufferedReader()?.use { it.readText() }
                        val configName = uri.lastPathSegment ?: uri.path ?: uri.toString()
                        if (jsonString == null) {
                            Toast.makeText(this, "Could not read file", Toast.LENGTH_LONG).show()
                            return
                        }
                        try {
                            JSONObject(jsonString)
                            // TODO: verify properties
                        } catch (e: Exception) {
                            Toast.makeText(this, "Invalid JSON", Toast.LENGTH_LONG).show()
                            return
                        }
                        applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().let {
                            it.putString(PREFS_AUTH_CONFIG_NAME_KEY, configName)
                            it.putString(PREFS_AUTH_CONFIG_KEY, jsonString)
                            it.commit()
                        }
                        startService(Intent().also {
                            it.component = ComponentName(CONFIGURATOR_APP_ID, UPLINK_SERVICE_ID)
                            it.putExtra(DATA_KEY, jsonString)
                        })
                        connectToService()
                    }
                } else {
                    serviceState = ServiceState.STOPPED
                }
            }
        }
    }

    private fun serviceRunning(): Boolean {
        val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        // On recent androids, ActivityManager.getRunningServices returns the list of running services that
        // belong to the current app, which is enough for this use case.
        @Suppress("DEPRECATION")
        for (info in am.getRunningServices(Int.MAX_VALUE)) {
            if (info.service.equals(ComponentName(CONFIGURATOR_APP_ID, UPLINK_SERVICE_ID))) {
                return true
            }
        }
        return false
    }

    private fun sendStopCommand() {
        val sudoPass = applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(PREFS_SERVICE_SUDO_PASS_KEY, "pass not found app")
        messenger!!.send(
            Message().also {
                it.what = STOP_SERVICE
                it.data = Bundle().also {
                    it.putString(DATA_KEY, sudoPass)
                }
            }
        )
        Executors.newSingleThreadExecutor().execute {
            while (serviceRunning()) {
                Thread.sleep(1000)
            }

            runOnUiThread {
                applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().also {
                    it.remove(PREFS_AUTH_CONFIG_NAME_KEY)
                    it.remove(PREFS_AUTH_CONFIG_KEY)
                    it.commit()
                }

                serviceState = ServiceState.STOPPED
                messenger = null
            }
        }
    }

    private fun connectToService() {
        Executors.newSingleThreadExecutor().execute {
            while (!serviceRunning()) {
                Thread.sleep(1000)
            }
            Intent().also {
                it.component = ComponentName(CONFIGURATOR_APP_ID, UPLINK_SERVICE_ID)
                bindService(
                    it,
                    this,
                    Context.BIND_NOT_FOREGROUND
                )
            }
        }
    }

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        messenger = Messenger(service)
        serviceState = ServiceState.RUNNING
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