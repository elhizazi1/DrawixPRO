package xyz.siwane.drawix.pro.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable fun PreferenceSlider(title: String, value: Float, range: ClosedFloatingPointRange<Float>, unit: String, shape: RoundedCornerShape = xyz.siwane.drawix.pro.ui.components.CardCorners.single, onChange: (Float) -> Unit) {
    var draft by remember(value) { mutableFloatStateOf(value.coerceIn(range)) }
    xyz.siwane.drawix.pro.ui.components.GlassSurface(shape = shape, color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(Modifier.padding(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(if (unit == "%") "${(draft * 100).toInt()}%" else "${draft.toInt()} $unit", color = MaterialTheme.colorScheme.primary)
            Slider(draft, { draft = it }, valueRange = range, onValueChangeFinished = { onChange(draft) })
        }
    }
}
