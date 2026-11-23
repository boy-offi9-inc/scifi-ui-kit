package com.boyoffi9.scifikit

import android.content.Context
import android.graphics.Canvas
import android.os.SystemClock
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.core.content.withStyledAttributes
import com.boyoffi9.scifikit.core.NeonProgressPainter
import com.boyoffi9.scifikit.core.animationsEnabled

class NeonProgressView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {

    private val painter = NeonProgressPainter()
    private var ticking = false

    var label: String
        get() = painter.label
        set(value) { painter.label = value; invalidate() }

    var progress: Float
        get() = painter.progress
        set(value) { painter.progress = value.coerceIn(0f, 1f); invalidate() }

    var accentColor: Int
        get() = painter.accentColor
        set(value) { painter.accentColor = value; invalidate() }

    var warningColor: Int
        get() = painter.warningColor
        set(value) { painter.warningColor = value; invalidate() }

    init {
        setWillNotDraw(false)
        val density = context.resources.displayMetrics.density
        painter.strokePx = 1.5f * density
        painter.textSizePx = 12f * density

        val palette = context.sciFiPalette()
        painter.accentColor = palette.accent
        painter.warningColor = palette.secondary
        painter.trackColor = palette.dim
        painter.labelColor = palette.text
        painter.glow = palette.glow

        context.withStyledAttributes(attrs, R.styleable.NeonProgressView, defStyleAttr, 0) {
            painter.label = getString(R.styleable.NeonProgressView_sfLabel) ?: ""
            painter.progress = getFloat(R.styleable.NeonProgressView_sfProgress, 0f).coerceIn(0f, 1f)
            painter.warning = getFloat(R.styleable.NeonProgressView_sfWarningAt, painter.warning)
            painter.ticks = getInt(R.styleable.NeonProgressView_sfTicks, painter.ticks)
            painter.showPercent = getBoolean(R.styleable.NeonProgressView_sfShowPercent, true)
            painter.segmented = getBoolean(R.styleable.NeonProgressView_sfProgressSegmented, false)
            painter.accentColor = getColor(R.styleable.NeonProgressView_sfAccentColor, painter.accentColor)
            painter.warningColor = getColor(R.styleable.NeonProgressView_sfWarningColor, painter.warningColor)
        }
        painter.animate = animationsEnabled()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = resolveSize(suggestedMinimumWidth, widthMeasureSpec)
        val textH = if (painter.showPercent || painter.label.isNotEmpty()) {
            painter.textHeightPx() + painter.strokePx * 3f
        } else 0f
        val h = textH + painter.barHeightPx + paddingTop + paddingBottom
        setMeasuredDimension(w, resolveSize(h.toInt(), heightMeasureSpec))
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
}
