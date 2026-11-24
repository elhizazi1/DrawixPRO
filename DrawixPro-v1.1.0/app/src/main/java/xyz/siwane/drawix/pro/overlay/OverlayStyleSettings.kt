package xyz.siwane.drawix.pro.overlay

/** Validated appearance values; malformed imported preferences cannot break a live overlay. */
data class OverlayStyleSettings(
    val shape: Int, val icon: Int, val followAccent: Boolean, val buttonColor: Int,
    val panelOpacity: Float, val roundness: Float, val border: Boolean
) {
    val cornerDp get() = 8f + roundness * 24f
    companion object {
        val keys = listOf("overlay_button_shape", "overlay_button_icon", "overlay_follow_accent",
            "overlay_button_color", "overlay_panel_opacity", "overlay_panel_roundness", "overlay_panel_border")
        fun from(values: Map<String, *>): OverlayStyleSettings {
            fun fraction(key: String, fallback: Float, min: Float): Float =
                ((values[key] as? Number)?.toFloat()?.takeIf { it.isFinite() } ?: fallback).coerceIn(min, 1f)
            return OverlayStyleSettings(
                (values[keys[0]] as? Int)?.takeIf { it in 0..2 } ?: 0,
                (values[keys[1]] as? Int)?.takeIf { it in 0..2 } ?: 0,
                values[keys[2]] as? Boolean ?: true,
                ((values[keys[3]] as? Int) ?: 0xFF006B5E.toInt()) or 0xFF000000.toInt(),
                fraction(keys[4], .96f, .65f), fraction(keys[5], 5f / 6f, 0f),
                values[keys[6]] as? Boolean ?: true)
        }
    }
}
