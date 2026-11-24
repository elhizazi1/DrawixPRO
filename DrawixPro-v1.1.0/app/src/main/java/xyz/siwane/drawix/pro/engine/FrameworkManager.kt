package xyz.siwane.drawix.pro.engine

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import xyz.siwane.drawix.pro.EngineManager

/** The module's official action is used for embedded managers; it does not require an exported host activity. */
object FrameworkManager {
    val packages = listOf("org.matrix.vector.manager", "org.lsposed.manager", "org.lsposed.lspd", "io.github.libxposed.manager")
    @Volatile var lastLaunch: String = "not_requested"
        private set
    private fun candidates(): List<String> {
        val family = LSPosedCaptureClient.readyHost.value?.family ?: if (EngineManager.isLSPosedActive())
            (if (EngineManager.lsposedManagerPackage() == "org.matrix.vector.manager") "Vector" else "LSPosed") else null
        return when (family) {
            "Vector" -> packages.filter { it == "org.matrix.vector.manager" }
            "LSPosed" -> packages.filter { it != "org.matrix.vector.manager" }
            else -> packages
        }
    }
    fun hasInstalledManager(context: Context) = candidates().any { context.packageManager.getLaunchIntentForPackage(it) != null }
    suspend fun open(context: Context, allowRootRequest: Boolean = false): Boolean {
        for (name in candidates()) {
            val intent = context.packageManager.getLaunchIntentForPackage(name) ?: continue
            runCatching { context.startActivity(intent) }.onSuccess { lastLaunch = "installed:$name"; return true }
                .onFailure { lastLaunch = "installed:$name: ${it.javaClass.simpleName}" }
        }
        // A new authorization is only requested after the explicitly labelled Root button is pressed.
        if (!allowRootRequest && com.topjohnwu.superuser.Shell.isAppGrantedRoot() != true) {
            lastLaunch = "embedded_manager_requires_existing_root_or_framework_notification"
            return false
        }
        return withContext(Dispatchers.IO) {
            runCatching {
                if (allowRootRequest && !EngineManager.requestRootAccess()) {
                    lastLaunch = "root_denied"; return@withContext false
                }
                com.topjohnwu.superuser.Shell.getCachedShell()?.let { if (!it.isRoot) it.close() }
                val shell = com.topjohnwu.superuser.Shell.getShell()
                if (!shell.isRoot) { lastLaunch = "cached_shell_is_not_root"; return@withContext false }
                val installed = shell.newJob().add(
                    "if [ -f /data/adb/modules/zygisk_vector/module.prop ] && [ ! -e /data/adb/modules/zygisk_vector/disable ] && [ ! -e /data/adb/modules/zygisk_vector/remove ]; then echo zygisk_vector; elif [ -f /data/adb/modules/zygisk_lsposed/module.prop ] && [ ! -e /data/adb/modules/zygisk_lsposed/disable ] && [ ! -e /data/adb/modules/zygisk_lsposed/remove ]; then echo zygisk_lsposed; fi"
                ).exec().out.firstOrNull()?.trim()
                val module = installed?.takeIf { it == "zygisk_vector" || it == "zygisk_lsposed" }
                val vector = module == "zygisk_vector" || (module == null && (LSPosedCaptureClient.readyHost.value?.family == "Vector" || EngineManager.lsposedManagerPackage() == "org.matrix.vector.manager"))
                val fallback = FrameworkLaunchCommand.command(vector, android.os.Build.VERSION.SDK_INT)
                val command = if (module == null) fallback else
                    "if [ -f /data/adb/modules/$module/action.sh ]; then /system/bin/sh /data/adb/modules/$module/action.sh; else $fallback; fi"
                val result = shell.newJob().add(command).exec()
                lastLaunch = "official_action:${module ?: if (vector) "Vector" else "LSPosed"}; exit=${result.code}; " + result.err.joinToString(" ").take(240)
                result.isSuccess
            }.getOrElse { lastLaunch = "official_action:${it.javaClass.simpleName}: ${it.message}"; false }
        }
    }
}
