package com.boyoffi9.scifikit.core

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Painter for HUD instrument dials: the shared HUD chassis (chamfered frame,
 * glow, scanlines, brackets, traveling light), a butt-cap arc with a bright
 * head-cap, ticks on an outside ring, and a bracketed digital readout that
 * lives inside the dial. Cockpit hardware, not a fitness donut.
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
        strokeCap = Paint.Cap.BUTT
    }
    private val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val framePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.MITER
        strokeCap = Paint.Cap.SQUARE
    }
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
    private val panel = HudFramePainter()

    fun isAnimating(): Boolean {
        val p = progress.coerceIn(0f, 1f)
        return animate && shown >= 0f && (shown - p).let { it > 0.0005f || it < -0.0005f }
    }

    fun draw(canvas: Canvas, width: Float, height: Float, nowMs: Long) {
        val s = strokePx
        val p = progress.coerceIn(0f, 1f)
        shown = if (shown < 0f || !animate) p else shown + (p - shown) * 0.2f
        val show = shown.coerceIn(0f, 1f)

        val color = if (p >= warning) warningColor else accentColor

        // Panel: the shared HUD chassis — chamfered frame, layered glow, scanlines,
        // white-mixed corner brackets and the traveling light segment — identical
        // to every other component in the kit. Warning shifts the whole panel.
        panel.accentColor = color
        panel.glow = glow
        panel.chamferPx = s * 5f
        panel.strokePx = s * 0.75f
        panel.bracketPx = s * 7f
        panel.scanlineGapPx = s * 1.5f
        panel.scanlines = true
        panel.sweep = animate
        panel.animate = animate
        panel.draw(canvas, width, height, nowMs)

        // Dial arc, inset inside the panel with room for ticks outside the track
        // and the readout row. The readout lives inside the dial, never on it.
        val inset = panel.insetPx + s
        val l = inset
        val t = inset
        val r = width - l
        val b = height - t
        if (r - l < s * 6f || b - t < s * 6f) return
        val cx = width / 2f
        val cy = t + (b - t) / 2f
        val maxR = min(r - l, b - t) / 2f - s * 2.8f
        val rad = maxR
        if (rad <= 0f) return

        val sweep = sweepDegrees.coerceIn(30f, 360f)
        val gapDeg = if (sweep < 360f) (360f - sweep) else 0f
        val start = if (sweep < 360f) startDegrees - gapDeg / 2f else startDegrees
        arc.set(cx - rad, cy - rad, cx + rad, cy + rad)

        // Track arc: hard-edged, no rounded caps.
        trackPaint.strokeWidth = s
        trackPaint.color = withAlpha(trackColor, 0.30f)
        canvas.drawArc(arc, start, sweep, false, trackPaint)

        // Fill arc with a bright head-cap: a short, brighter segment along the arc
        // at the leading edge. No wedge, no needle — instrument caps, not arrows.
        if (show > 0f) {
            fillPaint.strokeWidth = s
            fillPaint.color = withAlpha(color, 0.9f * glow)
            canvas.drawArc(arc, start, sweep * show, false, fillPaint)
            fillPaint.strokeWidth = s * 2.2f
            fillPaint.color = withAlpha(color, 0.10f * glow)
            canvas.drawArc(arc, start, sweep * show, false, fillPaint)

            val headDeg = start + sweep * show
            val cap = min(9f, sweep / 8f)
            fillPaint.strokeWidth = s * 1.7f
            fillPaint.color = mixColors(color, 0xFFFFFFFF.toInt(), 0.45f)
            canvas.drawArc(arc, headDeg - cap / 2f, cap, false, fillPaint)
        }

        // Major ticks on an outside ring between the track and the panel frame:
        // thin, accent-tinted, never crossing the arc or reaching the readout.
        if (ticks > 1) {
            tickPaint.strokeWidth = s * 0.5f
            tickPaint.color = withAlpha(color, 0.55f * glow)
            val inner = rad + s * 1.1f
            val outer = min(rad + s * 2.3f, min(r - cx, cy - t) - s * 0.4f)
            if (outer > inner) {
                for (i in 0 until ticks) {
                    val angle = Math.toRadians((start + sweep * i / (ticks - 1)).toDouble())
                    val cosA = Math.cos(angle).toFloat()
                    val sinA = Math.sin(angle).toFloat()
                    canvas.drawLine(
                        cx + inner * cosA, cy + inner * sinA,
                        cx + outer * cosA, cy + outer * sinA,
                        tickPaint,
                    )
                }
            }
        }

        // Center readout: digital value flanked by bracket marks, unit row under.
        textPaint.textSize = textSizePx
        val label = when (readout) {
            Readout.Percent -> "%03d".format((show * 100f).toInt())
            Readout.Value -> formatValue(value * show)
            Readout.None -> ""
        }
        // Fit the digits inside the dial: shrink until label + brackets clear the track.
        val fit = (rad - s * 2.2f) * 1.9f / maxOf(1f, label.length * 0.62f)
        textPaint.textSize = minOf(textSizePx, fit)
        textPaint.color = textColor
        val fm = textPaint.fontMetrics
        val textH = fm.descent - fm.ascent
        val unitShift = if (unit.isEmpty()) 0f else textH * 0.22f
        val midY = cy - fm.descent + textH / 2f - unitShift
        if (label.isNotEmpty()) {
            // Faux glow: a soft accent pass under the crisp digits (no blur tricks).
            textPaint.color = withAlpha(color, 0.30f * glow)
            val g = s * 0.6f
            for ((dx, dy) in listOf(0f to -g, 0f to g, -g to 0f, g to 0f)) {
                canvas.drawText(label, cx + dx, midY + dy, textPaint)
            }
            textPaint.color = textColor
            canvas.drawText(label, cx, midY, textPaint)
            // Square brackets flanking the digits.
            val labelW = textPaint.measureText(label)
            val bracketH = textH * 0.62f
            val bracketInset = labelW / 2f + textH * 0.30f
            framePaint.strokeWidth = s * 0.7f
            framePaint.color = withAlpha(color, 0.8f * glow)
            val byTop = midY - textH * 0.42f
            val byBot = midY + fm.descent * 0.9f
            // left bracket [
            canvas.drawLine(cx - bracketInset, byTop, cx - bracketInset, byBot, framePaint)
            canvas.drawLine(cx - bracketInset, byTop, cx - bracketInset + textH * 0.18f, byTop, framePaint)
            canvas.drawLine(cx - bracketInset, byBot, cx - bracketInset + textH * 0.18f, byBot, framePaint)
            // right bracket ]
            canvas.drawLine(cx + bracketInset, byTop, cx + bracketInset, byBot, framePaint)
            canvas.drawLine(cx + bracketInset, byTop, cx + bracketInset - textH * 0.18f, byTop, framePaint)
            canvas.drawLine(cx + bracketInset, byBot, cx + bracketInset - textH * 0.18f, byBot, framePaint)
        }

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
