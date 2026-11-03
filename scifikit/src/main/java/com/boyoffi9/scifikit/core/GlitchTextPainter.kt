package com.boyoffi9.scifikit.core

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import kotlin.math.max

/**
 * Draws text that decodes into the real string, then glitches in short bursts:
 * RGB split plus horizontal slice displacement.
 *
 * Time is passed in by the caller, so there are no timers inside. The View
 * drives it with postInvalidateOnAnimation, Compose with withFrameMillis.
 */
class GlitchTextPainter {

    var text: String = ""
    var textSizePx: Float = 64f
    var color: Int = 0xFFE8FDFF.toInt()
    var splitColorA: Int = 0xFFFF2D6F.toInt()
    var splitColorB: Int = 0xFF00E5FF.toInt()
    var typeface: Typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)

    var glitch: GlitchConfig = GlitchMode.Standard.config
    var touch: TouchConfig = TouchMode.Burst.config

    /** 0 = no glitch bursts, 1 = default, above 1 = more violent. */
    var intensity: Float
        get() = glitch.intensity
        set(value) {
            glitch = glitch.copy(intensity = value)
        }

    var glitchIntervalMs: Long
        get() = glitch.intervalMs
        set(value) {
            glitch = glitch.copy(intervalMs = value)
        }

    var burstDurationMs: Long
        get() = glitch.burstMs
        set(value) {
            glitch = glitch.copy(burstMs = value)
        }

    var decode: DecodeConfig = DecodeMode.Classic.config

    /** False draws the final text once, with no scramble and no glitching. */
    var animate: Boolean = true

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val metrics = Paint.FontMetrics()
    private val decoder = TextDecoder()
    private var startMs = -1L
    private var forcedBurstUntilMs = 0L
    private var releaseUntilMs = 0L
    private var pressed = false
    private var pressStartMs = 0L
    private var touchX = 0f

    fun restart(nowMs: Long) {
        startMs = nowMs
    }

    fun triggerBurst(nowMs: Long) {
        forcedBurstUntilMs = nowMs + glitch.burstMs
    }

    fun press(x: Float, nowMs: Long) {
        pressed = true
        pressStartMs = nowMs
        touchX = x
        if (!touch.follow) triggerBurst(nowMs)
    }

    fun moveTo(x: Float) {
        touchX = x
    }

    fun release(nowMs: Long) {
        if (pressed && chargeLevel(touch, nowMs - pressStartMs) >= 1f) {
            releaseUntilMs = nowMs + glitch.burstMs * 2L
        }
        pressed = false
    }

    fun contentWidth(): Float {
        applyPaint()
        return paint.measureText(text)
    }

    fun contentHeight(): Float {
        applyPaint()
        paint.getFontMetrics(metrics)
        return metrics.descent - metrics.ascent
    }

    /** Draws into the rectangle (0, 0, contentWidth(), contentHeight()). */
    fun draw(canvas: Canvas, nowMs: Long) {
        if (text.isEmpty()) return
        applyPaint()
        if (startMs < 0L) startMs = nowMs
        paint.getFontMetrics(metrics)

        val baseline = -metrics.ascent
        val bottom = baseline + metrics.descent
        val count = decoder.fill(text, decode, startMs, nowMs, animate)
        val held = animate && pressed && (touch.follow || touch.charge)
        val level = if (animate && pressed) chargeLevel(touch, nowMs - pressStartMs) else 0f
        val releasing = animate && nowMs < releaseUntilMs
        val scale = if (releasing) 1f + touch.chargeBoost else 1f + touch.chargeBoost * level
        val windowed = held && touch.follow && !releasing
        val whole = animate && (isBursting(nowMs) || releasing || (held && !touch.follow))
        val width = paint.measureText(decoder.chars, 0, count)

        if (windowed && !whole) {
            val radius = textSizePx * touch.radius * (1f + level)
            val pad = textSizePx
            val left = touchX - radius
            val right = touchX + radius
            canvas.save()
            canvas.clipRect(-pad, -pad, left, bottom + pad)
            drawPass(canvas, count, baseline, bottom, width, nowMs, false, 1f)
            canvas.restore()
            canvas.save()
            canvas.clipRect(right, -pad, width + pad, bottom + pad)
            drawPass(canvas, count, baseline, bottom, width, nowMs, false, 1f)
            canvas.restore()
            canvas.save()
            canvas.clipRect(left, -pad, right, bottom + pad)
            drawPass(canvas, count, baseline, bottom, width, nowMs, true, scale)
            canvas.restore()
        } else {
            drawPass(canvas, count, baseline, bottom, width, nowMs, whole, scale)
        }
        drawCursor(canvas, baseline)
    }

    private fun drawPass(
        canvas: Canvas,
        count: Int,
        baseline: Float,
        bottom: Float,
        width: Float,
        nowMs: Long,
        bursting: Boolean,
        scale: Float,
    ) {
        val g = glitch
        val bucket = (nowMs / g.stepMs.coerceAtLeast(1L)).toInt()
        if (bursting && g.dropout > 0f && hashOf(bucket, 7) % 1000 < g.dropout * 1000f) return
        val power = g.intensity * scale
        val split = if (bursting) {
            textSizePx * g.splitBurst * power * (0.5f + (hashOf(bucket, 99) % 100) / 100f)
        } else {
            textSizePx * g.splitIdle * g.intensity
        }
        val strips = g.strips.coerceIn(1, 32)
        val stripH = bottom / strips

        drawLayer(canvas, count, -split, baseline, splitColorA, 0.75f, bursting, bucket, stripH, strips, width, 1, power)
        drawLayer(canvas, count, split, baseline, splitColorB, 0.75f, bursting, bucket, stripH, strips, width, 2, power)
        drawLayer(canvas, count, 0f, baseline, color, 1f, bursting, bucket, stripH, strips, width, 3, power)
    }

    private fun drawLayer(
        canvas: Canvas,
        count: Int,
        x: Float,
        baseline: Float,
        layerColor: Int,
        alpha: Float,
        bursting: Boolean,
        bucket: Int,
        stripH: Float,
        strips: Int,
        width: Float,
        layer: Int,
        power: Float,
    ) {
        val g = glitch
        var a = alpha
        if (bursting && g.flicker > 0f) {
            a *= 1f - g.flicker * (hashOf(bucket, 50 + layer) % 100 / 100f)
        }
        paint.color = withAlpha(layerColor, a)
        if (!bursting) {
            canvas.drawText(decoder.chars, 0, count, x, baseline, paint)
            return
        }
        val shift = textSizePx * g.sliceReach * power
        val reach = textSizePx * 0.6f + shift
        val chance = (g.sliceChance * 100f).toInt()
        for (i in 0 until strips) {
            val h = hashOf(bucket, i + 1)
            val dx = if (h % 100 < chance) ((h shr 4) % 200 / 100f - 1f) * shift else 0f
            canvas.save()
            canvas.clipRect(x - reach, i * stripH, x + width + reach, (i + 1) * stripH)
            canvas.drawText(decoder.chars, 0, count, x + dx, baseline, paint)
            canvas.restore()
        }
    }

    private fun drawCursor(canvas: Canvas, baseline: Float) {
        val index = decoder.cursorIndex
        if (index < 0) return
        val left = paint.measureText(decoder.chars, 0, index)
        val cursorWidth = max(paint.measureText(decoder.chars, index, 1), textSizePx * 0.5f)
        paint.color = color
        canvas.drawRect(
            left,
            baseline - textSizePx * 0.8f,
            left + cursorWidth * 0.9f,
            baseline + metrics.descent * 0.5f,
            paint,
        )
    }

    private fun applyPaint() {
        paint.textSize = textSizePx
        paint.typeface = typeface
    }

    private fun isBursting(now: Long): Boolean =
        now < forcedBurstUntilMs || burstActive(glitch, now - startMs)
}
