package com.boyoffi9.scifikit.core

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PathMeasure
import kotlin.math.max
import kotlin.math.min

/**
 * Draws a chamfered sci-fi panel: cut corners (top-left and bottom-right),
 * faked neon glow, scanlines, corner brackets and a light segment that
 * travels around the border.
 *
 * Pure Canvas code. Both the View and the Compose wrapper call this, so they
 * render identically. No bitmaps, no blur, no hardware-layer tricks.
 */
class HudFramePainter {

    var accentColor: Int = 0xFF00E5FF.toInt()
    var fillColor: Int = 0x1400E5FF
    var chamferPx: Float = 28f
    var strokePx: Float = 3f
    var bracketPx: Float = 44f
    var scanlineGapPx: Float = 6f
    var scanlines: Boolean = true
    var sweep: Boolean = true
    var animate: Boolean = true

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.MITER
        strokeCap = Paint.Cap.SQUARE
    }
    private val framePath = Path()
    private val segmentPath = Path()
    private val bracketPath = Path()
    private val measure = PathMeasure()

    /** Space the glow needs around the frame; keep children inside this. */
    val insetPx: Float get() = strokePx * 2f

    fun draw(canvas: Canvas, width: Float, height: Float, nowMs: Long) {
        val inset = insetPx
        val l = inset
        val t = inset
        val r = width - inset
        val b = height - inset
        if (r <= l || b <= t) return

        val c = min(chamferPx, min(r - l, b - t) / 2f)
        framePath.reset()
        framePath.moveTo(l + c, t)
        framePath.lineTo(r, t)
        framePath.lineTo(r, b - c)
        framePath.lineTo(r - c, b)
        framePath.lineTo(l, b)
        framePath.lineTo(l, t + c)
        framePath.close()

        // Body
        fillPaint.color = fillColor
        canvas.drawPath(framePath, fillPaint)

        // Scanlines, clipped to the panel shape
        if (scanlines && scanlineGapPx > 1f) {
            val gap = scanlineGapPx
            val offset = if (animate) (nowMs / 40f) % gap else 0f
            strokePaint.strokeWidth = max(1f, strokePx / 3f)
            strokePaint.color = withAlpha(accentColor, 0.10f)
            canvas.save()
            canvas.clipPath(framePath)
            var y = t + offset
            while (y < b) {
                canvas.drawLine(l, y, r, y, strokePaint)
                y += gap
            }
            canvas.restore()
        }

        // Neon border: two soft wide strokes fake the glow, then the crisp line
        strokePaint.strokeWidth = strokePx * 4f
        strokePaint.color = withAlpha(accentColor, 0.08f)
        canvas.drawPath(framePath, strokePaint)
        strokePaint.strokeWidth = strokePx * 2.2f
        strokePaint.color = withAlpha(accentColor, 0.18f)
        canvas.drawPath(framePath, strokePaint)
        strokePaint.strokeWidth = strokePx
        strokePaint.color = withAlpha(accentColor, 0.9f)
        canvas.drawPath(framePath, strokePaint)

        // Brackets on the two square corners (top-right, bottom-left)
        val bl = min(bracketPx, min(r - l, b - t) / 3f)
        if (bl > 0f) {
            bracketPath.reset()
            bracketPath.moveTo(r - bl, t)
            bracketPath.lineTo(r, t)
            bracketPath.lineTo(r, t + bl)
            bracketPath.moveTo(l + bl, b)
            bracketPath.lineTo(l, b)
            bracketPath.lineTo(l, b - bl)
            strokePaint.strokeWidth = strokePx * 2f
            strokePaint.color = mixColors(accentColor, 0xFFFFFFFF.toInt(), 0.55f)
            canvas.drawPath(bracketPath, strokePaint)
        }

        // Light segment travelling along the border
        if (sweep && animate) {
            measure.setPath(framePath, true)
            val len = measure.length
            if (len > 0f) {
                val segLen = len * 0.14f
                val start = ((nowMs % SWEEP_PERIOD_MS) / SWEEP_PERIOD_MS.toFloat()) * len
                val end = start + segLen
                strokePaint.strokeWidth = strokePx * 1.6f
                strokePaint.color = mixColors(accentColor, 0xFFFFFFFF.toInt(), 0.8f)
                segmentPath.reset()
                if (end <= len) {
                    measure.getSegment(start, end, segmentPath, true)
                    canvas.drawPath(segmentPath, strokePaint)
                } else {
                    measure.getSegment(start, len, segmentPath, true)
                    canvas.drawPath(segmentPath, strokePaint)
                    segmentPath.reset()
                    measure.getSegment(0f, end - len, segmentPath, true)
                    canvas.drawPath(segmentPath, strokePaint)
                }
            }
        }
    }

    private companion object {
        const val SWEEP_PERIOD_MS = 3200L
    }
}
