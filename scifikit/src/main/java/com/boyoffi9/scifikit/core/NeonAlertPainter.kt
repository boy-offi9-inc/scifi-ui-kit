package com.boyoffi9.scifikit.core

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min

/**
 * Painter for HUD notification blocks: cut-corner frame with corner brackets,
 * a severity glyph column on the leading edge, scanline texture and a marching
 * hazard edge. A cockpit advisory, not a rounded toast.
 */
class NeonAlertPainter {

    enum class Severity { Info, Success, Warning, Danger }

    var text: String = ""
    var severity: Severity = Severity.Info
    var accentColor: Int = 0xFF00E5FF.toInt()
    var successColor: Int = 0xFF3BFFA0.toInt()
    var warningColor: Int = 0xFFFFB020.toInt()
    var dangerColor: Int = 0xFFFF2D6F.toInt()
    var labelColor: Int = 0xFFE8FDFF.toInt()
    var glow: Float = 1f
    var strokePx: Float = 2f
    var textSizePx: Float = 30f
    var filled: Boolean = true
    var icon: Boolean = true          // leading glyph in the severity column
    var chevrons: Boolean = false     // trailing >> marks
    var hazard: Float = 0f            // 0 off, >0 marching hazard strip along the top
    var scanlines: Boolean = true     // faint CRT texture across the body
    var dangerPulse: Boolean = true   // danger alerts breathe
    var animate: Boolean = true

