package com.boyoffi9.scifikit.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.boyoffi9.scifikit.core.HoloRadarPainter
import com.boyoffi9.scifikit.core.RadarBlip
import com.boyoffi9.scifikit.core.RadarConfig
import com.boyoffi9.scifikit.core.RadarMode

private val DefaultBlips = HoloRadarPainter.defaultBlips(7)

/**
 * Holographic radar. Same renderer as [com.boyoffi9.scifikit.HoloRadarView].
 * Pick a look with [radar], for example `RadarMode.Tactical.config`.
 */
@Composable
fun HoloRadar(
    modifier: Modifier = Modifier,
    size: Dp = 220.dp,
    accentColor: Color = Color(LocalSciFiPalette.current.accent),
    strokeWidth: Dp = 1.5.dp,
    rings: Int = 4,
    sweep: Boolean = true,
    rotationPeriodMs: Long = 4000L,
    radar: RadarConfig = RadarMode.Classic.config,
    blips: List<RadarBlip> = DefaultBlips,
) {
    val painter = remember { HoloRadarPainter() }
    val palette = LocalSciFiPalette.current
    val animate = rememberAnimationsEnabled()
    var time by remember { mutableLongStateOf(0L) }

    if (animate && sweep) {
        LaunchedEffect(Unit) {
            while (true) {
                withFrameMillis { time = it }
            }
        }
    }

    Box(
        modifier = modifier
            .size(size)
            .drawBehind {
                painter.accentColor = accentColor.toArgb()
                painter.glow = palette.glow
                painter.paletteSecondary = palette.secondary
                painter.strokePx = strokeWidth.toPx()
                painter.rings = rings.coerceAtLeast(1)
                painter.sweep = sweep
                painter.rotationPeriodMs = rotationPeriodMs
                painter.radar = radar
                painter.blips = blips
                painter.animate = animate
                drawIntoCanvas { painter.draw(it.nativeCanvas, this.size.width, this.size.height, time) }
            },
    )
}
