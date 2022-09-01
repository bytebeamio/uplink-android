package io.bytebeam.uplink.configurator

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.widget.*
import androidx.appcompat.app.AlertDialog
import java.io.File
import java.util.*

const val FILE_NAME_KEY = "FILE_NAME"
const val FILE_CONTENT_KEY = "FILE_CONTENT"

class FilePicker : AppCompatActivity() {
    lateinit var upBtn: Button
    lateinit var filesListView: ListView
    val storageInfos = StorageUtils.getStorageList()

    lateinit var _currDir: String

    var currDir: String
        get() {
            return _currDir
        }
        set(newPath) {
            _currDir = newPath
            val entries = genEntries(_currDir)
            val adapter = ArrayAdapter(this, R.layout.file_list_entry, entries)
            filesListView.adapter = adapter
            filesListView.setOnItemClickListener { parent, view, position, id ->
                val entry = (view as TextView).text.toString()
                if (entry.endsWith("/")) {
                    currDir = File(currDir, entry).absolutePath
                } else {
                    try {
                        val content = File(_currDir, entry).readText()
                        setResult(RESULT_OK, Intent().also {
                            it.putExtra(FILE_NAME_KEY, entry)
                            it.putExtra(FILE_CONTENT_KEY, content)
                        })
                        finish()
                    } catch (e: Exception) {
                        showToast("couldn't read this file")
                    }
                }
            }
            adapter.notifyDataSetChanged()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_file_picker)

        upBtn = findViewById(R.id.go_up)
        upBtn.setOnClickListener {
            val parent = File(currDir).parent
            if (parent == null) {
                showToast("cannot go to parent")
            } else {
                if (File(parent).canRead()) {
                    currDir = parent
                } else {
                    showToast("parent directory not accessible")
                }
            }
        }
        filesListView = findViewById(R.id.files_list)

        findViewById<Button>(R.id.select_storage_btn).setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Select a storage device")
                .setAdapter(
                    ArrayAdapter(this, R.layout.file_list_entry, storageInfos.map { it.displayName })
                ) { _, which ->
                    currDir = storageInfos[which].path
                }
                .create()
                .show()
        }

        currDir = Environment.getExternalStorageDirectory().absolutePath
    }
}

fun genEntries(path: String) : List<String> {
    val children = try {
        File(path).listFiles() ?: return listOf()
    } catch (e: Exception) {
        return listOf()
    }
    return children.filter {
        if (it.isFile) {
            it.name.lowercase(Locale.getDefault()).endsWith(".json") && it.canRead()
        } else if (it.isDirectory) {
            true
        } else {
            false
        }
    }.map {
        if (it.isDirectory) {
            it.name + "/"
        } else {
            it.name
        }
    }
}