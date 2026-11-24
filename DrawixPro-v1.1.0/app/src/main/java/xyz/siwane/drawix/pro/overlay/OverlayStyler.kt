package xyz.siwane.drawix.pro.overlay

import android.content.Context
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.GradientDrawable
import android.view.ViewOutlineProvider
import androidx.core.graphics.ColorUtils
import com.google.android.material.color.MaterialColors
import xyz.siwane.drawix.pro.R
import xyz.siwane.drawix.pro.data.DrawPreferences
import xyz.siwane.drawix.pro.databinding.LayoutFloatingButtonBinding
import xyz.siwane.drawix.pro.databinding.LayoutToolsPanelBinding
import xyz.siwane.drawix.pro.databinding.LayoutTextInputBinding

/** Paint existing windows in place. Never replace views, listeners, drawing state or layout params. */
class OverlayStyler(private val base: Context, private val preferences: DrawPreferences) {
    private val density get() = base.resources.displayMetrics.density
    private fun contrast(color: Int) = if (ColorUtils.calculateLuminance(color) > .45) Color.BLACK else Color.WHITE
    fun apply(button: LayoutFloatingButtonBinding, panel: LayoutToolsPanelBinding,
        input: LayoutTextInputBinding?, systemDark: Boolean): Int {
        val context = OverlayAppearance.context(base, systemDark)
        val style = OverlayStyleSettings.from(preferences.store.all)
        fun color(attr: Int) = MaterialColors.getColor(context, attr, Color.GRAY)
        val dark = context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK == android.content.res.Configuration.UI_MODE_NIGHT_YES
        val dynamic = preferences.bool("dynamic_color") && android.os.Build.VERSION.SDK_INT >= 31
        val roles = MaterialColors.getColorRoles(preferences.int("accent_color", 0xFF006B5E.toInt()), !dark)
        val neutral = if (dark) 0xFF101211.toInt() else 0xFFFCFCFA.toInt()
        val surface = if (dynamic) color(com.google.android.material.R.attr.colorSurfaceContainerHigh)
            else ColorUtils.blendARGB(neutral, roles.accentContainer, .32f)
        val cell = if (dynamic) color(com.google.android.material.R.attr.colorSurfaceContainerHighest)
            else ColorUtils.blendARGB(neutral, roles.accentContainer, .42f)
        val foreground = color(com.google.android.material.R.attr.colorOnSurface)
        val accent = if (dynamic) color(androidx.appcompat.R.attr.colorPrimary) else roles.accent
        val edge = ColorUtils.setAlphaComponent(foreground, 45)
        fun solid(fill: Int, radius: Float) = GradientDrawable().apply {
            setColor(fill); cornerRadius = radius * density
        }
        fun glass(radius: Float) = GradientDrawable(GradientDrawable.Orientation.TL_BR,
            intArrayOf(ColorUtils.setAlphaComponent(ColorUtils.blendARGB(surface, accent, .10f), (255 * style.panelOpacity).toInt()),
                ColorUtils.setAlphaComponent(surface, (255 * style.panelOpacity).toInt()))).apply {
            cornerRadius = radius * density
            if (style.border) setStroke(density.toInt().coerceAtLeast(1), edge)
        }
        panel.panelContainer.background = glass(style.cornerDp)
        panel.colorsContainer.background = glass(style.cornerDp)
        panel.customPickerContainer.background = glass(style.cornerDp)
        val tools = listOf(panel.toolBrush, panel.toolRect, panel.toolCircle, panel.toolArrow, panel.toolText,
            panel.toolCensor, panel.toolColorPicker, panel.toolMove, panel.toolLaser, panel.toolLaserMode,
            panel.toolEraser, panel.toolUndo, panel.toolClear, panel.toolScreenshot)
        tools.forEach { it.background = solid(cell, 16f) }
        panel.toolSettings.background = glass(24f)
        panel.toolClose.background = glass(24f)
        panel.toolSettings.setColorFilter(foreground, PorterDuff.Mode.SRC_IN)
        panel.toolClose.setColorFilter(0xFFFF4444.toInt(), PorterDuff.Mode.SRC_IN)
        panel.colorCustom.background = solid(0xFF292D32.toInt(), 14f)
        panel.colorCustom.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN)
        panel.btnApplyCustomColor.background = solid(accent, 12f)
        panel.btnApplyCustomColor.setTextColor(contrast(accent))
        val buttonFill = if (style.followAccent) accent else style.buttonColor
        button.btnMainFab.apply {
            backgroundTintList = null
            background = solid(buttonFill, when (style.shape) { 1 -> 15f; 2 -> 9f; else -> 22f })
            outlineProvider = ViewOutlineProvider.BACKGROUND
            clipToOutline = true
            setImageResource(when (style.icon) { 1 -> R.drawable.ic_camera; 2 -> R.drawable.ic_pointer; else -> R.drawable.ic_brush_2 })
            setColorFilter(contrast(buttonFill), PorterDuff.Mode.SRC_IN)
            elevation = 0f; translationZ = 0f
        }
        input?.apply {
            textFrame.background = glass(style.cornerDp)
            etInput.setTextColor(foreground)
            etInput.setHintTextColor(ColorUtils.setAlphaComponent(foreground, 160))
        }
        return foreground
    }
}
