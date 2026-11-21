package com.boyoffi9.scifikit.compose

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.boyoffi9.scifikit.core.NeonGaugePainter
import com.boyoffi9.scifikit.core.NeonGaugePainter.Readout
import kotlin.math.roundToInt

@Composable
fun NeonGauge(
    progress: Float,
    modifier: Modifier = Modifier,
    value: Float = progress,
    maxValue: Float = 100f,
    unit: String = "%",
    readout: Readout = Readout.Percent,
    sweepDegrees: Float = 300f,
    ticks: Int = 0,
    warningAt: Float = 0.85f,
    textSize: TextUnit = 34.sp,
    strokeWidth: Dp = 8.dp,
    accentColor: Color = Color(LocalSciFiPalette.current.accent),
    warningColor: Color = Color(LocalSciFiPalette.current.secondary),
    trackColor: Color = Color(LocalSciFiPalette.current.dim),
    textColor: Color = Color(LocalSciFiPalette.current.text),
) {
    val density = LocalDensity.current
    val glow = LocalSciFiPalette.current.glow
    val painter = remember { NeonGaugePainter() }
    val animate = rememberAnimationsEnabled()
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = if (animate) 450 else 0),
        label = "neonGauge",
    )

    painter.progress = animated
    painter.value = value
    painter.maxValue = maxValue
    painter.unit = unit
    painter.readout = readout
    painter.sweepDegrees = sweepDegrees
    painter.ticks = ticks
    painter.warning = warningAt
    painter.accentColor = accentColor.toArgb()
    painter.warningColor = warningColor.toArgb()
    painter.trackColor = trackColor.toArgb()
    painter.textColor = textColor.toArgb()
    painter.glow = glow
    painter.strokePx = with(density) { strokeWidth.toPx() }
    painter.textSizePx = with(density) { textSize.toPx() }
    painter.animate = false // Compose drives the tween

    val pct = (progress.coerceIn(0f, 1f) * 100).roundToInt()
    val desc = when (readout) {
        Readout.Percent -> "Gauge $pct percent"
        Readout.Value -> "Gauge ${value.roundToInt()} $unit"
        Readout.None -> "Gauge"
    }
    Layout(
        content = {},
        modifier = modifier
            .semantics { contentDescription = desc }
            .drawBehind {
                drawIntoCanvas { painter.draw(it.nativeCanvas, size.width, size.height, 0L) }
            },
    ) { _, constraints ->
        val side = minOf(constraints.maxWidth, constraints.maxHeight)
        layout(constraints.constrainWidth(side), constraints.constrainHeight(side)) {}
    }
}
