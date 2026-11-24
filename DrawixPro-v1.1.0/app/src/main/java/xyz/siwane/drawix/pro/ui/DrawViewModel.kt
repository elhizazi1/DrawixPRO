package xyz.siwane.drawix.pro.ui

import android.app.Application
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import xyz.siwane.drawix.pro.*
import xyz.siwane.drawix.pro.data.DrawPreferences
import xyz.siwane.drawix.pro.engine.ServiceController

/** UI state is owned here, while system permission launchers belong to the Activity. */
class DrawViewModel(application: Application) : AndroidViewModel(application) {
    val preferences = DrawPreferences(application)
    private val mutableRevision = MutableStateFlow(0)
    val revision = mutableRevision.asStateFlow()
    val running = ServiceController.running
    private val mutableBusy = MutableStateFlow(false)
    val busy = mutableBusy.asStateFlow()
    private val mutableMessage = MutableStateFlow<Int?>(null)
    val message = mutableMessage.asStateFlow()
    init {
        viewModelScope.launch { preferences.changes.collect { refresh() } }
        viewModelScope.launch { ScreenshotAccessibilityService.connected.collect { refresh() } }
    }
    fun refresh() { mutableRevision.value++ }
    fun messageConsumed() { mutableMessage.value = null }
    fun showMessage(id: Int) { mutableMessage.value = id }
    private val selection = xyz.siwane.drawix.pro.engine.EngineSelection()
    private var selectionJob: kotlinx.coroutines.Job? = null
    private var shizukuTicket: Long? = null
    private var busyTicket: Long? = null
    private val mutableEngineIssue = MutableStateFlow<Int?>(null)
    val engineIssue = mutableEngineIssue.asStateFlow()
    fun dismissEngineIssue() { mutableEngineIssue.value = null }
    private fun commit(ticket: Long, engine: Int) {
        if (selection.finish(ticket, engine)) {
            preferences.put("core_engine_type", engine)
            refresh()
        }
    }
    fun onShizukuPermissionResult(granted: Boolean) {
        val ticket = shizukuTicket ?: return
        shizukuTicket = null
        if (granted && EngineManager.hasShizukuPermission()) commit(ticket, EngineManager.ENGINE_SHIZUKU)
        else if (selection.finish(ticket, EngineManager.ENGINE_SHIZUKU)) mutableEngineIssue.value = EngineManager.ENGINE_SHIZUKU
        refresh()
    }
    fun onShizukuDisconnected() {
        val ticket = shizukuTicket
        shizukuTicket = null
        if (ticket != null && selection.finish(ticket, EngineManager.ENGINE_SHIZUKU)) mutableEngineIssue.value = EngineManager.ENGINE_SHIZUKU
        refresh()
    }
    fun selectEngine(engine: Int, requestShizuku: () -> Unit) {
        // Avoid correlating two outstanding Android permission requests with one callback.
        if (engine == EngineManager.ENGINE_SHIZUKU && shizukuTicket != null) return
        val ticket = selection.begin(engine)
        selectionJob?.cancel()
        busyTicket = null
        mutableBusy.value = false
        mutableEngineIssue.value = null
        when (engine) {
            EngineManager.ENGINE_SHIZUKU -> when {
                !EngineManager.isShizukuRunning() -> { selection.finish(ticket, engine); mutableEngineIssue.value = engine }
                !EngineManager.hasShizukuPermission() -> {
                    shizukuTicket = ticket
                    runCatching { requestShizuku() }.onFailure {
                        shizukuTicket = null
                        if (selection.finish(ticket, engine)) mutableEngineIssue.value = engine
                    }
                }
                else -> commit(ticket, engine)
            }
            EngineManager.ENGINE_ROOT -> {
                busyTicket = ticket
                mutableBusy.value = true
                selectionJob = viewModelScope.launch {
                    try {
                        val granted = withContext(Dispatchers.IO) { EngineManager.requestRootAccess() }
                        if (granted) commit(ticket, engine)
                        else if (selection.finish(ticket, engine)) mutableEngineIssue.value = engine
                    } finally { if (busyTicket == ticket) { busyTicket = null; mutableBusy.value = false }; refresh() }
                }
            }
            EngineManager.ENGINE_LSPOSED -> {
                busyTicket = ticket
                mutableBusy.value = true
                selectionJob = viewModelScope.launch {
                    try {
                        xyz.siwane.drawix.pro.engine.LSPosedCaptureClient.ensureReady(getApplication())
                        commit(ticket, engine)
                    } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
                    catch (_: Exception) {
                        if (selection.finish(ticket, engine)) mutableEngineIssue.value = engine
                    } finally {
                        if (busyTicket == ticket) { busyTicket = null; mutableBusy.value = false }
                        refresh()
                    }
                }
            }
            else -> if (Build.VERSION.SDK_INT >= 30 && !hasAccessibility()) {
                selection.finish(ticket, EngineManager.ENGINE_STANDARD)
                mutableEngineIssue.value = EngineManager.ENGINE_STANDARD
            } else commit(ticket, EngineManager.ENGINE_STANDARD)
        }
    }
    fun toggleService() {
        if (running.value) {
            ServiceController.stop(getApplication())
            SmartNotificationHelper.scheduleRetentionReminder(getApplication())
        } else {
            ServiceController.start(getApplication()).onFailure { showMessage(R.string.toast_overlay_permission) }
        }
    }
    fun hasOverlay() = Settings.canDrawOverlays(getApplication())
    fun hasAccessibility() = ScreenshotAccessibilityService.instance != null
    fun hasNotifications() = NotificationManagerCompat.from(getApplication()).areNotificationsEnabled()
    fun hasStorage() = Build.VERSION.SDK_INT >= 29 || androidx.core.content.ContextCompat.checkSelfPermission(
        getApplication(), android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == android.content.pm.PackageManager.PERMISSION_GRANTED
    fun batteryExempt() = getApplication<Application>().getSystemService(PowerManager::class.java)
        .isIgnoringBatteryOptimizations(getApplication<Application>().packageName)
}
