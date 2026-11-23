package com.boyoffi9.scifikit.core

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import kotlin.math.max
import kotlin.math.min

/**
 * Painter for tags/badges/chips: small hex-framed labels
 * ("BETA", "NEW", "ONLINE", "PREMIUM") with state styling.
 */
class NeonBadgePainter {

    enum class State { Normal, Success, Warning, Danger, Muted }

    var text: String = ""
    var state: State = State.Normal
    var accentColor: Int = 0xFF00E5FF.toInt()
    var warningColor: Int = 0xFFFFB020.toInt()
    var dangerColor: Int = 0xFFFF2D6F.toInt()
    var successColor: Int = 0xFF3BFFA0.toInt()
    var labelColor: Int = 0xFFE8FDFF.toInt()
    var dimColor: Int = 0xFF7FB8C4.toInt()
    var glow: Float = 1f
    var strokePx: Float = 2f
    var textSizePx: Float = 28f
    var filled: Boolean = false
    var dot: Boolean = false          // status dot next to the label
    var animate: Boolean = true

    private val framePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        letterSpacing = 0.14f
    }
    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val hexPath = Path()

    fun stateColor(): Int = when (state) {
        State.Normal -> accentColor
        State.Success -> successColor
        State.Warning -> warningColor
        State.Danger -> dangerColor
        State.Muted -> dimColor
    }

    fun textWidthPx(): Float {
        textPaint.textSize = textSizePx
        return textPaint.measureText(text.uppercase())
    }

    fun preferredHeightPx(): Float = textHeightPx() + strokePx * 5f

    private fun textHeightPx(): Float {
        textPaint.textSize = textSizePx
        val fm = textPaint.fontMetrics
        return fm.descent - fm.ascent
    }

    fun isAnimating(): Boolean = animate && dot && state != State.Muted

    fun draw(canvas: Canvas, width: Float, height: Float, nowMs: Long) {
        val s = strokePx
        val color = stateColor()
        textPaint.textSize = textSizePx
        val fm = textPaint.fontMetrics
        val textH = fm.descent - fm.ascent
        val padX = s * 6f
        val contentW = textWidthPx() + (if (dot) textH * 0.9f + padX else 0f)
        val startX = (width - contentW) / 2f
        val baseline = (height + textH) / 2f - fm.descent

        // Flat-top hex frame, stretched to the widget bounds.
        val notch = min(height / 2f, s * 7f)
        hexPath.reset()
        hexPath.moveTo(startX - padX + notch, s)
        hexPath.lineTo(width - startX + padX - notch, s)
        hexPath.lineTo(width - startX + padX, height / 2f)
        hexPath.lineTo(width - startX + padX - notch, height - s)
        hexPath.lineTo(startX - padX + notch, height - s)
        hexPath.lineTo(startX - padX, height / 2f)
        hexPath.close()

        if (filled && state != State.Muted) {
            fillPaint.color = withAlpha(color, 0.16f * glow)
            canvas.drawPath(hexPath, fillPaint)
        }
        // Outer bloom: a few wide, faint strokes behind the frame read as a
        // glow on the dark background, and a MaskFilter would blur the corners.
        if (state != State.Muted && glow > 0f) {
            glowPaint.strokeWidth = s * 7f
            glowPaint.color = withAlpha(color, 0.10f * glow)
            canvas.drawPath(hexPath, glowPaint)
            glowPaint.strokeWidth = s * 4f
            glowPaint.color = withAlpha(color, 0.14f * glow)
            canvas.drawPath(hexPath, glowPaint)
        }
        framePaint.strokeWidth = s
        framePaint.color = withAlpha(color, if (state == State.Muted) 0.4f else 0.85f * glow)
        canvas.drawPath(hexPath, framePaint)

        var textX = width / 2f
        if (dot) {
            val r = textH * 0.22f
            val cx = startX + padX * 0.4f + r
            val cy = height / 2f
            val pulse = if (animate && state != State.Muted) {
                0.6f + 0.4f * ((nowMs % 1200L) / 1200f)
            } else 1f
            dotPaint.color = withAlpha(color, 0.18f * glow * pulse)
            canvas.drawCircle(cx, cy, r * (2.6f + 1.0f * pulse), dotPaint)
            dotPaint.color = withAlpha(color, 0.35f * glow)
            canvas.drawCircle(cx, cy, r * 1.9f, dotPaint)
            dotPaint.color = withAlpha(color, glow)
            canvas.drawCircle(cx, cy, r, dotPaint)
            textX += (textH * 0.9f + padX) / 2f
        }

        textPaint.color = if (state == State.Muted) withAlpha(labelColor, 0.7f) else labelColor
        canvas.drawText(text.uppercase(), textX, baseline, textPaint)
    }
}
