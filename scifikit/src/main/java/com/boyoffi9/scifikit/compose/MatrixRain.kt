package com.boyoffi9.scifikit.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.boyoffi9.scifikit.core.MatrixRainPainter

/**
 * Digital rain wall. Same renderer as [com.boyoffi9.scifikit.MatrixRainView].
 * Give it a size — `Modifier.fillMaxSize()` for a full-screen wall.
 */
@Composable
fun MatrixRain(
    modifier: Modifier = Modifier,
    accentColor: Color = Color(LocalSciFiPalette.current.accent),
    glyphSize: TextUnit = 14.sp,
    density: Float = 0.55f,
    flicker: Boolean = true,
    animate: Boolean = rememberAnimationsEnabled(),
) {
    val painter = remember { MatrixRainPainter() }
    val palette = LocalSciFiPalette.current
    var time by remember { mutableLongStateOf(0L) }

    if (animate) {
        LaunchedEffect(Unit) {
            while (true) {
                withFrameMillis { time = it }
            }
        }
    }

    Box(
        modifier = modifier
            .clipToBounds()
            .drawBehind {
                painter.accentColor = accentColor.toArgb()
                painter.glow = palette.glow
                painter.glyphSizePx = glyphSize.toPx()
                painter.density = density
                painter.flicker = flicker
                painter.animate = animate
                drawIntoCanvas { painter.draw(it.nativeCanvas, this.size.width, this.size.height, time) }
            },
    )
}
