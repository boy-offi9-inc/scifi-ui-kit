package com.boyoffi9.scifikit

import android.content.Context
import android.content.res.TypedArray
import android.graphics.Canvas
import android.os.SystemClock
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.content.withStyledAttributes
import com.boyoffi9.scifikit.core.NeonButtonPainter
import com.boyoffi9.scifikit.core.NeonConfig
import com.boyoffi9.scifikit.core.NeonStyle
import com.boyoffi9.scifikit.core.animationsEnabled
import kotlin.math.ceil
import kotlin.math.max

class NeonButtonView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    private val painter = NeonButtonPainter()
    private val density = context.resources.displayMetrics.density

    var text: CharSequence
        get() = painter.text
        set(value) {
            painter.text = value.toString()
            if (contentDescription.isNullOrEmpty()) contentDescription = painter.text
            requestLayout()
            invalidate()
        }

    var accentColor: Int
        get() = painter.accentColor
        set(value) {
            painter.accentColor = value
            invalidate()
        }

    var labelColor: Int
        get() = painter.labelColor
        set(value) {
            painter.labelColor = value
            invalidate()
        }

    var onAccentColor: Int
        get() = painter.onAccentColor
        set(value) {
            painter.onAccentColor = value
            invalidate()
        }

    var neon: NeonConfig
        get() = painter.config
        set(value) {
            painter.config = value
            invalidate()
        }

    var textSizePx: Float
        get() = painter.textSizePx
        set(value) {
            painter.textSizePx = value
            requestLayout()
            invalidate()
        }

    var strokeWidthPx: Float
        get() = painter.strokePx
        set(value) {
            painter.strokePx = value
            requestLayout()
            invalidate()
        }

    init {
        isClickable = true
        isFocusable = true
        painter.textSizePx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP, 16f, context.resources.displayMetrics,
        )
        painter.strokePx = 1.5f * density

        val palette = context.sciFiPalette()
        painter.accentColor = palette.accent
        painter.labelColor = palette.text
        painter.onAccentColor = palette.background
        painter.glow = palette.glow

        context.withStyledAttributes(attrs, R.styleable.NeonButtonView, defStyleAttr, 0) {
            getText(R.styleable.NeonButtonView_android_text)?.let { painter.text = it.toString() }
            painter.textSizePx = getDimension(R.styleable.NeonButtonView_android_textSize, painter.textSizePx)
            painter.labelColor = getColor(R.styleable.NeonButtonView_android_textColor, painter.labelColor)
            painter.accentColor = getColor(R.styleable.NeonButtonView_sfAccentColor, painter.accentColor)
            painter.onAccentColor = getColor(R.styleable.NeonButtonView_sfOnAccentColor, painter.onAccentColor)
            painter.strokePx = getDimension(R.styleable.NeonButtonView_sfStrokeWidth, painter.strokePx)
            painter.config = readNeon()
        }
        if (contentDescription.isNullOrEmpty() && painter.text.isNotEmpty()) {
            contentDescription = painter.text
        }
    }

    private fun TypedArray.readNeon(): NeonConfig {
        var config = NeonStyle.entries[getInt(R.styleable.NeonButtonView_sfNeonStyle, 0)].config
        if (hasValue(R.styleable.NeonButtonView_sfNeonFill)) {
            config = config.copy(fill = getFloat(R.styleable.NeonButtonView_sfNeonFill, config.fill))
        }
        if (hasValue(R.styleable.NeonButtonView_sfNeonGlow)) {
            config = config.copy(glow = getFloat(R.styleable.NeonButtonView_sfNeonGlow, config.glow))
        }
        if (hasValue(R.styleable.NeonButtonView_sfNeonPulse)) {
            config = config.copy(pulse = getFloat(R.styleable.NeonButtonView_sfNeonPulse, config.pulse))
        }
        if (hasValue(R.styleable.NeonButtonView_sfNeonSurge)) {
            config = config.copy(surge = getFloat(R.styleable.NeonButtonView_sfNeonSurge, config.surge))
        }
        if (hasValue(R.styleable.NeonButtonView_sfNeonFlicker)) {
            config = config.copy(flicker = getFloat(R.styleable.NeonButtonView_sfNeonFlicker, config.flicker))
        }
        if (hasValue(R.styleable.NeonButtonView_sfNeonSurgeMs)) {
            config = config.copy(surgeMs = getInt(R.styleable.NeonButtonView_sfNeonSurgeMs, 0).toLong())
        }
        if (hasValue(R.styleable.NeonButtonView_sfNeonCorner)) {
            config = config.copy(corner = getFloat(R.styleable.NeonButtonView_sfNeonCorner, config.corner))
        }
        return config
    }

    override fun setPressed(pressed: Boolean) {
        val was = isPressed
        super.setPressed(pressed)
        if (pressed == was) return
        if (pressed) painter.press(SystemClock.uptimeMillis()) else painter.release()
        invalidate()
    }

    override fun drawableStateChanged() {
        super.drawableStateChanged()
        invalidate()
    }

    override fun onWindowFocusChanged(hasWindowFocus: Boolean) {
        super.onWindowFocusChanged(hasWindowFocus)
        if (hasWindowFocus) invalidate()
    }

    override fun onInitializeAccessibilityNodeInfo(info: AccessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(info)
        info.className = android.widget.Button::class.java.name
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val pad = painter.glowPadPx
        val wanted = ceil(painter.textWidthPx() + 2f * HORIZONTAL_PADDING_DP * density + 2f * pad).toInt() +
            paddingLeft + paddingRight
        val tall = max(MIN_HEIGHT_DP * density, painter.textSizePx * 2.2f + 2f * pad)
        val high = ceil(tall).toInt() + paddingTop + paddingBottom
        setMeasuredDimension(resolveSize(wanted, widthMeasureSpec), resolveSize(high, heightMeasureSpec))
    }

    override fun onDraw(canvas: Canvas) {
        val now = SystemClock.uptimeMillis()
        painter.animate = animationsEnabled()
        painter.enabled = isEnabled
        painter.focused = isFocused
        canvas.save()
        canvas.translate(paddingLeft.toFloat(), paddingTop.toFloat())
        painter.draw(
            canvas,
            (width - paddingLeft - paddingRight).toFloat(),
            (height - paddingTop - paddingBottom).toFloat(),
            now,
        )
        canvas.restore()
        if (painter.isAnimating(now)) postInvalidateOnAnimation()
    }

    private companion object {
        const val HORIZONTAL_PADDING_DP = 20f
        const val MIN_HEIGHT_DP = 48f
    }
}
