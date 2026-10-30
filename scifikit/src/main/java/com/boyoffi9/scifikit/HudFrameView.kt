package com.boyoffi9.scifikit

import android.content.Context
import android.graphics.Canvas
import android.os.SystemClock
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.core.content.withStyledAttributes
import com.boyoffi9.scifikit.core.HudFramePainter
import com.boyoffi9.scifikit.core.animationsEnabled
import com.boyoffi9.scifikit.core.withAlpha

/**
 * A FrameLayout with a chamfered sci-fi HUD frame drawn behind its children.
 * Use padding to keep content clear of the border and glow.
 */
class HudFrameView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {

    private val painter = HudFramePainter()

    var accentColor: Int
        get() = painter.accentColor
        set(value) {
            painter.accentColor = value
            invalidate()
        }

    var fillColor: Int
        get() = painter.fillColor
        set(value) {
            painter.fillColor = value
            invalidate()
        }

    var scanlines: Boolean
        get() = painter.scanlines
        set(value) {
            painter.scanlines = value
            invalidate()
        }

    var sweep: Boolean
        get() = painter.sweep
        set(value) {
            painter.sweep = value
            invalidate()
        }

    init {
        setWillNotDraw(false)
        val density = context.resources.displayMetrics.density
        painter.chamferPx = 14f * density
        painter.strokePx = 1.5f * density
        painter.bracketPx = 20f * density
        painter.scanlineGapPx = 3f * density

        context.withStyledAttributes(attrs, R.styleable.HudFrameView, defStyleAttr, 0) {
            val accent = getColor(R.styleable.HudFrameView_sfAccentColor, painter.accentColor)
            painter.accentColor = accent
            painter.fillColor = getColor(R.styleable.HudFrameView_sfFillColor, withAlpha(accent, 0.08f))
            painter.chamferPx = getDimension(R.styleable.HudFrameView_sfChamfer, painter.chamferPx)
            painter.strokePx = getDimension(R.styleable.HudFrameView_sfStrokeWidth, painter.strokePx)
            painter.bracketPx = getDimension(R.styleable.HudFrameView_sfBracketLength, painter.bracketPx)
            painter.scanlines = getBoolean(R.styleable.HudFrameView_sfScanlines, true)
            painter.sweep = getBoolean(R.styleable.HudFrameView_sfSweep, true)
        }
    }

    override fun dispatchDraw(canvas: Canvas) {
        painter.animate = animationsEnabled()
        painter.draw(canvas, width.toFloat(), height.toFloat(), SystemClock.uptimeMillis())
        super.dispatchDraw(canvas)
        if (painter.animate && (painter.sweep || painter.scanlines)) {
            postInvalidateOnAnimation()
        }
    }
}
