package com.boyoffi9.scifikit.core

data class TouchConfig(
    val follow: Boolean = false,
    val charge: Boolean = false,
    val radius: Float = 1.5f,
    val chargeMs: Long = 900L,
    val chargeBoost: Float = 2f,
)

enum class TouchMode(val config: TouchConfig) {
    Burst(TouchConfig()),
    Follow(TouchConfig(follow = true)),
    Charge(TouchConfig(charge = true)),
    FollowCharge(TouchConfig(follow = true, charge = true)),
}

internal fun chargeLevel(config: TouchConfig, heldMs: Long): Float {
    if (!config.charge || heldMs <= 0L) return 0f
    if (config.chargeMs <= 0L) return 1f
    return (heldMs.toFloat() / config.chargeMs).coerceIn(0f, 1f)
}
