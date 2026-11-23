package com.boyoffi9.scifikit

import android.content.Context
import android.graphics.Canvas
import android.os.SystemClock
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.core.content.withStyledAttributes
import com.boyoffi9.scifikit.core.NeonAlertPainter
import com.boyoffi9.scifikit.core.NeonAlertPainter.Severity
import com.boyoffi9.scifikit.core.animationsEnabled

class NeonAlertView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {

    private val painter = NeonAlertPainter()
    private var ticking = false

    var text: String
        get() = painter.text
        set(value) { painter.text = value; invalidate() }

    var severity: Severity
        get() = painter.severity
        set(value) { painter.severity = value; invalidate() }

    init {
        setWillNotDraw(false)
        val density = context.resources.displayMetrics.density
        painter.strokePx = 1.5f * density
        painter.textSizePx = 13f * density

        val palette = context.sciFiPalette()
        painter.accentColor = palette.accent
        painter.dangerColor = palette.danger
        painter.labelColor = palette.text
        painter.glow = palette.glow

        context.withStyledAttributes(attrs, R.styleable.NeonAlertView, defStyleAttr, 0) {
            painter.text = getString(R.styleable.NeonAlertView_sfAlertText) ?: ""
            painter.severity = Severity.entries[
                getInt(R.styleable.NeonAlertView_sfSeverity, Severity.Info.ordinal)
            ]
            painter.filled = getBoolean(R.styleable.NeonAlertView_sfAlertFilled, true)
            painter.icon = getBoolean(R.styleable.NeonAlertView_sfAlertIcon, true)
            painter.chevrons = getBoolean(R.styleable.NeonAlertView_sfAlertChevrons, false)
            painter.hazard = getFloat(R.styleable.NeonAlertView_sfAlertHazard, 0f)
            painter.accentColor = getColor(R.styleable.NeonAlertView_sfAccentColor, painter.accentColor)
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
