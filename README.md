# SciFiKit

Sci-fi UI pieces for Android, drawn on Canvas. No GIFs, no Lottie, no image assets.
Works in XML Views and Jetpack Compose. Both call the same painter classes, so they look identical.

## What's in 0.1

| Piece | View | Compose |
|---|---|---|
| Chamfered HUD panel: cut corners, neon glow, scanlines, corner brackets, light sweep around the border | `HudFrameView` | `HudFrame` |
| Text that decodes from random characters, then glitches (RGB split + sliced displacement) in bursts and on touch | `GlitchTextView` | `GlitchText` |

Planned: `HoloRadarView`, `HexGridView`.

## Usage (XML)

```xml
<com.boyoffi9.scifikit.HudFrameView
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:padding="20dp"
    app:sfAccentColor="#00E5FF"
    app:sfChamfer="14dp"
    app:sfScanlines="true"
    app:sfSweep="true">

    <com.boyoffi9.scifikit.GlitchTextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="SYSTEM ONLINE"
        android:textSize="28sp"
        android:textColor="#E8FDFF"
        app:sfSplitColorA="#FF2D6F"
        app:sfSplitColorB="#00E5FF"
        app:sfIntensity="1"
        app:sfGlitchIntervalMs="2600"
        app:sfDecodeDurationMs="900" />

</com.boyoffi9.scifikit.HudFrameView>
```

## Usage (Compose)

```kotlin
HudFrame(accentColor = Color(0xFF00E5FF)) {
    GlitchText(text = "SYSTEM ONLINE", fontSize = 28.sp)
}
```

Change `replayKey` on `GlitchText`, or call `replay()` on the View, to play the decode again.

## Design notes

- Chamfered corners (top-left, bottom-right) instead of rounded ones, like cut metal plates.
- Neon on near-black, with a hot-pink / cyan RGB split on text.
- Glow is faked with wide low-alpha strokes, so there is no blur and no software layer.
- Glitch frames come from a hash of the time bucket, so there are no allocations per frame beyond one short string while decoding.
- If the user turned animations off, painters draw one calm static frame.