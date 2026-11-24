package xyz.siwane.drawix.pro.engine

/** Latest user intent wins; a late authorization cannot replace a newer choice. */
class EngineSelection {
    private var revision = 0L
    private var pendingEngine: Int? = null
    @Synchronized fun begin(engine: Int): Long { pendingEngine = engine; return ++revision }
    @Synchronized fun finish(ticket: Long, engine: Int): Boolean {
        if (ticket != revision || pendingEngine != engine) return false
        pendingEngine = null
        return true
    }
}
