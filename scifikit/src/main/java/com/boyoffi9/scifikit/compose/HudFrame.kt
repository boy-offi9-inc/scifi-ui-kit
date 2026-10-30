package com.boyoffi9.scifikit.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
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
import com.boyoffi9.scifikit.core.HudFramePainter
import com.boyoffi9.scifikit.core.animationsEnabled

/** Chamfered sci-fi HUD panel. Same renderer as [com.boyoffi9.scifikit.HudFrameView]. */
@Composable
fun HudFrame(
    modifier: Modifier = Modifier,
    accentColor: Color = Color(0xFF00E5FF),
    fillColor: Color = accentColor.copy(alpha = 0.08f),
    chamfer: Dp = 14.dp,
    strokeWidth: Dp = 1.5.dp,
    bracketLength: Dp = 20.dp,
    scanlines: Boolean = true,
    sweep: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable BoxScope.() -> Unit,
) {
    val painter = remember { HudFramePainter() }
    val animate = remember { animationsEnabled() }
    var time by remember { mutableLongStateOf(0L) }

    if (animate && (scanlines || sweep)) {
        LaunchedEffect(Unit) {
            while (true) {
                withFrameMillis { time = it }
            }
        }
    }

    Box(
        modifier = modifier
            .drawBehind {
                painter.accentColor = accentColor.toArgb()
                painter.fillColor = fillColor.toArgb()
                painter.chamferPx = chamfer.toPx()
                painter.strokePx = strokeWidth.toPx()
                painter.bracketPx = bracketLength.toPx()
                painter.scanlineGapPx = 3.dp.toPx()
                painter.scanlines = scanlines
                painter.sweep = sweep
                painter.animate = animate
                drawIntoCanvas { painter.draw(it.nativeCanvas, size.width, size.height, time) }
            }
            .padding(contentPadding),
        content = content,
    )
}
