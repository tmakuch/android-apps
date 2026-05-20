# Watch Face Format (WFF) — Google's Declarative Watch Face Approach

Introduced at Google I/O 2023. Available on Wear OS 4+ (API 33+).

## Overview

A declarative XML schema for defining watch faces. Instead of writing Kotlin code that draws on a `Canvas`, you describe the watch face in XML — backgrounds, time display, complications, animations, and styles. The system renders it using an optimized engine; no app-side Canvas code runs per frame.

## Current App vs WFF

| Aspect | Current (`CanvasRenderer2`) | Watch Face Format |
|---|---|---|
| **Language** | Kotlin + imperative Canvas drawing | Declarative XML only |
| **Rendering** | App draws every frame manually | System renders from XML — more battery efficient |
| **Packaging** | Must be an APK/AAB | Standalone `.watchface` bundle (ZIP) **or** embedded in an APK |
| **Flexibility** | Full control (custom shapes, animations) | Limited to schema-defined elements |
| **Custom drawing** | Arbitrary Canvas operations | Predefined elements only (no `drawLine`, `drawArc`, etc.) |
| **Complications** | Manual positioning + rendering | Declared in XML by position |
| **Styles** | ViewModel + `UserStyleSchema` in code | XML-defined with schema validation |
| **Build tooling** | Gradle + Kotlin compiler | XSD schema validation; Samsung Watch Face Studio can author WFF |

## Distribution Models

### Standalone WFF
- Pure XML + asset files (images, fonts) in a `.watchface` file (ZIP archive)
- No Kotlin, no Gradle, no APK — just a bundle
- Distributed via Play Store (Wear OS listing)
- Limited to what the schema supports

### Embedded WFF
- WFF XML lives inside a regular Android APK
- Can still have Kotlin code for companion features (phone app, settings activity)
- Watch face rendering is XML-driven, no `CanvasRenderer2`
- Uses WFF-specific service classes from `androidx.wear.watchface` library

## Structure of a WFF Watch Face

```
MyWatchFace.watchface (ZIP)
├── watchface.xml          # Main configuration
├── images/               # Background images, hands, etc.
├── fonts/                # Custom fonts
└── metadata.json         # Optional metadata
```

Or when embedded in an APK, the XML and assets go into standard Android resource directories.

## Key Capabilities

- **Time display**: Analog hands (hour/minute/second), digital text
- **Date display**: Day, month, year, weekday
- **Complications**: Declarative placement with type filtering
- **Styles/Customization**: User-selectable color palettes, image variants, visibility toggles
- **Ambient mode**: Separate ambient-specific rendering configuration
- **Animations**: Limited built-in animations (e.g., sweep second hand)
- **Background**: Solid color, gradient, or bitmap image

## Limitations vs Programmatic (Canvas)

| Feature | WFF | Canvas |
|---|---|---|
| Custom shapes/paths | No | Yes |
| Arbitrary text positioning | Limited | Yes |
| Procedural animations | Basic only | Yes |
| Conditional rendering logic | XML expressions only | Full Kotlin |
| Battery efficiency | Excellent (system-rendered) | Depends on optimization |
| Learning curve | Low (XML) | Medium-high (Canvas + lifecycle) |

## Migration Feasibility for SimplyWatchWear

### Easy to migrate
- `drawMainTime()` — time text rendering → WFF digital time element
- `drawDate()` — date text → WFF date element
- `drawComplications()` — complication slots → WFF complication elements
- Color palette / `ColorPalette` → WFF styles
- `UserStyleSchema` / settings toggles → WFF style configuration

### Difficult or impossible to migrate
- `drawRing()` — division ring (custom Canvas arc/path drawing) → no WFF equivalent
- `drawSeconds()` — custom second text positioning → limited equivalent
- Any custom `Canvas.drawText()` with precise offset calculations → may not map cleanly

### Service layer
- `DigitalWatchFaceService` (extends `WatchFaceService`) would be replaced by a WFF-specific service
- `DigitalWatchCanvasRenderer` (extends `Renderer.CanvasRenderer2`) would be removed entirely
- `WatchFaceConfigActivity` could remain but would use WFF style APIs instead of `UserStyleSchema`

## Relevant Libraries

The `androidx.wear.watchface` library (currently v1.2.1 in this project) supports both the programmatic Canvas approach and the WFF approach within the same artifact.

## Samsung Watch Face Studio

Samsung's desktop tool (Watch Face Studio) can author WFF-format watch faces visually. Output is a `.watchface` file compatible with both Samsung watches and standard Wear OS devices. This is the WYSIWYG alternative to hand-writing WFF XML.

## References

- Android Developers: Watch Face Format documentation (developer.android.com/training/wearables/watch-faces/watch-face-format)
- Google I/O 2023 announcement
- `androidx.wear.watchface` library release notes (v1.2.0+ added WFF support)
- Samsung Watch Face Studio: developer.samsung.com/watch-face-studio
