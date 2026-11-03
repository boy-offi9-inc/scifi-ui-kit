package com.boyoffi9.scifikit.compose

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.boyoffi9.scifikit.core.DecodeConfig
import com.boyoffi9.scifikit.core.DecodeMode
import com.boyoffi9.scifikit.core.GlitchConfig
import com.boyoffi9.scifikit.core.GlitchMode
import com.boyoffi9.scifikit.core.GlitchTextPainter
import com.boyoffi9.scifikit.core.TouchConfig
import com.boyoffi9.scifikit.core.TouchMode

/**
 * Text that decodes from random characters, glitches in bursts, and glitches
 * when pressed. Same renderer as [com.boyoffi9.scifikit.GlitchTextView].
 *
 * Pick a glitch style with [glitch], for example `GlitchMode.Violent.config`;
 * [intensity] and [glitchIntervalMs] override just those two values.
 * Pick a touch style with [touch]: `TouchMode.Follow` glitches under the finger,
 * `TouchMode.Charge` builds up while held. Presses are never consumed.
 * Pick a decode style with [decode], for example `DecodeMode.Active.config`, and
 * tune it with `copy(...)`. [decodeDurationMs] overrides just the duration.
 * Change [replayKey] to play the decode animation again.
 */
@Composable
fun GlitchText(
    text: String,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 28.sp,
    color: Color = Color(0xFFE8FDFF),
    splitColorA: Color = Color(0xFFFF2D6F),
    splitColorB: Color = Color(0xFF00E5FF),
    glitch: GlitchConfig = GlitchMode.Standard.config,
    intensity: Float? = null,
    glitchIntervalMs: Long? = null,
    touch: TouchConfig = TouchMode.Burst.config,
    decode: DecodeConfig = DecodeMode.Classic.config,
    decodeDurationMs: Long? = null,
    replayKey: Any? = null,
) {
    val density = LocalDensity.current
    val painter = remember { GlitchTextPainter() }
    val animate = rememberAnimationsEnabled()
    var time by remember { mutableLongStateOf(0L) }

    painter.text = text
    painter.textSizePx = with(density) { fontSize.toPx() }
    painter.color = color.toArgb()
    painter.splitColorA = splitColorA.toArgb()
    painter.splitColorB = splitColorB.toArgb()
    var glitchConfig = glitch
    if (intensity != null) glitchConfig = glitchConfig.copy(intensity = intensity)
    if (glitchIntervalMs != null) glitchConfig = glitchConfig.copy(intervalMs = glitchIntervalMs)
    painter.glitch = glitchConfig
    painter.touch = touch
    val config = if (decodeDurationMs != null) decode.copy(durationMs = decodeDurationMs) else decode
    painter.decode = config
    painter.animate = animate

    if (animate) {
        LaunchedEffect(text, replayKey, config) {
            var first = true
            while (true) {
                withFrameMillis { ms ->
                    if (first) {
                        painter.restart(ms)
                        first = false
                    }
                    time = ms
                }
            }
        }
    }

    val width = with(density) { painter.contentWidth().toDp() }
    val height = with(density) { painter.contentHeight().toDp() }

    Box(
        modifier = modifier
            .size(width, height)
            .semantics { contentDescription = text }
            .pointerInput(Unit) {
                // Watch presses in the Initial pass and consume nothing, so a
                // clickable parent (like a HudFrame button) still gets its tap.
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    painter.press(down.position.x, time)
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val change = event.changes.firstOrNull { it.id == down.id }
                        if (change == null || !change.pressed) break
                        painter.moveTo(change.position.x)
                    }
                    painter.release(time)
                }
            }
            .drawBehind {
                drawIntoCanvas { painter.draw(it.nativeCanvas, time) }
            },
    )
}
