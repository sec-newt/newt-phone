package io.github.secnewt.dialer.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.widget.Toast

/** Puts a number on the clipboard. Android 13 and later show their own "Copied" message. */
fun copyNumber(context: Context, number: String) {
    context.getSystemService(ClipboardManager::class.java)
        .setPrimaryClip(ClipData.newPlainText("Phone number", number))
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        Toast.makeText(context, "Number copied", Toast.LENGTH_SHORT).show()
    }
}

/** The text on the clipboard, or null when there isn't any. */
fun clipboardText(context: Context): String? {
    val clip = context.getSystemService(ClipboardManager::class.java).primaryClip
    return clip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.coerceToText(context)?.toString()
}
