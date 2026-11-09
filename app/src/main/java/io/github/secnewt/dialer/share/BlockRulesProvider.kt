package io.github.secnewt.dialer.share

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import io.github.secnewt.dialer.screening.BlockRule
import io.github.secnewt.dialer.screening.SpamSettingsStore

/**
 * Shares my "starting with" block rules with other apps signed with the Newt key (Newt
 * Messages silences texts from those numbers too). Exact numbers aren't here: they're already on
 * Android's own block list, which every app shares. Read-only, and guarded by a signature
 * permission, so no other app can read it.
 */
class BlockRulesProvider : ContentProvider() {

    override fun onCreate(): Boolean = true

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor? {
        if (uri.path != PATH) return null
        val context = context ?: return null
        return MatrixCursor(arrayOf(COLUMN_STARTS_WITH)).apply {
            startsWith(SpamSettingsStore(context).blockRules()).forEach { addRow(arrayOf(it)) }
        }
    }

    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0

    companion object {
        const val PATH = "/startswith"
        const val COLUMN_STARTS_WITH = "starts_with"

        /** Just the "starting with" digits, the part only this app knows. */
        fun startsWith(rules: List<BlockRule>): List<String> =
            rules.filterIsInstance<BlockRule.StartsWith>().map { it.digits }.filter { it.isNotBlank() }.distinct()
    }
}
