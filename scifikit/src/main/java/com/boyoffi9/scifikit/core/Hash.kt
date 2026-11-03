package com.boyoffi9.scifikit.core

internal fun hashOf(a: Int, b: Int): Int {
    var h = (a * 73856093) xor (b * 19349663)
    h = h xor (h ushr 13)
    h *= 0x5bd1e995
    h = h xor (h ushr 15)
    return h and 0x7fffffff
}
