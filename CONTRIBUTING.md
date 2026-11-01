# Contributing to SciFiKit

Thanks for helping out. This guide covers setup, how the code is organised, and what a good pull request contains.

## Reporting bugs and requesting features

Open an issue and pick the matching template.

For bugs, include your device, Android version, SciFiKit version, and whether you use XML Views or Compose. A screenshot or screen recording helps a lot, because most problems here are visual.

For feature requests, describe what you are trying to build before describing the feature. It makes the request much easier to judge.

## Setup

1. Fork the repository and clone your fork.
2. Open it in Android Studio, or build from the command line:

```bash
gradle :scifikit:assembleRelease
```

JDK 17 is required. The code style is the official Kotlin style.

The [demo app](https://github.com/boy-offi9-inc/scifi-kit-demo) shows the released version of every component, which is handy for comparing against your change.

## Project structure

```
scifikit/src/main/java/com/boyoffi9/scifikit/
  core/        Canvas painters and helpers. No View or Compose types.
  *.kt         View wrappers: HudFrameView, GlitchTextView, HoloRadarView
  compose/     Compose wrappers: HudFrame, GlitchText, HoloRadar
scifikit/src/main/res/values/attrs.xml    XML attributes for the Views
```

## Rules every component follows

- **Draw on Canvas.** No bitmaps, GIFs, Lottie or other image assets.
- **Put rendering in a painter in `core/`.** A painter takes a Canvas and the current time and keeps no timers of its own. The View and Compose wrappers only drive it. This is what keeps both versions identical.
- **Do not allocate while drawing.** Reuse `Paint`, `Path` and arrays, and cache anything that depends on size.
- **Respect the system animation setting.** Use `animationsEnabled()` and draw one calm static frame when animations are off.
- **Be accessible.** Give the view a sensible content description, or mark it decorative.
- **Name XML attributes with the `sf` prefix** and declare them in `attrs.xml`. If an attribute already exists, reference it instead of declaring it a second time.
- **Keep Compose `compileOnly`.** Open an issue before adding any runtime dependency.

## Commits

Keep commits small and use [Conventional Commits](https://www.conventionalcommits.org/):

```
feat(core): add a new painter
fix(view): stop the frame clipping its glow
docs: clarify the install steps
```

Common types are `feat`, `fix`, `docs`, `chore`, `ci` and `refactor`.

## Pull requests

- For a new component or any public API change, open an issue first so the shape can be agreed before you write code.
- Keep each pull request focused on one change.
- Fill in the pull request template, and include before and after screenshots for visual changes.
- Update the README when the public API changes.
