package xyz.siwane.drawix.pro.engine

import android.content.Context

/** Kept separately from readiness probes, which must not erase the last capture failure. */
object CaptureAttemptStore {
    fun record(context: Context, detail: String) {
        context.getSharedPreferences("capture_diagnostics", Context.MODE_PRIVATE).edit()
            .putString("last_lsposed_capture", "time=${System.currentTimeMillis()}; ${detail.take(2000)}").apply()
    }
    fun read(context: Context): String = context.getSharedPreferences("capture_diagnostics", Context.MODE_PRIVATE)
        .getString("last_lsposed_capture", "not_attempted") ?: "not_attempted"
}
