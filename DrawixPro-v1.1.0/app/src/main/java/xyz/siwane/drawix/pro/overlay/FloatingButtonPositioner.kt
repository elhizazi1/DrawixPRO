package xyz.siwane.drawix.pro.overlay

import android.animation.ValueAnimator
import android.content.Context
import android.view.View
import android.view.WindowManager

/** Edge docking and normalized position persist across rotation and density changes. */
class FloatingButtonPositioner(context: Context, private val view: View,
    private val params: WindowManager.LayoutParams, private val bounds: () -> Pair<Int, Int>,
    private val update: () -> Unit) {
    private val prefs = context.getSharedPreferences("DrawSettings", Context.MODE_PRIVATE)
    private val margin = (6 * context.resources.displayMetrics.density).toInt()
    private var animator: ValueAnimator? = null
    fun restore() {
        val (width, height) = bounds()
        val maxX = (width - view.width).coerceAtLeast(0)
        params.x = FloatingButtonPlacement.edge(if (prefs.getBoolean("floating_left", false)) maxX else 0, width, view.width, margin)
        params.y = FloatingButtonPlacement.y(prefs.getFloat("floating_y", .35f), height, view.height)
        update()
    }
    fun dock() {
        cancel()
        val (width, height) = bounds()
        val target = FloatingButtonPlacement.edge(params.x, width, view.width, margin)
        params.y = params.y.coerceIn(0, (height - view.height).coerceAtLeast(0))
        prefs.edit().putBoolean("floating_left", target > (width - view.width) / 2)
            .putFloat("floating_y", params.y.toFloat() / (height - view.height).coerceAtLeast(1)).apply()
        animator = ValueAnimator.ofInt(params.x, target).apply {
            duration = 160
            addUpdateListener { params.x = it.animatedValue as Int; update() }
            start()
        }
    }
    fun cancel() { animator?.cancel(); animator = null }
}
