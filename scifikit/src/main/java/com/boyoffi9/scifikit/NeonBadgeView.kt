package com.boyoffi9.scifikit

import android.content.Context
import android.graphics.Canvas
import android.os.SystemClock
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.core.content.withStyledAttributes
import com.boyoffi9.scifikit.core.NeonBadgePainter
import com.boyoffi9.scifikit.core.NeonBadgePainter.State
import com.boyoffi9.scifikit.core.animationsEnabled

class NeonBadgeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {

    private val painter = NeonBadgePainter()
    private var ticking = false

    var text: String
        get() = painter.text
        set(value) { painter.text = value; invalidate() }

    var state: State
        get() = painter.state
        set(value) { painter.state = value; invalidate() }

    init {
        setWillNotDraw(false)
        val density = context.resources.displayMetrics.density
        painter.strokePx = 1.5f * density
        painter.textSizePx = 11f * density

        val palette = context.sciFiPalette()
        painter.accentColor = palette.accent
        painter.warningColor = palette.secondary
        painter.dangerColor = palette.danger
        painter.labelColor = palette.text
        painter.dimColor = palette.dim
        painter.glow = palette.glow

        context.withStyledAttributes(attrs, R.styleable.NeonBadgeView, defStyleAttr, 0) {
            painter.text = getString(R.styleable.NeonBadgeView_sfText) ?: ""
            painter.state = State.entries[
                getInt(R.styleable.NeonBadgeView_sfState, State.Normal.ordinal)
            ]
            painter.filled = getBoolean(R.styleable.NeonBadgeView_sfFilled, false)
            painter.dot = getBoolean(R.styleable.NeonBadgeView_sfDot, false)
            painter.accentColor = getColor(R.styleable.NeonBadgeView_sfAccentColor, painter.accentColor)
            painter.warningColor = getColor(R.styleable.NeonBadgeView_sfWarningColor, painter.warningColor)
            painter.dangerColor = getColor(R.styleable.NeonBadgeView_sfDangerColor, painter.dangerColor)
        }
        painter.animate = animationsEnabled()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(
            resolveSize(measuredWidth, widthMeasureSpec),
            resolveSize(painter.preferredHeightPx().toInt() + paddingTop + paddingBottom, heightMeasureSpec),
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        painter.draw(canvas, width.toFloat(), height.toFloat(), SystemClock.uptimeMillis())
        val animating = painter.isAnimating()
        if (animating && !ticking) {
            ticking = true
            postInvalidateOnAnimation()
        } else if (!animating) {
            ticking = false
        }
    }
}
