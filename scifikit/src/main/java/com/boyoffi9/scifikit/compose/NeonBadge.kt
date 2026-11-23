package com.boyoffi9.scifikit.compose

import androidx.compose.ui.layout.Layout
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.boyoffi9.scifikit.core.NeonBadgePainter
import com.boyoffi9.scifikit.core.NeonBadgePainter.State

@Composable
fun NeonBadge(
    text: String,
    modifier: Modifier = Modifier,
    state: State = State.Normal,
    filled: Boolean = false,
    showDot: Boolean = false,
    fontSize: TextUnit = 12.sp,
    minWidth: Dp = 64.dp,
    accentColor: Color = Color(LocalSciFiPalette.current.accent),
    warningColor: Color = Color(LocalSciFiPalette.current.secondary),
    dangerColor: Color = Color(LocalSciFiPalette.current.danger),
    successColor: Color = Color(0xFF3BFFA0),
    textColor: Color = Color(LocalSciFiPalette.current.text),
    dimColor: Color = Color(LocalSciFiPalette.current.dim),
) {
    val density = LocalDensity.current
    val glow = LocalSciFiPalette.current.glow
    val painter = remember { NeonBadgePainter() }
    val animate = rememberAnimationsEnabled()
    var time by remember { mutableLongStateOf(0L) }

    painter.text = text
    painter.state = state
    painter.filled = filled
    painter.dot = showDot
    painter.accentColor = accentColor.toArgb()
    painter.warningColor = warningColor.toArgb()
    painter.dangerColor = dangerColor.toArgb()
    painter.successColor = successColor.toArgb()
    painter.labelColor = textColor.toArgb()
    painter.dimColor = dimColor.toArgb()
    painter.glow = glow
    painter.strokePx = with(density) { 1.5.dp.toPx() }
    painter.textSizePx = with(density) { fontSize.toPx() }
    painter.animate = animate

    LaunchedEffect(animate, state, showDot) {
        if (animate && showDot && state != State.Muted) {
            while (true) time = withFrameMillis { it }
        } else {
            time = withFrameMillis { it }
        }
    }

    Layout(
        content = {},
        modifier = modifier
            .semantics { contentDescription = text }
            .drawBehind {
                drawIntoCanvas { painter.draw(it.nativeCanvas, size.width, size.height, time) }
            },
    ) { _, constraints ->
        val pad = painter.strokePx * 8f
        val wantW = painter.textWidthPx() + pad * 2f + (if (showDot) painter.textSizePx else 0f)
        val wantH = painter.textSizePx * 1.9f + pad
        val minW = with(density) { minWidth.toPx() }
        layout(
            constraints.constrainWidth(maxOf(wantW, minW).toInt()),
            constraints.constrainHeight(wantH.toInt()),
        ) {}
    }
}
