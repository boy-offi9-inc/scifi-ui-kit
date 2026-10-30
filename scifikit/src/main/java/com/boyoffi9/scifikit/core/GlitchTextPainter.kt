package com.boyoffi9.scifikit.core

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface

/**
 * Draws text that "decodes" from random characters into the real string, then
 * glitches in short bursts: RGB split plus horizontal slice displacement.
 *
 * Time is passed in by the caller ([draw]'s nowMs), so there are no timers
 * inside. The View drives it with postInvalidateOnAnimation, Compose with
 * withFrameMillis.
 */
class GlitchTextPainter {

    var text: String = ""
    var textSizePx: Float = 64f
    var color: Int = 0xFFE8FDFF.toInt()
    var splitColorA: Int = 0xFFFF2D6F.toInt()
    var splitColorB: Int = 0xFF00E5FF.toInt()
    var typeface: Typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)

    /** 0 = no glitch bursts, 1 = default, above 1 = more violent. */
    var intensity: Float = 1f
    var glitchIntervalMs: Long = 2600L
    var burstDurationMs: Long = 260L
    var decodeDurationMs: Long = 900L

    /** False draws the final text once, with no scramble and no glitching. */
    var animate: Boolean = true

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val metrics = Paint.FontMetrics()
    private var startMs = -1L
    private var forcedBurstUntilMs = 0L

    /** Extra room left and right so displaced slices are not clipped. */
    val marginPx: Float get() = textSizePx * 0.3f

    fun restart(nowMs: Long) {
        startMs = nowMs
    }

    fun triggerBurst(nowMs: Long) {
        forcedBurstUntilMs = nowMs + burstDurationMs
    }

    fun contentWidth(): Float {
        applyPaint()
        return paint.measureText(text) + marginPx * 2f
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
        val x = marginPx

        val shown = if (animate) decoded(nowMs) else text
        val bursting = animate && isBursting(nowMs)
        val bucket = (nowMs / 45L).toInt()
        val split = if (bursting) {
            textSizePx * 0.06f * intensity * (0.5f + (hash(bucket, 99) % 100) / 100f)
        } else {
            textSizePx * 0.012f * intensity
        }
        val width = paint.measureText(shown)
        val stripH = bottom / STRIPS

        drawLayer(canvas, shown, x - split, baseline, splitColorA, 0.75f, bursting, bucket, stripH, width)
        drawLayer(canvas, shown, x + split, baseline, splitColorB, 0.75f, bursting, bucket, stripH, width)
        drawLayer(canvas, shown, x, baseline, color, 1f, bursting, bucket, stripH, width)
    }

    private fun drawLayer(
        canvas: Canvas,
        s: String,
        x: Float,
        baseline: Float,
        layerColor: Int,
        alpha: Float,
        bursting: Boolean,
        bucket: Int,
        stripH: Float,
        width: Float,
    ) {
        paint.color = withAlpha(layerColor, alpha)
        if (!bursting) {
            canvas.drawText(s, x, baseline, paint)
            return
        }
        val reach = textSizePx * 0.6f
        for (i in 0 until STRIPS) {
            // Same hash for every layer, so the slices stay aligned across colours.
            val h = hash(bucket, i + 1)
            val dx = if (h % 10 < 4) {
                ((h shr 4) % 200 / 100f - 1f) * textSizePx * 0.25f * intensity
            } else {
                0f
            }
            canvas.save()
            canvas.clipRect(x - reach, i * stripH, x + width + reach, (i + 1) * stripH)
            canvas.drawText(s, x + dx, baseline, paint)
            canvas.restore()
        }
    }

    private fun applyPaint() {
        paint.textSize = textSizePx
        paint.typeface = typeface
    }

    private fun decoded(now: Long): String {
        val t = now - startMs
        if (decodeDurationMs <= 0L || t >= decodeDurationMs) return text
        val resolved = (text.length * (t.toFloat() / decodeDurationMs)).toInt()
        val bucket = (now / 55L).toInt()
        val sb = StringBuilder(text.length)
        for (i in text.indices) {
            val ch = text[i]
            if (i < resolved || ch == ' ') {
                sb.append(ch)
            } else {
                sb.append(CHARSET[hash(i, bucket) % CHARSET.length])
            }
        }
        return sb.toString()
    }

    private fun isBursting(now: Long): Boolean {
        if (now < forcedBurstUntilMs) return true
        if (glitchIntervalMs <= 0L || intensity <= 0f) return false
        val phase = (now - startMs) % glitchIntervalMs
        return phase >= glitchIntervalMs - burstDurationMs
    }

    /** Cheap deterministic hash, so the same frame always glitches the same way. */
    private fun hash(a: Int, b: Int): Int {
        var h = (a * 73856093) xor (b * 19349663)
        h = h xor (h ushr 13)
        h *= 0x5bd1e995
        h = h xor (h ushr 15)
        return h and 0x7fffffff
    }

    private companion object {
        const val STRIPS = 6
        const val CHARSET = "01<>/|[]{}#%&*+=-ABCDEFXYZ"
    }
}
