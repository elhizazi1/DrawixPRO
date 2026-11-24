package xyz.siwane.drawix.pro.overlay

import android.content.Context
import android.content.res.Configuration
import android.view.ContextThemeWrapper
import com.google.android.material.color.DynamicColors
import xyz.siwane.drawix.pro.R
import xyz.siwane.drawix.pro.data.AppLocale
import xyz.siwane.drawix.pro.data.DrawPreferences

object OverlayAppearance {
    fun context(base: Context, systemDark: Boolean? = null): Context {
        val preferences = DrawPreferences(base)
        val localized = AppLocale.wrap(base)
        val configuration = Configuration().apply {
            uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
                if (xyz.siwane.drawix.pro.ui.theme.ThemeMode.resolve(
                    preferences.store.getString("theme_mode", null),
                    if (preferences.store.contains("dark_mode_enabled")) preferences.bool("dark_mode_enabled") else null
                ).isDark(systemDark ?: (localized.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES))) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
        }
        val themed = ContextThemeWrapper(localized.createConfigurationContext(configuration), R.style.Theme_DrawView)
        return if (preferences.bool("dynamic_color")) DynamicColors.wrapContextIfAvailable(themed) else themed
    }
}
