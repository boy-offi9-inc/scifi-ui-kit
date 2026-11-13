package com.boyoffi9.scifikit.core

import kotlin.math.PI
import kotlin.math.sin

data class NeonConfig(
    val fill: Float = 0.08f,
    val border: Boolean = true,
    val underline: Boolean = false,
    val inverseText: Boolean = false,
    val corner: Float = 0.3f,
    val glow: Float = 1f,
    val pulse: Float = 0f,
    val surge: Float = 1f,
    val flicker: Float = 0.5f,
    val surgeMs: Long = 360L,
)

enum class NeonStyle(val config: NeonConfig) {
    Outline(NeonConfig()),
    Solid(NeonConfig(fill = 0.92f, inverseText = true, glow = 0.8f)),
    Ghost(NeonConfig(fill = 0f, border = false, underline = true, corner = 0f)),
}

private const val PULSE_PERIOD_MS = 2400.0

internal fun surgeLevel(config: NeonConfig, sinceMs: Long): Float {
    if (sinceMs < 0L || config.surgeMs <= 0L) return 0f
    return (1f - sinceMs.toFloat() / config.surgeMs).coerceIn(0f, 1f)
}

internal fun flickerAlpha(config: NeonConfig, level: Float, bucket: Int): Float {
    if (level <= 0f || config.flicker <= 0f) return 1f
    val strength = config.flicker.coerceAtMost(1f) * level.coerceAtMost(1f)
    return 1f - strength * (hashOf(bucket, 17) % 100 / 100f)
}

internal fun pulseFactor(config: NeonConfig, nowMs: Long): Float {
    if (config.pulse <= 0f) return 1f
    return 1f + config.pulse * 0.5f * sin(2.0 * PI * (nowMs % PULSE_PERIOD_MS.toLong()) / PULSE_PERIOD_MS).toFloat()
}
