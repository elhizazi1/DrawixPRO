package xyz.siwane.drawix.pro.capture

import android.content.*
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import xyz.siwane.drawix.pro.*
import xyz.siwane.drawix.pro.data.ScreenshotRepository
import xyz.siwane.drawix.pro.engine.ShizukuCaptureClient
import java.io.File
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Owns screenshot IO and reports success only after a completed save. */
class CaptureCoordinator(private val context: Context) {
    suspend fun capture(engine: Int) {
        when (engine) {
            EngineManager.ENGINE_SHIZUKU -> withContext(Dispatchers.IO) {
                val bitmap = ShizukuCaptureClient.capture(context)
                try { ScreenshotRepository(context).save(bitmap) } finally { bitmap.recycle() }
            }
            EngineManager.ENGINE_ROOT -> withContext(Dispatchers.IO) {
                check(com.topjohnwu.superuser.Shell.isAppGrantedRoot() == true) { context.getString(R.string.dialog_root_missing_desc) }
                val file = File.createTempFile("capture-", ".png", context.cacheDir)
                try {
                    val escaped = "'" + file.absolutePath.replace("'", "'\\''") + "'"
                    check(com.topjohnwu.superuser.Shell.cmd("screencap -p > $escaped").exec().isSuccess)
                    val bitmap = requireNotNull(BitmapFactory.decodeFile(file.absolutePath))
                    try { ScreenshotRepository(context).save(bitmap) } finally { bitmap.recycle() }
                } finally { file.delete() }
            }
            EngineManager.ENGINE_LSPOSED -> xyz.siwane.drawix.pro.engine.LSPosedCaptureClient.capture(context)
            else -> when (StandardCapturePolicy.route(Build.VERSION.SDK_INT, ScreenshotAccessibilityService.instance != null)) {
                StandardCapturePolicy.Route.SETUP_ACCESSIBILITY -> error(context.getString(R.string.capture_standard_setup))
                StandardCapturePolicy.Route.PROJECTION -> {
                val request = ProjectionSession.begin()
                try {
                    context.startActivity(Intent(context, ScreenCaptureActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    request.await()
                } finally { ProjectionSession.clear(request) }
                }
                StandardCapturePolicy.Route.ACCESSIBILITY -> withTimeout(20_000) {
                suspendCancellableCoroutine<Unit> { continuation ->
                    val service = ScreenshotAccessibilityService.instance
                    if (service == null) continuation.resumeWithException(IllegalStateException(context.getString(R.string.toast_accessibility_required)))
                    else service.takeSilentScreenshot(
                        onSuccess = { if (continuation.isActive) continuation.resume(Unit) },
                        onError = { if (continuation.isActive) continuation.resumeWithException(IllegalStateException(it)) })
                }
            }
            }
        }
    }
}
