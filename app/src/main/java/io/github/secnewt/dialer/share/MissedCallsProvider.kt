package io.github.secnewt.dialer.share

import android.Manifest
import android.content.ContentProvider
import android.content.ContentValues
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.CallLog.Calls

/**
 * Shares the number of unseen missed calls with other apps signed with the Newt key (like
 * Newt Home, which shows "2 missed" under Phone). Only the count: no numbers, names or times.
 * Read-only, and guarded by a signature permission, so no other app can read it.
 */
class MissedCallsProvider : ContentProvider() {

    override fun onCreate(): Boolean {
        val context = context ?: return false
        // Tell readers when the call history changes, while this process is running.
        try {
            context.contentResolver.registerContentObserver(
                Calls.CONTENT_URI,
                true,
                object : ContentObserver(Handler(Looper.getMainLooper())) {
                    override fun onChange(selfChange: Boolean) {
                        context.contentResolver.notifyChange(countUri(context.packageName), null)
                    }
                },
            )
        } catch (e: SecurityException) {
            // No call log access yet; readers still ask again when they come back.
        }
        return true
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor? {
        if (!MissedCallCount.isCountPath(uri.path)) return null
        val cursor = MatrixCursor(arrayOf(MissedCallCount.COLUMN))
        cursor.addRow(arrayOf(MissedCallCount.clamp(unseenMissed())))
        return cursor
    }

    private fun unseenMissed(): Int {
        val context = context ?: return 0
        if (context.checkSelfPermission(Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) return 0
        return try {
            context.contentResolver.query(
                Calls.CONTENT_URI,
                arrayOf(Calls._ID),
                "${Calls.TYPE} = ? AND ${Calls.NEW} = 1",
                arrayOf(Calls.MISSED_TYPE.toString()),
                null,
            )?.use { it.count } ?: 0
        } catch (e: RuntimeException) {
            0
        }
    }

    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0

    companion object {
        fun countUri(packageName: String): Uri = Uri.parse("content://$packageName.missedcalls/count")
    }
}
