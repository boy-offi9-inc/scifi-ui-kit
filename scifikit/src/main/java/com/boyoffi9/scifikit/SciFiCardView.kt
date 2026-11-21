package com.boyoffi9.scifikit

import android.content.Context
import android.graphics.Canvas
import android.os.SystemClock
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.core.content.withStyledAttributes
import com.boyoffi9.scifikit.core.CardConfig
import com.boyoffi9.scifikit.core.CardStyle
import com.boyoffi9.scifikit.core.SciFiCardPainter
import com.boyoffi9.scifikit.core.animationsEnabled

class SciFiCardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {

    private val painter = SciFiCardPainter()

    var accentColor: Int
        get() = painter.accentColor
        set(value) {
            painter.accentColor = value
            invalidate()
        }

    var card: CardConfig
        get() = painter.config
        set(value) {
            painter.config = value
            invalidate()
        }

    var cutPx: Float
        get() = painter.cutPx
        set(value) {
            painter.cutPx = value
            invalidate()
        }

    var strokeWidthPx: Float
        get() = painter.strokePx
        set(value) {
            painter.strokePx = value
            invalidate()
        }

    init {
        setWillNotDraw(false)
        val density = context.resources.displayMetrics.density
        painter.cutPx = 14f * density
        painter.strokePx = 1.5f * density

        val palette = context.sciFiPalette()
        painter.accentColor = palette.accent
        painter.glow = palette.glow

        context.withStyledAttributes(attrs, R.styleable.SciFiCardView, defStyleAttr, 0) {
            painter.accentColor = getColor(R.styleable.SciFiCardView_sfAccentColor, painter.accentColor)
            painter.cutPx = getDimension(R.styleable.SciFiCardView_sfCardCut, painter.cutPx)
            painter.strokePx = getDimension(R.styleable.SciFiCardView_sfStrokeWidth, painter.strokePx)
            var config = CardStyle.entries[getInt(R.styleable.SciFiCardView_sfCardStyle, 0)].config
            if (hasValue(R.styleable.SciFiCardView_sfCardFill)) {
                config = config.copy(fill = getFloat(R.styleable.SciFiCardView_sfCardFill, config.fill))
            }
            if (hasValue(R.styleable.SciFiCardView_sfCardGrid)) {
                config = config.copy(grid = getFloat(R.styleable.SciFiCardView_sfCardGrid, config.grid))
            }
            if (hasValue(R.styleable.SciFiCardView_sfCardGlow)) {
                config = config.copy(glow = getFloat(R.styleable.SciFiCardView_sfCardGlow, config.glow))
            }
            if (hasValue(R.styleable.SciFiCardView_sfCardMarch)) {
                config = config.copy(march = getFloat(R.styleable.SciFiCardView_sfCardMarch, config.march))
            }
            painter.config = config
        }

        if (paddingLeft == 0 && paddingTop == 0 && paddingRight == 0 && paddingBottom == 0) {
            val pad = (20f * density).toInt()
            setPadding(pad, pad, pad, pad)
        }
    }

    override fun onWindowFocusChanged(hasWindowFocus: Boolean) {
        super.onWindowFocusChanged(hasWindowFocus)
        if (hasWindowFocus) invalidate()
    }

    override fun dispatchDraw(canvas: Canvas) {
        painter.animate = animationsEnabled()
        painter.draw(canvas, width.toFloat(), height.toFloat(), SystemClock.uptimeMillis())
        super.dispatchDraw(canvas)
        if (painter.isAnimating()) postInvalidateOnAnimation()
    }
}
