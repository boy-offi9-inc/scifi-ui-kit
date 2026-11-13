package com.boyoffi9.scifikit.core

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import kotlin.math.max

class NeonButtonPainter {

    var text: String = ""
    var textSizePx: Float = 42f
    var accentColor: Int = 0xFF00E5FF.toInt()
    var labelColor: Int = 0xFFE8FDFF.toInt()
    var onAccentColor: Int = 0xFF05080C.toInt()
    var glow: Float = 1f
    var strokePx: Float = 3f
    var config: NeonConfig = NeonStyle.Outline.config
    var enabled: Boolean = true
    var focused: Boolean = false
    var animate: Boolean = true

    val glowPadPx: Float get() = strokePx * 4f

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        letterSpacing = 0.1f
    }
    private val metrics = Paint.FontMetrics()

    private var pressed = false
    private var pressStartMs = Long.MIN_VALUE / 4

    fun textWidthPx(): Float {
        textPaint.textSize = textSizePx
        return textPaint.measureText(text)
    }

    fun press(nowMs: Long) {
        pressed = true
        pressStartMs = nowMs
    }

    fun release() {
        pressed = false
    }

    fun isAnimating(nowMs: Long): Boolean =
        animate && enabled && (config.pulse > 0f || pressed || surgeLevel(config, nowMs - pressStartMs) > 0f)

    fun draw(canvas: Canvas, width: Float, height: Float, nowMs: Long) {
        val pad = glowPadPx
        val l = pad
        val t = pad
        val r = width - pad
        val b = height - pad
        if (r <= l || b <= t) return

        val cfg = config
        val level = if (animate && enabled) surgeLevel(cfg, nowMs - pressStartMs) else 0f
        val held = if (pressed && enabled) HOLD_LIFT else 0f
        val lift = max(level, held) * cfg.surge
        val flick = if (animate && enabled) flickerAlpha(cfg, level, (nowMs / FLICKER_STEP_MS).toInt()) else 1f
        val pulse = if (animate && enabled) pulseFactor(cfg, nowMs) else 1f
        val dim = if (enabled) 1f else DISABLED_ALPHA
        val glowK = if (enabled) cfg.glow * glow * pulse * (1f + lift * 1.5f) else 0f
        val radius = cfg.corner.coerceIn(0f, 0.5f) * (b - t)

        val fillAlpha = (cfg.fill + if (pressed && enabled) 0.12f else 0f).coerceAtMost(1f)
        if (fillAlpha > 0f) {
            fillPaint.color = withAlpha(mixColors(accentColor, 0xFFFFFFFF.toInt(), lift * 0.3f), fillAlpha * dim)
            canvas.drawRoundRect(l, t, r, b, radius, radius, fillPaint)
        }

        if (cfg.underline) {
            drawUnderline(canvas, l, r, b - strokePx, glowK, dim * flick)
        } else if (glowK > 0f) {
            linePaint.strokeWidth = strokePx * 4f
            linePaint.color = withAlpha(accentColor, 0.08f * glowK)
            canvas.drawRoundRect(l, t, r, b, radius, radius, linePaint)
            linePaint.strokeWidth = strokePx * 2.2f
            linePaint.color = withAlpha(accentColor, 0.18f * glowK)
            canvas.drawRoundRect(l, t, r, b, radius, radius, linePaint)
        }

        if (cfg.border) {
            linePaint.strokeWidth = strokePx
            linePaint.color = withAlpha(accentColor, 0.9f * dim * flick)
            canvas.drawRoundRect(l, t, r, b, radius, radius, linePaint)
        }

        if (focused && enabled) {
            val gap = strokePx * 2.2f
            linePaint.strokeWidth = max(1f, strokePx * 0.8f)
            linePaint.color = mixColors(accentColor, 0xFFFFFFFF.toInt(), 0.8f)
            canvas.drawRoundRect(l - gap, t - gap, r + gap, b + gap, radius + gap, radius + gap, linePaint)
        }

        drawLabel(canvas, (l + r) / 2f, (t + b) / 2f, glowK, dim * flick)
    }

    private fun drawUnderline(canvas: Canvas, l: Float, r: Float, y: Float, glowK: Float, alpha: Float) {
        if (glowK > 0f) {
            linePaint.strokeWidth = strokePx * 4f
            linePaint.color = withAlpha(accentColor, 0.08f * glowK)
            canvas.drawLine(l, y, r, y, linePaint)
            linePaint.strokeWidth = strokePx * 2.2f
            linePaint.color = withAlpha(accentColor, 0.18f * glowK)
            canvas.drawLine(l, y, r, y, linePaint)
        }
        linePaint.strokeWidth = strokePx
        linePaint.color = withAlpha(accentColor, 0.9f * alpha)
        canvas.drawLine(l, y, r, y, linePaint)
    }

    private fun drawLabel(canvas: Canvas, cx: Float, cy: Float, glowK: Float, alpha: Float) {
        textPaint.textSize = textSizePx
        textPaint.getFontMetrics(metrics)
        val baseline = cy - (metrics.ascent + metrics.descent) / 2f

        if (!config.inverseText && glowK > 0f) {
            textPaint.style = Paint.Style.STROKE
            textPaint.strokeWidth = strokePx * 2f
            textPaint.color = withAlpha(accentColor, 0.18f * glowK)
            canvas.drawText(text, cx, baseline, textPaint)
        }
        textPaint.style = Paint.Style.FILL
        textPaint.color = withAlpha(if (config.inverseText) onAccentColor else labelColor, alpha)
        canvas.drawText(text, cx, baseline, textPaint)
    }

    private companion object {
        const val HOLD_LIFT = 0.5f
        const val DISABLED_ALPHA = 0.35f
        const val FLICKER_STEP_MS = 40L
    }
}
