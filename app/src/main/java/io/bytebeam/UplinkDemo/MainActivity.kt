package io.bytebeam.UplinkDemo

import android.content.Intent
import android.content.res.Resources
import android.os.Bundle
import android.widget.Button
import androidx.annotation.RawRes
import androidx.appcompat.app.AppCompatActivity

fun Resources.getRawTextFile(@RawRes id: Int) =
    openRawResource(id).bufferedReader().use { it.readText() }

const val TAG = "==APP=="

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.start_btn).setOnClickListener {
            Intent(this, UplinkActivity::class.java).also {
                startActivity(it)
            }
        }
    }
}