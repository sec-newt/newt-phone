package io.github.secnewt.dialer.screening

import android.content.ContentValues
import android.content.Context
import android.provider.BlockedNumberContract
import android.provider.BlockedNumberContract.BlockedNumbers

/**
 * Android's own block list. Only the default phone app (or texting app) may read or change it.
 * Numbers on it are turned away before they ring, and texts from them are blocked too.
 */
class SystemBlockList(private val context: Context) {

    fun isAvailable(): Boolean = try {
        BlockedNumberContract.canCurrentUserBlockNumbers(context)
    } catch (e: Exception) {
        false
    }

    fun numbers(): List<String> = try {
        context.contentResolver.query(
            BlockedNumbers.CONTENT_URI, arrayOf(BlockedNumbers.COLUMN_ORIGINAL_NUMBER), null, null, null,
        )?.use { cursor ->
            buildList { while (cursor.moveToNext()) cursor.getString(0)?.let(::add) }
        }.orEmpty()
    } catch (e: Exception) {
        // Not the phone app (any more): nothing to read.
        emptyList()
    }

    fun add(number: String) {
        try {
            val values = ContentValues().apply { put(BlockedNumbers.COLUMN_ORIGINAL_NUMBER, number) }
            context.contentResolver.insert(BlockedNumbers.CONTENT_URI, values)
        } catch (e: Exception) {
            // Stays on this app's list, which still blocks it while screening.
        }
    }

    fun remove(number: String) {
        try {
            BlockedNumberContract.unblock(context, number)
        } catch (e: Exception) {
            // Nothing to undo.
        }
    }
}
