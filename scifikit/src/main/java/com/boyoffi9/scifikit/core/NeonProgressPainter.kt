package com.boyoffi9.scifikit.core

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sin

/**
 * Painter for labeled status/progress bars ("SYS. CORE LOAD 84.2%").
 *
 * Renders one bar: a bracketed track, a glowing fill with tick marks,
 * a monospace label on the left and a percentage readout on the right.
 */
class NeonProgressPainter {

    var label: String = ""
    var progress: Float = 0f          // 0..1, clamped
    var accentColor: Int = 0xFF00E5FF.toInt()
    var warningColor: Int = 0xFFFF2D6F.toInt()
    var trackColor: Int = 0xFF7FB8C4.toInt()
    var labelColor: Int = 0xFFE8FDFF.toInt()
    var glow: Float = 1f
    var strokePx: Float = 2f
    var textSizePx: Float = 30f
    var showPercent: Boolean = true
    var ticks: Int = 8
    var warning: Float = 0.85f        // >= this turns the bar red
    var segmented: Boolean = false    // LED-segment look instead of a continuous slab
    var animate: Boolean = true

    /** Smoothly displayed progress (lags [progress] for animated fills). */
    private var shown: Float = 0f

    val barHeightPx: Float get() = strokePx * 8f

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val segPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val shapePath = Path()
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        letterSpacing = 0.08f
    }

    init { shown = progress }

    fun textHeightPx(): Float {
        textPaint.textSize = textSizePx
        val fm = textPaint.fontMetrics
        return fm.descent - fm.ascent
    }

    fun isAnimating(): Boolean =
        animate && (progress > 0f || progress.coerceIn(0f, 1f) >= warning || (shown - progress).let { it > 0.0005f || it < -0.0005f })

    fun draw(canvas: Canvas, width: Float, height: Float, nowMs: Long) {
        val s = strokePx
        val p = progress.coerceIn(0f, 1f)
        shown = if (animate) shown + (p - shown) * 0.25f else p
        val show = shown.coerceIn(0f, 1f)

        textPaint.textSize = textSizePx
        val fm = textPaint.fontMetrics
        val textH = fm.descent - fm.ascent
        val barH = min(barHeightPx, (height - textH).coerceAtLeast(s * 4f))
        val gap = s * 3f

        val barTop = if (showPercent || label.isNotEmpty()) height - barH else (height - barH) / 2f
        val barLeft = 0f
        val barRight = width
        val barBottom = barTop + barH

        val color = if (p >= warning) warningColor else accentColor
        val midY = barTop + barH / 2f
        // Warning bars breathe in place: the whole fill alpha oscillates so a
        // critical reading catches the eye without changing layout.
        val warnPulse = if (p >= warning && animate) {
            0.72f + 0.28f * sin((nowMs % WARN_PERIOD_MS) / WARN_PERIOD_MS.toDouble() * 2.0 * PI).toFloat()
        } else 1f

        // Track: thin outline bar with angled end caps.
        trackPaint.strokeWidth = s
        trackPaint.color = withAlpha(trackColor, 0.35f)
        val cap = min(barH / 2f, s * 4f)
        canvas.drawLine(barLeft + cap, barTop, barRight - cap, barTop, trackPaint)
        canvas.drawLine(barLeft + cap, barBottom, barRight - cap, barBottom, trackPaint)
        canvas.drawLine(barLeft, midY, barLeft + cap, barTop, trackPaint)
        canvas.drawLine(barLeft, midY, barLeft + cap, barBottom, trackPaint)
        canvas.drawLine(barRight, midY, barRight - cap, barTop, trackPaint)
        canvas.drawLine(barRight, midY, barRight - cap, barBottom, trackPaint)

        // Fill: glowing slab with brighter segment bands and a hot leading edge.
        if (show > 0f) {
            val fillRight = barLeft + (barRight - barLeft) * show
            fillPaint.color = withAlpha(color, 0.16f * glow * warnPulse)
            canvas.drawRect(barLeft + cap, barTop + s, fillRight, barBottom - s, fillPaint)
            if (segmented) {
                drawSegments(canvas, barLeft + cap, barRight - cap, barTop + s, barBottom - s, show, color, nowMs, warnPulse)
            } else if (animate) {
                // Angled bands give the fill a segmented, metered look; the most
                // recent band is brightest so the bar reads as charging forward.
                val band = s * 9f
                val phase = (nowMs * 0.05f) % band
                var x = barLeft + cap - band + phase
                while (x < fillRight) {
                    val fresh = (fillRight - x) < band * 3f
                    segPaint.color = withAlpha(color, (if (fresh) 0.16f else 0.09f) * glow * warnPulse)
                    canvas.drawRect(x, barTop + s, min(x + band * 0.45f, fillRight), barBottom - s, segPaint)
                    x += band
                }
            }
            fillPaint.color = withAlpha(color, 0.85f * glow * warnPulse)
            canvas.drawRect(fillRight - s * 1.2f, barTop + s, fillRight, barBottom - s, fillPaint)
            fillPaint.color = withAlpha(color, warnPulse)
            canvas.drawRect(fillRight - s * 0.5f, barTop + s * 0.5f, fillRight, barBottom - s * 0.5f, fillPaint)
            // Leading edge spark: a small triangle tip pushing into the track.
            segPaint.color = withAlpha(color, glow * warnPulse)
            shapePath.reset()
            shapePath.moveTo(fillRight, barTop)
            shapePath.lineTo(fillRight + s * 2.2f, midY)
            shapePath.lineTo(fillRight, barBottom)
            shapePath.close()
            canvas.drawPath(shapePath, segPaint)
        }

        // Tick marks along the track.
        if (ticks > 0) {
            tickPaint.strokeWidth = s * 0.8f
            tickPaint.color = withAlpha(trackColor, 0.5f)
            val span = barRight - barLeft - 2f * cap
            for (i in 1 until ticks) {
                val x = barLeft + cap + span * i / ticks
                canvas.drawLine(x, barTop + s, x, barTop + barH * 0.3f, tickPaint)
            }
        }

        // Label row above the bar.
        if (showPercent || label.isNotEmpty()) {
            val baseline = barTop - gap
            if (label.isNotEmpty()) {
                textPaint.textAlign = Paint.Align.LEFT
                textPaint.color = labelColor
                canvas.drawText(label.uppercase(), barLeft, baseline, textPaint)
            }
            if (showPercent) {
                textPaint.textAlign = Paint.Align.RIGHT
                textPaint.color = color
                canvas.drawText("%05.1f%%".format(show * 100f), barRight, baseline, textPaint)
            }
        }
    }

    private fun drawSegments(
        canvas: Canvas,
        left: Float,
        right: Float,
        top: Float,
        bottom: Float,
        show: Float,
        color: Int,
        nowMs: Long,
        pulse: Float,
    ) {
        val lit = (show * 20f).toInt().coerceAtLeast(if (show > 0f) 1 else 0)
        val gap = strokePx * 1.6f
        val segW = ((right - left) - gap * 19f) / 20f
        // The newest segment flickers brighter on a short cycle, like an LED
        // bank charging its last cell.
        val headPulse = 0.7f + 0.3f * ((nowMs % HEAD_PERIOD_MS) / HEAD_PERIOD_MS.toFloat())
        for (i in 0 until lit) {
            val x = left + i * (segW + gap)
            val isHead = i == lit - 1
            val a = if (isHead) (0.55f + 0.45f * headPulse) else 0.8f
            segPaint.color = withAlpha(color, a * glow * pulse)
            canvas.drawRect(x, top, min(x + segW, right), bottom, segPaint)
        }
    }

    private companion object {
        const val WARN_PERIOD_MS = 1200L
        const val HEAD_PERIOD_MS = 700L
    }
}
