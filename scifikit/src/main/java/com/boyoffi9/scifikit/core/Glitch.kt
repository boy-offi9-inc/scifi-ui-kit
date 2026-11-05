package com.boyoffi9.scifikit.core

data class GlitchConfig(
    val intensity: Float = 1f,
    val intervalMs: Long = 2600L,
    val burstMs: Long = 260L,
    val jitter: Float = 0f,
    val strips: Int = 6,
    val sliceChance: Float = 0.4f,
    val sliceReach: Float = 0.25f,
    val splitIdle: Float = 0.012f,
    val splitBurst: Float = 0.06f,
    val stepMs: Long = 45L,
    val dropout: Float = 0f,
    val flicker: Float = 0f,
)

enum class GlitchMode(val config: GlitchConfig) {
    Subtle(
        GlitchConfig(
            intensity = 0.6f,
            intervalMs = 3600L,
            burstMs = 180L,
            jitter = 0.4f,
            strips = 4,
            sliceChance = 0.2f,
            sliceReach = 0.12f,
            splitIdle = 0.008f,
            splitBurst = 0.035f,
        ),
    ),
    Standard(GlitchConfig()),
    Violent(
        GlitchConfig(
            intensity = 1.8f,
            intervalMs = 1500L,
            burstMs = 420L,
            jitter = 0.5f,
            strips = 10,
            sliceChance = 0.7f,
            sliceReach = 0.4f,
            splitIdle = 0.02f,
            splitBurst = 0.1f,
            stepMs = 30L,
        ),
    ),
    SignalLoss(
        GlitchConfig(
            intensity = 1.2f,
            intervalMs = 2200L,
            burstMs = 520L,
            jitter = 0.6f,
            strips = 8,
            sliceChance = 0.55f,
            sliceReach = 0.3f,
            splitBurst = 0.07f,
            stepMs = 60L,
            dropout = 0.35f,
            flicker = 0.5f,
        ),
    ),
}

private const val MAX_SHIFT = 0.45f
private const val MAX_SPLIT = 0.14f
internal const val REAL_SHARE = 0.35f

internal fun sliceShift(config: GlitchConfig, textSizePx: Float, power: Float): Float =
    minOf(textSizePx * config.sliceReach * power, textSizePx * MAX_SHIFT)

internal fun splitShift(config: GlitchConfig, textSizePx: Float, power: Float, variation: Float): Float =
    minOf(textSizePx * config.splitBurst * power * (0.5f + variation), textSizePx * MAX_SPLIT)

internal fun burstActive(config: GlitchConfig, elapsedMs: Long): Boolean {
    val interval = config.intervalMs
    if (elapsedMs < 0L || interval <= 0L || config.burstMs <= 0L || config.intensity <= 0f) return false
    val phase = elapsedMs % interval
    var length = config.burstMs.coerceAtMost(interval)
    var end = interval
    val jitter = config.jitter.coerceIn(0f, 1f)
    if (jitter > 0f) {
        val cycle = (elapsedMs / interval).toInt()
        val scale = 1f + jitter * (hashOf(cycle, 31) % 1000 / 1000f - 0.5f)
        length = (length * scale).toLong().coerceIn(1L, interval)
        val slack = ((interval - length) * jitter * (hashOf(cycle, 53) % 1000 / 1000f)).toLong()
        end = interval - slack
    }
    return phase >= end - length && phase < end
}
