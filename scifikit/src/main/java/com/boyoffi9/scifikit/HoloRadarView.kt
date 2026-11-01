package com.boyoffi9.scifikit

import android.content.Context
import android.graphics.Canvas
import android.os.SystemClock
import android.util.AttributeSet
import android.view.View
import androidx.core.content.withStyledAttributes
import com.boyoffi9.scifikit.core.HoloRadarPainter
import com.boyoffi9.scifikit.core.RadarBlip
import com.boyoffi9.scifikit.core.animationsEnabled

/**
 * A holographic radar with a rotating sweep and pinging blips.
 * Defaults to 220dp square when you use wrap_content.
 */
class HoloRadarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    private val painter = HoloRadarPainter()
    private val defaultSizePx: Int

    var accentColor: Int
        get() = painter.accentColor
        set(value) {
            painter.accentColor = value
            invalidate()
        }

    var sweep: Boolean
        get() = painter.sweep
        set(value) {
            painter.sweep = value
            invalidate()
        }

    /** The contacts to show. Replace the list to move or recolour them. */
    var blips: List<RadarBlip>
        get() = painter.blips
        set(value) {
            painter.blips = value
            invalidate()
        }

    init {
        val density = context.resources.displayMetrics.density
        defaultSizePx = (220f * density).toInt()
        painter.strokePx = 1.5f * density

        context.withStyledAttributes(attrs, R.styleable.HoloRadarView, defStyleAttr, 0) {
            painter.accentColor = getColor(R.styleable.HoloRadarView_sfAccentColor, painter.accentColor)
            painter.strokePx = getDimension(R.styleable.HoloRadarView_sfStrokeWidth, painter.strokePx)
            painter.sweep = getBoolean(R.styleable.HoloRadarView_sfSweep, true)
            painter.rings = getInt(R.styleable.HoloRadarView_sfRings, painter.rings).coerceAtLeast(1)
            painter.rotationPeriodMs =
                getInt(R.styleable.HoloRadarView_sfRotationPeriodMs, painter.rotationPeriodMs.toInt()).toLong()
            if (hasValue(R.styleable.HoloRadarView_sfBlipCount)) {
                painter.blips = HoloRadarPainter.defaultBlips(getInt(R.styleable.HoloRadarView_sfBlipCount, 0))
            }
        }

        // Purely decorative unless the app gives it a description.
        if (contentDescription.isNullOrEmpty()) {
            importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = resolveSize(defaultSizePx + paddingLeft + paddingRight, widthMeasureSpec)
        val h = resolveSize(defaultSizePx + paddingTop + paddingBottom, heightMeasureSpec)
        setMeasuredDimension(w, h)
    }

    override fun onDraw(canvas: Canvas) {
        painter.animate = animationsEnabled()
        canvas.save()
        canvas.translate(paddingLeft.toFloat(), paddingTop.toFloat())
        painter.draw(
            canvas,
            (width - paddingLeft - paddingRight).toFloat(),
            (height - paddingTop - paddingBottom).toFloat(),
            SystemClock.uptimeMillis(),
        )
        canvas.restore()
        if (painter.animate && painter.sweep) postInvalidateOnAnimation()
    }
}
