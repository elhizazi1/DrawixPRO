package xyz.siwane.drawix.pro.capture

import kotlinx.coroutines.CompletableDeferred

/** In-process handoff; the media-projection consent token is never persisted. */
object ProjectionSession {
    var pending: CompletableDeferred<Unit>? = null
        private set
    private var uiDismissed: CompletableDeferred<Unit>? = null
    fun markUiDismissed() { uiDismissed?.complete(Unit) }
    suspend fun awaitUiDismissed() { requireNotNull(uiDismissed).await() }
    fun begin(): CompletableDeferred<Unit> {
        check(pending == null) { "Capture already requested" }
        uiDismissed = CompletableDeferred()
        return CompletableDeferred<Unit>().also { pending = it }
    }
    fun complete(error: Throwable? = null) {
        val request = pending ?: return
        pending = null
        uiDismissed?.cancel(); uiDismissed = null
        if (error == null) request.complete(Unit) else request.completeExceptionally(error)
    }
    fun clear(request: CompletableDeferred<Unit>) {
        if (pending === request) { pending = null; uiDismissed?.cancel(); uiDismissed = null; request.cancel() }
    }
}
