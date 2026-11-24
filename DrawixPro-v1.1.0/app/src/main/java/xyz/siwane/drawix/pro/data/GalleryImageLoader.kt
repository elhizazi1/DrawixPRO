package xyz.siwane.drawix.pro.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri

/** The viewer and cropped previews have distinct pixel requirements; both read the stored file. */
class GalleryImageLoader(private val context: Context) {
    fun decode(uri: Uri, targetEdge: Int = 4096): Bitmap? = decodeWith(uri) { width, height, limit ->
        ImageDecodePolicy.sample(width, height, targetEdge, limit)
    }
    fun decodePreview(uri: Uri, widthPixels: Int, heightPixels: Int): Bitmap? = decodeWith(uri) { width, height, limit ->
        ImageDecodePolicy.previewSample(width, height, widthPixels, heightPixels, limit)
    }
    private fun decodeWith(uri: Uri, sample: (Int, Int, Long) -> Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        val options = BitmapFactory.Options().apply {
            inPreferredConfig = Bitmap.Config.ARGB_8888
            inSampleSize = sample(bounds.outWidth, bounds.outHeight,
                (Runtime.getRuntime().maxMemory() / 32).coerceIn(2_000_000L, 12_000_000L))
        }
        return context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
    }
}
