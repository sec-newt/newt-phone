package io.github.secnewt.dialer.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import io.github.secnewt.dialer.contacts.Contact
import io.github.secnewt.dialer.contacts.ContactList
import io.github.secnewt.dialer.ui.theme.DialerFonts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Loads contact photos. Screens work without one (tests, previews): they show initials. */
fun interface PhotoLoader {
    suspend fun load(uri: String, maxSizePx: Int): ImageBitmap?
}

val LocalPhotoLoader = staticCompositionLocalOf<PhotoLoader?> { null }

/** Reads photos from the phone's contacts, shrunk to the size shown and kept in a small cache. */
class ContactsPhotoLoader(private val context: Context) : PhotoLoader {

    private val cache = object : LruCache<String, Bitmap>(CACHE_BYTES) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount
    }

    override suspend fun load(uri: String, maxSizePx: Int): ImageBitmap? {
        val key = "$uri@$maxSizePx"
        cache.get(key)?.let { return it.asImageBitmap() }
        val bitmap = withContext(Dispatchers.IO) { decode(Uri.parse(uri), maxSizePx) } ?: return null
        cache.put(key, bitmap)
        return bitmap.asImageBitmap()
    }

    private fun decode(uri: Uri, maxSizePx: Int): Bitmap? = try {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= maxSizePx && bounds.outHeight / (sample * 2) >= maxSizePx) {
            sample *= 2
        }
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
    } catch (e: Exception) {
        null
    }

    private companion object {
        const val CACHE_BYTES = 16 * 1024 * 1024
    }
}

/** Tokyo Night accents for people without a photo. Dark text on each stays above 7:1. */
private val AvatarColors = listOf(
    Color(0xFF7AA2F7), Color(0xFFBB9AF7), Color(0xFF7DCFFF), Color(0xFF9ECE6A),
    Color(0xFFFF9E64), Color(0xFFF7768E), Color(0xFFE0AF68), Color(0xFF73DACA),
)
private val AvatarText = Color(0xFF0B0C10)

/** The color that stands for this contact: behind their initial, and around their tile. */
fun contactColor(name: String): Color = AvatarColors[ContactList.colorIndex(name, AvatarColors.size)]

/**
 * The contact's photo, or their initial on their own color. Decorative for screen readers,
 * because the name is always shown next to it.
 */
@Composable
fun ContactAvatar(
    contact: Contact,
    sizePx: Int,
    initialStyle: TextStyle,
    modifier: Modifier = Modifier,
    shape: Shape = RectangleShape,
    fullPhoto: Boolean = false,
) {
    val uri = if (fullPhoto) contact.photoUri ?: contact.thumbnailUri else contact.thumbnailUri ?: contact.photoUri
    val loader = LocalPhotoLoader.current
    val photo by produceState<ImageBitmap?>(null, uri, loader, sizePx) {
        value = if (uri != null && loader != null) loader.load(uri, sizePx) else null
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(shape)
            .background(contactColor(contact.name))
            .clearAndSetSemantics { },
    ) {
        val current = photo
        if (current != null) {
            Image(current, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        } else {
            Text(
                text = ContactList.initial(contact.name),
                style = initialStyle.copy(fontFamily = DialerFonts.Display, fontWeight = FontWeight.Bold),
                color = AvatarText,
            )
        }
    }
}
