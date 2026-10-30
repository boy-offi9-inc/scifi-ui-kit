package com.boyoffi9.scifikit.core

import android.animation.ValueAnimator
import android.os.Build

/**
 * False when the user turned animations off (system "remove animations" /
 * animator duration scale 0). Painters then draw a calm, static frame.
 */
internal fun animationsEnabled(): Boolean =
    Build.VERSION.SDK_INT < 26 || ValueAnimator.areAnimatorsEnabled()
