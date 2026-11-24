package xyz.siwane.drawix.pro.ui.layout

/** Window width selects spacing; Android density and accessibility font scale are untouched. */
data class LayoutSpec(val padding: Int, val cardPadding: Int, val gap: Int, val artwork: Int, val title: Int) {
    companion object {
        fun forWidth(widthDp: Int) = when {
            widthDp < 360 -> LayoutSpec(12, 10, 8, 56, 26)
            widthDp < 600 -> LayoutSpec(16, 12, 10, 64, 28)
            else -> LayoutSpec(24, 16, 12, 80, 32)
        }
    }
}
