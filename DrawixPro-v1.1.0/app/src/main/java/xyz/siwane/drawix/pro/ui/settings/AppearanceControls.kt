package xyz.siwane.drawix.pro.ui.settings

import android.os.Build
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import xyz.siwane.drawix.pro.R
import xyz.siwane.drawix.pro.ui.components.*
import xyz.siwane.drawix.pro.ui.theme.ThemeMode

@Composable
fun AppearanceControls(prefs: PreferenceSnapshot) {
    val mode = ThemeMode.resolve(prefs.store.getString("theme_mode", null),
        if (prefs.store.contains("dark_mode_enabled")) prefs.bool("dark_mode_enabled") else null)
    val seed = prefs.int("accent_color", 0xFF006B5E.toInt())
    var custom by rememberSaveable { mutableStateOf(false) }
    var hex by rememberSaveable(seed) { mutableStateOf("%06X".format(seed and 0xFFFFFF)) }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionTitle(text(R.string.appearance_mode))
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            ThemeMode.entries.forEachIndexed { index, option ->
                SegmentedButton(selected = mode == option,
                    onClick = { prefs.put("theme_mode", option.key) },
                    shape = SegmentedButtonDefaults.itemShape(index, 3)) {
                    Text(text(when (option) {
                        ThemeMode.SYSTEM -> R.string.appearance_system
                        ThemeMode.LIGHT -> R.string.appearance_light
                        ThemeMode.DARK -> R.string.appearance_dark
                    }), style = MaterialTheme.typography.labelMedium)
                }
            }
        }
        if (Build.VERSION.SDK_INT >= 31) ToggleRow(text(R.string.ui_dynamic_color),
            text(R.string.ui_dynamic_desc), prefs.bool("dynamic_color")) { prefs.put("dynamic_color", it) }
        SectionTitle(text(R.string.appearance_accent))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(2.dp)) {
            val colors = listOf(0xFF006B5E, 0xFF3568A8, 0xFF7651A8, 0xFFB34470, 0xFF986000, 0xFF56752B, 0xFF00AFA5, 0xFF0F52BA)
            items(colors.size) { index ->
                val value = colors[index].toInt()
                val label = text(when (index) { 6 -> R.string.appearance_turquoise; 7 -> R.string.appearance_sapphire; else -> R.string.appearance_accent }) + " #" + "%06X".format(value and 0xFFFFFF)
                Surface(onClick = { prefs.put("accent_color", value); prefs.put("dynamic_color", false) },
                    shape = CircleShape, color = Color(value),
                    border = if (seed == value && !prefs.bool("dynamic_color")) androidx.compose.foundation.BorderStroke(3.dp, MaterialTheme.colorScheme.onSurface) else null,
                    modifier = Modifier.size(48.dp).semantics { contentDescription = label }) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(if (seed == value && !prefs.bool("dynamic_color")) "✓" else "", color = Color.White,
                            modifier = Modifier,
                            style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
        OutlinedButton(onClick = { custom = true }, modifier = Modifier.fillMaxWidth()) {
            DrawIcon(R.drawable.ic_color_swatch, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp)); Text(text(R.string.appearance_custom))
        }
    }
    if (custom) AlertDialog(onDismissRequest = { custom = false },
        title = { Text(text(R.string.appearance_custom)) },
        text = { Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            val color = hex.removePrefix("#").takeIf { it.matches(Regex("[0-9a-fA-F]{6}")) }?.toLongOrNull(16)
            val hsv = FloatArray(3)
            android.graphics.Color.colorToHSV(((color ?: (seed.toLong() and 0xFFFFFF)) or 0xFF000000).toInt(), hsv)
            fun update(channel: Int, value: Float) {
                hsv[channel] = value
                hex = "%06X".format(android.graphics.Color.HSVToColor(hsv) and 0xFFFFFF)
            }
            Text(text(R.string.appearance_hue), style = MaterialTheme.typography.labelLarge)
            Slider(hsv[0], { update(0, it) }, valueRange = 0f..360f)
            Text(text(R.string.appearance_saturation), style = MaterialTheme.typography.labelLarge)
            Slider(hsv[1], { update(1, it) }, valueRange = 0f..1f)
            Text(text(R.string.appearance_brightness), style = MaterialTheme.typography.labelLarge)
            Slider(hsv[2], { update(2, it) }, valueRange = 0f..1f)
            Box(Modifier.fillMaxWidth().height(40.dp).background(Color((color ?: (seed.toLong() and 0xFFFFFF)) or 0xFF000000), CircleShape))
            OutlinedTextField(hex, { hex = it.take(7) }, singleLine = true,
                label = { Text(text(R.string.appearance_hex)) },
                isError = color == null, modifier = Modifier.fillMaxWidth())
        } },
        confirmButton = { TextButton(enabled = hex.removePrefix("#").matches(Regex("[0-9a-fA-F]{6}")), onClick = {
            prefs.put("accent_color", (hex.removePrefix("#").toLong(16) or 0xFF000000).toInt())
            prefs.put("dynamic_color", false); custom = false
        }) { Text(text(R.string.btn_apply)) } },
        dismissButton = { TextButton(onClick = { custom = false }) { Text(text(R.string.dialog_cancel)) } })
}
