package com.boyoffi9.scifikit

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.TypedArray
import android.graphics.Canvas
import android.graphics.Typeface
import android.os.SystemClock
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import androidx.core.content.withStyledAttributes
import com.boyoffi9.scifikit.core.DecodeConfig
import com.boyoffi9.scifikit.core.DecodeMode
import com.boyoffi9.scifikit.core.DecodeOrder
import com.boyoffi9.scifikit.core.GlitchConfig
import com.boyoffi9.scifikit.core.GlitchMode
import com.boyoffi9.scifikit.core.GlitchTextPainter
import com.boyoffi9.scifikit.core.TouchConfig
import com.boyoffi9.scifikit.core.TouchMode
import com.boyoffi9.scifikit.core.animationsEnabled
import kotlin.math.ceil

/**
 * Text that decodes from random characters on attach, glitches in short
 * bursts, and glitches on touch.
 */
class GlitchTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    private val painter = GlitchTextPainter()

    var text: CharSequence
        get() = painter.text
        set(value) {
            painter.text = value.toString()
            if (contentDescription.isNullOrEmpty()) contentDescription = painter.text
            requestLayout()
            replay()
        }

    var textColor: Int
        get() = painter.color
        set(value) {
            painter.color = value
            invalidate()
        }

    var intensity: Float
        get() = painter.intensity
        set(value) {
            painter.intensity = value
            invalidate()
        }

    var glitch: GlitchConfig
        get() = painter.glitch
        set(value) {
            painter.glitch = value
            invalidate()
        }

    var touch: TouchConfig
        get() = painter.touch
        set(value) {
            painter.touch = value
            invalidate()
        }

    var decode: DecodeConfig
        get() = painter.decode
        set(value) {
            painter.decode = value
            replay()
        }

    var splitColorA: Int
        get() = painter.splitColorA
        set(value) {
            painter.splitColorA = value
            invalidate()
        }

    var splitColorB: Int
        get() = painter.splitColorB
        set(value) {
            painter.splitColorB = value
            invalidate()
        }

    var glitchIntervalMs: Long
        get() = painter.glitchIntervalMs
        set(value) {
            painter.glitchIntervalMs = value
            invalidate()
        }

    var burstDurationMs: Long
        get() = painter.burstDurationMs
        set(value) {
            painter.burstDurationMs = value
            invalidate()
        }

    var textSizePx: Float
        get() = painter.textSizePx
        set(value) {
            painter.textSizePx = value
            requestLayout()
            invalidate()
        }

    var typeface: Typeface
        get() = painter.typeface
        set(value) {
            painter.typeface = value
            requestLayout()
            invalidate()
        }

    init {
        painter.textSizePx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP, 28f, context.resources.displayMetrics,
        )
        context.withStyledAttributes(attrs, R.styleable.GlitchTextView, defStyleAttr, 0) {
            getText(R.styleable.GlitchTextView_android_text)?.let { painter.text = it.toString() }
            painter.textSizePx = getDimension(R.styleable.GlitchTextView_android_textSize, painter.textSizePx)
            painter.color = getColor(R.styleable.GlitchTextView_android_textColor, painter.color)
            painter.splitColorA = getColor(R.styleable.GlitchTextView_sfSplitColorA, painter.splitColorA)
            painter.splitColorB = getColor(R.styleable.GlitchTextView_sfSplitColorB, painter.splitColorB)
            painter.glitch = readGlitch()
            painter.decode = readDecode()
            painter.touch = readTouch()
        }
        if (contentDescription.isNullOrEmpty() && painter.text.isNotEmpty()) {
            contentDescription = painter.text
        }
    }

    private fun TypedArray.readGlitch(): GlitchConfig {
        var config = GlitchMode.entries[getInt(R.styleable.GlitchTextView_sfGlitchMode, 1)].config
        if (hasValue(R.styleable.GlitchTextView_sfIntensity)) {
            config = config.copy(intensity = getFloat(R.styleable.GlitchTextView_sfIntensity, 1f))
        }
        if (hasValue(R.styleable.GlitchTextView_sfGlitchIntervalMs)) {
            config = config.copy(intervalMs = getInt(R.styleable.GlitchTextView_sfGlitchIntervalMs, 0).toLong())
        }
        if (hasValue(R.styleable.GlitchTextView_sfBurstDurationMs)) {
            config = config.copy(burstMs = getInt(R.styleable.GlitchTextView_sfBurstDurationMs, 0).toLong())
        }
        if (hasValue(R.styleable.GlitchTextView_sfGlitchJitter)) {
            config = config.copy(jitter = getFloat(R.styleable.GlitchTextView_sfGlitchJitter, 0f))
        }
        if (hasValue(R.styleable.GlitchTextView_sfGlitchStrips)) {
            config = config.copy(strips = getInt(R.styleable.GlitchTextView_sfGlitchStrips, 6))
        }
        if (hasValue(R.styleable.GlitchTextView_sfGlitchSliceChance)) {
            config = config.copy(sliceChance = getFloat(R.styleable.GlitchTextView_sfGlitchSliceChance, 0.4f))
        }
        if (hasValue(R.styleable.GlitchTextView_sfGlitchSliceReach)) {
            config = config.copy(sliceReach = getFloat(R.styleable.GlitchTextView_sfGlitchSliceReach, 0.25f))
        }
        if (hasValue(R.styleable.GlitchTextView_sfGlitchDropout)) {
            config = config.copy(dropout = getFloat(R.styleable.GlitchTextView_sfGlitchDropout, 0f))
        }
        if (hasValue(R.styleable.GlitchTextView_sfGlitchFlicker)) {
            config = config.copy(flicker = getFloat(R.styleable.GlitchTextView_sfGlitchFlicker, 0f))
        }
        return config
    }

    private fun TypedArray.readTouch(): TouchConfig {
        var config = TouchMode.entries[getInt(R.styleable.GlitchTextView_sfTouchMode, 0)].config
        if (hasValue(R.styleable.GlitchTextView_sfTouchRadius)) {
            config = config.copy(radius = getFloat(R.styleable.GlitchTextView_sfTouchRadius, 1.5f))
        }
        if (hasValue(R.styleable.GlitchTextView_sfChargeMs)) {
            config = config.copy(chargeMs = getInt(R.styleable.GlitchTextView_sfChargeMs, 0).toLong())
        }
        if (hasValue(R.styleable.GlitchTextView_sfChargeBoost)) {
            config = config.copy(chargeBoost = getFloat(R.styleable.GlitchTextView_sfChargeBoost, 2f))
        }
        return config
    }

    private fun TypedArray.readDecode(): DecodeConfig {
        var config = DecodeMode.entries[getInt(R.styleable.GlitchTextView_sfDecodeMode, 0)].config
        if (hasValue(R.styleable.GlitchTextView_sfDecodeOrder)) {
            config = config.copy(order = DecodeOrder.entries[getInt(R.styleable.GlitchTextView_sfDecodeOrder, 0)])
        }
        if (hasValue(R.styleable.GlitchTextView_sfDecodeDurationMs)) {
            config = config.copy(durationMs = getInt(R.styleable.GlitchTextView_sfDecodeDurationMs, 0).toLong())
        }
        getString(R.styleable.GlitchTextView_sfDecodeCharset)?.takeIf { it.isNotEmpty() }?.let {
            config = config.copy(charset = it)
        }
        if (hasValue(R.styleable.GlitchTextView_sfDecodeSwapMs)) {
            config = config.copy(swapMs = getInt(R.styleable.GlitchTextView_sfDecodeSwapMs, 0).toLong())
        }
        if (hasValue(R.styleable.GlitchTextView_sfDecodeShimmer)) {
            config = config.copy(shimmer = getFloat(R.styleable.GlitchTextView_sfDecodeShimmer, 0f))
        }
        if (hasValue(R.styleable.GlitchTextView_sfDecodeScramble)) {
            config = config.copy(scramble = getBoolean(R.styleable.GlitchTextView_sfDecodeScramble, true))
        }
        if (hasValue(R.styleable.GlitchTextView_sfDecodeCursor)) {
            config = config.copy(cursor = getBoolean(R.styleable.GlitchTextView_sfDecodeCursor, false))
        }
        return config
    }

    /** Plays the decode animation again. */
    fun replay() {
        painter.restart(SystemClock.uptimeMillis())
        invalidate()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        replay()
    }

    override fun onWindowFocusChanged(hasWindowFocus: Boolean) {
        super.onWindowFocusChanged(hasWindowFocus)
        if (hasWindowFocus) invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = ceil(painter.contentWidth() + paddingLeft + paddingRight).toInt()
        val h = ceil(painter.contentHeight() + paddingTop + paddingBottom).toInt()
        setMeasuredDimension(resolveSize(w, widthMeasureSpec), resolveSize(h, heightMeasureSpec))
    }

    override fun onDraw(canvas: Canvas) {
        painter.animate = animationsEnabled()
        canvas.save()
        canvas.translate(paddingLeft.toFloat(), paddingTop.toFloat())
        painter.draw(canvas, SystemClock.uptimeMillis())
        canvas.restore()
        if (painter.animate) postInvalidateOnAnimation()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val now = SystemClock.uptimeMillis()
        val x = event.x - paddingLeft
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                painter.press(x, now)
                invalidate()
                if (painter.touch.follow || painter.touch.charge) {
                    super.onTouchEvent(event)
                    return true
                }
            }
            MotionEvent.ACTION_MOVE -> painter.moveTo(x)
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                painter.release(now)
                invalidate()
            }
        }
        return super.onTouchEvent(event)
    }
}
