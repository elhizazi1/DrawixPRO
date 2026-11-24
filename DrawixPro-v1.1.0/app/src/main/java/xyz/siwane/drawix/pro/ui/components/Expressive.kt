package xyz.siwane.drawix.pro.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.asComposePath
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.ColorDrawable
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.animation.core.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import xyz.siwane.drawix.pro.R
import xyz.siwane.drawix.pro.ui.layout.LocalLayoutSpec
import androidx.compose.ui.unit.sp

/** Decorative, non-interactive artwork built from the same palette as the actual UI. */
@Composable fun DrawixBloom(icon: Int, modifier: Modifier = Modifier.size(112.dp), tone: Color = MaterialTheme.colorScheme.primaryContainer) {
    val motion = rememberInfiniteTransition(label = "hero icon")
    val turning = icon == R.drawable.ic_setting_2
    val angle by motion.animateFloat(if (turning) 0f else -18f, if (turning) 360f else 18f,
        infiniteRepeatable(tween(if (turning) 3200 else 850, easing = if (turning) LinearEasing else FastOutSlowInEasing),
            if (turning) RepeatMode.Restart else RepeatMode.Reverse), label = "icon movement")
    val systemMask = remember {
        if (Build.VERSION.SDK_INT >= 26) AdaptiveIconDrawable(ColorDrawable(0), ColorDrawable(0)) else null
    }
    Box(modifier, contentAlignment = Alignment.Center) {
        // Follow the device's adaptive icon mask; do not recreate touch-marker dots.
        Canvas(Modifier.fillMaxSize()) {
            if (Build.VERSION.SDK_INT >= 26 && systemMask != null) {
                systemMask.setBounds(0, 0, size.width.toInt(), size.height.toInt())
                drawPath(systemMask.iconMask.asComposePath(), tone)
            } else {
                drawRoundRect(tone, cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.minDimension * .3f))
            }
        }
        Icon(painterResource(icon), null, tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxSize(.48f).rotate(angle))
    }
}

@Composable fun PageHero(title: String, subtitle: String, icon: Int) {
    val orbit = rememberInfiniteTransition(label = "hero icons")
    val drift by orbit.animateFloat(-9f, 9f, infiniteRepeatable(tween(850, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "hero drift")
    Column(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        BoxWithConstraints(Modifier.fillMaxWidth().height((LocalLayoutSpec.current.artwork + 24).dp), contentAlignment = Alignment.Center) {
            val symbols = listOf(R.drawable.ic_brush_2, R.drawable.ic_text, R.drawable.ic_hashtag,
                R.drawable.ic_color_swatch, R.drawable.ic_pointer, R.drawable.ic_format_circle,
                R.drawable.ic_eraser, R.drawable.ic_rotate_left, R.drawable.ic_vibe,
                R.drawable.ic_code, R.drawable.ic_magicpen, R.drawable.ic_camera)
            val points = listOf(.07f to .13f, .24f to .03f, .74f to .05f, .88f to .16f,
                .02f to .51f, .21f to .45f, .76f to .43f, .94f to .57f,
                .1f to .85f, .29f to .79f, .7f to .86f, .87f to .84f)
            symbols.forEachIndexed { index, symbol ->
                Icon(painterResource(symbol), null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .24f),
                    modifier = Modifier.align(Alignment.TopStart).offset(x = maxWidth * points[index].first, y = maxHeight * points[index].second)
                        .offset(y = (if (index % 2 == 0) drift else -drift).dp)
                        .size(if (index % 3 == 0) 26.dp else 19.dp).rotate(if (index % 2 == 0) -12f + drift else 15f - drift))
            }
            DrawixBloom(icon, Modifier.size(LocalLayoutSpec.current.artwork.dp))
        }
        Text(title, style = MaterialTheme.typography.headlineMedium, fontSize = LocalLayoutSpec.current.title.sp, fontWeight = FontWeight.Bold)
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable fun CategoryCard(title: String, subtitle: String, icon: Int, tone: Color,
    shape: RoundedCornerShape = CardCorners.single, onClick: () -> Unit) {
    GlassSurface(onClick = onClick, color = tone.copy(alpha = .35f), shape = shape) {
        Row(Modifier.fillMaxWidth().padding(LocalLayoutSpec.current.cardPadding.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(color = tone, shape = CircleShape) { DrawIcon(icon, modifier = Modifier.padding(8.dp).size(20.dp)) }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            DrawIcon(R.drawable.ic_arrow_back, modifier = Modifier.size(18.dp).rotate(180f))
        }
    }
}

@Composable fun StatusPill(label: String, active: Boolean = false) {
    Surface(shape = CircleShape, color = if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest) {
        Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
    }
}
