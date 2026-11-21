package com.boyoffi9.scifikit.core

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import kotlin.math.min

/**
 * Painter for radial gauges: donut percentage, 0..max speedometer-style dial,
 * and in-between arcs. One arc engine, three readouts.
 */
class NeonGaugePainter {

    enum class Readout { Percent, Value, None }

    var progress: Float = 0f          // 0..1, clamped
    var value: Float = 0f             // shown when readout == Value
    var maxValue: Float = 100f
    var unit: String = ""             // "KM/H", "%", "°C"
    var readout: Readout = Readout.Percent
    var sweepDegrees: Float = 300f    // total arc span; 360 = full donut
    var startDegrees: Float = -90f    // arc start (0 = 3 o'clock, -90 = 12)
    var accentColor: Int = 0xFF00E5FF.toInt()
    var warningColor: Int = 0xFFFF2D6F.toInt()
    var trackColor: Int = 0xFF7FB8C4.toInt()
    var textColor: Int = 0xFFE8FDFF.toInt()
    var glow: Float = 1f
    var strokePx: Float = 6f
    var textSizePx: Float = 40f
    var ticks: Int = 0                // major ticks along the arc (speedo style)
    var warning: Float = 0.85f
    var animate: Boolean = true

    /** Smoothly displayed progress (lags [progress] when animating). */
    private var shown: Float = -1f

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.BUTT
    }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        letterSpacing = 0.05f
    }
    private val unitPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.MONOSPACE
        letterSpacing = 0.12f
    }
    private val arc = RectF()

    fun isAnimating(): Boolean {
        val p = progress.coerceIn(0f, 1f)
        return animate && shown >= 0f && (shown - p).let { it > 0.0005f || it < -0.0005f }
    }

    fun draw(canvas: Canvas, width: Float, height: Float, nowMs: Long) {
        val s = strokePx
        val p = progress.coerceIn(0f, 1f)
        shown = if (shown < 0f || !animate) p else shown + (p - shown) * 0.2f
        val show = shown.coerceIn(0f, 1f)

        val cx = width / 2f
        val cy = height / 2f
        val maxR = min(width, height) / 2f - s
        val r = maxR - s / 2f

        val sweep = sweepDegrees.coerceIn(30f, 360f)
        val gapDeg = if (sweep < 360f) (360f - sweep) else 0f
        val start = if (sweep < 360f) startDegrees - gapDeg / 2f else startDegrees
        val color = if (p >= warning) warningColor else accentColor

        // Track arc.
        trackPaint.strokeWidth = s
        trackPaint.color = withAlpha(trackColor, 0.3f)
        arc.set(cx - r, cy - r, cx + r, cy + r)
        canvas.drawArc(arc, start, sweep, false, trackPaint)

        // Fill arc: eased sweep, rounded tip.
        if (show > 0f) {
            fillPaint.strokeWidth = s
            fillPaint.color = withAlpha(color, 0.9f * glow)
            canvas.drawArc(arc, start, sweep * show, false, fillPaint)
            // Soft outer glow pass.
            fillPaint.strokeWidth = s * 2.2f
            fillPaint.color = withAlpha(color, 0.10f * glow)
            canvas.drawArc(arc, start, sweep * show, false, fillPaint)
        }

        // Major ticks along the arc.
        if (ticks > 1) {
            tickPaint.strokeWidth = s * 0.7f
            tickPaint.color = withAlpha(trackColor, 0.7f)
            val inner = r - s * 1.6f
            val outer = r - s * 0.7f
            for (i in 0 until ticks) {
                val angle = Math.toRadians((start + sweep * i / (ticks - 1)).toDouble())
                val cos = Math.cos(angle).toFloat()
                val sin = Math.sin(angle).toFloat()
                canvas.drawLine(
                    cx + inner * cos, cy + inner * sin,
                    cx + outer * cos, cy + outer * sin,
                    tickPaint,
                )
            }
        }

        // Center readout: big value, small unit under it.
        textPaint.textSize = textSizePx
        textPaint.color = textColor
        val label = when (readout) {
            Readout.Percent -> "%03d".format((show * 100f).toInt())
            Readout.Value -> formatValue(value * show)
            Readout.None -> ""
        }
        val fm = textPaint.fontMetrics
        val textH = fm.descent - fm.ascent
        val unitShift = if (unit.isEmpty()) 0f else textH * 0.22f
        val midY = cy - fm.descent + textH / 2f - unitShift
        if (label.isNotEmpty()) canvas.drawText(label, cx, midY, textPaint)

        if (unit.isNotEmpty()) {
            unitPaint.textSize = textSizePx * 0.34f
            unitPaint.color = withAlpha(textColor, 0.75f)
            canvas.drawText(unit.uppercase(), cx, midY + textH * 0.52f, unitPaint)
        }
    }

    private fun formatValue(v: Float): String =
        if (v >= 1000f || v == v.toLong().toFloat()) "%04d".format(v.toInt())
        else "%04.1f".format(v)
}
