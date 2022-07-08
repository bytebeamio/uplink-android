package io.bytebeam.uplink.configurator

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import io.bytebeam.uplink.common.Constants.PREFS_NAME

class ServiceStateProvider : ContentProvider() {
    override fun onCreate(): Boolean {
        return true
    }

    /**
     * Service state is a boolean flag, we use null as false value here
     */
    override fun query(
        uri: Uri,
        projection: Array<String>?,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?
    ): Cursor? {
        val running = context?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).let {
            it?.getBoolean(PREFS_SERVICE_RUNNING_KEY, false)
        } ?: false
        if (running) {
            return MatrixCursor(arrayOf(), 0)
        } else {
            return null
        }
    }

    override fun getType(uri: Uri): String? {
        return null
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        return null
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int {
        return 0
    }

    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<String>?): Int {
        return 0
    }
}