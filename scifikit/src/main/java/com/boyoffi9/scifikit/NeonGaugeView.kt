package com.boyoffi9.scifikit

import android.content.Context
import android.graphics.Canvas
import android.os.SystemClock
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.core.content.withStyledAttributes
import com.boyoffi9.scifikit.core.NeonGaugePainter
import com.boyoffi9.scifikit.core.NeonGaugePainter.Readout
import com.boyoffi9.scifikit.core.animationsEnabled

class NeonGaugeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {

    private val painter = NeonGaugePainter()
    private var ticking = false

    var progress: Float
        get() = painter.progress
        set(value) { painter.progress = value.coerceIn(0f, 1f); invalidate() }

    var value: Float
        get() = painter.value
        set(value) { painter.value = value; invalidate() }

    var unit: String
        get() = painter.unit
        set(value) { painter.unit = value; invalidate() }

    init {
        setWillNotDraw(false)
        val density = context.resources.displayMetrics.density
        painter.strokePx = 8f * density
        painter.textSizePx = 34f * density

        val palette = context.sciFiPalette()
        painter.accentColor = palette.accent
        painter.warningColor = palette.secondary
        painter.trackColor = palette.dim
        painter.textColor = palette.text
        painter.glow = palette.glow

        context.withStyledAttributes(attrs, R.styleable.NeonGaugeView, defStyleAttr, 0) {
            painter.progress = getFloat(R.styleable.NeonGaugeView_sfGaugeValue, 0f).coerceIn(0f, 1f)
            painter.value = getFloat(R.styleable.NeonGaugeView_sfGaugeValue2, painter.progress * 100f)
            painter.unit = getString(R.styleable.NeonGaugeView_sfUnit) ?: ""
            painter.readout = when (getInt(R.styleable.NeonGaugeView_sfReadout, 0)) {
                1 -> Readout.Value
                2 -> Readout.None
                else -> Readout.Percent
            }
            painter.sweepDegrees = getFloat(R.styleable.NeonGaugeView_sfGaugeSweep, painter.sweepDegrees)
            painter.ticks = getInt(R.styleable.NeonGaugeView_sfGaugeTicks, painter.ticks)
            painter.warning = getFloat(R.styleable.NeonGaugeView_sfGaugeWarningAt, painter.warning)
            painter.accentColor = getColor(R.styleable.NeonGaugeView_sfAccentColor, painter.accentColor)
        }
        painter.animate = animationsEnabled()
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
