package io.bytebeam.uplink.configurator

import android.Manifest
import android.app.ActivityManager
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.jaiselrahman.filepicker.activity.FilePickerActivity
import com.jaiselrahman.filepicker.model.MediaFile
import io.bytebeam.uplink.common.Constants.*
import org.json.JSONObject
import java.util.concurrent.Executors

const val PICK_AUTH_CONFIG = 1
const val ALLOW_MANAGE_STORAGE = 2
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

        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 1)

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
                        Intent(this, FilePicker::class.java),
                        PICK_AUTH_CONFIG
                    )

                }
                ServiceState.WORKING -> {}
            }
        }

        handler = Handler(Looper.getMainLooper())
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            1 -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "storage permission granted")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        if (!Environment.isExternalStorageManager()) {
                            Log.d(TAG, "asking for storage manager permission")
                            Toast.makeText(this, "please grant storage management permissions", Toast.LENGTH_LONG)
                                .show()
                            startActivityForResult(
                                Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION),
                                ALLOW_MANAGE_STORAGE,
                            )
                        }
                    }
                } else {
                    Log.d(TAG, "storage permission denied")
                    showToast("Storage permission required")
                    finish()
                }
            }
        }
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
                try {
                    if (resultCode == RESULT_OK) {
                        val files = data?.getParcelableArrayListExtra<MediaFile>(FilePickerActivity.MEDIA_FILES)
                        if (files == null || files.isEmpty()) {
                            throw Exception("no file selected")
                        }
                        if (files[0].mimeType != "application/json") {
                            throw Exception("file type ${files[0].mimeType} is invalid")
                        }
                        val uri = files[0].uri
                        if (uri != null) {
                            val inputStream = contentResolver.openInputStream(uri)
                            val configName = uri.lastPathSegment ?: uri.path ?: uri.toString()
                            val jsonString = try {
                                inputStream?.bufferedReader()?.use { it.readText() }
                            } catch (e: Exception) {
                                throw Exception("Could not read file")
                            } ?: throw Exception("Could not read file")
                            try {
                                JSONObject(jsonString)
                                // TODO: verify properties
                            } catch (e: Exception) {
                                throw Exception("Invalid JSON")
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
                        throw Exception("no file selected")
                    }
                } catch (e: Exception) {
                    showToast(e.message)
                    serviceState = ServiceState.STOPPED
                }
            }
            ALLOW_MANAGE_STORAGE -> {
                if (!Environment.isExternalStorageManager()) {
                    showToast("Need external storage permissions")
                    finish()
                } else {
                    Log.d(TAG, "storage manager permission granted")
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