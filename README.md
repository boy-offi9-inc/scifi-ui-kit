# SciFiKit

Sci-fi HUD components for Android, drawn entirely on Canvas.
No GIFs, no Lottie, no image assets. Works in XML Views and Jetpack Compose.

[![JitPack](https://jitpack.io/v/boy-offi9-inc/scifi-ui-kit.svg)](https://jitpack.io/#boy-offi9-inc/scifi-ui-kit)
![Min SDK](https://img.shields.io/badge/minSdk-21-brightgreen)
![Kotlin](https://img.shields.io/badge/Kotlin-1.9-7F52FF)

## Components

| Component | View | Compose | What it does |
|---|---|---|---|
| HUD frame | `HudFrameView` | `HudFrame` | Chamfered panel with neon glow, scanlines, corner brackets and a light segment that travels around the border. Holds your content. |
| Glitch text | `GlitchTextView` | `GlitchText` | Text that decodes from random characters, then glitches in bursts (RGB split and sliced displacement) and when touched. Six decode modes, four glitch modes and four touch modes. |
| Holo radar | `HoloRadarView` | `HoloRadar` | Radar with range rings and labels, degree ticks, HUD corner brackets, a rotating sweep with a fading trail, targeting reticles on fresh contacts, and blips that ping when the sweep reaches them. Six looks, from clean to CRT. |
| Sci-fi card | `SciFiCardView` | `SciFiCard` | Content card with a cut-corner frame, layered border, accent bands, hazard stripes, target ring and optional grid. Five styles. |
| Neon button | `NeonButtonView` | `NeonButton` | Button in six styles (outline, solid, ghost, charge, angled, slab). Cut-corner frame, inner echo line, corner notch marks; pressing it surges the glow and flickers the edge. |
| Neon progress | `NeonProgressView` | `NeonProgress` | Labeled HUD data bar — square terminal brackets, layered neon fill (soft halo, bright filament core, hot chamfered leading edge), notch ticks, warning colour past a threshold. |
| Neon badge | `NeonBadgeView` | `NeonBadge` | Hex-framed tag chip in five states (normal, success, warning, danger, muted), with an optional pulsing status dot. |
| Neon gauge | `NeonGaugeView` | `NeonGauge` | HUD instrument dial — the shared HUD chassis (chamfered panel, scanlines, brackets, traveling light), butt-cap arc with a bright head-cap, ticks on an outer ring, bracketed digital readout fitted inside the dial. Warning colour past a threshold. |
| Neon alert | `NeonAlertView` | `NeonAlert` | HUD notification block in four severities (info, success, warning, danger) — cut-corner frame with corner brackets, a severity glyph column on the leading edge, scanline texture, optional marching hazard strip and trailing chevrons. |

Both versions of each component call the same painter class, so they render identically.

## Installation

Add JitPack to your repositories in `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}
```

Add the dependency to your app module:

```kotlin
dependencies {
    implementation("com.github.boy-offi9-inc:scifi-ui-kit:v0.12.0")
}
```

Groovy DSL:

```groovy
maven { url 'https://jitpack.io' }
implementation 'com.github.boy-offi9-inc:scifi-ui-kit:v0.12.0'
```

The version is the Git tag of the release. The badge above shows the latest one.

**Requirements:** minSdk 21. Jetpack Compose is optional: the library declares it `compileOnly`, so View-only apps do not pull it in, and Compose apps already have it.

## Quick start

XML Views:

```xml
<com.boyoffi9.scifikit.HudFrameView
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:padding="20dp"
    app:sfAccentColor="#00E5FF">

    <com.boyoffi9.scifikit.GlitchTextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="SYSTEM ONLINE"
        android:textColor="#E8FDFF"
        android:textSize="22sp" />

</com.boyoffi9.scifikit.HudFrameView>
```

Jetpack Compose:

```kotlin
HudFrame(modifier = Modifier.fillMaxWidth()) {
    GlitchText(text = "SYSTEM ONLINE", fontSize = 22.sp)
}
```

## HUD frame

`HudFrameView` is a `FrameLayout`. Use padding to keep content clear of the border and glow.

| XML attribute | Compose parameter | Default |
|---|---|---|
| `sfAccentColor` | `accentColor` | theme accent (`#00E5FF`) |
| `sfFillColor` | `fillColor` | accent at 8% alpha |
| `sfChamfer` | `chamfer` | 14dp |
| `sfStrokeWidth` | `strokeWidth` | 1.5dp |
| `sfBracketLength` | `bracketLength` | 20dp |
| `sfScanlines` | `scanlines` | `true` |
| `sfSweep` | `sweep` | `true` |
| none (use padding) | `contentPadding` | 16dp |

## Glitch text

The text decodes on attach, glitches in bursts, and reacts to touch. Three independent settings shape it: how it **decodes**, how it **glitches** and how it responds to **touch**. Each has presets, and every preset can be fine-tuned.

| XML attribute | Compose parameter | Default |
|---|---|---|
| `android:text` | `text` | none |
| `android:textSize` | `fontSize` | 28sp |
| `android:textColor` | `color` | theme text (`#E8FDFF`) |
| `sfSplitColorA` | `splitColorA` | theme secondary (`#FF2D6F`) |
| `sfSplitColorB` | `splitColorB` | theme accent (`#00E5FF`) |
| `sfDecodeMode` | `decode` | `classic` |
| `sfGlitchMode` | `glitch` | `standard` |
| `sfTouchMode` | `touch` | `burst` |
| `sfIntensity` | `intensity` | from the glitch mode |
| `sfGlitchIntervalMs` | `glitchIntervalMs` | from the glitch mode |
| `sfDecodeDurationMs` | `decodeDurationMs` | from the decode mode |

Play the decode again with `replay()` on the View, or by changing `replayKey` in Compose.

### Decode modes

| Mode | Behaviour |
|---|---|
| `classic` | Left to right, 0.9 s. The original. |
| `relaxed` | Slow and calm: 1.6 s, soft characters, slow swapping. |
| `active` | Fast, and keeps shimmering after it resolves. |
| `random` | Letters resolve in a random order. |
| `centerOut` | Resolves from the middle outwards. |
| `typewriter` | Types in with a block cursor, no scramble. |

```xml
<com.boyoffi9.scifikit.GlitchTextView
    android:text="ACCESS GRANTED"
    app:sfDecodeMode="active" />
```

```kotlin
GlitchText(text = "ACCESS GRANTED", decode = DecodeMode.Active.config)
```

Fine-tune a preset with `copy`:

```kotlin
GlitchText(
    text = "ACCESS GRANTED",
    decode = DecodeMode.Relaxed.config.copy(
        durationMs = 2400L,
        charset = DecodeCharsets.Hex,
        order = DecodeOrder.CenterOut,
    ),
)
```

`DecodeConfig` fields: `order`, `durationMs`, `charset`, `swapMs`, `shimmer`, `scramble`, `cursor`. In XML the same settings are `sfDecodeOrder`, `sfDecodeDurationMs`, `sfDecodeCharset`, `sfDecodeSwapMs`, `sfDecodeShimmer`, `sfDecodeScramble` and `sfDecodeCursor`, and each overrides the chosen mode.

### Glitch modes

| Mode | Behaviour |
|---|---|
| `subtle` | Rare, short, small. Easy to live with. |
| `standard` | The original glitch. |
| `violent` | Frequent, long, tearing hard. |
| `signalLoss` | The text drops out and flickers during a burst. |
| `onTouch` | Perfectly still until touched. No ambient glitching. |

Bursts in `subtle`, `violent` and `signalLoss` vary in length and position from cycle to cycle, so they never look metronomic. `GlitchConfig` fields: `intensity`, `intervalMs`, `burstMs`, `jitter`, `strips`, `sliceChance`, `sliceReach`, `splitIdle`, `splitBurst`, `stepMs`, `dropout`, `flicker`. XML overrides: `sfBurstDurationMs`, `sfGlitchJitter`, `sfGlitchStrips`, `sfGlitchSliceChance`, `sfGlitchSliceReach`, `sfGlitchDropout`, `sfGlitchFlicker`.

### Touch modes

| Mode | Behaviour |
|---|---|
| `burst` | A press triggers one burst. The default. |
| `follow` | The glitch happens only around your finger and moves with it. |
| `charge` | Holding builds intensity. Releasing after a full charge fires a large burst. |
| `followCharge` | Both: the glitch window follows your finger and widens as it charges. |

`TouchConfig` fields: `follow`, `charge`, `radius`, `chargeMs`, `chargeBoost`. XML overrides: `sfTouchRadius`, `sfChargeMs`, `sfChargeBoost`.

In Compose, presses are never consumed, so a clickable parent still receives its tap. In Views, `burst` does not consume touches either, but `follow` and `charge` must, to track the drag, so a parent will not receive clicks from presses on the text itself.

### Changing settings at runtime

Every setting can be changed while the screen is showing:

```kotlin
glitchText.glitch = GlitchMode.Violent.config
glitchText.decode = DecodeMode.Typewriter.config
glitchText.touch = TouchMode.FollowCharge.config
glitchText.splitColorA = Color.MAGENTA
```

Setting `decode` or `text` replays the decode. `HudFrameView` and `HoloRadarView` apply property changes the same way.

## Holo radar

Angles start at 3 o'clock and increase clockwise. Distance is 0 at the centre and 1 at the rim. The view is 220dp square when sized with `wrap_content`. The radar is standalone: it does not need a frame or card around it. The `hud` flag draws corner brackets around the disc and targeting reticles on freshly swept contacts; `rangeLabels` numbers the rings. Both are on by default and configurable on `RadarConfig`.

| XML attribute | Compose parameter | Default |
|---|---|---|
| `sfAccentColor` | `accentColor` | theme accent (`#00E5FF`) |
| `sfStrokeWidth` | `strokeWidth` | 1.5dp |
| `sfSweep` | `sweep` | `true` |
| `sfRings` | `rings` | 4 |
| `sfRotationPeriodMs` | `rotationPeriodMs` | 4000 |
| `sfBlipCount` | `blips` | 7 generated blips |
| `sfRadarMode` | `radar` | `classic` |
| none | `size` | 220dp |

### Radar modes

| Mode | Look |
|---|---|
| `classic` | Clean glowing rings and a round-dot sweep. The original. |
| `tactical` | Diamond contacts, dashed rings, compass letters, a faint grid and a narrow sweep. |
| `cyberpunk` | Square contacts alternating with the theme's secondary colour, a doubled sweep line and flicker. |
| `phosphor` | CRT look: long trail, lingering blips, scanlines. |
| `hologram` | Cross contacts, a scan band moving over the disc and a hologram flicker. |
| `sonar` | Diamond contacts in the secondary colour, a wide trail, dashed rings and compass letters. |

```xml
<com.boyoffi9.scifikit.HoloRadarView
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    app:sfAccentColor="#7CFFB2"
    app:sfRadarMode="tactical"
    app:sfBlipCount="8" />
```

```kotlin
HoloRadar(
    accentColor = Color(0xFF7CFFB2),
    radar = RadarMode.Tactical.config,
    blips = listOf(
        RadarBlip(angleDeg = 30f, distance = 0.6f),
        RadarBlip(angleDeg = 200f, distance = 0.4f, color = 0xFFFF2D6F.toInt()),
    ),
)
```

`RadarConfig` fields: `blipShape` (`Dot`, `Diamond`, `Square`, `Cross`), `blipFade`, `trailDeg`, `trailAlpha`, `ringsDashed`, `ticks`, `cardinals`, `grid`, `scanlines`, `scanBand`, `glow`, `fill`, `flicker`, `split`, `secondaryColor`. XML overrides: `sfRadarTrailDeg`, `sfRadarGlow`, `sfRadarFlicker`, `sfRadarScanlines`, `sfRadarSecondaryColor`.

In Views, set the `blips` property to a `List<RadarBlip>` and the `radar` property to change the look. `RadarBlip` and the config types live in `com.boyoffi9.scifikit.core`.

## Sci-fi card

A container for your own content, framed like a HUD panel. Cut corners, a layered border, thick accent bands, hazard stripes and small detail marks are drawn on Canvas behind the content. Nothing inside is drawn for you, so put any views or composables in it.

| Style | Look |
|---|---|
| `Panel` | Cuts on all four corners, double border, thick accent bands, side ticks and hazard stripes. |
| `Outline` | Thin frame with a layered border, corner brackets, a target ring, arrows, dotted rows, a hazard footer and a faint grid. |
| `Targeted` | Flat-top hostile frame: bracket corners, side ticks, a top hazard strip and a marching target ring. |
| `Terminal` | Calm console frame with one sweep-cut corner, a layered inner line, dot rows and edge arrows. |
| `Plain` | Cut corners and a thin border only. |

```xml
<com.boyoffi9.scifikit.SciFiCardView
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    app:sfCardStyle="panel"
    app:sfAccentColor="#FFC400">

    <TextView android:layout_width="wrap_content" android:layout_height="wrap_content" android:text="NEW UPDATE" />
</com.boyoffi9.scifikit.SciFiCardView>
```

```kotlin
SciFiCard(
    accentColor = Color(0xFFFFC400),
    style = CardStyle.Outline.config.copy(grid = 0.2f),
) {
    Text("NEW UPDATE")
}
```

`CardConfig` fields: `cutTL`, `cutTR`, `cutBR`, `cutBL` (multiples of the cut size, 0 for a square corner), `fill`, `line`, `layers`, `bars`, `brackets`, `ticks`, `hazardTop`, `hazardBottom`, `footer`, `target`, `arrows`, `dots`, `grid`, `glow`, `march` (speed of the moving hazard stripes, 0 is still). XML overrides: `sfCardCut`, `sfCardFill`, `sfCardGrid`, `sfCardGlow`, `sfCardMarch`, plus `sfAccentColor` and `sfStrokeWidth`.

The card picks up the theme palette like every other component. Without padding of your own, the view adds 20dp. The card is decorative: it has no semantics, so screen readers read only what is inside. With animations turned off, the hazard stripes stay still.

## Neon button

A button drawn on Canvas with a neon edge. Six styles, one press effect.

| Style | Look |
|---|---|
| `Outline` | Glowing border over a faint fill. The default. |
| `Solid` | Filled with the accent colour, text in the on-accent colour. |
| `Ghost` | No border or fill, only the text and an underline. |
| `Charge` | Cut-corner frame that breathes with a slow idle pulse. |
| `Angled` | Deep corner cuts with bright notch marks and a soft flicker. |
| `Slab` | Solid square-cornered slab, no cut corners or notches. |

Pressing a button surges the glow to full strength and flickers it back down. While held, the glow stays lifted.

```xml
<com.boyoffi9.scifikit.NeonButtonView
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:text="ENGAGE"
    app:sfNeonStyle="solid" />
```

```kotlin
NeonButton(
    text = "ENGAGE",
    onClick = { },
    style = NeonStyle.Solid.config,
)
```

Fine-tune a style with `copy`: `NeonStyle.Outline.config.copy(surge = 1.5f, flicker = 0f)`.

`NeonConfig` fields: `fill`, `border`, `underline`, `inverseText`, `corner`, `cut` (cut-corner silhouette), `notch` (corner notch marks, 0 is off), `glow`, `pulse` (idle breathing, 0 is off), `surge`, `flicker`, `surgeMs`. XML overrides: `sfNeonFill`, `sfNeonGlow`, `sfNeonPulse`, `sfNeonSurge`, `sfNeonFlicker`, `sfNeonSurgeMs`, `sfNeonCorner`, `sfNeonCut`, `sfNeonNotch`, plus `sfAccentColor`, `sfOnAccentColor` and `sfStrokeWidth`. Compose adds `cut` and `notch` overrides.

Colours default to the theme palette. A disabled button is dimmed and ignores presses, and keyboard focus draws a ring. The button reports itself as a button to screen readers, and with animations turned off the press effect is skipped.

## Neon progress

A labeled HUD data bar: square terminal brackets open the track, the fill is a layered neon slab — a soft halo around an angular fill with a bright filament running down its middle and a hot chamfered leading edge pushing into the track — notch ticks hang from the top rail, a monospace label sits on the left and a zero-padded percentage on the right. Above the warning threshold the bar turns to the warning colour (default 85%) and breathes. `segmented` swaps the continuous fill for a 20-cell LED bank, each cell glowing behind its own halo, whose newest segment flickers.

```xml
<com.boyoffi9.scifikit.NeonProgressView
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    app:sfLabel="SYS. CORE LOAD"
    app:sfProgress="0.842" />
```

```kotlin
NeonProgress(
    label = "SYS. CORE LOAD",
    progress = 0.842f,
)
```

The fill animates to new values. XML attributes: `sfLabel`, `sfProgress` (0–1), `sfWarningAt` (default 0.85), `sfTicks` (default 8, 0 hides them), `sfShowPercent` (default true), `sfProgressSegmented` (default false), `sfAccentColor`, `sfWarningColor`. Compose adds `barHeight`, `warningAt`, `segmented` and per-colour overrides. The view reports the label and current percent to screen readers.

## Neon badge

A small hex-framed chip for tags and states — "BETA", "NEW", "ONLINE", "PREMIUM". Five states: `Normal`, `Success`, `Warning`, `Danger`, `Muted`. With a dot enabled the badge pulses gently (Success/Normal only when animating). `filled = true` tints the frame's interior.

```xml
<com.boyoffi9.scifikit.NeonBadgeView
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    app:sfText="ONLINE"
    app:sfState="success"
    app:sfDot="true" />
```

```kotlin
NeonBadge(
    text = "ONLINE",
    state = NeonBadgeState.Success,
    showDot = true,
)
```

XML attributes: `sfText`, `sfState` (`normal`/`success`/`warning`/`danger`/`muted`), `sfFilled`, `sfDot`, `sfAccentColor`, `sfWarningColor`, `sfDangerColor`. In Compose the `state` parameter takes `NeonBadgePainter.State`, and `successColor`, `warningColor` and `dangerColor` are overridable per badge. Muted badges dim both frame and label and never animate.

## Neon gauge

A HUD instrument dial on one arc engine: a full-ring readout (`sweepDegrees = 360`), a 300° speedometer dial with major ticks, or any arc in between. The dial sits on the shared HUD chassis — a chamfered panel with scanlines, corner brackets and a traveling light segment; the fill arc carries a bright head-cap at its leading edge and the ticks sit on an outer ring between the arc and the panel frame. Three readouts — `Percent` (zero-padded `072`), `Value` (the raw value over `maxValue`) and `None` — render bracket-flanked in big mono, with the unit label small under. Past the warning threshold (default 85%) the fill, chassis glow and head-cap all turn to the warning colour, so a redlining speedometer goes red on its own.

```xml
<com.boyoffi9.scifikit.NeonGaugeView
    android:layout_width="120dp"
    android:layout_height="120dp"
    app:sfGaugeValue="0.72"
    app:sfReadout="percent" />

<com.boyoffi9.scifikit.NeonGaugeView
    android:layout_width="160dp"
    android:layout_height="120dp"
    app:sfGaugeValue="0.72"
    app:sfGaugeValue2="130"
    app:sfUnit="KM/H"
    app:sfReadout="value"
    app:sfGaugeSweep="300"
    app:sfGaugeTicks="10" />
```

```kotlin
NeonGauge(
    progress = 0.72f,
    readout = Readout.Percent,
)

NeonGauge(
    progress = 130f / 180f,
    value = 130f,
    unit = "KM/H",
    readout = Readout.Value,
    sweepDegrees = 300f,
    ticks = 10,
)
```

The arc animates to new values (Compose tweens; the view eases on its own). XML attributes: `sfGaugeValue` (0–1 progress), `sfGaugeValue2` (raw value), `sfUnit`, `sfReadout` (`percent`/`value`/`none`), `sfGaugeSweep` (30–360), `sfGaugeTicks`, `sfGaugeWarningAt`, `sfAccentColor`. Compose adds per-colour overrides and `strokeWidth`. The gauge reports its current percent (or value) to screen readers.

## Neon alert

A HUD notification block: "SCAN COMPLETE — NO HOSTILES DETECTED", "WARNING", "SIGNAL STABLE". Four severities, each with its own colour and glyph — `Info` (diamond), `Success` (check), `Warning` (triangle), `Danger` (triangle). The frame is cut-corner with corner brackets on the square corners; a severity glyph column fills the leading edge behind the mark, a scanline texture runs across the body, and a `hazard` value greater than zero adds a marching hazard strip along the top. Danger alerts breathe with a pulsing border.

```xml
<com.boyoffi9.scifikit.NeonAlertView
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    app:sfAlertText="REACTOR OVERLOAD IMMINENT"
    app:sfSeverity="danger"
    app:sfAlertHazard="0.3"
    app:sfAlertChevrons="true" />
```

```kotlin
NeonAlert(
    text = "SCAN COMPLETE — NO HOSTILES DETECTED",
    severity = Severity.Success,
    chevrons = true,
)

NeonAlert(
    text = "REACTOR OVERLOAD IMMINENT",
    severity = Severity.Danger,
    hazard = 0.3f,
)
```

The hazard strip marches while animating and freezes with animations off. XML attributes: `sfAlertText`, `sfSeverity` (`info`/`success`/`warning`/`danger`), `sfAlertFilled`, `sfAlertIcon`, `sfAlertChevrons`, `sfAlertHazard` (0–0.3 of the height, 0 is off), `sfAlertScanlines`, `sfAccentColor`. Compose overrides every severity colour individually. The view reads as "<severity> alert: <text>" to screen readers.

## Theming

One palette drives every component. Set it once and every SciFiKit view picks it up; a value set on a single view still wins.

| Field | Used for | Default |
|---|---|---|
| accent | Frame and radar colour, glitch text colour split B | `#00E5FF` |
| secondary | Glitch text colour split A, the second colour in the cyberpunk radar | `#FF2D6F` |
| text | Glitch text colour | `#E8FDFF` |
| dim | Secondary text in your own screens | `#7FB8C4` |
| background | Screen background in your own screens | `#05080C` |
| glow | Glow strength of frames and radars. 1 is the default look, 0 removes the glow. | `1` |

With no theme set, everything looks exactly as it did before.

### Views

Set the attributes in your app theme. They are ordinary theme attributes, so `values-night` and theme overlays work.

```xml
<style name="Theme.MyApp" parent="Theme.Material3.DayNight.NoActionBar">
    <item name="sfThemeAccent">#FFC400</item>
    <item name="sfThemeSecondary">#FF2D6F</item>
    <item name="sfThemeText">#FFF4D6</item>
    <item name="sfThemeDim">#C9A24A</item>
    <item name="sfThemeBackground">#0A0700</item>
    <item name="sfThemeGlow">1.2</item>
</style>
```

Reference them from your own layouts with `?attr/sfThemeAccent`, `?attr/sfThemeBackground` and so on, and read the whole palette in code with `context.sciFiPalette()`. Views read the theme when they are created, so after changing a theme at runtime, recreate the activity.

### Compose

```kotlin
SciFiTheme(
    palette = SciFiPalette(
        accent = 0xFFFFC400.toInt(),
        secondary = 0xFFFF2D6F.toInt(),
        glow = 1.2f,
    ),
) {
    HudFrame { GlitchText(text = "AMBER") }
}
```

Read values anywhere inside with `SciFiTheme.palette`. Components resolve their default colours at composition, so changing the palette restyles everything below `SciFiTheme` immediately.

## Accessibility and motion

- `GlitchText` exposes its text as the content description, so screen readers read the real string, not the scrambled frames.
- `HoloRadar` is treated as decorative unless you give it a content description.
- On Android 8.0 and later, the components draw one static frame when the user has turned animations off, and Compose follows the setting live. Below 8.0 the system gives no such signal, so they always animate.
- Text sizes are in sp and follow the user's font scale.

## Architecture

Each component is a painter class in `com.boyoffi9.scifikit.core` (`HudFramePainter`, `GlitchTextPainter`, `HoloRadarPainter`, `NeonButtonPainter`, `SciFiCardPainter`) plus two thin wrappers: a View and a composable. The painters take a `Canvas` and the current time, hold no timers, and are written to avoid per-frame allocation. They are public, so you can draw them into your own Canvas.

Glow is faked with wide, low-alpha strokes, so there is no blur, no bitmap and no software layer.

## Known limitations

- Animated components redraw every frame while visible. Performance on low-end devices has not been profiled yet.
- In Views, glitch slices that move past the edge of `GlitchTextView` are clipped. Set `android:clipChildren="false"` on the parent to let them overshoot. Compose does not clip them.
- Compose must be on your classpath to use the composables.

## Demo app

[scifi-kit-demo](https://github.com/boy-offi9-inc/scifi-kit-demo) shows every component in both an XML screen and a Compose screen.

## Contributing

Issues and pull requests are welcome. Read [CONTRIBUTING.md](CONTRIBUTING.md) first: it covers setup, project structure and the rules every component follows.

---

Built by [Boy Offi9](https://github.com/Boy-Offi9).
