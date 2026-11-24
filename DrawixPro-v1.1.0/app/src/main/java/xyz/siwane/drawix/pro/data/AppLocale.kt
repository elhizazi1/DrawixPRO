package xyz.siwane.drawix.pro.data

import android.content.Context
import android.content.res.Configuration
import java.util.Locale
import xyz.siwane.drawix.pro.DrawixContextWrapper
import xyz.siwane.drawix.pro.TranslationEngine

object AppLocale {
    fun wrap(context: Context): Context {
        val language = DrawPreferences(context).text("app_language")
        val tag = when {
            language.startsWith("custom_rtl_") -> "ar"
            language.startsWith("custom_") -> "en"
            else -> language
        }
        val locale = Locale.forLanguageTag(tag)
        // Override locale only. Copying the full configuration pins uiMode, density,
        // and font scale to their values at process startup instead of following the device.
        val config = Configuration().apply {
            setLocale(locale); setLayoutDirection(locale)
        }
        TranslationEngine.init(context)
        val localized = context.createConfigurationContext(config)
        return if (TranslationEngine.isCustomLanguageActive) DrawixContextWrapper(localized) else localized
    }
}
