package com.boyoffi9.scifikit.core

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Painter for HUD instrument dials: a cut-corner instrument frame, a butt-cap
 * arc with an angular leading edge, notch ticks crossing the frame, and a
 * bracketed digital readout. Cockpit hardware, not a fitness donut.
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
    private val framePath = Path()
    private val headPath = Path()
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

        val color = if (p >= warning) warningColor else accentColor

        // Instrument frame: cut corners on the top-left and bottom-right, like
        // the card/button silhouette. The dial lives inside it. Insets scale
        // with the view, not the stroke, so small/high-density views still draw.
        val l = s
        val t = s
        val r = width - l
        val b = height - t
        if (r - l < s * 6f || b - t < s * 6f) return
        val cut = min(r - l, b - t) * 0.10f
        framePath.reset()
        framePath.moveTo(l + cut, t)
        framePath.lineTo(r, t)
        framePath.lineTo(r, b - cut)
        framePath.lineTo(r - cut, b)
        framePath.lineTo(l, b)
        framePath.lineTo(l, t + cut)
        framePath.close()

        framePaint.strokeWidth = s * 4f
        framePaint.color = withAlpha(color, 0.06f * glow)
        canvas.drawPath(framePath, framePaint)
        framePaint.strokeWidth = s
        framePaint.color = withAlpha(color, 0.55f * glow)
        canvas.drawPath(framePath, framePaint)

        // Corner brackets on the uncut corners (top-right, bottom-left).
        val bl = min(cut * 1.6f, s * 7f)
        framePaint.color = withAlpha(color, 0.9f * glow)
        // top-right bracket
        canvas.drawLine(r - bl, t + s, r - s, t + s, framePaint)
        canvas.drawLine(r - s, t + s, r - s, t + bl, framePaint)
        // bottom-left bracket
        canvas.drawLine(l + s, b - bl, l + s, b - s, framePaint)
        canvas.drawLine(l + s, b - s, l + bl, b - s, framePaint)

        // Dial arc, inset from the frame with room for the readout row.
        val cx = width / 2f
        val dialTop = t + s
        val cy = dialTop + (b - dialTop) / 2f
        val maxR = min(r - l, b - dialTop) / 2f - s * 1.5f
        val rad = maxR - s / 2f
        if (rad <= 0f) return

        val sweep = sweepDegrees.coerceIn(30f, 360f)
        val gapDeg = if (sweep < 360f) (360f - sweep) else 0f
        val start = if (sweep < 360f) startDegrees - gapDeg / 2f else startDegrees
        arc.set(cx - rad, cy - rad, cx + rad, cy + rad)

        // Track arc: hard-edged, no rounded caps.
        trackPaint.strokeWidth = s
        trackPaint.color = withAlpha(trackColor, 0.30f)
        canvas.drawArc(arc, start, sweep, false, trackPaint)

        // Fill arc with a bright leading-edge marker: a chevron wedge at the
        // arc head, squared off like an instrument needle.
        if (show > 0f) {
            fillPaint.strokeWidth = s
            fillPaint.color = withAlpha(color, 0.9f * glow)
            canvas.drawArc(arc, start, sweep * show, false, fillPaint)
            fillPaint.strokeWidth = s * 2.2f
            fillPaint.color = withAlpha(color, 0.10f * glow)
            canvas.drawArc(arc, start, sweep * show, false, fillPaint)

            val headDeg = Math.toRadians((start + sweep * show).toDouble())
            val hx = cx + rad * Math.cos(headDeg).toFloat()
            val hy = cy + rad * Math.sin(headDeg).toFloat()
            val hw = s * 1.6f
            headPath.reset()
            headPath.moveTo(hx + hw * cos(headDeg + Math.PI / 2).toFloat(), hy + hw * sin(headDeg + Math.PI / 2).toFloat())
            headPath.lineTo(hx + hw * 2.2f * cos(headDeg).toFloat(), hy + hw * 2.2f * sin(headDeg).toFloat())
            headPath.lineTo(hx + hw * cos(headDeg - Math.PI / 2).toFloat(), hy + hw * sin(headDeg - Math.PI / 2).toFloat())
            headPath.close()
            fillPaint.style = Paint.Style.FILL
            fillPaint.color = withAlpha(color, glow)
            canvas.drawPath(headPath, fillPaint)
            fillPaint.style = Paint.Style.STROKE
        }

        // Major ticks crossing the track: notch marks, not decoration dots.
        if (ticks > 1) {
            tickPaint.strokeWidth = s * 0.7f
            tickPaint.color = withAlpha(trackColor, 0.75f)
            val inner = rad - s * 1.8f
            val outer = rad + s * 1.4f
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

        // Center readout: digital value flanked by bracket marks, unit row under.
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
        if (label.isNotEmpty()) {
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
