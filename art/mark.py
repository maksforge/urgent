"""The mark: the single definition of this app's identity.

Two calls. A small plain dot, then a larger dot wearing a ring: the first call stays quiet, the
second one rings. Both generators import it (`make-launcher-icon.py` emits it as `VectorDrawable`
path data, `make-feature-graphic.py` rasterises it with PIL), so the launcher icon, the notification
icon and the feature graphic cannot drift into being different marks.

The geometry is four circles, written as path data here rather than traced from an image, so there
is no concept file and no outside art to license (`art/README.md`, *Provenance*). To change the mark,
replace PARTS — by hand for geometry this simple, or with `python3 art/trace-mark.py <image>` for a
drawn one, pasting the traced curves into PARTS as `Subpath` entries (solids, and holes with
`hole=True`, in paint order); in that case **commit the image beside this file** and record where it
came from.

**Use art you have the right to ship.** An icon traced from an emoji font, a stock icon or someone
else's mark is a licence obligation at best and a rejected Play upload at worst. Art you generated
from your own prompt, or drew, is clean.

Coordinates are the design spec's 64×40 frame; callers fit them to their own canvas with `fit()`.
y grows downward, as it does in SVG, PIL and Compose.
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


# The geometry of the design spec, on its 64×40 frame: the first call a plain dot, the second a
# larger dot inside a ring drawn as a 3-unit stroke. The in-app mark (`ui/common/MarkGeometry.kt`)
# is generated from these same numbers.
VIEW_WIDTH, VIEW_HEIGHT = 64, 40
FIRST_CALL = (10, 20, 5)  # cx, cy, r
SECOND_CALL = (44, 20, 9)
RING = (44, 20, 16, 3)  # cx, cy, centre-line radius, stroke width


def circle(cx, cy, r):
    """A circle as four clockwise cubic segments with the usual 0.5523·r handles."""
    k = 0.5523 * r

    def p(x, y):
        return f"{x:g},{y:g}"

    return (
        f"M{p(cx + r, cy)}"
        f"C{p(cx + r, cy + k)} {p(cx + k, cy + r)} {p(cx, cy + r)}"
        f"C{p(cx - k, cy + r)} {p(cx - r, cy + k)} {p(cx - r, cy)}"
        f"C{p(cx - r, cy - k)} {p(cx - k, cy - r)} {p(cx, cy - r)}"
        f"C{p(cx + k, cy - r)} {p(cx + r, cy - k)} {p(cx + r, cy)}"
        "Z"
    )


_rx, _ry, _rr, _rw = RING

# Declaration order is paint order for the raster renderers, which have no winding rule: the dot
# comes after the hole it sits in, or `make-launcher-icon.py` would punch it away.
PARTS = (
    Subpath(circle(*FIRST_CALL)),
    Subpath(circle(_rx, _ry, _rr + _rw / 2)),
    Subpath(circle(_rx, _ry, _rr - _rw / 2), hole=True),
    Subpath(circle(*SECOND_CALL)),
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
