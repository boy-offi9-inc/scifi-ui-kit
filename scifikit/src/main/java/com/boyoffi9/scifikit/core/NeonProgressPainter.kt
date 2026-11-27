package com.boyoffi9.scifikit.core

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sin

/**
 * Painter for labeled HUD data bars ("SYS. CORE LOAD 84.2%").
 *
 * A bracketed terminal-style track: square end brackets, an angular segmented
 * fill with notch ticks, a monospace label on the left and a readout on the
 * right. No rounded track, no soft caps.
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

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.SQUARE
    }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val segPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val corePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.BUTT
    }
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

        // Track: square bracket ends + top/bottom rails. The brackets read as
        // [ ... ] terminals, not rounded caps.
        trackPaint.strokeWidth = s
        trackPaint.color = withAlpha(trackColor, 0.45f)
        val bracket = min(barH * 0.45f, s * 5f)
        canvas.drawLine(barLeft + bracket, barTop, barRight - bracket, barTop, trackPaint)
        canvas.drawLine(barLeft + bracket, barBottom, barRight - bracket, barBottom, trackPaint)
        // Left bracket [
        canvas.drawLine(barLeft, barTop, barLeft, barBottom, trackPaint)
        canvas.drawLine(barLeft, barTop, barLeft + bracket, barTop, trackPaint)
        canvas.drawLine(barLeft, barBottom, barLeft + bracket, barBottom, trackPaint)
        // Right bracket ]
        canvas.drawLine(barRight, barTop, barRight, barBottom, trackPaint)
        canvas.drawLine(barRight - bracket, barTop, barRight, barTop, trackPaint)
        canvas.drawLine(barRight - bracket, barBottom, barRight, barBottom, trackPaint)

        // Fill: angular slab between the brackets, hard edges, with brighter
        // segment bands and a chamfered leading edge pushing into the track.
        val innerL = barLeft + bracket
        val innerR = barRight - bracket
        if (show > 0f) {
            val fillRight = innerL + (innerR - innerL) * show
            val chamfer = min(barH * 0.3f, s * 3f)
            shapePath.reset()
            shapePath.moveTo(innerL, barTop + s)
            shapePath.lineTo(fillRight - chamfer, barTop + s)
            shapePath.lineTo(fillRight, midY)
            shapePath.lineTo(fillRight - chamfer, barBottom - s)
            shapePath.lineTo(innerL, barBottom - s)
            shapePath.close()
            // Layered neon: a soft halo pass around the whole fill, then the
            // slab, then a bright filament down the middle (0.14/4px + core,
            // same layered-glow recipe as the rest of the kit).
            fillPaint.style = Paint.Style.STROKE
            fillPaint.strokeWidth = s * 4f
            fillPaint.color = withAlpha(color, 0.14f * glow * warnPulse)
            canvas.drawPath(shapePath, fillPaint)
            fillPaint.style = Paint.Style.FILL
            fillPaint.color = withAlpha(color, 0.34f * glow * warnPulse)
            canvas.drawPath(shapePath, fillPaint)
            corePaint.strokeWidth = s * 0.9f
            corePaint.color = withAlpha(color, 0.95f * glow * warnPulse)
            canvas.drawLine(innerL, midY, fillRight - chamfer * 0.6f, midY, corePaint)
            if (segmented) {
                drawSegments(canvas, innerL, innerR, barTop + s, barBottom - s, show, color, nowMs, warnPulse)
            } else if (animate) {
                // Angled bands give the fill a metered look; the most recent
                // band is brightest so the bar reads as charging forward.
                val band = s * 9f
                val phase = (nowMs * 0.05f) % band
                var x = innerL - band + phase
                while (x < fillRight - chamfer) {
                    val fresh = (fillRight - chamfer - x) < band * 3f
                    segPaint.color = withAlpha(color, (if (fresh) 0.30f else 0.20f) * glow * warnPulse)
                    canvas.drawRect(x, barTop + s, min(x + band * 0.45f, fillRight - chamfer), barBottom - s, segPaint)
                    x += band
                }
            }
            // Hot leading edge along the chamfer, with its own glow.
            fillPaint.style = Paint.Style.STROKE
            fillPaint.strokeWidth = s * 2.5f
            fillPaint.color = withAlpha(color, 0.18f * glow * warnPulse)
            shapePath.reset()
            shapePath.moveTo(fillRight - chamfer, barTop + s)
            shapePath.lineTo(fillRight, midY)
            shapePath.lineTo(fillRight - chamfer, barBottom - s)
            canvas.drawPath(shapePath, fillPaint)
            fillPaint.style = Paint.Style.FILL
            fillPaint.color = withAlpha(color, 0.95f * glow * warnPulse)
            shapePath.reset()
            shapePath.moveTo(fillRight - chamfer, barTop + s)
            shapePath.lineTo(fillRight, midY)
            shapePath.lineTo(fillRight - chamfer, barBottom - s)
            shapePath.close()
            canvas.drawPath(shapePath, fillPaint)
        }

        // Tick marks along the track: small notches hanging from the top rail.
        if (ticks > 0) {
            tickPaint.strokeWidth = s * 0.8f
            tickPaint.color = withAlpha(trackColor, 0.5f)
            val span = innerR - innerL
            for (i in 1 until ticks) {
                val x = innerL + span * i / ticks
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
            val a = if (isHead) (0.55f + 0.45f * headPulse) else 0.85f
            // Soft glow behind each lit cell so the LED bank reads as neon.
            segPaint.color = withAlpha(color, 0.16f * glow * pulse)
            canvas.drawRect(x - gap, top - s, min(x + segW + gap, right) + gap, bottom + s, segPaint)
            segPaint.color = withAlpha(color, a * glow * pulse)
            canvas.drawRect(x, top, min(x + segW, right), bottom, segPaint)
        }
    }

    private companion object {
        const val WARN_PERIOD_MS = 1200L
        const val HEAD_PERIOD_MS = 700L
    }
}
