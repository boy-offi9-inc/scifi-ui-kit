package com.boyoffi9.scifikit.core

/** Scales the alpha channel of [color] by [factor] (0..1). */
internal fun withAlpha(color: Int, factor: Float): Int {
    val base = color ushr 24
    val a = (base * factor).toInt().coerceIn(0, 255)
    return (color and 0x00FFFFFF) or (a shl 24)
}

/** Linear mix of two ARGB colors. [f] = 0 gives [from], 1 gives [to]. */
internal fun mixColors(from: Int, to: Int, f: Float): Int {
    fun ch(shift: Int): Int {
        val a = (from shr shift) and 0xFF
        val b = (to shr shift) and 0xFF
        return (a + (b - a) * f).toInt().coerceIn(0, 255)
    }
    return (ch(24) shl 24) or (ch(16) shl 16) or (ch(8) shl 8) or ch(0)
}
