package xyz.siwane.drawix.pro.engine

import android.content.Context
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Adds only DrawixPro log lines, using an already-open root shell; never requests root. */
object FrameworkDiagnosticReport {
    suspend fun create(context: Context): String = withContext(Dispatchers.IO) {
        try { LSPosedCaptureClient.ensureReady(context) }
        catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
        catch (_: Exception) { /* Probe details are retained in the report. */ }
        val base = LSPosedCaptureClient.diagnostics(context)
        val shell = Shell.getCachedShell()?.takeIf { it.isRoot }
            ?: return@withContext base + "root_log=unavailable (no existing root shell)\n"
        runCatching {
            val result = shell.newJob().add("logcat -d -v brief -t 2500 -e DrawixPro").exec()
            val lines = result.out.filter { it.contains("DrawixPro") }.takeLast(80)
            base + "DrawixPro log (exit=${result.code}):\n" + lines.joinToString("\n")
        }.getOrElse { base + "root_log=${it.javaClass.simpleName}: ${it.message}\n" }
    }
}
