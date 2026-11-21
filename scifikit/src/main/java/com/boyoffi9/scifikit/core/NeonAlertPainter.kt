package com.boyoffi9.scifikit.core

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import kotlin.math.min

/**
 * Painter for alert banners and strips: "WARNING", "DANGER", "SCAN COMPLETE",
 * "SIGNAL STABLE". Severity drives the colour; optional leading icon triangle,
 * trailing status chevrons, and a marching hazard edge.
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
    var icon: Boolean = true          // leading triangle icon
    var chevrons: Boolean = false     // trailing >> marks
    var hazard: Float = 0f            // 0 off, >0 marching hazard strip along the top
    var animate: Boolean = true

    private val framePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
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

    fun isAnimating(): Boolean = animate && hazard > 0f

    fun draw(canvas: Canvas, width: Float, height: Float, nowMs: Long) {
        val s = strokePx
        val color = severityColor()
        val hz = min(hazard, height * 0.3f)
        val stripTop = if (hz > 0f) hz else 0f
        val innerTop = stripTop + s

        // Frame: cut-corner strip (notches on the left edge).
        val notch = min(height / 3f, s * 6f)
        clipPath.reset()
        clipPath.moveTo(s, innerTop)
        clipPath.lineTo(s + notch, innerTop)
        clipPath.lineTo(s + notch * 2f, height - s)
        clipPath.lineTo(width - s, height - s)
        clipPath.lineTo(width - s, innerTop)
        clipPath.close()

        if (filled) {
            fillPaint.color = withAlpha(color, 0.13f * glow)
            canvas.drawPath(clipPath, fillPaint)
        }
        framePaint.strokeWidth = s
        framePaint.color = withAlpha(color, 0.85f * glow)
        canvas.drawPath(clipPath, framePaint)

        // Marching hazard strip along the top edge.
        if (hz > 0f && animate) {
            val period = s * 8f
            val offset = (nowMs * 0.03f) % period
            canvas.save()
            canvas.clipRect(s, 0f, width - s, hz)
            hazardPaint.color = withAlpha(color, 0.55f * glow)
            var x = -period + offset
            while (x < width) {
                canvas.drawRect(x, 0f, x + period / 2f, hz, hazardPaint)
                x += period
            }
            hazardPaint.color = withAlpha(color, 0.10f * glow)
            canvas.drawRect(s, 0f, width - s, hz, hazardPaint)
            canvas.restore()
        } else if (hz > 0f) {
            hazardPaint.color = withAlpha(color, 0.30f * glow)
            canvas.drawRect(s, 0f, width - s, hz, hazardPaint)
        }

        // Leading icon: triangle (danger/warning), diamond (info), check (success).
        textPaint.textSize = textSizePx
        val fm = textPaint.fontMetrics
        val textH = fm.descent - fm.ascent
        val cy = (innerTop + height) / 2f
        val baseline = cy + textH / 2f - fm.descent
        var textX = s + notch * 2f + textH * 0.55f
        if (icon) {
            val r = textH * 0.30f
            val ix = textX - textH * 0.55f
            iconPath.reset()
            when (severity) {
                Severity.Danger, Severity.Warning -> {
                    iconPath.moveTo(ix, cy - r)
                    iconPath.lineTo(ix + r * 1.8f, cy + r)
                    iconPath.lineTo(ix - r * 1.8f, cy + r)
                    iconPath.close()
                }
                Severity.Success -> {
                    chevronPaint.strokeWidth = s
                    chevronPaint.color = withAlpha(color, glow)
                    canvas.drawLine(ix - r, cy, ix - r * 0.1f, cy + r, chevronPaint)
                    canvas.drawLine(ix - r * 0.1f, cy + r, ix + r * 1.4f, cy - r, chevronPaint)
                }
                Severity.Info -> {
                    iconPath.moveTo(ix, cy - r)
                    iconPath.lineTo(ix + r, cy)
                    iconPath.lineTo(ix, cy + r)
                    iconPath.lineTo(ix - r, cy)
                    iconPath.close()
                }
            }
            if (!iconPath.isEmpty) {
                iconPaint.color = withAlpha(color, glow)
                canvas.drawPath(iconPath, iconPaint)
            }
        }

        // Label.
        textPaint.textAlign = Paint.Align.LEFT
        textPaint.color = labelColor
        canvas.drawText(text.uppercase(), textX, baseline, textPaint)

        // Trailing chevrons.
        if (chevrons) {
            chevronPaint.strokeWidth = s
            chevronPaint.color = withAlpha(color, 0.8f * glow)
            val cw = textH * 0.34f
            var x = width - s - cw * 2.2f
            for (i in 0 until 2) {
                canvas.drawLine(x, cy - textH * 0.22f, x + cw, cy, chevronPaint)
                canvas.drawLine(x + cw, cy, x, cy + textH * 0.22f, chevronPaint)
                x += cw * 1.3f
            }
        }
    }
}
