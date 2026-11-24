package xyz.siwane.drawix.pro

import android.content.pm.PackageManager
import com.topjohnwu.superuser.Shell
import rikka.shizuku.Shizuku

/** Compatibility facade retained for the LSPosed self hook and stored engine values. */
object EngineManager {
    const val ENGINE_STANDARD = 0
    const val ENGINE_SHIZUKU = 1
    const val ENGINE_ROOT = 2
    const val ENGINE_LSPOSED = 3
    fun isShizukuRunning() = runCatching { Shizuku.pingBinder() }.getOrDefault(false)
    fun hasShizukuPermission() = isShizukuRunning() && runCatching {
        Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    }.getOrDefault(false)
    fun requestShizukuPermission(requestCode: Int) { check(isShizukuRunning()) { "Shizuku disconnected" }; Shizuku.requestPermission(requestCode) }
    /** Invoke on Dispatchers.IO, only following the user's explicit engine selection. */
    @Synchronized fun requestRootAccess() = runCatching {
        // A previously denied non-root shell must not make all later retries fail.
        Shell.getCachedShell()?.let { cached -> if (!cached.isRoot) cached.close() }
        Shell.getShell().isRoot
    }.getOrDefault(false)
    fun lsposedManagerPackage(): String = "org.lsposed.manager"
    fun lsposedLoadedProtocol(): Int = 0
    fun isLSPosedActive(): Boolean = false
}
