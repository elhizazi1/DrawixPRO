package xyz.siwane.drawix.pro

import android.graphics.*
import kotlin.math.*

// الممحاة تعود كأداة رسم مستقلة
enum class DrawMode { BRUSH, RECT, CIRCLE, ARROW, ERASER, TEXT, MOVE, LASER, NONE }

@Suppress("DEPRECATION")
data class DrawShape(
    val type: DrawMode,
    val path: Path,
    val paint: Paint,
    var startX: Float,
    var startY: Float,
    var endX: Float,
    var endY: Float,
    var text: String = "",
    var scale: Float = 1.0f,
    var rotation: Float = 0f 
) {
    fun moveBy(dx: Float, dy: Float) {
        startX += dx
        endX += dx
        startY += dy
        endY += dy
        if (type == DrawMode.BRUSH || type == DrawMode.ERASER || type == DrawMode.LASER) {
            path.offset(dx, dy)
        }
    }

    fun getCenterX(): Float {
        return when (type) {
            DrawMode.BRUSH, DrawMode.ERASER, DrawMode.LASER -> { 
                val b = RectF()
                path.computeBounds(b, true)
                b.centerX() 
            }
            DrawMode.TEXT -> startX + (paint.measureText(text) / 2)
            DrawMode.CIRCLE -> startX 
            else -> (startX + endX) / 2
        }
    }

    fun getCenterY(): Float {
        return when (type) {
            DrawMode.BRUSH, DrawMode.ERASER, DrawMode.LASER -> { 
                val b = RectF()
                path.computeBounds(b, true)
                b.centerY() 
            }
            DrawMode.TEXT -> startY - (paint.textSize / 2)
            DrawMode.CIRCLE -> startY 
            else -> (startY + endY) / 2
        }
    }

    fun contains(x: Float, y: Float): Boolean {
        val cx = getCenterX()
        val cy = getCenterY()
        
        val angleRad = Math.toRadians(-rotation.toDouble())
        val cosA = cos(angleRad).toFloat()
        val sinA = sin(angleRad).toFloat()
        
        val unRotX = cx + (x - cx) * cosA - (y - cy) * sinA
        val unRotY = cy + (x - cx) * sinA + (y - cy) * cosA

        val unscaledX = cx + (unRotX - cx) / scale
        val unscaledY = cy + (unRotY - cy) / scale
        val padding = 100f 

        return when (type) {
            DrawMode.BRUSH, DrawMode.ERASER, DrawMode.LASER -> {
                val b = RectF()
                path.computeBounds(b, true)
                unscaledX in (b.left - padding)..(b.right + padding) && 
                unscaledY in (b.top - padding)..(b.bottom + padding)
            }
            DrawMode.TEXT -> {
                val w = paint.measureText(text)
                unscaledX in (startX - padding)..(startX + w + padding) && 
                unscaledY in (startY - paint.textSize - padding)..(startY + padding)
            }
            DrawMode.CIRCLE -> {
                val radius = hypot((endX - startX).toDouble(), (endY - startY).toDouble()).toFloat()
                hypot((unscaledX - startX).toDouble(), (unscaledY - startY).toDouble()).toFloat() <= (radius + padding)
            }
            else -> {
                val left = startX.coerceAtMost(endX)
                val right = startX.coerceAtLeast(endX)
                val top = startY.coerceAtMost(endY)
                val bottom = startY.coerceAtLeast(endY)
                unscaledX in (left - padding)..(right + padding) && 
                unscaledY in (top - padding)..(bottom + padding)
            }
        }
    }
}

