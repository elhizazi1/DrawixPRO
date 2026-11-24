package xyz.siwane.drawix.pro.engine

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.IBinder
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import rikka.shizuku.Shizuku
import xyz.siwane.drawix.pro.EngineManager

object ShizukuCaptureClient {
    suspend fun capture(context: Context): Bitmap = withContext(Dispatchers.IO) {
        check(EngineManager.hasShizukuPermission()) { "Shizuku unavailable" }
        val connected = CompletableDeferred<ICaptureService>()
        val args = Shizuku.UserServiceArgs(ComponentName(context, ShizukuCaptureService::class.java))
            .daemon(false).processNameSuffix("capture").version(1)
        val connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName, binder: IBinder) {
                connected.complete(ICaptureService.Stub.asInterface(binder))
            }
            override fun onServiceDisconnected(name: ComponentName) {
                connected.completeExceptionally(IllegalStateException("Shizuku disconnected"))
            }
        }
        try {
            Shizuku.bindUserService(args, connection)
            val remote = withTimeout(15_000) { connected.await() }
            ParcelFileDescriptor.AutoCloseInputStream(remote.capture()).use {
                requireNotNull(BitmapFactory.decodeStream(it)) { "Invalid screenshot" }
            }
        } finally { runCatching { Shizuku.unbindUserService(args, connection, true) } }
    }
}
