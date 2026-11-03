package com.boyoffi9.scifikit.core

enum class DecodeOrder { LeftToRight, RightToLeft, CenterOut, Random }

object DecodeCharsets {
    const val Default = "01<>/|[]{}#%&*+=-ABCDEFXYZ"
    const val Binary = "01"
    const val Hex = "0123456789ABCDEF"
    const val Symbols = "<>/|[]{}#%&*+=-"
    const val Soft = ".:-=+*"
}

/**
 * How text resolves into its final string.
 *
 * [shimmer] is 0..1: how often already-resolved letters flicker back to a random
 * character. [scramble] false leaves unresolved positions blank instead of random.
 * [cursor] draws a block at the next letter to resolve.
 */
data class DecodeConfig(
    val order: DecodeOrder = DecodeOrder.LeftToRight,
    val durationMs: Long = 900L,
    val charset: String = DecodeCharsets.Default,
    val swapMs: Long = 55L,
    val shimmer: Float = 0f,
    val scramble: Boolean = true,
    val cursor: Boolean = false,
)

/** Ready-made decode styles. Copy [config] to tune one: `DecodeMode.Relaxed.config.copy(durationMs = 2000)`. */
enum class DecodeMode(val config: DecodeConfig) {
    Classic(DecodeConfig()),
    Relaxed(DecodeConfig(durationMs = 1600L, charset = DecodeCharsets.Soft, swapMs = 120L)),
    Active(DecodeConfig(durationMs = 700L, swapMs = 35L, shimmer = 0.35f)),
    Random(DecodeConfig(order = DecodeOrder.Random, durationMs = 1100L)),
    CenterOut(DecodeConfig(order = DecodeOrder.CenterOut, durationMs = 1000L)),
    Typewriter(DecodeConfig(durationMs = 1200L, swapMs = 70L, scramble = false, cursor = true)),
}
