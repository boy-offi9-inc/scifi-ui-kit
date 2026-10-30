package com.boyoffi9.scifikit

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.os.SystemClock
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import androidx.core.content.withStyledAttributes
import com.boyoffi9.scifikit.core.GlitchTextPainter
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
            painter.intensity = getFloat(R.styleable.GlitchTextView_sfIntensity, painter.intensity)
            painter.glitchIntervalMs =
                getInt(R.styleable.GlitchTextView_sfGlitchIntervalMs, painter.glitchIntervalMs.toInt()).toLong()
            painter.decodeDurationMs =
                getInt(R.styleable.GlitchTextView_sfDecodeDurationMs, painter.decodeDurationMs.toInt()).toLong()
        }
        if (contentDescription.isNullOrEmpty() && painter.text.isNotEmpty()) {
            contentDescription = painter.text
        }
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
        if (event.actionMasked == MotionEvent.ACTION_DOWN) {
            painter.triggerBurst(SystemClock.uptimeMillis())
            invalidate()
        }
        return super.onTouchEvent(event)
    }
}
