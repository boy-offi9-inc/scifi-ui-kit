package com.boyoffi9.scifikit.compose

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
import com.boyoffi9.scifikit.core.NeonAlertPainter
import com.boyoffi9.scifikit.core.NeonAlertPainter.Severity

@Composable
fun NeonAlert(
    text: String,
    modifier: Modifier = Modifier,
    severity: Severity = Severity.Info,
    filled: Boolean = true,
    showIcon: Boolean = true,
    showChevrons: Boolean = false,
    hazard: Float = 0f,
    fontSize: TextUnit = 14.sp,
    minHeight: Dp = 40.dp,
    accentColor: Color = Color(LocalSciFiPalette.current.accent),
    successColor: Color = Color(0xFF3BFFA0),
    warningColor: Color = Color(0xFFFFB020),
    dangerColor: Color = Color(LocalSciFiPalette.current.secondary),
    textColor: Color = Color(LocalSciFiPalette.current.text),
) {
    val density = LocalDensity.current
    val glow = LocalSciFiPalette.current.glow
    val painter = remember { NeonAlertPainter() }
    val animate = rememberAnimationsEnabled()
    var time by remember { mutableLongStateOf(0L) }

    painter.text = text
    painter.severity = severity
    painter.filled = filled
    painter.icon = showIcon
    painter.chevrons = showChevrons
    painter.hazard = hazard
    painter.accentColor = accentColor.toArgb()
    painter.successColor = successColor.toArgb()
    painter.warningColor = warningColor.toArgb()
    painter.dangerColor = dangerColor.toArgb()
    painter.labelColor = textColor.toArgb()
    painter.glow = glow
    painter.strokePx = with(density) { 1.5.dp.toPx() }
    painter.textSizePx = with(density) { fontSize.toPx() }
    painter.animate = animate

    LaunchedEffect(animate, hazard) {
        if (animate && hazard > 0f) {
            while (true) time = withFrameMillis { it }
        } else {
            time = withFrameMillis { it }
        }
    }

    Layout(
        content = {},
        modifier = modifier
            .semantics { contentDescription = "$severity alert: $text" }
            .drawBehind {
                drawIntoCanvas { painter.draw(it.nativeCanvas, size.width, size.height, time) }
            },
    ) { _, constraints ->
        val pad = painter.strokePx * 4f
        val wantH = painter.textSizePx * 1.8f + pad + (if (hazard > 0f) painter.textSizePx * 0.35f else 0f)
        layout(
            constraints.constrainWidth(constraints.maxWidth),
            constraints.constrainHeight(maxOf(wantH, with(density) { minHeight.toPx() }).toInt()),
        ) {}
    }
}
