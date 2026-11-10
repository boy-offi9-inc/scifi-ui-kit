package com.boyoffi9.scifikit

import android.content.Context
import com.boyoffi9.scifikit.core.SciFiPalette

fun Context.sciFiPalette(): SciFiPalette {
    val defaults = SciFiPalette()
    val a = obtainStyledAttributes(R.styleable.SciFiTheme)
    try {
        return SciFiPalette(
            accent = a.getColor(R.styleable.SciFiTheme_sfThemeAccent, defaults.accent),
            secondary = a.getColor(R.styleable.SciFiTheme_sfThemeSecondary, defaults.secondary),
            text = a.getColor(R.styleable.SciFiTheme_sfThemeText, defaults.text),
            dim = a.getColor(R.styleable.SciFiTheme_sfThemeDim, defaults.dim),
            background = a.getColor(R.styleable.SciFiTheme_sfThemeBackground, defaults.background),
            glow = a.getFloat(R.styleable.SciFiTheme_sfThemeGlow, defaults.glow),
        )
    } finally {
        a.recycle()
    }
}
