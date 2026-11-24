package xyz.siwane.drawix.pro.engine

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import xyz.siwane.drawix.pro.FloatingDrawService

/** Service publishes its actual state; a switch never reports success before startup. */
object ServiceController {
    private val mutableRunning = MutableStateFlow(false)
    val running = mutableRunning.asStateFlow()
    fun publish(running: Boolean) { mutableRunning.value = running }
    fun start(context: Context): Result<Unit> = runCatching {
        check(Settings.canDrawOverlays(context))
        ContextCompat.startForegroundService(context, Intent(context, FloatingDrawService::class.java))
        Unit
    }
    fun stop(context: Context) { context.stopService(Intent(context, FloatingDrawService::class.java)) }
}
