package com.boyoffi9.scifikit.compose

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.layout.Layout
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.boyoffi9.scifikit.core.NeonProgressPainter
import kotlin.math.roundToInt

@Composable
fun NeonProgress(
    label: String,
    progress: Float,
    modifier: Modifier = Modifier,
    showPercent: Boolean = true,
    fontSize: TextUnit = 14.sp,
    barHeight: Dp = 8.dp,
    accentColor: Color = Color(LocalSciFiPalette.current.accent),
    warningColor: Color = Color(LocalSciFiPalette.current.secondary),
    trackColor: Color = Color(LocalSciFiPalette.current.dim),
    textColor: Color = Color(LocalSciFiPalette.current.text),
    warningAt: Float = 0.85f,
    ticks: Int = 8,
    segmented: Boolean = false,
) {
    val density = LocalDensity.current
    val glow = LocalSciFiPalette.current.glow
    val painter = remember { NeonProgressPainter() }
    val animate = rememberAnimationsEnabled()
    // Compose-side animation of the value; painter tracks it 1:1.
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = if (animate) 450 else 0),
        label = "neonProgress",
    )

    painter.label = label
    painter.progress = animated
    painter.accentColor = accentColor.toArgb()
    painter.warningColor = warningColor.toArgb()
    painter.trackColor = trackColor.toArgb()
    painter.labelColor = textColor.toArgb()
    painter.glow = glow
    painter.strokePx = with(density) { 1.5.dp.toPx() }
    painter.textSizePx = with(density) { fontSize.toPx() }
    painter.showPercent = showPercent
    painter.ticks = ticks
    painter.warning = warningAt
    painter.segmented = segmented
    painter.animate = false // painter-side easing off; Compose drives the tween

    val desc = if (showPercent) "$label ${(progress * 100).roundToInt()}%" else label
    Layout(
        content = {},
        modifier = modifier
            .semantics { contentDescription = desc }
            .drawBehind {
                drawIntoCanvas { painter.draw(it.nativeCanvas, size.width, size.height, 0L) }
            },
    ) { _, constraints ->
        val textH = painter.textHeightPx()
        val barH = with(density) { barHeight.toPx() }
        val want = if (showPercent || label.isNotEmpty()) textH + painter.strokePx * 3f + barH else barH
        layout(constraints.constrainWidth(constraints.maxWidth), constraints.constrainHeight(want.roundToInt())) {}
    }
}
