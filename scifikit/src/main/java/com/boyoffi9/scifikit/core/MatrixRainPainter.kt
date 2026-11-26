package com.boyoffi9.scifikit.core

import android.graphics.Canvas
import android.graphics.Paint
import kotlin.math.max
import kotlin.random.Random

/**
 * Draws a digital rain wall: columns of glyphs falling at their own speeds,
 * bright leading glyph and a fading tail, characters flickering to new values
 * as they descend, optional top/bottom fade into the background.
 *
 * Pure Canvas code shared by the View and the Compose wrapper.
 */
class MatrixRainPainter {

    var accentColor: Int = 0xFF00E5FF.toInt()
    var glow: Float = 1f
    var glyphSizePx: Float = 14f
    var density: Float = 0.55f
    var trailAlpha: Int = 42
    var flicker: Boolean = true
    var backgroundColor: Int = 0xFF05080C.toInt()

    /** False draws one calm static frame. */
    var animate: Boolean = true

    private val glyphPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
    }
    private val fadePaint = Paint().apply { style = Paint.Style.FILL }
    private val charset: CharArray = CHARSET

    private var cols: Int = 0
    private var colStepPx: Float = glyphSizePx * 1.2f
    private var speeds = FloatArray(0)
    private var heads = FloatArray(0)
    private var glyphs = Array(0) { CharArray(0) }
    private var seed = Random(0x51CF)

    private var lastMs = -1L
    private var rng = Random(0x51CF)

    fun draw(canvas: Canvas, width: Float, height: Float, nowMs: Long) {
        ensureLayout(width, height)
        // background wash — keeps the trail-fade honest even on a bare Canvas
        fadePaint.color = backgroundColor
        canvas.drawRect(0f, 0f, width, height, fadePaint)

        val dt = if (animate && lastMs >= 0) (nowMs - lastMs).coerceIn(0L, 100L) else 0L
        lastMs = nowMs

        glyphPaint.textSize = glyphSizePx
        glyphPaint.typeface = MONO
        val leadPaint = Paint(glyphPaint)
        leadPaint.isFakeBoldText = true

        for (c in 0 until cols) {
            if (animate) {
                heads[c] += speeds[c] * dt / 1000f
                if (heads[c] > height + trailRows() * glyphSizePx) {
                    heads[c] = -rng.nextFloat() * height * 0.8f
                    speeds[c] = baseSpeed() * (0.6f + rng.nextFloat() * 0.9f)
                }
            }
            val headRow = (heads[c] / glyphSizePx).toInt()
            val x = (c + 0.5f) * colStepPx
            val tail = trailRows()
            for (r in max(0, headRow - tail)..headRow) {
                val y = (r + 1f) * glyphSizePx
                if (y - glyphSizePx > height) break
                val age = headRow - r
                val ch = if (flicker && animate && rng.nextInt(24) == 0) {
                    charset[rng.nextInt(charset.size)].also { glyphs[c][r % glyphs[c].size] = it }
                } else {
                    glyphs[c][r % glyphs[c].size]
                }
                if (age == 0) {
                    leadPaint.color = blendWhite(accentColor, 0.75f)
                    glyphPaint.color = leadPaint.color
                    canvas.drawText(ch.toString(), x, y, glyphPaint)
                } else {
                    val a = (trailAlpha * (1f - age.toFloat() / tail)).toInt().coerceIn(0, 255)
                    glyphPaint.color = withAlpha(accentColor, a)
                    canvas.drawText(ch.toString(), x, y, glyphPaint)
                }
            }
        }
    }

    private fun trailRows(): Int = 12

    private fun baseSpeed(): Float = glyphSizePx * 9f

    private fun ensureLayout(width: Float, height: Float) {
        val step = max(glyphSizePx * 1.2f, 1f)
        val want = max(1, (width / step).toInt())
        if (want != cols) {
            cols = want
            colStepPx = step
            speeds = FloatArray(cols) { baseSpeed() * (0.6f + seed.nextFloat() * 0.9f) }
            heads = FloatArray(cols) { seed.nextFloat() * 800f }
            glyphs = Array(cols) { CharArray(80) { charset[seed.nextInt(charset.size)] } }
        }
    }

    private fun withAlpha(color: Int, alpha: Int): Int = (color and 0xFFFFFF) or (alpha.coerceIn(0, 255) shl 24)

    private fun blendWhite(color: Int, t: Float): Int {
        val r = (color shr 16 and 0xFF)
        val g = (color shr 8 and 0xFF)
        val b = (color and 0xFF)
        fun mix(v: Int) = (v + (255 - v) * t).toInt().coerceAtMost(255)
        return 0xFF000000.toInt() or (mix(r) shl 16) or (mix(g) shl 8) or mix(b)
    }

    fun isAnimating(): Boolean = animate

    companion object {
        private val MONO = android.graphics.Typeface.MONOSPACE

        /** Katakana + digits, the classic rain alphabet. */
        val CHARSET = charArrayOf(
            'ア','ィ','ウ','ェ','カ','キ','ク','ケ','コ','サ','シ','ス','セ','ソ',
            'タ','チ','ツ','テ','ト','ナ','ニ','ヌ','ネ','ノ','ハ','ヒ','フ','ヘ',
            'ホ','マ','ミ','ム','メ','モ','ヤ','ユ','ヨ','ラ','リ','ル','レ','ロ',
            'ワ','ン','0','1','2','3','4','5','6','7','8','9',
        )
    }
}
