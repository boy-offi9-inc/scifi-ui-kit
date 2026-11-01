package com.boyoffi9.scifikit.core

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.SweepGradient
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * One contact on the radar.
 *
 * [angleDeg]: 0 is 3 o'clock, increasing clockwise.
 * [distance]: 0 is the centre, 1 is the rim.
 * [color]: 0 means "use the radar's accent colour".
 */
class RadarBlip(val angleDeg: Float, val distance: Float, val color: Int = 0)

/**
 * Draws a holographic radar: glowing rim, range rings, crosshair, degree ticks,
 * a rotating sweep with a fading trail, and blips that ping when the sweep
 * passes them and then fade until it comes round again.
 *
 * Pure Canvas code, shared by the View and the Compose wrapper.
 */
class HoloRadarPainter {

    var accentColor: Int = 0xFF00E5FF.toInt()
    var strokePx: Float = 3f
    var rings: Int = 4
    var sweep: Boolean = true
    var rotationPeriodMs: Long = 4000L
    var trailDeg: Float = 70f
    var blips: List<RadarBlip> = defaultBlips(7)

    /** False draws one calm frame: sweep parked, every blip steady. */
    var animate: Boolean = true

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val trailPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val arcRect = RectF()

    private var shader: SweepGradient? = null
    private var shaderKey = ""

    private var ticks = FloatArray(0)
    private var ticksKey = ""

    fun draw(canvas: Canvas, width: Float, height: Float, nowMs: Long) {
        val cx = width / 2f
        val cy = height / 2f
        val radius = min(width, height) / 2f - strokePx * 3f
        if (radius <= strokePx * 4f) return

        val angle = if (animate && sweep && rotationPeriodMs > 0L) {
            (nowMs % rotationPeriodMs) / rotationPeriodMs.toFloat() * 360f
        } else {
            PARKED_ANGLE
        }

        // Disc
        fillPaint.color = withAlpha(accentColor, 0.06f)
        canvas.drawCircle(cx, cy, radius, fillPaint)

        // Range rings and crosshair
        linePaint.strokeWidth = max(1f, strokePx / 2f)
        linePaint.color = withAlpha(accentColor, 0.28f)
        for (i in 1 until rings) {
            canvas.drawCircle(cx, cy, radius * i / rings, linePaint)
        }
        canvas.drawLine(cx - radius, cy, cx + radius, cy, linePaint)
        canvas.drawLine(cx, cy - radius, cx, cy + radius, linePaint)

        // Degree ticks just inside the rim
        ensureTicks(cx, cy, radius)
        linePaint.color = withAlpha(accentColor, 0.6f)
        canvas.drawLines(ticks, linePaint)

        // Sweep: fading trail, then the bright leading edge
        if (sweep) {
            ensureShader(cx, cy)
            arcRect.set(cx - radius, cy - radius, cx + radius, cy + radius)
            canvas.save()
            canvas.rotate(angle, cx, cy)
            trailPaint.shader = shader
            canvas.drawArc(arcRect, -trailDeg, trailDeg, true, trailPaint)
            linePaint.strokeWidth = strokePx * 1.6f
            linePaint.color = mixColors(accentColor, 0xFFFFFFFF.toInt(), 0.8f)
            canvas.drawLine(cx, cy, cx + radius, cy, linePaint)
            canvas.restore()
        }

        // Blips
        val blipR = strokePx * 2.4f
        for (b in blips) {
            val rad = Math.toRadians(b.angleDeg.toDouble())
            val bx = cx + cos(rad).toFloat() * radius * b.distance
            val by = cy + sin(rad).toFloat() * radius * b.distance
            val color = if (b.color != 0) b.color else accentColor

            val since = if (sweep) (((angle - b.angleDeg) % 360f) + 360f) % 360f else 0f
            val fade = 1f - since / 360f
            val alpha = if (sweep) 0.15f + 0.85f * fade * fade else 0.8f

            fillPaint.color = withAlpha(color, alpha)
            canvas.drawCircle(bx, by, blipR, fillPaint)

            // Expanding ping ring right after the sweep hits the blip
            if (animate && sweep && since < PING_DEG) {
                val t = since / PING_DEG
                linePaint.strokeWidth = max(1f, strokePx / 2f)
                linePaint.color = withAlpha(color, (1f - t) * 0.8f)
                canvas.drawCircle(bx, by, blipR * (1f + 3f * t), linePaint)
            }
        }

        // Rim: two soft wide strokes fake the glow, then the crisp line
        linePaint.strokeWidth = strokePx * 3.5f
        linePaint.color = withAlpha(accentColor, 0.08f)
        canvas.drawCircle(cx, cy, radius, linePaint)
        linePaint.strokeWidth = strokePx * 2f
        linePaint.color = withAlpha(accentColor, 0.18f)
        canvas.drawCircle(cx, cy, radius, linePaint)
        linePaint.strokeWidth = strokePx
        linePaint.color = withAlpha(accentColor, 0.9f)
        canvas.drawCircle(cx, cy, radius, linePaint)

        // Centre dot
        fillPaint.color = mixColors(accentColor, 0xFFFFFFFF.toInt(), 0.6f)
        canvas.drawCircle(cx, cy, strokePx * 1.6f, fillPaint)
    }

    private fun ensureShader(cx: Float, cy: Float) {
        val key = "$cx,$cy,$accentColor,$trailDeg"
        if (key == shaderKey && shader != null) return
        shaderKey = key
        // Transparent until the last trailDeg degrees before 0, then ramps up
        // to the leading edge. drawArc(-trailDeg, trailDeg) paints exactly that part.
        shader = SweepGradient(
            cx,
            cy,
            intArrayOf(0x00000000, 0x00000000, withAlpha(accentColor, 0.5f)),
            floatArrayOf(0f, (360f - trailDeg) / 360f, 1f),
        )
    }

    private fun ensureTicks(cx: Float, cy: Float, radius: Float) {
        val key = "$cx,$cy,$radius"
        if (key == ticksKey) return
        ticksKey = key
        val count = 36
        val out = FloatArray(count * 4)
        for (i in 0 until count) {
            val major = i % 3 == 0
            val len = radius * if (major) 0.09f else 0.04f
            val rad = Math.toRadians(i * 10.0)
            val c = cos(rad).toFloat()
            val s = sin(rad).toFloat()
            out[i * 4] = cx + c * radius
            out[i * 4 + 1] = cy + s * radius
            out[i * 4 + 2] = cx + c * (radius - len)
            out[i * 4 + 3] = cy + s * (radius - len)
        }
        ticks = out
    }

    companion object {
        private const val PARKED_ANGLE = 40f
        private const val PING_DEG = 50f

        /** A repeatable scatter of [count] contacts, the same every time for the same [seed]. */
        fun defaultBlips(count: Int, seed: Int = 7): List<RadarBlip> = List(count) { i ->
            val a = hash(seed, i * 2 + 1)
            val d = hash(seed, i * 2 + 2)
            RadarBlip(angleDeg = (a % 3600) / 10f, distance = 0.2f + (d % 700) / 1000f)
        }

        private fun hash(a: Int, b: Int): Int {
            var h = (a * 73856093) xor (b * 19349663)
            h = h xor (h ushr 13)
            h *= 0x5bd1e995
            h = h xor (h ushr 15)
            return h and 0x7fffffff
        }
    }
}
