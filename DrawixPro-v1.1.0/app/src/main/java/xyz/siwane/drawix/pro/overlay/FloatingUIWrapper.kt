package xyz.siwane.drawix.pro.overlay

import android.content.Context
import android.view.View
import android.widget.FrameLayout

class FloatingUIWrapper(context: Context, val childView: View, val isCenterPivot: Boolean = false) : FrameLayout(context) {
    
    var targetScale: Float = 1.0f
        set(value) {
            field = value
            childView.scaleX = value
            childView.scaleY = value
            requestLayout()
        }

    var minimumTouchSizePx: Int = 0
    var baseWidth: Int = 0
    var baseHeight: Int = 0
    private var baseMeasured = false
    private var anchorX = 1f
    private var anchorY = 0f

    /** Physical coordinates: animation anchors must not mirror with the app language. */
    fun setAnimationAnchor(horizontal: Float, vertical: Float) {
        anchorX = horizontal.coerceIn(0f, 1f)
        anchorY = vertical.coerceIn(0f, 1f)
        if (width > 0 && height > 0) positionChild()
        requestLayout()
    }

    init { 
        clipChildren = false
        clipToPadding = false
        addView(childView) 
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val unconstrained = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
        childView.measure(unconstrained, unconstrained)
        
        val w = childView.measuredWidth
        val h = childView.measuredHeight
        
        if (!baseMeasured && w > 0 && h > 0) {
            baseWidth = w
            baseHeight = h
            baseMeasured = true
        }

        setMeasuredDimension(
            (w * targetScale).toInt().coerceAtLeast(minimumTouchSizePx),
            (h * targetScale).toInt().coerceAtLeast(minimumTouchSizePx)
        )
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        positionChild()
    }

    private fun positionChild() {
        val w = childView.measuredWidth
        val h = childView.measuredHeight
        val scaledW = measuredWidth
        val scaledH = measuredHeight

        if (isCenterPivot) {
            val offsetLeft = (scaledW - w) / 2
            val offsetTop = scaledH - h 
            
            childView.layout(offsetLeft, offsetTop, offsetLeft + w, offsetTop + h)
            
            childView.pivotX = w / 2f
            childView.pivotY = h.toFloat() 
        } else {
            val pivotX = w * anchorX
            val pivotY = h * anchorY
            val offsetLeft = ((scaledW - w * targetScale) / 2 - pivotX * (1f - targetScale)).toInt()
            val offsetTop = ((scaledH - h * targetScale) / 2 - pivotY * (1f - targetScale)).toInt()
            childView.layout(offsetLeft, offsetTop, offsetLeft + w, offsetTop + h)
            childView.pivotX = pivotX
            childView.pivotY = pivotY
        }
    }
}

