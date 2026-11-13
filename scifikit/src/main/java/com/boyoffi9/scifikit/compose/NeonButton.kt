package com.boyoffi9.scifikit.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.boyoffi9.scifikit.core.NeonButtonPainter
import com.boyoffi9.scifikit.core.NeonConfig
import com.boyoffi9.scifikit.core.NeonStyle
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
fun NeonButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    fontSize: TextUnit = 16.sp,
    accentColor: Color = Color(LocalSciFiPalette.current.accent),
    textColor: Color = Color(LocalSciFiPalette.current.text),
    onAccentColor: Color = Color(LocalSciFiPalette.current.background),
    strokeWidth: Dp = 1.5.dp,
    style: NeonConfig = NeonStyle.Outline.config,
) {
    val density = LocalDensity.current
    val glow = LocalSciFiPalette.current.glow
    val painter = remember { NeonButtonPainter() }
    val animate = rememberAnimationsEnabled()
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val focused by interaction.collectIsFocusedAsState()
    var time by remember { mutableLongStateOf(0L) }

    painter.text = text
    painter.textSizePx = with(density) { fontSize.toPx() }
    painter.accentColor = accentColor.toArgb()
    painter.labelColor = textColor.toArgb()
    painter.onAccentColor = onAccentColor.toArgb()
    painter.strokePx = with(density) { strokeWidth.toPx() }
    painter.config = style
    painter.glow = glow
    painter.enabled = enabled
    painter.focused = focused
    painter.animate = animate

    LaunchedEffect(pressed, animate, enabled, style) {
        val start = withFrameMillis { it }
        time = start
        if (pressed) painter.press(start) else painter.release()
        if (!animate || !enabled) return@LaunchedEffect
        while (painter.isAnimating(time)) {
            time = withFrameMillis { it }
        }
    }

    Layout(
        content = {},
        modifier = modifier
            .semantics { contentDescription = text }
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .drawBehind {
                drawIntoCanvas { painter.draw(it.nativeCanvas, size.width, size.height, time) }
            },
    ) { _, constraints ->
        val pad = painter.glowPadPx
        val wanted = (painter.textWidthPx() + 2f * 20.dp.toPx() + 2f * pad).roundToInt()
        val tall = max(48.dp.toPx(), painter.textSizePx * 2.2f + 2f * pad).roundToInt()
        layout(constraints.constrainWidth(wanted), constraints.constrainHeight(tall)) {}
    }
}
