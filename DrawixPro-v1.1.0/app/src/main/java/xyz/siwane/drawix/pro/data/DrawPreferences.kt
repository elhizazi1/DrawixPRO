package xyz.siwane.drawix.pro.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow

/** Existing preference keys are retained so upgrades preserve customization. */
class DrawPreferences(context: Context) {
    val store: SharedPreferences = context.getSharedPreferences("DrawSettings", Context.MODE_PRIVATE)
    val changes = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> trySend(Unit) }
        store.registerOnSharedPreferenceChangeListener(listener)
        trySend(Unit)
        awaitClose { store.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    fun bool(key: String, fallback: Boolean = true) = store.getBoolean(key, fallback)
    fun float(key: String, fallback: Float = 1f) = store.getFloat(key, fallback)
    fun int(key: String, fallback: Int = 0) = store.getInt(key, fallback)
    fun text(key: String, fallback: String = "ar") = store.getString(key, fallback) ?: fallback
    fun put(key: String, value: Boolean) = store.edit().putBoolean(key, value).apply()
    fun put(key: String, value: Float) = store.edit().putFloat(key, value).apply()
    fun put(key: String, value: Int) = store.edit().putInt(key, value).apply()
    fun put(key: String, value: String) = store.edit().putString(key, value).apply()
}