    private val framePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.MITER
        strokeCap = Paint.Cap.SQUARE
    }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        letterSpacing = 0.16f
    }
    private val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val chevronPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.SQUARE
    }
    private val hazardPaint = Paint().apply { style = Paint.Style.FILL }
    private val clipPath = Path()
    private val iconPath = Path()

    fun severityColor(): Int = when (severity) {
        Severity.Info -> accentColor
        Severity.Success -> successColor
        Severity.Warning -> warningColor
        Severity.Danger -> dangerColor
    }

    fun textWidthPx(): Float {
        textPaint.textSize = textSizePx
        return textPaint.measureText(text.uppercase())
    }

    fun isAnimating(): Boolean = animate && (hazard > 0f || (severity == Severity.Danger && dangerPulse))

    fun draw(canvas: Canvas, width: Float, height: Float, nowMs: Long) {
        val s = strokePx
        val color = severityColor()
        val hz = min(hazard, height * 0.3f)
        val stripTop = if (hz > 0f) hz else 0f
        val innerTop = stripTop + s

        val l = s
        val t = innerTop
        val r = width - s
        val b = height - s
        if (r - l < s * 24f || b - t < s * 10f) return

        // Frame: one cut corner (top-left, opposite the glyph column) plus a
        // smaller cut on the bottom-right; square corners elsewhere.
        val cutTL = min(height * 0.35f, s * 9f)
        val cutBR = cutTL * 0.5f
        clipPath.reset()
        clipPath.moveTo(l + cutTL, t)
        clipPath.lineTo(r, t)
        clipPath.lineTo(r, b - cutBR)
        clipPath.lineTo(r - cutBR, b)
        clipPath.lineTo(l, b)
        clipPath.lineTo(l, t)
        clipPath.close()

        // Soft glow backing under the whole shape.
        framePaint.strokeWidth = s * 5f
        framePaint.color = withAlpha(color, 0.10f * glow)
        canvas.drawPath(clipPath, framePaint)

        if (filled) {
            fillPaint.color = withAlpha(color, 0.13f * glow)
            canvas.drawPath(clipPath, fillPaint)
        }
        if (scanlines) {
            val step = max(s * 2.4f, 3f)
            fillPaint.color = withAlpha(0xFF000000.toInt(), 0.22f)
            canvas.save()
            canvas.clipPath(clipPath)
            var y = t + step
            while (y < b) {
                canvas.drawRect(l, y, r, y + step * 0.4f, fillPaint)
                y += step
            }
            canvas.restore()
        }
        framePaint.strokeWidth = s
        framePaint.color = withAlpha(color, 0.85f * glow)
        canvas.drawPath(clipPath, framePaint)

        // Corner brackets on the square corners (top-right, bottom-left).
        val bl = min(cutTL * 1.4f, s * 8f)
        framePaint.strokeWidth = s * 1.4f
        framePaint.color = withAlpha(color, 0.9f * glow)
        canvas.drawLine(r - bl, t + s * 0.5f, r - s * 0.5f, t + s * 0.5f, framePaint)
        canvas.drawLine(r - s * 0.5f, t + s * 0.5f, r - s * 0.5f, t + bl, framePaint)
        canvas.drawLine(l + s * 0.5f, b - bl, l + s * 0.5f, b - s * 0.5f, framePaint)
        canvas.drawLine(l + s * 0.5f, b - s * 0.5f, l + bl, b - s * 0.5f, framePaint)
        framePaint.strokeWidth = s

        // Severity glyph column on the leading edge: a vertical slot with the
        // severity mark inside, separated from the label by a tick line.
        val colW = min(height * 0.22f, s * 7f)
        fillPaint.color = withAlpha(color, 0.16f * glow)
        canvas.drawRect(l, t, l + colW, b, fillPaint)
        fillPaint.color = withAlpha(color, 0.9f * glow)
        canvas.drawRect(l + colW, t, l + colW + s * 0.6f, b, fillPaint)

        // Pulsing double border for danger alerts.
        if (severity == Severity.Danger && dangerPulse && animate) {
            val wave = 0.5f - 0.5f * cos(2.0 * Math.PI * (nowMs % DANGER_PERIOD_MS) / DANGER_PERIOD_MS.toDouble()).toFloat()
            framePaint.strokeWidth = s * 2.2f
            framePaint.color = withAlpha(color, (0.25f + 0.45f * wave) * glow)
            canvas.drawPath(clipPath, framePaint)
            framePaint.strokeWidth = s
        }
        // Marching hazard strip along the top edge.
        if (hz > 0f && animate) {
            val period = s * 8f
            val offset = (nowMs * 0.03f) % period
            canvas.save()
            canvas.clipRect(l, 0f, r, hz)
            hazardPaint.color = withAlpha(color, 0.55f * glow)
            var x = -period + offset
            while (x < width) {
                canvas.drawRect(x, 0f, x + period / 2f, hz, hazardPaint)
                x += period
            }
            hazardPaint.color = withAlpha(color, 0.10f * glow)
            canvas.drawRect(l, 0f, r, hz, hazardPaint)
            canvas.restore()
        } else if (hz > 0f) {
            hazardPaint.color = withAlpha(color, 0.30f * glow)
            canvas.drawRect(l, 0f, r, hz, hazardPaint)
        }

        // Leading glyph inside the column: triangle (danger/warning), diamond
        // (info), check (success).
        textPaint.textSize = textSizePx
        val fm = textPaint.fontMetrics
        val textH = fm.descent - fm.ascent
        val cy = (t + b) / 2f
        val baseline = cy + textH / 2f - fm.descent
        val colCenter = l + colW / 2f
        if (icon) {
            val gSize = min(colW, textH) * 0.34f
            iconPath.reset()
            when (severity) {
                Severity.Danger, Severity.Warning -> {
                    iconPath.moveTo(colCenter, cy - gSize)
                    iconPath.lineTo(colCenter + gSize * 1.8f, cy + gSize)
                    iconPath.lineTo(colCenter - gSize * 1.8f, cy + gSize)
                    iconPath.close()
                }
                Severity.Success -> {
                    chevronPaint.strokeWidth = s
                    chevronPaint.color = withAlpha(color, glow)
                    canvas.drawLine(colCenter - gSize, cy, colCenter - gSize * 0.1f, cy + gSize, chevronPaint)
                    canvas.drawLine(colCenter - gSize * 0.1f, cy + gSize, colCenter + gSize * 1.4f, cy - gSize, chevronPaint)
                }
                Severity.Info -> {
                    iconPath.moveTo(colCenter, cy - gSize)
                    iconPath.lineTo(colCenter + gSize, cy)
                    iconPath.lineTo(colCenter, cy + gSize)
                    iconPath.lineTo(colCenter - gSize, cy)
                    iconPath.close()
                }
            }
            if (!iconPath.isEmpty) {
                iconPaint.color = withAlpha(color, glow)
                canvas.drawPath(iconPath, iconPaint)
            }
        }

        // Label, indented past the glyph column with a small gap.
        var textX = l + colW + s * 0.6f + textH * 0.55f
        textPaint.textAlign = Paint.Align.LEFT
        textPaint.color = labelColor
        canvas.drawText(text.uppercase(), textX, baseline, textPaint)

        // Trailing chevrons, drawn only when the label leaves room for them.
        if (chevrons) {
            val cw = textH * 0.34f
            val chevronsW = cw * 3.3f
            if (textX + textPaint.measureText(text.uppercase()) + chevronsW < r) {
                chevronPaint.strokeWidth = s
                chevronPaint.color = withAlpha(color, 0.8f * glow)
                var x = r - cw * 2.2f
                for (i in 0 until 2) {
                    canvas.drawLine(x, cy - textH * 0.22f, x + cw, cy, chevronPaint)
                    canvas.drawLine(x + cw, cy, x, cy + textH * 0.22f, chevronPaint)
                    x += cw * 1.3f
                }
            }
        }
    }

    private companion object {
        const val DANGER_PERIOD_MS = 1600L
    }
}
