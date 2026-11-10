package com.boyoffi9.scifikit.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import com.boyoffi9.scifikit.core.SciFiPalette

val LocalSciFiPalette = staticCompositionLocalOf { SciFiPalette() }

object SciFiTheme {
    val palette: SciFiPalette
        @Composable
        @ReadOnlyComposable
        get() = LocalSciFiPalette.current
}

@Composable
fun SciFiTheme(palette: SciFiPalette, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalSciFiPalette provides palette, content = content)
}
