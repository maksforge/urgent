"""The mark: the single definition of this app's identity.

Two calls. A small plain dot, then a larger dot wearing a ring: the first call stays quiet, the
second one rings. Both generators import it (`make-launcher-icon.py` emits it as `VectorDrawable`
path data, `make-feature-graphic.py` rasterises it with PIL), so the launcher icon, the notification
icon and the feature graphic cannot drift into being different marks.

The geometry is four circles, written as path data here rather than traced from an image, so there
is no concept file and no outside art to license (`art/README.md`, *Provenance*). To change the mark,
replace PARTS — by hand for geometry this simple, or with `python3 art/trace-mark.py <image>` for a
drawn one, in which case **commit the image beside this file** and record where it came from.

**Use art you have the right to ship.** An icon traced from an emoji font, a stock icon or someone
else's mark is a licence obligation at best and a rejected Play upload at worst. Art you generated
from your own prompt, or drew, is clean.

Coordinates are in arbitrary units around an origin near the mark's centre; callers fit them to
their own canvas with `fit()`. y grows downward, as it does in both SVG and PIL.
"""

import re
from dataclasses import dataclass

# The icon's two colours — take both from the app's generated scheme (`scripts/gen_scheme.py`) rather
# than picking them for the icon, so the identity and the app agree. They are the *only* copy:
# make-launcher-icon.py writes ic_launcher_background.xml from GROUND, so recolouring the identity is
# an edit here and a re-run, never a second value somewhere else to keep in step. (It used to be
# two — this file and a hand-written background drawable — and an app built from the template found
# them disagreeing the first time its palette moved.)
#
# ⚠ **A dark tone of a saturated warm hue is brown.** The first app built from this template tried
# its `primary` darkened for contrast as the ground and it read as mud; then a gradient, whose
# middle is the same mud at 44dp. The charcoal below gets away with being warm because its chroma
# is low — it is a neutral, not a darkened orange. Judge the ground at launcher size on a real home
# screen, light and dark.
GROUND = (0x30, 0x29, 0x21)  # the icon's flat field — the dark scheme's `surfaceContainerHigh`
MARK_INK = (0xFF, 0xB8, 0x5D)  # the mark on that field — the dark scheme's `tertiary`, the amber


@dataclass(frozen=True)
class Subpath:
    """One closed contour, as cubic path data.

    `d` is always wound CLOCKWISE on screen (positive shoelace area with y downward). A hole is
    stored the same way and reversed at read time by `nodes()`, so it is subtracted by the non-zero
    fill rule that both SVG and VectorDrawable use by default. That is what makes the hole a real
    hole rather than a shape painted in the background colour — and it is why it survives the
    monochrome layer, where Android tints everything one flat colour and a painted hole would vanish.
    """

    d: str
    hole: bool = False


# Each circle is four cubic segments with the usual 0.5523·r handles, centred on (cx, 0).
# The first call: a plain dot, r 24 at x −62.
FIRST_CALL = (
    "M-38,0"
        "C-38,13.25 -48.75,24 -62,24C-75.25,24 -86,13.25 -86,0"
        "C-86,-13.25 -75.25,-24 -62,-24C-48.75,-24 -38,-13.25 -38,0"
        "Z"
)

# The second call, which rings: a ring of outer r 66 and inner r 44 around a dot of r 26, all at x 38.
RING_OUTER = (
    "M104,0"
        "C104,36.45 74.45,66 38,66C1.55,66 -28,36.45 -28,0"
        "C-28,-36.45 1.55,-66 38,-66C74.45,-66 104,-36.45 104,0"
        "Z"
)
RING_INNER = (
    "M82,0"
        "C82,24.3 62.3,44 38,44C13.7,44 -6,24.3 -6,0"
        "C-6,-24.3 13.7,-44 38,-44C62.3,-44 82,-24.3 82,0"
        "Z"
)
SECOND_CALL = (
    "M64,0"
        "C64,14.36 52.36,26 38,26C23.64,26 12,14.36 12,0"
        "C12,-14.36 23.64,-26 38,-26C52.36,-26 64,-14.36 64,0"
        "Z"
)

# Declaration order is paint order for the raster renderers, which have no winding rule: the dot
# comes after the hole it sits in, or `make-launcher-icon.py` would punch it away.
PARTS = (
    Subpath(FIRST_CALL),
    Subpath(RING_OUTER),
    Subpath(RING_INNER, hole=True),
    Subpath(SECOND_CALL),
)

_NUM = re.compile(r"-?\d*\.?\d+(?:e-?\d+)?")


def _points(d):
    """Path data -> the flat control-point list: 1 start point plus 3 per cubic segment."""
    pts = []
    for chunk in re.finditer(r"[MC]([^MCZz]*)", d):
        nums = [float(v) for v in _NUM.findall(chunk.group(1))]
        pts += list(zip(nums[0::2], nums[1::2]))
    return pts


def nodes(e, tf=lambda p: p):
    """Control points, caller-transformed, wound for fill or hole."""
    pts = _points(e.d)
    if e.hole:
        pts.reverse()
    return [tf(p) for p in pts]


def path_data(e, tf=lambda p: p, precision=2):
    """SVG/VectorDrawable path data for one subpath."""
    n = nodes(e, tf)

    def f(p):
        return f"{round(p[0], precision):g},{round(p[1], precision):g}"

    out = [f"M{f(n[0])}"]
    for i in range(1, len(n), 3):
        out.append("C" + " ".join(f(n[i + j]) for j in range(3)))
    return "".join(out) + "Z"


def outline(e, tf=lambda p: p, steps=32):
    """The same subpath flattened to a polygon, for renderers that can't draw curves."""
    n = nodes(e, tf)
    pts = []
    for i in range(1, len(n), 3):
        (x0, y0), (x1, y1), (x2, y2), (x3, y3) = n[i - 1], n[i], n[i + 1], n[i + 2]
        for s in range(steps):
            t = s / steps
            u = 1 - t
            pts.append((
                u * u * u * x0 + 3 * u * u * t * x1 + 3 * u * t * t * x2 + t * t * t * x3,
                u * u * u * y0 + 3 * u * u * t * y1 + 3 * u * t * t * y2 + t * t * t * y3,
            ))
    return pts


def bounds(parts=PARTS):
    """Bounding box of the silhouette. Holes are ignored — a hole cannot extend the mark."""
    xs, ys = [], []
    for e in parts:
        if e.hole:
            continue
        for x, y in outline(e):
            xs.append(x)
            ys.append(y)
    return min(xs), min(ys), max(xs), max(ys)


def fit(canvas, art_size, parts=PARTS):
    """A transform placing the mark centred on a square canvas at a given longest-side size.

    Returns a function mapping mark coordinates to canvas coordinates.
    """
    x0, y0, x1, y1 = bounds(parts)
    scale = art_size / max(x1 - x0, y1 - y0)
    cx, cy = (x0 + x1) / 2, (y0 + y1) / 2
    return lambda p: (canvas / 2 + (p[0] - cx) * scale, canvas / 2 + (p[1] - cy) * scale)
