package com.boyoffi9.scifikit.core

data class CardConfig(
    val cutTL: Float = 1f,
    val cutTR: Float = 0f,
    val cutBR: Float = 1f,
    val cutBL: Float = 0f,
    val fill: Float = 0.06f,
    val line: Float = 0.6f,
    val layers: Boolean = false,
    val bars: Boolean = false,
    val brackets: Boolean = false,
    val ticks: Int = 0,
    val hazardTop: Boolean = false,
    val hazardBottom: Boolean = false,
    val footer: Boolean = false,
    val target: Boolean = false,
    val arrows: Boolean = false,
    val dots: Boolean = false,
    val grid: Float = 0f,
    val glow: Float = 1f,
    val march: Float = 0f,
)

enum class CardStyle(val config: CardConfig) {
    Panel(
        CardConfig(
            cutTL = 1f,
            cutTR = 0.5f,
            cutBR = 1f,
            cutBL = 0.5f,
            fill = 0.05f,
            layers = true,
            bars = true,
            ticks = 5,
            hazardTop = true,
            hazardBottom = true,
        ),
    ),
    Outline(
        CardConfig(
            fill = 0.03f,
            line = 0.75f,
            brackets = true,
            hazardTop = true,
            footer = true,
            target = true,
            arrows = true,
            dots = true,
            grid = 0.1f,
            layers = true,
            glow = 1.3f,
        ),
    ),
    Targeted(
        CardConfig(
            cutTL = 0.6f,
            cutTR = 0f,
            cutBR = 0.6f,
            cutBL = 0f,
            fill = 0.04f,
            line = 0.8f,
            brackets = true,
            target = true,
            ticks = 4,
            hazardTop = true,
            grid = 0.06f,
            glow = 1.2f,
            march = 1f,
        ),
    ),
    Terminal(
        CardConfig(
            cutTL = 0f,
            cutTR = 1.6f,
            cutBR = 0f,
            cutBL = 0f,
            fill = 0.07f,
            line = 0.5f,
            layers = true,
            dots = true,
            arrows = true,
            glow = 0.9f,
        ),
    ),
    Plain(CardConfig(fill = 0.08f)),
}

internal fun marchOffset(config: CardConfig, nowMs: Long, period: Float): Float {
    if (config.march <= 0f || period <= 0f) return 0f
    return (nowMs * 0.03f * config.march) % period
}
