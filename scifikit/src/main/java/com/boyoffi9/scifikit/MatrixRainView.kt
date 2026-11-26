package com.boyoffi9.scifikit

import android.content.Context
import android.graphics.Canvas
import android.os.SystemClock
import android.util.AttributeSet
import android.view.View
import androidx.core.content.withStyledAttributes
import com.boyoffi9.scifikit.core.MatrixRainPainter
import com.boyoffi9.scifikit.core.animationsEnabled

/**
 * Digital rain wall. Fill any area: columns of glyphs fall at their own
 * speeds with a bright leading character and a fading tail.
 */
class MatrixRainView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    private val painter = MatrixRainPainter()
    private var ticking = false

    var accentColor: Int
        get() = painter.accentColor
        set(value) {
            painter.accentColor = value
            invalidate()
        }

    var glyphSize: Float
        get() = painter.glyphSizePx
        set(value) {
            painter.glyphSizePx = value
            invalidate()
        }

    var rainDensity: Float
        get() = painter.density
        set(value) {
            painter.density = value.coerceIn(0.2f, 1.5f)
            invalidate()
        }

    var flicker: Boolean
        get() = painter.flicker
        set(value) {
            painter.flicker = value
            invalidate()
        }

    init {
        val density = context.resources.displayMetrics.density
        painter.glyphSizePx = 14f * density
        painter.accentColor = context.sciFiPalette().accent
        painter.animate = animationsEnabled()

        context.withStyledAttributes(attrs, R.styleable.MatrixRainView, defStyleAttr, 0) {
            painter.accentColor = getColor(R.styleable.MatrixRainView_sfAccentColor, painter.accentColor)
            painter.glyphSizePx = getDimension(R.styleable.MatrixRainView_sfGlyphSize, painter.glyphSizePx)
            painter.density = getFloat(R.styleable.MatrixRainView_sfDensity, painter.density)
            painter.flicker = getBoolean(R.styleable.MatrixRainView_sfFlicker, painter.flicker)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        painter.draw(canvas, width.toFloat(), height.toFloat(), SystemClock.uptimeMillis())
        if (painter.isAnimating() && !ticking) {
            ticking = true
            postInvalidateOnAnimation()
        } else if (!painter.isAnimating()) {
            ticking = false
        }
    }

    override fun onDetachedFromWindow() {
        ticking = false
        super.onDetachedFromWindow()
    }
}
