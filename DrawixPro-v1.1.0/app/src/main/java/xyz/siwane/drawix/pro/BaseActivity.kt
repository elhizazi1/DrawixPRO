package xyz.siwane.drawix.pro

import android.content.Context
import android.content.ContextWrapper
import android.content.res.Resources
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import xyz.siwane.drawix.pro.data.AppLocale
import xyz.siwane.drawix.pro.data.DrawPreferences

/** Avoids getString -> getText recursion while retaining community translations in XML. */
class DrawixContextWrapper(base: Context) : ContextWrapper(base) {
    private val translated by lazy {
        val original = super.getResources()
        @Suppress("DEPRECATION")
        object : Resources(original.assets, original.displayMetrics, original.configuration) {
            override fun getString(id: Int): String =
                TranslationEngine.getCustomString(this@DrawixContextWrapper, id, super.getText(id).toString())
            override fun getText(id: Int): CharSequence =
                TranslationEngine.getCustomString(this@DrawixContextWrapper, id, super.getText(id).toString())
        }
    }
    override fun getResources(): Resources {
        val original = super.getResources()
        return translated.also {
            // Custom translation resources are not managed by ResourcesManager.
            // Keep their configuration in sync with the locale-wrapped base context.
            if (it.configuration != original.configuration) {
                @Suppress("DEPRECATION")
                it.updateConfiguration(original.configuration, original.displayMetrics)
            }
        }
    }
}

open class BaseActivity : AppCompatActivity() {
    override fun attachBaseContext(newBase: Context) { super.attachBaseContext(AppLocale.wrap(newBase)) }
    override fun onCreate(savedInstanceState: Bundle?) {
        // Compose resolves explicit appearance independently; resources follow the device.
        delegate.localNightMode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        super.onCreate(savedInstanceState)
    }
}
