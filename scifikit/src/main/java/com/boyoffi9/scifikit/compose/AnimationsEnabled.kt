package com.boyoffi9.scifikit.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.boyoffi9.scifikit.core.animationsEnabled
import kotlinx.coroutines.delay

@Composable
internal fun rememberAnimationsEnabled(): Boolean {
    var enabled by remember { mutableStateOf(animationsEnabled()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L)
            enabled = animationsEnabled()
        }
    }
    return enabled
}
