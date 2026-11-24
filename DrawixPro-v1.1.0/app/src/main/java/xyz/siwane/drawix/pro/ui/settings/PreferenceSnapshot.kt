package xyz.siwane.drawix.pro.ui.settings

import xyz.siwane.drawix.pro.data.DrawPreferences

/** Immutable reads make lazy Compose rows observe SharedPreferences updates reliably. */
class PreferenceSnapshot(private val source: DrawPreferences) {
    private val values = source.store.all.toMap()
    val store get() = source.store
    fun bool(key: String, fallback: Boolean = true) = values[key] as? Boolean ?: fallback
    fun float(key: String, fallback: Float = 1f) = values[key] as? Float ?: fallback
    fun int(key: String, fallback: Int = 0) = values[key] as? Int ?: fallback
    fun text(key: String, fallback: String = "ar") = values[key] as? String ?: fallback
    fun put(key: String, value: Boolean) = source.put(key, value)
    fun put(key: String, value: Float) = source.put(key, value)
    fun put(key: String, value: Int) = source.put(key, value)
    fun put(key: String, value: String) = source.put(key, value)
}
