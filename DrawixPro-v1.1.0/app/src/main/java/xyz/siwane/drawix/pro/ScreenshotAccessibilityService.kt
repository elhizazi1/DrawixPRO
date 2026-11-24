package xyz.siwane.drawix.pro

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.asStateFlow
import xyz.siwane.drawix.pro.data.ScreenshotRepository

class ScreenshotAccessibilityService : AccessibilityService() {
    companion object {
        @Volatile var instance: ScreenshotAccessibilityService? = null; private set
        private val mutableConnected = kotlinx.coroutines.flow.MutableStateFlow(false)
        val connected = mutableConnected.asStateFlow()
    }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    override fun onServiceConnected() { super.onServiceConnected(); instance = this; mutableConnected.value = true }
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}
    override fun onUnbind(intent: Intent?): Boolean { if (instance === this) { instance = null; mutableConnected.value = false }; return super.onUnbind(intent) }
    override fun onDestroy() { if (instance === this) { instance = null; mutableConnected.value = false }; scope.cancel(); super.onDestroy() }
    fun takeSilentScreenshot(onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (Build.VERSION.SDK_INT < 30) { onError(getString(R.string.error_requires_android_11)); return }
        takeScreenshot(0, mainExecutor, object : TakeScreenshotCallback {
            override fun onSuccess(result: ScreenshotResult) {
                val bitmap = try {
                    val wrapped = Bitmap.wrapHardwareBuffer(result.hardwareBuffer, result.colorSpace)
                    try { requireNotNull(wrapped?.copy(Bitmap.Config.ARGB_8888, false)) }
                    finally { wrapped?.recycle() }
                } catch (e: Exception) { onError(e.message ?: getString(R.string.error_extract_image)); return }
                finally { result.hardwareBuffer.close() }
                scope.launch {
                    try {
                        withContext(Dispatchers.IO) { ScreenshotRepository(this@ScreenshotAccessibilityService).save(bitmap) }
                        onSuccess()
                    } catch (e: Exception) { onError(e.message ?: getString(R.string.note_error_desc)) }
                    finally { bitmap.recycle() }
                }
            }
            override fun onFailure(code: Int) { onError(getString(R.string.error_system_code, code)) }
        })
    }
}
