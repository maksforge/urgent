# Identity assets

Source art for the launcher icon and the Play listing. **Everything in `app/src/main/res` that draws
the mark is generated from here** — edit `mark.py` and regenerate, never hand-edit the path data in
the `VectorDrawable`s.

| Asset | Source | State |
| --- | --- | --- |
| `mark.py`'s PARTS | authored geometry — see *The mark* | current |
| `concept.png` | only if a mark is ever traced from an image again — **commit it**, with its provenance below | — |
| `drawable/ic_launcher_{background,foreground,monochrome}.xml`, `drawable/ic_notification.xml`, `mipmap-*`, `play-icon-512.png`, `ui/common/MarkGeometry.kt` | `mark.py` → `make-launcher-icon.py` | generated |
| `play-feature-graphic.png` (1024×500) | `mark.py` + `theme/Color.kt` (+ `feature-background.png` if present) → `make-feature-graphic.py` | generated |
| `play-screenshots/<n>_<scene>-<tag>.png` | `scripts/screenshots.py --build release --numbered` → `pad-screenshot.py` | — |

## The mark

`mark.py` is the single definition, as **several cubic subpaths** — solids and holes, declared in
`PARTS`. Both generators import it, so the launcher icon, the notification icon and the feature
graphic cannot drift into being different marks.

The mark is **two calls**: a small plain dot, then a larger dot wearing a ring. The first call
stays quiet; the second one rings. It is four circles on the design spec's 64×40 frame, declared
as numbers in `mark.py` rather than traced from an image, so there is no concept file and nothing
to license. The ring is a solid disc, a hole, and a solid dot declared in that order — the order
matters to the raster renderer, which has no winding rule and paints parts as they come. The same
numbers are written to `ui/common/MarkGeometry.kt` for the mark Compose draws in the app.

- **Recolouring is two values in `mark.py` and a re-run**: `GROUND` (the icon's field) and
  `MARK_INK` (the mark on it). `make-launcher-icon.py` writes every drawable from them, the
  background included — it used to be hand-written, and a second copy of a colour in a file nobody
  regenerates is the drift the first app built from this template found the first time its palette
  moved.
- Take the colours from the app's generated scheme (`scripts/gen_scheme.py`) rather than picking
  them for the icon, so the identity and the app agree. Today that is the dark scheme's
  `surfaceContainerHigh` as the ground — a warm charcoal, which is where the neutral seed being
  warm comes from — and its `tertiary`, the amber, as the ink. **Judge the ground at 44dp on a
  real home screen, light and dark**: a dark tone of a *saturated* warm hue is brown, and a
  gradient's middle is the same brown at launcher size; the charcoal works because its chroma is
  low. Before it, a flat mid-tone role under `primary` was what survived.
- The feature graphic **reads its colours out of `theme/Color.kt`**, so a regenerated palette
  reaches the listing by re-running the script. Pasted inks stay the old hue on a ground that moved.
- **The hole must be a real hole in the path**, not a shape painted in the background colour. A
  themed launcher (API 33+) tints the whole monochrome layer one flat colour, and a painted hole
  simply vanishes.

## Where the art has to come from

**Use art you have the right to ship.** This is not a formality — it is the thing most likely to
block a first Play upload, and it is discovered late.

- An icon traced from an **emoji font** carries that font's licence. Noto Emoji is Apache-2.0 or
  OFL depending on the release, and both impose obligations on the *outlines* — which is what
  tracing copies.
- A **stock icon** is somebody's asset with somebody's terms, whatever the site's marketing says.
- **Art you drew, or art generated from your own prompt, is clean.** Keep a note of which, in this
  file, so the provenance survives you forgetting it.

Rendering *text* with a font is a different question from redistributing its *outlines*: the OFL
explicitly permits documents produced with a font. `make-feature-graphic.py` renders the wordmark
with Noto Sans on that basis and ships no glyph outlines.

## Replacing the mark

```bash
# 1. Draw or generate a concept image, and put it beside this file.
python3 art/trace-mark.py concept.png   # prints cubic path data
#    Set CROP and the concept's two tones at the top of trace-mark.py first — they describe the
#    *image being traced*, not the app. Paste the curves into art/mark.py's PARTS as Subpath
#    entries — solids, and holes with hole=True, in paint order — with your colours.

# 2. Regenerate everything that derives from it.
python3 art/make-launcher-icon.py       # icons, mipmaps, the 512² Play asset
python3 art/make-feature-graphic.py     # the 1024×500 listing graphic
```

`make-launcher-icon.py` prints a **safe-radius check** and will tell you when the art is clipped by
a circular launcher mask. It is a real constraint rather than a formality: a launcher may mask the
icon to a 66dp circle, so anything outside a 33dp radius from the centre is cut off. Shrink
`ART_SIZE` until it says `inside`.

Two sizes worth knowing, both already handled:

- The **notification icon** is scaled up relative to the launcher icon. At the launcher's own art
  size the mark would be a speck in a 24dp slot.
- The **flat mipmaps** are cropped to the 72dp a launcher actually shows of the 108dp adaptive
  canvas, so they match what the adaptive icon looks like rather than appearing to shrink.

## The feature graphic and Play's crop

Play shows the 1024×500 graphic in a **16:9 box with `object-fit: cover`** on the search-results
card, so 68px of each side never reaches a viewer. It is correct in every local preview and in the
Console's uploader, and wrong only on the surface most people see. `make-feature-graphic.py` lays out
against the cropped frame, prints the spans it measured, and **exits 1** when the copy is clipped or
runs into the mark — shorten the tagline rather than moving the margin.

A photographic ground is `art/feature-background.png` (cover-fitted, never stretched). Record where
it came from in the provenance table below, like the concept image.

## Provenance

| File | Where it came from | Licence / terms |
| --- | --- | --- |
| `mark.py` geometry | Authored in this repository as circle arithmetic, 2026-10-17, and redrawn the same day to the commissioned design spec's 64×40 geometry; no image was traced and no outside art was used | Ours, under the repository's LICENSE |
| `concept.png` | *(none — see above; fill in if a mark is ever traced)* | |
| `feature-background.png` | *(not used; the ground is a gradient of two scheme roles)* | |
