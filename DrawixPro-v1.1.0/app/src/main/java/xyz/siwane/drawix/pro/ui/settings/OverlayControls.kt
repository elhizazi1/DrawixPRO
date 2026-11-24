package xyz.siwane.drawix.pro.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import xyz.siwane.drawix.pro.R
import xyz.siwane.drawix.pro.overlay.OverlayStyleSettings
import xyz.siwane.drawix.pro.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun OverlayControls(prefs: PreferenceSnapshot) {
    val style = OverlayStyleSettings.from(prefs.store.all)
    val accent = if (style.followAccent) MaterialTheme.colorScheme.primary else Color(style.buttonColor)
    val buttonShape = RoundedCornerShape(when (style.shape) { 1 -> 15.dp; 2 -> 9.dp; else -> 22.dp })
    Column(verticalArrangement = Arrangement.spacedBy(CardCorners.spacing)) {
        SectionTitle(text(R.string.overlay_customize))
        GlassSurface(shape = CardCorners.single, color = MaterialTheme.colorScheme.surfaceContainer) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(text(R.string.overlay_live), style = MaterialTheme.typography.bodyMedium)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = buttonShape, color = accent,
                        contentColor = if (androidx.core.graphics.ColorUtils.calculateLuminance(
                                if (style.followAccent) accent.toArgb() else style.buttonColor) > .45) Color.Black else Color.White,
                        modifier = Modifier.size(44.dp).alpha(prefs.float("btn_alpha_value").coerceIn(.2f, 1f))) {
                        Box(contentAlignment = Alignment.Center) { DrawIcon(when (style.icon) {
                            1 -> R.drawable.ic_camera; 2 -> R.drawable.ic_pointer; else -> R.drawable.ic_brush_2
                        }, modifier = Modifier.size(22.dp)) }
                    }
                    Surface(shape = RoundedCornerShape(style.cornerDp.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = style.panelOpacity),
                        border = if (style.border) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else null,
                        modifier = Modifier.weight(1f)) {
                        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                            listOf(R.drawable.ic_brush_2, R.drawable.ic_format_square, R.drawable.ic_eraser, R.drawable.ic_camera).forEach {
                                DrawIcon(it, modifier = Modifier.size(22.dp))
                            }
                        }
                    }
                }
            }
        }
        SectionTitle(text(R.string.overlay_shape))
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            listOf(R.string.overlay_circle, R.string.overlay_rounded, R.string.overlay_soft_square).forEachIndexed { index, label ->
                SegmentedButton(selected = style.shape == index, onClick = { prefs.put("overlay_button_shape", index) },
                    shape = SegmentedButtonDefaults.itemShape(index, 3)) { Text(text(label), style = MaterialTheme.typography.labelMedium) }
            }
        }
        SectionTitle(text(R.string.overlay_icon))
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            listOf(R.string.overlay_brush, R.string.overlay_camera, R.string.overlay_pointer).forEachIndexed { index, label ->
                SegmentedButton(selected = style.icon == index, onClick = { prefs.put("overlay_button_icon", index) },
                    shape = SegmentedButtonDefaults.itemShape(index, 3)) { Text(text(label), style = MaterialTheme.typography.labelMedium) }
            }
        }
        Text(text(R.string.overlay_icon_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        ToggleRow(text(R.string.overlay_follow_accent), text(R.string.overlay_follow_hint), style.followAccent) {
            prefs.put("overlay_follow_accent", it)
        }
        if (!style.followAccent) {
            SectionTitle(text(R.string.overlay_color))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(2.dp)) {
                val colors = listOf(0xFF006B5E, 0xFF00AFA5, 0xFF0F52BA, 0xFF7651A8, 0xFFB34470, 0xFF986000, 0xFF56752B, 0xFF292D32)
                items(colors.size) { index ->
                    val color = colors[index].toInt()
                    val label = text(R.string.overlay_color) + " #" + "%06X".format(color and 0xFFFFFF)
                    Surface(onClick = { prefs.put("overlay_button_color", color) }, color = Color(color),
                        shape = RoundedCornerShape(50), modifier = Modifier.size(44.dp).semantics { contentDescription = label },
                        border = if (style.buttonColor == color) BorderStroke(3.dp, MaterialTheme.colorScheme.onSurface) else null) {
                        Box(contentAlignment = Alignment.Center) { if (style.buttonColor == color) Text("✓", color = Color.White) }
                    }
                }
            }
        }
        SectionTitle(text(R.string.overlay_panel))
        PreferenceSlider(text(R.string.overlay_panel_opacity), style.panelOpacity, .65f..1f, "%", shape = CardCorners.grouped(0, 3)) {
            prefs.put("overlay_panel_opacity", it)
        }
        PreferenceSlider(text(R.string.overlay_roundness), style.roundness, 0f..1f, "%", shape = CardCorners.grouped(1, 3)) {
            prefs.put("overlay_panel_roundness", it)
        }
        ToggleRow(text(R.string.overlay_border), text(R.string.overlay_border_hint), style.border, shape = CardCorners.grouped(2, 3)) {
            prefs.put("overlay_panel_border", it)
        }
        OutlinedButton(onClick = {
            val editor = prefs.store.edit()
            OverlayStyleSettings.keys.forEach { editor.remove(it) }
            editor.apply()
        }, modifier = Modifier.fillMaxWidth()) { Text(text(R.string.overlay_reset)) }
    }
}
