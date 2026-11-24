package xyz.siwane.drawix.pro.capture

import android.app.*
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.*
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import xyz.siwane.drawix.pro.R
import xyz.siwane.drawix.pro.data.ScreenshotRepository

class ProjectionCaptureService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var projection: MediaProjection? = null
    private var display: VirtualDisplay? = null
    private var reader: ImageReader? = null
    private var saving = false
    override fun onBind(intent: Intent?) = null
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (projection != null) return START_NOT_STICKY
        try {
            val manager = getSystemService(NotificationManager::class.java)
            if (Build.VERSION.SDK_INT >= 26) manager.createNotificationChannel(NotificationChannel("DrawixCapture", getString(R.string.service_title), NotificationManager.IMPORTANCE_LOW))
            val notification = NotificationCompat.Builder(this, "DrawixCapture").setSmallIcon(R.drawable.ic_camera)
                .setContentTitle(getString(R.string.ui_capturing)).setOngoing(true).build()
            if (Build.VERSION.SDK_INT >= 29) startForeground(55, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
            else startForeground(55, notification)
            @Suppress("DEPRECATION")
            val data = intent?.getParcelableExtra<Intent>("data") ?: error("Missing capture consent")
            projection = getSystemService(MediaProjectionManager::class.java).getMediaProjection(intent.getIntExtra("resultCode", 0), data)
            val callback = object : MediaProjection.Callback() {
                override fun onStop() {
                    if (!saving) ProjectionSession.complete(IllegalStateException(getString(R.string.ui_capture_cancelled)))
                    stopSelf()
                }
            }
            projection!!.registerCallback(callback, Handler(Looper.getMainLooper()))
            val metrics = resources.displayMetrics
            val width = metrics.widthPixels
            val height = metrics.heightPixels
            val images = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
            reader = images
            images.setOnImageAvailableListener({ source ->
                val image = source.acquireLatestImage() ?: return@setOnImageAvailableListener
                if (saving) { image.close(); return@setOnImageAvailableListener }
                saving = true
                scope.launch {
                    try {
                        withContext(Dispatchers.IO) {
                            val bitmap = image.use {
                                val plane = it.planes[0]
                                val paddedWidth = plane.rowStride / plane.pixelStride
                                val padded = Bitmap.createBitmap(paddedWidth, height, Bitmap.Config.ARGB_8888)
                                padded.copyPixelsFromBuffer(plane.buffer)
                                if (paddedWidth == width) padded else Bitmap.createBitmap(padded, 0, 0, width, height).also { padded.recycle() }
                            }
                            try { ScreenshotRepository(this@ProjectionCaptureService).save(bitmap) } finally { bitmap.recycle() }
                        }
                        ProjectionSession.complete()
                    } catch (e: Exception) { ProjectionSession.complete(e) }
                    finally { stopSelf() }
                }
            }, Handler(Looper.getMainLooper()))
            scope.launch {
                try {
                    withTimeout(3_000) { ProjectionSession.awaitUiDismissed() }
                    // Create the display after the consent activity and its exit transition disappear.
                    delay(300)
                    display = projection!!.createVirtualDisplay("Drawix screenshot", width, height, metrics.densityDpi,
                        DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, images.surface, null, null)
                } catch (failure: Exception) { ProjectionSession.complete(failure); stopSelf() }
            }
            scope.launch { delay(15_000); if (!saving) { ProjectionSession.complete(IllegalStateException(getString(R.string.ui_capture_timeout))); stopSelf() } }
        } catch (e: Exception) { ProjectionSession.complete(e); stopSelf() }
        return START_NOT_STICKY
    }
    override fun onDestroy() {
        display?.release(); reader?.close(); projection?.stop(); scope.cancel()
        if (ProjectionSession.pending != null) ProjectionSession.complete(IllegalStateException(getString(R.string.ui_capture_cancelled)))
        super.onDestroy()
    }
}
