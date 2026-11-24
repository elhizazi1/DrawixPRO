package xyz.siwane.drawix.pro.engine

import android.os.ParcelFileDescriptor
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.system.exitProcess

/** Loaded by Shizuku in a shell process. No arbitrary command or caller-supplied path API. */
class ShizukuCaptureService : ICaptureService.Stub() {
    override fun capture(): ParcelFileDescriptor {
        val output = File.createTempFile("drawix-", ".png", File("/data/local/tmp"))
        try {
            val process = ProcessBuilder("/system/bin/screencap", "-p", output.absolutePath)
                .redirectErrorStream(true).start()
            val deadline = android.os.SystemClock.elapsedRealtime() + 15_000
            while (true) {
                val exited = try { process.exitValue(); true } catch (_: IllegalThreadStateException) { false }
                if (exited) break
                if (android.os.SystemClock.elapsedRealtime() >= deadline) {
                    process.destroy()
                    error("Screenshot timed out")
                }
                Thread.sleep(100)
            }
            check(process.exitValue() == 0 && output.length() > 0) { "Screenshot failed" }
            return ParcelFileDescriptor.open(output, ParcelFileDescriptor.MODE_READ_ONLY)
        } finally { output.delete() }
    }
    override fun destroy() { exitProcess(0) }
}
