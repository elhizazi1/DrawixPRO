package xyz.siwane.drawix.pro.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.dp

/** Only the soft page artwork is sampled by cards: there is no recursive capture of card content. */
@Stable class GlassScene(val layer: GraphicsLayer) {
    var origin by mutableStateOf(Offset.Zero)
}
val LocalGlassScene = staticCompositionLocalOf<GlassScene?> { null }

@Composable fun LiquidBackdrop(scene: GlassScene, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val dark = colors.surface.luminance() < .5f
    Canvas(modifier.fillMaxSize().onGloballyPositioned { scene.origin = it.positionInRoot() }
        .drawWithContent {
            scene.layer.record { this@drawWithContent.drawContent() }
            drawLayer(scene.layer)
        }) {
        drawRect(colors.surface)
        // Broad color transitions support the glass without decorative dots or circles.
        drawRect(Brush.linearGradient(colorStops = arrayOf(
            0f to colors.surface,
            .22f to lerp(colors.surface, colors.primary, if (dark) .14f else .13f),
            .58f to colors.surface,
            .82f to lerp(colors.surface, colors.primary, if (dark) .10f else .09f),
            1f to colors.surface), start = Offset.Zero, end = Offset(size.width, size.height)))
    }
}

/** Tinted glass with a sampled soft backdrop, curved highlights and an inner reflection. No elevation shadow. */
@Composable fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = CardCorners.single,
    color: Color = MaterialTheme.colorScheme.surfaceContainer,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val scene = LocalGlassScene.current
    var origin by remember { mutableStateOf(Offset.Zero) }
    val dark = MaterialTheme.colorScheme.surface.luminance() < .5f
    val line = MaterialTheme.colorScheme.outlineVariant
    val glass = modifier.onGloballyPositioned { origin = it.positionInRoot() }.clip(shape).drawWithContent {
        scene?.let { translate(it.origin.x - origin.x, it.origin.y - origin.y) { drawLayer(it.layer) } }
        // Retain each card's own hue; highlights add depth without whitening the whole interface.
        drawRect(color.copy(alpha = if (dark) .72f else .56f))
        drawRect(Brush.linearGradient(colorStops = arrayOf(
            0f to Color.White.copy(alpha = if (dark) .12f else .56f),
            .17f to Color.White.copy(alpha = if (dark) .035f else .12f),
            .34f to Color.Transparent,
            .78f to color.copy(alpha = .18f),
            1f to Color.White.copy(alpha = if (dark) .045f else .24f)),
            start = Offset.Zero, end = Offset(size.width * .7f, size.height)))
        val outline = shape.createOutline(size, layoutDirection, this)
        drawOutline(outline, Brush.linearGradient(listOf(Color.White.copy(alpha = if (dark) .35f else .98f),
            line.copy(alpha = if (dark) .5f else .72f), Color.White.copy(alpha = if (dark) .14f else .82f))),
            style = Stroke(2.dp.toPx()))
        val edge = 2.dp.toPx()
        inset(edge) {
            drawOutline(shape.createOutline(size, layoutDirection, this),
                color = line.copy(alpha = if (dark) .15f else .25f), style = Stroke(.7.dp.toPx()))
        }
        drawContent()
    }
    if (onClick == null) Surface(modifier = glass, shape = shape, color = Color.Transparent,
        contentColor = contentColor, content = content)
    else Surface(onClick = onClick, modifier = glass, shape = shape, color = Color.Transparent,
        contentColor = contentColor, content = content)
}
