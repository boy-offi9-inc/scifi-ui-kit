package com.boyoffi9.scifikit.core

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import kotlin.math.max
import kotlin.math.min

class SciFiCardPainter {

    var accentColor: Int = 0xFF00E5FF.toInt()
    var glow: Float = 1f
    var cutPx: Float = 28f
    var strokePx: Float = 3f
    var config: CardConfig = CardStyle.Panel.config
    var animate: Boolean = true

    val insetPx: Float get() = strokePx * 2f

    fun isAnimating(): Boolean = animate && config.march > 0f

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.MITER
        strokeCap = Paint.Cap.SQUARE
    }
    private val framePath = Path()
    private val innerPath = Path()
    private val barPath = Path()
    private val bracketPath = Path()
    private val shapePath = Path()

    fun draw(canvas: Canvas, width: Float, height: Float, nowMs: Long) {
        val s = strokePx
        val l = insetPx
        val t = insetPx
        val r = width - insetPx
        val b = height - insetPx
        if (r - l < s * 12f || b - t < s * 12f) return
        val w = r - l
        val h = b - t
        val cfg = config
        val limit = min(w, h) / 3f
        val tl = min(cutPx * cfg.cutTL, limit)
        val tr = min(cutPx * cfg.cutTR, limit)
        val br = min(cutPx * cfg.cutBR, limit)
        val bl = min(cutPx * cfg.cutBL, limit)
        val g = glow * cfg.glow
        val period = s * 6f
        val offset = if (animate) marchOffset(cfg, nowMs, period) else 0f

        cutPath(framePath, l, t, r, b, tl, tr, br, bl)

        fillPaint.color = withAlpha(accentColor, cfg.fill)
        canvas.drawPath(framePath, fillPaint)

        if (cfg.grid > 0f) drawGrid(canvas, l, t, r, b, cfg.grid)

        strokePaint.strokeWidth = s * 4f
        strokePaint.color = withAlpha(accentColor, 0.07f * g)
        canvas.drawPath(framePath, strokePaint)
        strokePaint.strokeWidth = s
        strokePaint.color = withAlpha(accentColor, cfg.line)
        canvas.drawPath(framePath, strokePaint)

        if (cfg.layers && w > s * 30f && h > s * 30f) {
            val d = s * 4f
            cutPath(innerPath, l + d, t + d, r - d, b - d, tl * 0.7f, tr * 0.7f, br * 0.7f, bl * 0.7f)
            strokePaint.strokeWidth = s * 0.7f
            strokePaint.color = withAlpha(accentColor, 0.3f)
            canvas.drawPath(innerPath, strokePaint)
        }

        if (cfg.bars) drawBars(canvas, l, t, r, b, tl, br, g)
        if (cfg.brackets) drawBrackets(canvas, l, t, r, b, tr, bl)
        if (cfg.ticks > 0) drawTicks(canvas, l, t, r, b, cfg.ticks)

        fillPaint.color = withAlpha(accentColor, 0.95f)
        if (cfg.hazardTop) {
            val x0 = l + tl + w * 0.12f
            hazard(canvas, x0, x0 + w * 0.22f, t - s * 1.5f, t + s * 1.5f, offset)
        }
        if (cfg.hazardBottom) {
            val x1 = r - br - w * 0.12f
            hazard(canvas, x1 - w * 0.22f, x1, b - s * 1.5f, b + s * 1.5f, offset)
        }
        if (cfg.footer) drawFooter(canvas, l, r, b, w, br, offset)
        if (cfg.target) drawTarget(canvas, l, t, tl)
        if (cfg.arrows && h > s * 40f) drawArrows(canvas, l, t, h)
        if (cfg.dots && w > s * 60f) drawDots(canvas, l, t, r, b, w, tr)
    }

    private fun cutPath(
        path: Path,
        l: Float,
        t: Float,
        r: Float,
        b: Float,
        tl: Float,
        tr: Float,
        br: Float,
        bl: Float,
    ) {
        path.reset()
        path.moveTo(l + tl, t)
        path.lineTo(r - tr, t)
        if (tr > 0f) path.lineTo(r, t + tr)
        path.lineTo(r, b - br)
        if (br > 0f) path.lineTo(r - br, b)
        path.lineTo(l + bl, b)
        if (bl > 0f) path.lineTo(l, b - bl)
        path.lineTo(l, t + tl)
        path.close()
    }

    private fun drawGrid(canvas: Canvas, l: Float, t: Float, r: Float, b: Float, alpha: Float) {
        val gap = strokePx * 10f
        strokePaint.strokeWidth = max(1f, strokePx / 3f)
        strokePaint.color = withAlpha(accentColor, alpha)
        canvas.save()
        canvas.clipPath(framePath)
        var x = l + gap
        while (x < r) {
            canvas.drawLine(x, t, x, b, strokePaint)
            x += gap
        }
        var y = t + gap
        while (y < b) {
            canvas.drawLine(l, y, r, y, strokePaint)
            y += gap
        }
        canvas.restore()
    }

    private fun drawBars(canvas: Canvas, l: Float, t: Float, r: Float, b: Float, tl: Float, br: Float, g: Float) {
        val w = r - l
        val h = b - t
        barPath.reset()
        barPath.moveTo(l, t + h * 0.55f)
        barPath.lineTo(l, t + tl)
        if (tl > 0f) barPath.lineTo(l + tl, t)
        barPath.lineTo(l + w * 0.5f, t)
        barPath.moveTo(r, b - h * 0.55f)
        barPath.lineTo(r, b - br)
        if (br > 0f) barPath.lineTo(r - br, b)
        barPath.lineTo(r - w * 0.5f, b)
        strokePaint.strokeWidth = strokePx * 7f
        strokePaint.color = withAlpha(accentColor, 0.12f * g)
        canvas.drawPath(barPath, strokePaint)
        strokePaint.strokeWidth = strokePx * 3f
        strokePaint.color = withAlpha(accentColor, 0.95f)
        canvas.drawPath(barPath, strokePaint)
    }

    private fun drawBrackets(canvas: Canvas, l: Float, t: Float, r: Float, b: Float, tr: Float, bl: Float) {
        val len = min(r - l, b - t) * 0.16f
        bracketPath.reset()
        if (tr <= 0f) {
            bracketPath.moveTo(r - len, t)
            bracketPath.lineTo(r, t)
            bracketPath.lineTo(r, t + len)
        }
        if (bl <= 0f) {
            bracketPath.moveTo(l + len, b)
            bracketPath.lineTo(l, b)
            bracketPath.lineTo(l, b - len)
        }
        strokePaint.strokeWidth = strokePx * 2f
        strokePaint.color = mixColors(accentColor, 0xFFFFFFFF.toInt(), 0.45f)
        canvas.drawPath(bracketPath, strokePaint)
    }

    private fun drawTicks(canvas: Canvas, l: Float, t: Float, r: Float, b: Float, count: Int) {
        val s = strokePx
        val h = b - t
        strokePaint.strokeWidth = s * 0.9f
        strokePaint.color = withAlpha(accentColor, 0.8f)
        for (i in 0 until count) {
            val yl = b - h * 0.3f - i * s * 2.6f
            canvas.drawLine(l + s * 2.5f, yl, l + s * 4.5f, yl - s * 2f, strokePaint)
            val yr = t + h * 0.35f + i * s * 2.6f
            canvas.drawLine(r - s * 2.5f, yr, r - s * 4.5f, yr + s * 2f, strokePaint)
        }
    }

    private fun hazard(canvas: Canvas, x0: Float, x1: Float, y0: Float, y1: Float, offset: Float) {
        val hh = y1 - y0
        val sw = hh * 0.9f
        val period = sw * 2f
        canvas.save()
        canvas.clipRect(x0, y0, x1, y1)
        var x = x0 - hh - period + offset
        while (x < x1) {
            shapePath.reset()
            shapePath.moveTo(x + hh, y0)
            shapePath.lineTo(x + hh + sw, y0)
            shapePath.lineTo(x + sw, y1)
            shapePath.lineTo(x, y1)
            shapePath.close()
            canvas.drawPath(shapePath, fillPaint)
            x += period
        }
        canvas.restore()
    }

    private fun drawFooter(canvas: Canvas, l: Float, r: Float, b: Float, w: Float, br: Float, offset: Float) {
        val s = strokePx
        val x0 = l + w * 0.4f
        val x1 = r - br - w * 0.04f
        val y1 = b + s * 1.5f
        val y0 = y1 - s * 5f
        val span = x1 - x0
        val skew = s * 3f
        hazard(canvas, x0, x0 + span * 0.62f, y0, y1, offset)
        val xs = x0 + span * 0.66f
        shapePath.reset()
        shapePath.moveTo(xs + skew, y0)
        shapePath.lineTo(x1, y0)
        shapePath.lineTo(x1 - skew, y1)
        shapePath.lineTo(xs, y1)
        shapePath.close()
        canvas.drawPath(shapePath, fillPaint)
    }

    private fun drawTarget(canvas: Canvas, l: Float, t: Float, tl: Float) {
        val s = strokePx
        val rad = s * 3.2f
        val c = max(tl * 0.9f, rad * 1.6f)
        strokePaint.strokeWidth = s
        strokePaint.color = withAlpha(accentColor, 0.9f)
        canvas.drawCircle(l + c, t + c, rad, strokePaint)
        fillPaint.color = withAlpha(accentColor, 0.95f)
        canvas.drawCircle(l + c, t + c, s * 1.1f, fillPaint)
    }

    private fun drawArrows(canvas: Canvas, l: Float, t: Float, h: Float) {
        val s = strokePx
        val x = l + s * 4f
        fillPaint.color = withAlpha(accentColor, 0.8f)
        for (i in 0 until 4) {
            val y = t + h * 0.45f + i * s * 3.5f
            shapePath.reset()
            shapePath.moveTo(x, y - s * 1.2f)
            shapePath.lineTo(x + s * 0.9f, y + s * 0.8f)
            shapePath.lineTo(x - s * 0.9f, y + s * 0.8f)
            shapePath.close()
            canvas.drawPath(shapePath, fillPaint)
        }
    }

    private fun drawDots(canvas: Canvas, l: Float, t: Float, r: Float, b: Float, w: Float, tr: Float) {
        val s = strokePx
        val gap = s * 3.2f
        val size = s * 0.9f
        fillPaint.color = withAlpha(accentColor, 0.9f)
        for (i in 0 until 6) {
            val x = r - tr - s * 4f - i * gap
            canvas.drawRect(x, t + s * 3f, x + size, t + s * 3f + size, fillPaint)
        }
        for (i in 0 until 5) {
            val x = l + w * 0.2f + i * gap
            canvas.drawRect(x, b - s * 3f - size, x + size, b - s * 3f, fillPaint)
        }
    }
}
