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
import com.boyoffi9.scifikit.core.CardConfig
import com.boyoffi9.scifikit.core.CardStyle
import com.boyoffi9.scifikit.core.SciFiCardPainter

@Composable
fun SciFiCard(
    modifier: Modifier = Modifier,
    accentColor: Color = Color(LocalSciFiPalette.current.accent),
    cut: Dp = 14.dp,
    strokeWidth: Dp = 1.5.dp,
    style: CardConfig = CardStyle.Panel.config,
    contentPadding: PaddingValues = PaddingValues(20.dp),
    content: @Composable BoxScope.() -> Unit,
) {
    val painter = remember { SciFiCardPainter() }
    val glow = LocalSciFiPalette.current.glow
    val animate = rememberAnimationsEnabled()
    var time by remember { mutableLongStateOf(0L) }

    if (animate && style.march > 0f) {
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
                painter.glow = glow
                painter.cutPx = cut.toPx()
                painter.strokePx = strokeWidth.toPx()
                painter.config = style
                painter.animate = animate
                drawIntoCanvas { painter.draw(it.nativeCanvas, size.width, size.height, time) }
            }
            .padding(contentPadding),
        content = content,
    )
}
