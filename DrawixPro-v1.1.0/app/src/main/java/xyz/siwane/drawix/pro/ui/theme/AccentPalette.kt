package xyz.siwane.drawix.pro.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class AccentPalette(val mint: Color, val sky: Color, val lilac: Color, val peach: Color, val rose: Color) {
    fun at(index: Int) = listOf(mint, sky, lilac, peach, rose)[index % 5]
}
fun accentPalette(dark: Boolean) = if (dark) AccentPalette(Color(0xFF16483F), Color(0xFF173F57), Color(0xFF40315C), Color(0xFF573923), Color(0xFF552E43))
    else AccentPalette(Color(0xFFC2EFDE), Color(0xFFCCE8FC), Color(0xFFE2D7FF), Color(0xFFFFDEC3), Color(0xFFFBD3E4))
val LocalAccents = staticCompositionLocalOf { accentPalette(false) }
