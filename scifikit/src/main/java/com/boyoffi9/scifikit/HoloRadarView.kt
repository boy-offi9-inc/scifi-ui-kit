package com.boyoffi9.scifikit

import android.content.Context
import android.content.res.TypedArray
import android.graphics.Canvas
import android.os.SystemClock
import android.util.AttributeSet
import android.view.View
import androidx.core.content.withStyledAttributes
import com.boyoffi9.scifikit.core.HoloRadarPainter
import com.boyoffi9.scifikit.core.RadarBlip
import com.boyoffi9.scifikit.core.RadarConfig
import com.boyoffi9.scifikit.core.RadarMode
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

    var radar: RadarConfig
        get() = painter.radar
        set(value) {
            painter.radar = value
            invalidate()
        }

    var rings: Int
        get() = painter.rings
        set(value) {
            painter.rings = value.coerceAtLeast(1)
            invalidate()
        }

    var rotationPeriodMs: Long
        get() = painter.rotationPeriodMs
        set(value) {
            painter.rotationPeriodMs = value
            invalidate()
        }

    var strokeWidthPx: Float
        get() = painter.strokePx
        set(value) {
            painter.strokePx = value
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
            painter.radar = readRadar()
            if (hasValue(R.styleable.HoloRadarView_sfBlipCount)) {
                painter.blips = HoloRadarPainter.defaultBlips(getInt(R.styleable.HoloRadarView_sfBlipCount, 0))
            }
        }

        if (contentDescription.isNullOrEmpty()) {
            importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
        }
    }

    private fun TypedArray.readRadar(): RadarConfig {
        var config = RadarMode.entries[getInt(R.styleable.HoloRadarView_sfRadarMode, 0)].config
        if (hasValue(R.styleable.HoloRadarView_sfRadarTrailDeg)) {
            config = config.copy(trailDeg = getFloat(R.styleable.HoloRadarView_sfRadarTrailDeg, 70f))
        }
        if (hasValue(R.styleable.HoloRadarView_sfRadarGlow)) {
            config = config.copy(glow = getFloat(R.styleable.HoloRadarView_sfRadarGlow, 1f))
        }
        if (hasValue(R.styleable.HoloRadarView_sfRadarFlicker)) {
            config = config.copy(flicker = getFloat(R.styleable.HoloRadarView_sfRadarFlicker, 0f))
        }
        if (hasValue(R.styleable.HoloRadarView_sfRadarScanlines)) {
            config = config.copy(scanlines = getBoolean(R.styleable.HoloRadarView_sfRadarScanlines, false))
        }
        if (hasValue(R.styleable.HoloRadarView_sfRadarSecondaryColor)) {
            config = config.copy(secondaryColor = getColor(R.styleable.HoloRadarView_sfRadarSecondaryColor, 0))
        }
        return config
    }

    override fun onWindowFocusChanged(hasWindowFocus: Boolean) {
        super.onWindowFocusChanged(hasWindowFocus)
        if (hasWindowFocus) invalidate()
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
