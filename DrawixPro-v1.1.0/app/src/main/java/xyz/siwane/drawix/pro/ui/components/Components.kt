package xyz.siwane.drawix.pro.ui.components

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import xyz.siwane.drawix.pro.TranslationEngine
import xyz.siwane.drawix.pro.ui.layout.LocalLayoutSpec
import xyz.siwane.drawix.pro.ui.theme.LocalAccents
import androidx.compose.ui.graphics.Color

@Composable fun text(@StringRes id: Int): String {
    val context = LocalContext.current
    return TranslationEngine.getCustomString(context, id, androidx.compose.ui.res.stringResource(id))
}
@Composable fun DrawIcon(@DrawableRes icon: Int, description: String? = null, modifier: Modifier = Modifier.size(24.dp)) {
    Icon(painterResource(icon), description, modifier)
}
@Composable fun SectionTitle(title: String) {
    Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 8.dp, top = 12.dp, bottom = 4.dp))
}
@Composable fun InfoCard(title: String, description: String, icon: Int, tone: Color = LocalAccents.current.mint, shape: RoundedCornerShape = CardCorners.single) {
    GlassSurface(modifier = Modifier.fillMaxWidth(), shape = shape, color = tone) {
        Row(Modifier.fillMaxWidth().padding(LocalLayoutSpec.current.cardPadding.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            DrawIcon(icon)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(description, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
@Composable fun ActionRow(title: String, subtitle: String? = null, icon: Int? = null, tone: Color = LocalAccents.current.sky, shape: RoundedCornerShape = CardCorners.single, onClick: () -> Unit) {
    GlassSurface(onClick = onClick, shape = shape, color = tone.copy(alpha = .55f)) {
        Row(Modifier.fillMaxWidth().padding(LocalLayoutSpec.current.cardPadding.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (icon != null) Surface(shape = CircleShape, color = tone) { DrawIcon(icon, modifier = Modifier.padding(8.dp).size(20.dp)) }
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
@Composable fun ToggleRow(title: String, subtitle: String, checked: Boolean, shape: RoundedCornerShape = CardCorners.single, onCheckedChange: (Boolean) -> Unit) {
    GlassSurface(shape = shape, color = MaterialTheme.colorScheme.surfaceContainer) {
        Row(Modifier.fillMaxWidth().padding(LocalLayoutSpec.current.cardPadding.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked, onCheckedChange)
        }
    }
}
