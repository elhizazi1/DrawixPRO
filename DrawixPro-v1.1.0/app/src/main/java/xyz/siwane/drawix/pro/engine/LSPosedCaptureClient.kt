package xyz.siwane.drawix.pro.engine

import android.content.*
import android.graphics.BitmapFactory
import android.os.*
import androidx.core.content.FileProvider
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import xyz.siwane.drawix.pro.EngineManager
import xyz.siwane.drawix.pro.R
import xyz.siwane.drawix.pro.capture.LSPosedProtocol as Protocol
import xyz.siwane.drawix.pro.data.ScreenshotRepository
import java.io.File
import java.util.concurrent.ConcurrentHashMap

object LSPosedCaptureClient {
    data class Host(val name: String, val family: String, val uid: Int, val revision: String = "unknown")
    private val mutableHost = MutableStateFlow<Host?>(null)
    val readyHost = mutableHost.asStateFlow()
    private val attempts = ConcurrentHashMap<String, String>()
    private var checkedAt = 0L
    private val legacySeen = java.util.concurrent.atomic.AtomicBoolean(false)
    private suspend fun request(context: Context, host: String, uri: android.net.Uri? = null, protocol: Int = Protocol.VERSION): Pair<Int, Bundle> {
        val result = CompletableDeferred<Pair<Int, Bundle>>()
        val callback = object : ResultReceiver(Handler(Looper.getMainLooper())) {
            override fun onReceiveResult(code: Int, data: Bundle?) {
                if (data?.getInt("version") == protocol) {
                    attempts["$host/v$protocol"] = "${if (uri == null) "probe" else "capture"}: reply=$code; uid=${data.getInt("uid", -1)}; error=${data.getString("error") }"
                    result.complete(code to data)
                }
            }
        }
        try {
            context.sendBroadcast(Intent(if (protocol == Protocol.VERSION) Protocol.ACTION else "${Protocol.PACKAGE}.LSPOSED_REQUEST_V$protocol").setPackage(host)
                .putExtra("reply", FrameworkReply.forTransport(callback)).putExtra("output", uri).putExtra("version", protocol))
            return withTimeout(if (uri == null) 3_500L else 15_000L) { result.await() }
        } catch (timeout: TimeoutCancellationException) {
            attempts["$host/v$protocol"] = "${if (uri == null) "probe" else "capture"}: no_reply"
            throw timeout
        } finally { result.cancel() }
    }
    fun needsRestart() = legacySeen.get() || (EngineManager.isLSPosedActive() && EngineManager.lsposedLoadedProtocol() != Protocol.VERSION)
    suspend fun ensureReady(context: Context): Host = coroutineScope {
        mutableHost.value = null; attempts.clear(); legacySeen.set(false); checkedAt = System.currentTimeMillis()
        val found = CompletableDeferred<Host>()
        val jobs = Protocol.HOSTS.map { target -> launch {
            try {
                val (code, data) = request(context, target)
                attempts["$target/v${Protocol.VERSION}"] = "reply=$code; uid=${data.getInt("uid", -1)}; family=${data.getString("family") }"
                if (code == Protocol.READY) {
                    val revision = data.getString("revision") ?: "unknown"
                    if (revision == Protocol.REVISION) found.complete(Host(target, data.getString("family") ?: "LSPosed", data.getInt("uid", -1), revision))
                    else {
                        legacySeen.set(true)
                        attempts["$target/v${Protocol.VERSION}"] = "stale_revision=$revision; expected=${Protocol.REVISION}; restart_required"
                    }
                }
            } catch (_: TimeoutCancellationException) { attempts["$target/v${Protocol.VERSION}"] = "no_reply" }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { attempts["$target/v${Protocol.VERSION}"] = error.javaClass.simpleName + ": " + error.message }
        } } + listOf(2, 3).flatMap { previous -> Protocol.HOSTS.map { target -> launch {
            try {
                val (code, _) = request(context, target, protocol = previous)
                if (code == Protocol.READY) legacySeen.set(true)
            } catch (_: TimeoutCancellationException) { }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { }
        } } }
        try {
            val host = withTimeoutOrNull(4_000) { found.await() }
                ?: error(context.getString(if (needsRestart())
                    R.string.capture_lsposed_restart else R.string.capture_lsposed_hosts_missing))
            mutableHost.value = host
            host
        } finally { jobs.forEach { it.cancel() }; found.cancel() }
    }
    suspend fun capture(context: Context) {
        var stage = "probe"
        try {
        val host = ensureReady(context)
        withContext(Dispatchers.IO) {
            val directory = File(context.cacheDir, "lsposed-captures").apply { mkdirs() }
            val file = File.createTempFile("capture-", ".png", directory)
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            try {
                stage = "grant_output/${host.name}"
                context.grantUriPermission(host.name, uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                stage = "host_capture/${host.name}"
                CaptureAttemptStore.record(context, "pending; host=${host.name}; revision=${host.revision}")
                val (code, reply) = request(context, host.name, uri)
                check(code == Protocol.READY) { "reply=$code; ${reply.getString("error") ?: "no host detail"}" }
                stage = "decode_png"
                val bitmap = requireNotNull(BitmapFactory.decodeFile(file.absolutePath)) { context.getString(R.string.capture_lsposed_failed) }
                stage = "save_image"
                try { ScreenshotRepository(context).save(bitmap) } finally { bitmap.recycle() }
            } finally { context.revokeUriPermission(uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION); file.delete() }
        }
        CaptureAttemptStore.record(context, "success")
        } catch (timeout: TimeoutCancellationException) {
            currentCoroutineContext().ensureActive()
            val detail = "$stage: capture request timed out"
            CaptureAttemptStore.record(context, detail)
            throw IllegalStateException(context.getString(R.string.capture_lsposed_failed) + "\n" + detail, timeout)
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (error: Exception) {
            val detail = "$stage: ${error.javaClass.simpleName}: ${error.message}"
            CaptureAttemptStore.record(context, detail)
            throw IllegalStateException(context.getString(R.string.capture_lsposed_failed) + "\n" + detail, error)
        }
    }
    fun diagnostics(context: Context): String = buildString {
        appendLine("Drawix Pro / LSPosed bridge")
        appendLine("SDK=${Build.VERSION.SDK_INT}; app=${context.packageName}; protocol=${Protocol.VERSION}")
        appendLine("expected_revision=${Protocol.REVISION}")
        appendLine("last_capture=${CaptureAttemptStore.read(context)}")
        appendLine("legacy_bridge_reply=${legacySeen.get()}")
        appendLine("self_hook=${EngineManager.isLSPosedActive()}; loaded_protocol=${EngineManager.lsposedLoadedProtocol()}")
        appendLine("last_probe_ms=$checkedAt; ready_host=${readyHost.value}")
        attempts.toSortedMap().forEach { (endpoint, result) -> appendLine("$endpoint: $result") }
        try {
            val status = context.contentResolver.call(android.net.Uri.parse("content://${context.packageName}.bridge-status"), "read", null, null)
            Protocol.HOSTS.forEach { appendLine("host_status/$it=${status?.getString(it) ?: "not_reported"}") }
        } catch (error: Exception) { appendLine("host_status_error=${error.javaClass.simpleName}: ${error.message}") }
        appendLine("root_granted=${com.topjohnwu.superuser.Shell.isAppGrantedRoot()}")
        appendLine("manager_launch=${FrameworkManager.lastLaunch}")
    }
}
