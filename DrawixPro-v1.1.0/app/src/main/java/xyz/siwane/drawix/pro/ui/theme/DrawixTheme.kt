package xyz.siwane.drawix.pro.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp

private val Light = lightColorScheme(
    primary = Color(0xFF006B5E), onPrimary = Color.White,
    primaryContainer = Color(0xFF9DF2DC), onPrimaryContainer = Color(0xFF00201B),
    secondary = Color(0xFF555B91), secondaryContainer = Color(0xFFE2E1FF), onSecondaryContainer = Color(0xFF171B46),
    tertiary = Color(0xFF8C4A60), tertiaryContainer = Color(0xFFFFD9E4), onTertiaryContainer = Color(0xFF3A071D),
    surface = Color(0xFFFAFCF7),
    surfaceContainer = Color(0xFFE8EFEB), surfaceContainerHigh = Color(0xFFE2E9E5))
private val Dark = darkColorScheme(
    primary = Color(0xFF81D6C1), onPrimary = Color(0xFF00382F),
    primaryContainer = Color(0xFF005046), onPrimaryContainer = Color(0xFF9DF2DC),
    secondary = Color(0xFFC2C4FF), secondaryContainer = Color(0xFF3C416C), onSecondaryContainer = Color(0xFFE2E1FF),
    tertiary = Color(0xFFFFB1CC), tertiaryContainer = Color(0xFF713449), onTertiaryContainer = Color(0xFFFFD9E4),
    surface = Color(0xFF101511),
    surfaceContainer = Color(0xFF1B211E), surfaceContainerHigh = Color(0xFF252C28))

@Composable
fun DrawixTheme(dark: Boolean = isSystemInDarkTheme(), dynamic: Boolean = true, seed: Int = 0xFF006B5E.toInt(), content: @Composable () -> Unit) {
    val context = LocalContext.current
    val scheme = if (dynamic && Build.VERSION.SDK_INT >= 31) {
        if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else seedScheme(seed, dark)
    androidx.compose.runtime.CompositionLocalProvider(LocalAccents provides accentPalette(dark)) {
        MaterialExpressiveTheme(colorScheme = scheme, typography = Typography(
            displaySmall = androidx.compose.ui.text.TextStyle(fontSize = 28.sp, lineHeight = 36.sp),
            displayMedium = androidx.compose.ui.text.TextStyle(fontSize = 30.sp, lineHeight = 38.sp),
            displayLarge = androidx.compose.ui.text.TextStyle(fontSize = 36.sp, lineHeight = 44.sp)
        )) {
            androidx.compose.runtime.CompositionLocalProvider(LocalContentColor provides scheme.onSurface, content = content)
        }
    }
}

/** Uses the supported Material color-role API; no restricted MCU implementation classes. */
private fun seedScheme(seed: Int, dark: Boolean): ColorScheme {
    fun roles(color: Int) = com.google.android.material.color.MaterialColors.getColorRoles(color, !dark)
    fun related(rotation: Float, saturation: Float): Int {
        val hsl = FloatArray(3)
        androidx.core.graphics.ColorUtils.colorToHSL(seed, hsl)
        hsl[0] = (hsl[0] + rotation) % 360f
        hsl[1] = saturation
        return androidx.core.graphics.ColorUtils.HSLToColor(hsl)
    }
    val main = roles(seed)
    val second = roles(related(20f, .3f))
    val third = roles(related(60f, .5f))
    val base = if (dark) Dark else Light
    val container = Color(main.accentContainer)
    fun neutral(level: Float) = androidx.compose.ui.graphics.lerp(
        if (dark) Color(0xFF101211) else Color(0xFFFCFCFA), container, level)
    return base.copy(
        primary = Color(main.accent), onPrimary = Color(main.onAccent),
        primaryContainer = container, onPrimaryContainer = Color(main.onAccentContainer),
        secondary = Color(second.accent), onSecondary = Color(second.onAccent),
        secondaryContainer = Color(second.accentContainer), onSecondaryContainer = Color(second.onAccentContainer),
        tertiary = Color(third.accent), onTertiary = Color(third.onAccent),
        tertiaryContainer = Color(third.accentContainer), onTertiaryContainer = Color(third.onAccentContainer),
        background = neutral(.04f), surface = neutral(.04f),
        surfaceContainerLowest = neutral(0f), surfaceContainerLow = neutral(.12f),
        surfaceContainer = neutral(.22f), surfaceContainerHigh = neutral(.32f),
        surfaceContainerHighest = neutral(.42f), surfaceVariant = neutral(.42f),
        inversePrimary = Color(com.google.android.material.color.MaterialColors.getColorRoles(seed, dark).accent),
        surfaceTint = Color(main.accent))
}
