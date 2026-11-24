package xyz.siwane.drawix.pro.overlay

object FloatingButtonPlacement {
    /** Anchor toward the nearest physical screen edge (independent of RTL). */
    fun animationAnchor(left: Int, top: Int, width: Int, height: Int,
        screenWidth: Int, screenHeight: Int): Pair<Float, Float> {
        val distances = listOf(left, screenWidth - left - width, top, screenHeight - top - height)
        return when (distances.indices.minByOrNull { distances[it] } ?: 0) {
            0 -> 0f to .5f
            1 -> 1f to .5f
            2 -> .5f to 0f
            else -> .5f to 1f
        }
    }

    fun edge(x: Int, screenWidth: Int, buttonWidth: Int, margin: Int): Int {
        val max = (screenWidth - buttonWidth).coerceAtLeast(0)
        val inset = margin.coerceIn(0, max / 2)
        return if (x < max / 2) inset else max - inset
    }
    fun y(fraction: Float, screenHeight: Int, buttonHeight: Int) =
        ((screenHeight - buttonHeight).coerceAtLeast(0) * fraction.coerceIn(0f, 1f)).toInt()
}
