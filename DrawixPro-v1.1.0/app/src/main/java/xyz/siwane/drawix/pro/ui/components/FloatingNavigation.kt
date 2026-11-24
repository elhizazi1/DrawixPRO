package xyz.siwane.drawix.pro.ui.components

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.*
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.*
import xyz.siwane.drawix.pro.ui.Destination

/** Samples the page layer, never the dock itself. API 24–30 retain clear tinted glass. */
@Composable fun FloatingNavigation(selected: Destination, backdrop: GraphicsLayer,
    backdropOrigin: Offset, onSelect: (Destination) -> Unit) {
    var origin by remember { mutableStateOf(Offset.Zero) }
    val frost = rememberGraphicsLayer()
    val tint = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = if (Build.VERSION.SDK_INT >= 31) .62f else .88f)
    val dark = MaterialTheme.colorScheme.surface.luminance() < .5f
    val shape = RoundedCornerShape(32.dp)
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val index = Destination.entries.indexOf(selected).let { if (rtl) 3 - it else it }
    val indicator by animateDpAsState((15 + index * 58).dp, spring(dampingRatio = .8f, stiffness = 450f), label = "dock slide")
    Box(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp), contentAlignment = Alignment.Center) {
        Box(Modifier.width(246.dp).height(60.dp).onGloballyPositioned { origin = it.positionInRoot() }
            .clip(shape).drawWithContent {
                if (Build.VERSION.SDK_INT >= 31) {
                    frost.renderEffect = BlurEffect(22.dp.toPx(), 22.dp.toPx(), TileMode.Clamp)
                    frost.record {
                        translate(backdropOrigin.x - origin.x, backdropOrigin.y - origin.y) { drawLayer(backdrop) }
                    }
                    drawLayer(frost)
                }
                drawRect(tint)
                drawRect(Brush.linearGradient(listOf(Color.White.copy(alpha = if (dark) .08f else .3f),
                    Color.Transparent), Offset.Zero, Offset(size.width * .5f, size.height)))
                drawContent()
            }.border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .5f), shape)) {
            Box(Modifier.align(AbsoluteAlignment.TopLeft).absoluteOffset(x = indicator)                .padding(top = 9.dp).size(42.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
            Row(Modifier.selectableGroup().padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Destination.entries.forEach { item ->
                    val active = item == selected
                    val color by animateColorAsState(if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant, label = "dock icon")
                    val label = text(item.label)
                    Box(Modifier.size(48.dp).clip(RoundedCornerShape(24.dp)).selectable(active, role = Role.Tab, onClick = { onSelect(item) })
                        .semantics { contentDescription = label }, contentAlignment = Alignment.Center) {
                        CompositionLocalProvider(LocalContentColor provides color) { DrawIcon(item.icon, modifier = Modifier.size(22.dp)) }
                    }
                }
            }
        }
    }
}
