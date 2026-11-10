package com.boyoffi9.scifikit.core

import kotlin.math.pow

enum class BlipShape { Dot, Diamond, Square, Cross }

data class RadarConfig(
    val blipShape: BlipShape = BlipShape.Dot,
    val blipFade: Float = 2f,
    val trailDeg: Float = 70f,
    val trailAlpha: Float = 0.5f,
    val ringsDashed: Boolean = false,
    val ticks: Int = 36,
    val cardinals: Boolean = false,
    val grid: Boolean = false,
    val scanlines: Boolean = false,
    val scanBand: Boolean = false,
    val glow: Float = 1f,
    val fill: Float = 0.06f,
    val flicker: Float = 0f,
    val split: Float = 0f,
    val secondaryColor: Int = 0,
    val alternate: Boolean = false,
)

enum class RadarMode(val config: RadarConfig) {
    Classic(RadarConfig()),
    Tactical(
        RadarConfig(
            blipShape = BlipShape.Diamond,
            blipFade = 3f,
            trailDeg = 38f,
            trailAlpha = 0.4f,
            ringsDashed = true,
            ticks = 72,
            cardinals = true,
            grid = true,
            glow = 0.3f,
            fill = 0.04f,
        ),
    ),
    Cyberpunk(
        RadarConfig(
            blipShape = BlipShape.Square,
            trailDeg = 95f,
            trailAlpha = 0.6f,
            ticks = 48,
            glow = 1.6f,
            fill = 0.08f,
            flicker = 0.18f,
            split = 2.5f,
            alternate = true,
        ),
    ),
    Phosphor(
        RadarConfig(
            blipShape = BlipShape.Dot,
            blipFade = 0.7f,
            trailDeg = 130f,
            trailAlpha = 0.55f,
            ticks = 24,
            scanlines = true,
            glow = 0.7f,
            fill = 0.1f,
            flicker = 0.08f,
        ),
    ),
    Hologram(
        RadarConfig(
            blipShape = BlipShape.Cross,
            trailDeg = 60f,
            trailAlpha = 0.45f,
            ticks = 60,
            scanBand = true,
            glow = 1.3f,
            fill = 0.12f,
            flicker = 0.25f,
        ),
    ),
}

internal fun blipAlpha(config: RadarConfig, sinceDeg: Float): Float {
    val fade = (1f - sinceDeg / 360f).coerceIn(0f, 1f)
    val power = if (config.blipFade == 2f) fade * fade else fade.pow(config.blipFade.coerceAtLeast(0.05f))
    return 0.15f + 0.85f * power
}

internal fun flickerFactor(config: RadarConfig, bucket: Int): Float {
    if (config.flicker <= 0f) return 1f
    return 1f - config.flicker.coerceAtMost(1f) * (hashOf(bucket, 3) % 100 / 100f)
}

internal fun radarSecondary(config: RadarConfig, accent: Int, paletteSecondary: Int): Int = when {
    config.secondaryColor != 0 -> config.secondaryColor
    config.alternate -> paletteSecondary
    else -> accent
}
