package xyz.siwane.drawix.pro

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import xyz.siwane.drawix.pro.capture.ProjectionCaptureService
import xyz.siwane.drawix.pro.capture.ProjectionSession

/** Android's visible consent flow for devices without accessibility screenshot support. */
class ScreenCaptureActivity : ComponentActivity() {
    private val consent = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null && ProjectionSession.pending != null) {
            runCatching {
                ContextCompat.startForegroundService(this, Intent(this, ProjectionCaptureService::class.java)
                    .putExtra("resultCode", result.resultCode).putExtra("data", result.data))
            }.onFailure { ProjectionSession.complete(it) }
        } else ProjectionSession.complete(IllegalStateException(getString(R.string.ui_capture_cancelled)))
        finish()
    }
    override fun onDestroy() {
        if (isFinishing) ProjectionSession.markUiDismissed()
        super.onDestroy()
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (ProjectionSession.pending == null) { finish(); return }
        if (savedInstanceState == null) {
            val manager = getSystemService(MediaProjectionManager::class.java)
            val request = if (android.os.Build.VERSION.SDK_INT >= 34) manager.createScreenCaptureIntent(
                android.media.projection.MediaProjectionConfig.createConfigForDefaultDisplay()) else manager.createScreenCaptureIntent()
            consent.launch(request)
        }
    }
}
