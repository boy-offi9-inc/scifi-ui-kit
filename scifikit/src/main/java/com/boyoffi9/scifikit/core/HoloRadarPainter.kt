package com.boyoffi9.scifikit.core

import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.SweepGradient
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

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
 * [radar] picks the look; see [RadarMode]. Pure Canvas code, shared by the View
 * and the Compose wrapper.
 */
class HoloRadarPainter {

    var accentColor: Int = 0xFF00E5FF.toInt()
    var strokePx: Float = 3f
    var rings: Int = 4
    var sweep: Boolean = true
    var rotationPeriodMs: Long = 4000L
    var radar: RadarConfig = RadarMode.Classic.config
    var blips: List<RadarBlip> = defaultBlips(7)

    var trailDeg: Float
        get() = radar.trailDeg
        set(value) {
            radar = radar.copy(trailDeg = value)
        }

    /** False draws one calm frame: sweep parked, every blip steady. */
    var animate: Boolean = true

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val trailPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }
    private val arcRect = RectF()
    private val disc = Path()

    private var shader: SweepGradient? = null
    private var shaderCx = 0f
    private var shaderCy = 0f
    private var shaderAccent = 0
    private var shaderTrail = 0f
    private var shaderAlpha = 0f

    private var dash: DashPathEffect? = null
    private var dashStroke = 0f

    private var ticks = FloatArray(0)
    private var grid = FloatArray(0)
    private var lines = FloatArray(0)
    private var geoCx = -1f
    private var geoCy = -1f
    private var geoRadius = -1f
    private var geoStroke = -1f
    private var geoTicks = -1
    private var geoLines = false

    fun draw(canvas: Canvas, width: Float, height: Float, nowMs: Long) {
        val cx = width / 2f
        val cy = height / 2f
        val radius = min(width, height) / 2f - strokePx * 3f
        if (radius <= strokePx * 4f) return

        val cfg = radar
        val angle = if (animate && sweep && rotationPeriodMs > 0L) {
            (nowMs % rotationPeriodMs) / rotationPeriodMs.toFloat() * 360f
        } else {
            PARKED_ANGLE
        }
        val m = if (animate) flickerFactor(cfg, (nowMs / FLICKER_STEP_MS).toInt()) else 1f
        val secondary = if (cfg.secondaryColor != 0) cfg.secondaryColor else accentColor

        ensureGeometry(cx, cy, radius, cfg)

        fillPaint.color = withAlpha(accentColor, cfg.fill * m)
        canvas.drawCircle(cx, cy, radius, fillPaint)

        linePaint.strokeWidth = max(1f, strokePx / 2f)
        if (cfg.grid) {
            linePaint.color = withAlpha(accentColor, 0.1f * m)
            canvas.drawLines(grid, linePaint)
        }

        linePaint.color = withAlpha(accentColor, 0.28f * m)
        if (cfg.ringsDashed) linePaint.pathEffect = dashEffect()
        for (i in 1 until rings) {
            canvas.drawCircle(cx, cy, radius * i / rings, linePaint)
        }
        linePaint.pathEffect = null
        canvas.drawLine(cx - radius, cy, cx + radius, cy, linePaint)
        canvas.drawLine(cx, cy - radius, cx, cy + radius, linePaint)

        linePaint.color = withAlpha(accentColor, 0.6f * m)
        canvas.drawLines(ticks, linePaint)

        if (cfg.cardinals) drawCardinals(canvas, cx, cy, radius, m)

        if (sweep) {
            ensureShader(cx, cy, cfg)
            arcRect.set(cx - radius, cy - radius, cx + radius, cy + radius)
            canvas.save()
            canvas.rotate(angle, cx, cy)
            trailPaint.shader = shader
            canvas.drawArc(arcRect, -cfg.trailDeg, cfg.trailDeg, true, trailPaint)
            linePaint.strokeWidth = strokePx * 1.6f
            linePaint.color = withAlpha(mixColors(accentColor, 0xFFFFFFFF.toInt(), 0.8f), m)
            canvas.drawLine(cx, cy, cx + radius, cy, linePaint)
            canvas.restore()
            if (cfg.split > 0f) {
                canvas.save()
                canvas.rotate(angle - cfg.split, cx, cy)
                linePaint.strokeWidth = max(1f, strokePx)
                linePaint.color = withAlpha(secondary, 0.8f * m)
                canvas.drawLine(cx, cy, cx + radius, cy, linePaint)
                canvas.restore()
            }
        }

        val blipR = strokePx * 2.4f
        for (i in blips.indices) {
            val b = blips[i]
            val rad = Math.toRadians(b.angleDeg.toDouble())
            val bx = cx + cos(rad).toFloat() * radius * b.distance
            val by = cy + sin(rad).toFloat() * radius * b.distance
            val color = when {
                b.color != 0 -> b.color
                i % 2 == 1 -> secondary
                else -> accentColor
            }

            val since = if (sweep) (((angle - b.angleDeg) % 360f) + 360f) % 360f else 0f
            val alpha = if (sweep) blipAlpha(cfg, since) else 0.8f

            fillPaint.color = withAlpha(color, alpha * m)
            drawBlip(canvas, cfg.blipShape, bx, by, blipR)

            if (animate && sweep && since < PING_DEG) {
                val t = since / PING_DEG
                linePaint.strokeWidth = max(1f, strokePx / 2f)
                linePaint.color = withAlpha(color, (1f - t) * 0.8f * m)
                canvas.drawCircle(bx, by, blipR * (1f + 3f * t), linePaint)
            }
        }

        if (cfg.scanlines || (cfg.scanBand && animate)) {
            canvas.save()
            canvas.clipPath(disc)
            if (cfg.scanlines) {
                linePaint.strokeWidth = max(1f, strokePx / 2f)
                linePaint.color = withAlpha(0xFF000000.toInt(), 0.35f)
                canvas.drawLines(lines, linePaint)
            }
            if (cfg.scanBand && animate) drawScanBand(canvas, cx, cy, radius, nowMs, m)
            canvas.restore()
        }

        val glow = cfg.glow
        if (glow > 0f) {
            linePaint.strokeWidth = strokePx * 3.5f
            linePaint.color = withAlpha(accentColor, 0.08f * glow * m)
            canvas.drawCircle(cx, cy, radius, linePaint)
            linePaint.strokeWidth = strokePx * 2f
            linePaint.color = withAlpha(accentColor, 0.18f * glow * m)
            canvas.drawCircle(cx, cy, radius, linePaint)
        }
        if (cfg.split > 0f) {
            linePaint.strokeWidth = max(1f, strokePx / 2f)
            linePaint.color = withAlpha(secondary, 0.55f * m)
            canvas.drawCircle(cx, cy, radius + strokePx * 0.9f, linePaint)
        }
        linePaint.strokeWidth = strokePx
        linePaint.color = withAlpha(accentColor, 0.9f * m)
        canvas.drawCircle(cx, cy, radius, linePaint)

        fillPaint.color = mixColors(accentColor, 0xFFFFFFFF.toInt(), 0.6f)
        canvas.drawCircle(cx, cy, strokePx * 1.6f, fillPaint)
    }

    private fun drawBlip(canvas: Canvas, shape: BlipShape, x: Float, y: Float, r: Float) {
        when (shape) {
            BlipShape.Dot -> canvas.drawCircle(x, y, r, fillPaint)
            BlipShape.Square -> canvas.drawRect(x - r, y - r, x + r, y + r, fillPaint)
            BlipShape.Diamond -> {
                canvas.save()
                canvas.rotate(45f, x, y)
                canvas.drawRect(x - r * 0.85f, y - r * 0.85f, x + r * 0.85f, y + r * 0.85f, fillPaint)
                canvas.restore()
            }
            BlipShape.Cross -> {
                linePaint.strokeWidth = max(1f, strokePx)
                linePaint.color = fillPaint.color
                canvas.drawLine(x - r * 1.3f, y, x + r * 1.3f, y, linePaint)
                canvas.drawLine(x, y - r * 1.3f, x, y + r * 1.3f, linePaint)
            }
        }
    }

    private fun drawCardinals(canvas: Canvas, cx: Float, cy: Float, radius: Float, m: Float) {
        val size = radius * 0.11f
        val inset = radius * 0.74f
        val drop = size * 0.35f
        textPaint.textSize = size
        textPaint.color = withAlpha(accentColor, 0.8f * m)
        canvas.drawText("N", cx, cy - inset + drop, textPaint)
        canvas.drawText("E", cx + inset, cy + drop, textPaint)
        canvas.drawText("S", cx, cy + inset + drop, textPaint)
        canvas.drawText("W", cx - inset, cy + drop, textPaint)
    }

    private fun drawScanBand(canvas: Canvas, cx: Float, cy: Float, radius: Float, nowMs: Long, m: Float) {
        val travel = (nowMs % BAND_PERIOD_MS) / BAND_PERIOD_MS.toFloat()
        val y = cy - radius + travel * radius * 2f
        val h = radius * 0.2f
        fillPaint.color = withAlpha(accentColor, 0.1f * m)
        canvas.drawRect(cx - radius, y - h, cx + radius, y, fillPaint)
        fillPaint.color = withAlpha(accentColor, 0.3f * m)
        canvas.drawRect(cx - radius, y - strokePx, cx + radius, y, fillPaint)
    }

    private fun dashEffect(): DashPathEffect {
        val current = dash
        if (current != null && dashStroke == strokePx) return current
        dashStroke = strokePx
        return DashPathEffect(floatArrayOf(strokePx * 3f, strokePx * 3f), 0f).also { dash = it }
    }

    private fun ensureShader(cx: Float, cy: Float, cfg: RadarConfig) {
        if (shader != null && cx == shaderCx && cy == shaderCy &&
            accentColor == shaderAccent && cfg.trailDeg == shaderTrail && cfg.trailAlpha == shaderAlpha
        ) return
        shaderCx = cx
        shaderCy = cy
        shaderAccent = accentColor
        shaderTrail = cfg.trailDeg
        shaderAlpha = cfg.trailAlpha
        val trail = cfg.trailDeg.coerceIn(1f, 359f)
        // Transparent until the last trailDeg degrees before 0, then ramps up
        // to the leading edge. drawArc(-trailDeg, trailDeg) paints exactly that part.
        shader = SweepGradient(
            cx,
            cy,
            intArrayOf(0x00000000, 0x00000000, withAlpha(accentColor, cfg.trailAlpha)),
            floatArrayOf(0f, (360f - trail) / 360f, 1f),
        )
    }

    private fun ensureGeometry(cx: Float, cy: Float, radius: Float, cfg: RadarConfig) {
        if (cx == geoCx && cy == geoCy && radius == geoRadius && strokePx == geoStroke &&
            cfg.ticks == geoTicks && cfg.scanlines == geoLines
        ) return
        geoCx = cx
        geoCy = cy
        geoRadius = radius
        geoStroke = strokePx
        geoTicks = cfg.ticks
        geoLines = cfg.scanlines

        val count = cfg.ticks.coerceIn(4, 180)
        val majorEvery = max(1, count / 12)
        val out = FloatArray(count * 4)
        for (i in 0 until count) {
            val len = radius * if (i % majorEvery == 0) 0.09f else 0.04f
            val rad = Math.toRadians(i * 360.0 / count)
            val c = cos(rad).toFloat()
            val s = sin(rad).toFloat()
            out[i * 4] = cx + c * radius
            out[i * 4 + 1] = cy + s * radius
            out[i * 4 + 2] = cx + c * (radius - len)
            out[i * 4 + 3] = cy + s * (radius - len)
        }
        ticks = out

        disc.rewind()
        disc.addCircle(cx, cy, radius, Path.Direction.CW)

        val g = FloatArray(24)
        for (k in 0 until 3) {
            val off = radius * (k + 1) / 4f
            val half = sqrt(radius * radius - off * off)
            g[k * 8] = cx + off
            g[k * 8 + 1] = cy - half
            g[k * 8 + 2] = cx + off
            g[k * 8 + 3] = cy + half
            g[k * 8 + 4] = cx - off
            g[k * 8 + 5] = cy - half
            g[k * 8 + 6] = cx - off
            g[k * 8 + 7] = cy + half
        }
        val gridOut = FloatArray(48)
        System.arraycopy(g, 0, gridOut, 0, 24)
        for (k in 0 until 3) {
            val off = radius * (k + 1) / 4f
            val half = sqrt(radius * radius - off * off)
            gridOut[24 + k * 8] = cx - half
            gridOut[24 + k * 8 + 1] = cy + off
            gridOut[24 + k * 8 + 2] = cx + half
            gridOut[24 + k * 8 + 3] = cy + off
            gridOut[24 + k * 8 + 4] = cx - half
            gridOut[24 + k * 8 + 5] = cy - off
            gridOut[24 + k * 8 + 6] = cx + half
            gridOut[24 + k * 8 + 7] = cy - off
        }
        grid = gridOut

        if (cfg.scanlines) {
            val step = max(3f, strokePx * 2f)
            val n = (radius * 2f / step).toInt()
            val rows = FloatArray(n * 4)
            for (i in 0 until n) {
                val y = cy - radius + i * step
                rows[i * 4] = cx - radius
                rows[i * 4 + 1] = y
                rows[i * 4 + 2] = cx + radius
                rows[i * 4 + 3] = y
            }
            lines = rows
        }
    }

    companion object {
        private const val PARKED_ANGLE = 40f
        private const val PING_DEG = 50f
        private const val FLICKER_STEP_MS = 70L
        private const val BAND_PERIOD_MS = 3200L

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
