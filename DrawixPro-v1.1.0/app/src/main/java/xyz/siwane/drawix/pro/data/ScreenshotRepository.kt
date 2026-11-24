package xyz.siwane.drawix.pro.data

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.IOException

/** Own MediaStore entries need no broad media permission on Android 10+. */
class ScreenshotRepository(private val context: Context) {
    data class Image(val uri: Uri, val name: String, val date: Long)
    fun save(bitmap: Bitmap): Uri {
        val png = DrawPreferences(context).bool("save_as_png")
        val name = "DrawixPro_${System.currentTimeMillis()}.${if (png) "png" else "jpg"}"
        val format = if (png) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG
        if (Build.VERSION.SDK_INT >= 29) {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, name)
                put(MediaStore.Images.Media.MIME_TYPE, if (png) "image/png" else "image/jpeg")
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/DrawixPro/")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
            val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?: throw IOException("Could not create image")
            try {
                val stream = context.contentResolver.openOutputStream(uri) ?: throw IOException("Could not open image")
                stream.use { check(bitmap.compress(format, 95, it)) }
                context.contentResolver.update(uri, ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }, null, null)
                return uri
            } catch (e: Exception) { context.contentResolver.delete(uri, null, null); throw e }
        }
        @Suppress("DEPRECATION")
        val folder = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "DrawixPro")
        check(folder.isDirectory || folder.mkdirs())
        val file = File(folder, name)
        try { file.outputStream().use { check(bitmap.compress(format, 95, it)) } }
        catch (e: Exception) { file.delete(); throw e }
        MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), null, null)
        return androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
    }
    fun list(): List<Image> {
        if (Build.VERSION.SDK_INT < 29) {
            @Suppress("DEPRECATION")
            val folder = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "DrawixPro")
            return folder.listFiles().orEmpty().filter { it.extension.lowercase() in setOf("png", "jpg", "jpeg") }
                .sortedByDescending { it.lastModified() }.map {
                    Image(androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.provider", it), it.name, it.lastModified())
                }
        }
        val images = mutableListOf<Image>()
        context.contentResolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            arrayOf(MediaStore.Images.Media._ID, MediaStore.Images.Media.DISPLAY_NAME, MediaStore.Images.Media.DATE_ADDED),
            "${MediaStore.Images.Media.RELATIVE_PATH} = ?", arrayOf("Pictures/DrawixPro/"), "${MediaStore.Images.Media.DATE_ADDED} DESC")?.use { cursor ->
            while (cursor.moveToNext()) images += Image(ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, cursor.getLong(0)), cursor.getString(1), cursor.getLong(2) * 1000L)
        }
        return images
    }
    fun delete(image: Image): Boolean = context.contentResolver.delete(image.uri, null, null) > 0
    fun thumbnail(uri: Uri, size: Int): Bitmap? {
        if (Build.VERSION.SDK_INT >= 29) return context.contentResolver.loadThumbnail(uri, android.util.Size(size, size), null)
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        val options = BitmapFactory.Options().apply {
            inSampleSize = 1
            while (bounds.outWidth / inSampleSize > size * 2 || bounds.outHeight / inSampleSize > size * 2) inSampleSize *= 2
        }
        return context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
    }
}
