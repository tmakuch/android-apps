# SimplyWatchWear — Migration Plan

**Last Updated**: 2026-05-20

## Status

Legacy Canvas-based Android watch face (Kotlin, `app/legacy/`) is **deprecated**. Samsung Galaxy Watch 6 picker does not discover third-party `CanvasRenderer2` watch faces installed via ADB. Moving to Google's **Watch Face Format (WFF)** — a declarative XML approach, no APK required.

**Target**: Standalone `.watchface` ZIP bundle, developed in `app/wff/`.

---

## Session History

### 2026-05-20 — WFF Research & Planning

- Read all legacy source in `app/legacy/` — inventoried 11 features (time, seconds, date with Polish abbreviations, division ring, complication slot, 2 settings toggles, ambient mode, color palette, 2 custom fonts, preview image)
- Researched WFF XML schema from Google official samples (`android/wear-os-samples` — SimpleDigital, Complications, Flavors) and XSD spec (`google/watchface`)
- Confirmed all 11 legacy features have WFF equivalents; 2 caveats: colon blinking unverified, custom font family syntax TBD
- Documented WFF XML structure, element reference, BooleanConfiguration pattern, ComplicationSlot pattern, Variant/ambient pattern
- Identified verification tools: `google/watchface` XSD validator, memory footprint evaluator, WFF optimizer
- User created `division_ring.png` at `app/wff/division_ring.png`
- Updated all Serena memories to reflect legacy → WFF transition

---

## Phase 1 — Research (DONE)

### Feature Match Matrix

| # | Legacy Feature | WFF Equivalent | Status |
|---|---------------|----------------|--------|
| 1 | Time `HH:MM` | `<TimeText format="hh:mm">` with `hourFormat="SYNC_TO_DEVICE"` | Confirmed |
| 2 | Seconds `:SS` | `<TimeText format=":ss">` or separate `<PartText>` | Confirmed |
| 3 | Date `dayOfWeek.dayOfMonth` | `<PartText>` with `[DAY_OF_WEEK_SHORT].[DAY_OF_MONTH]` | Confirmed |
| 4 | Division ring (orange circle) | `<PartImage>` with `division_ring.png` (450x450), visibility via BooleanConfiguration | Confirmed |
| 5 | Complication (1 slot, top-center) | `<ComplicationSlot>` with `<BoundingOval>`, `<DefaultProviderPolicy>`, `<Complication type="...">` | Confirmed |
| 6 | Ring visibility toggle | `<BooleanConfiguration id="show_ring" defaultValue="TRUE">` | Confirmed |
| 7 | Ring on ambient toggle | `<BooleanConfiguration id="show_on_ambient" defaultValue="TRUE">` + compound expression | Confirmed |
| 8 | Ambient mode (hide seconds/date/complications) | `<Variant mode="AMBIENT" target="alpha" value="0"/>` on non-ambient elements | Confirmed |
| 9 | Color palette (black bg, white text, orange ring) | Hardcoded in attributes | Confirmed |
| 10 | Custom fonts | `<Font family="digital">` — exact custom font syntax TBD | Needs verification |
| 11 | Preview image | Preview image in bundle root | Confirmed |

### Open Questions

- **Colon blinking**: WFF may support `[SCE]` (centiseconds) expression for visibility toggling. If not, accept always-visible colon.
- **Custom font family name**: Google samples use `SYNC_TO_DEVICE`. Need to confirm how standalone WFF maps font filenames to family names.
- **Compound Boolean expression**: `[CONFIGURATION.show_ring] AND [CONFIGURATION.show_on_ambient]` — verify AND syntax works in WFF expressions.

### Key References

- Google WFF samples: `github.com/android/wear-os-samples/tree/main/WatchFaceFormat`
- XSD spec v1: `github.com/google/watchface/tree/main/third_party/wff/specification/documents/1`
- XSD validator: `github.com/google/watchface`
- Android docs: `developer.android.com/training/wearables/wff`

---

## Phase 2 — Asset Preparation (TODO)

- [ ] Create directory structure: `app/wff/fonts/`, `app/wff/images/`
- [ ] Copy `app/legacy/src/main/res/font/digital.ttf` → `app/wff/fonts/digital.ttf`
- [ ] Copy `app/legacy/src/main/res/font/digital_empty.ttf` → `app/wff/fonts/digital_empty.ttf`
- [ ] Copy `app/legacy/src/main/res/drawable-nodpi/watch_preview.png` → `app/wff/images/watch_preview.png`
- [ ] Move `app/wff/division_ring.png` → `app/wff/images/division_ring.png`
- [ ] Read legacy dimens.xml, colors.xml, RenderUtils.kt for exact pixel positions and sizes to translate to 450x450 WFF coordinate space

---

## Phase 3 — Write watchface.xml (TODO)

- [ ] Create `app/wff/watchface.xml`
- [ ] Root element: `<WatchFace width="450" height="450">`
- [ ] Metadata: `CLOCK_TYPE=DIGITAL`, `PREVIEW_TIME=10:08:32`
- [ ] UserConfigurations: `BooleanConfiguration show_ring`, `BooleanConfiguration show_on_ambient`
- [ ] Active scene:
  - [ ] Division ring via `<PartImage>` with visibility `[CONFIGURATION.show_ring]`
  - [ ] Main time `HH:MM` via `<TimeText>` with `digital.ttf` font
  - [ ] Seconds `:SS` via `<TimeText>` with smaller font
  - [ ] Date via `<PartText>` with `[DAY_OF_WEEK_SHORT].[DAY_OF_MONTH]`
  - [ ] Complication slot at top-center (bounds from legacy `ComplicationUtils.kt`)
- [ ] Ambient mode via `<Variant mode="AMBIENT">`:
  - [ ] Hide seconds, date, complication (alpha=0)
  - [ ] Show ring only if both `show_ring` AND `show_on_ambient`
  - [ ] Use `digital_empty.ttf` for time text
- [ ] Font sizes translated from legacy sp values: 85sp → ~85px, 55sp → ~55px, 40sp → ~40px (at 450px screen)
- [ ] Colors from legacy: text `#ffffffff`, background `#ff000000`, ring embedded in PNG

---

## Phase 4 — Validate & Package (TODO)

- [ ] Set up WFF XSD validator from `google/watchface`
- [ ] Validate `watchface.xml` against XSD schema
- [ ] Create build script (`build.ps1`):
  - Validate XML
  - ZIP `app/wff/` contents into `SimplyWatchWear.watchface`
- [ ] Verify `.watchface` ZIP structure is correct

---

## Phase 5 — Deploy & Test (TODO)

- [ ] Push `.watchface` bundle to Samsung Galaxy Watch 6 via ADB or Play test track
- [ ] Verify watch face appears in Samsung picker
- [ ] Verify time, seconds, date display correctly
- [ ] Verify division ring visibility toggles work
- [ ] Verify ambient mode behavior
- [ ] Verify complication renders and is configurable
- [ ] Verify preview image appears in picker

---

## Phase 6 — Polish (TODO)

- [ ] Investigate colon blinking in WFF (if feasible)
- [ ] Verify Polish day abbreviations match or accept system defaults
- [ ] Fine-tune positions to visually match legacy watch face
- [ ] Run memory footprint validation
