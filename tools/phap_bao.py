"""Pháp bảo (3.6): a flying sword over a cloud, the treasure not yet forged.

    art_crystal.png   in colour, pixel art on the 32 grid of the ores (the empty furnace, the empty
                      Bảo khố, Home); the name is kept so every place that showed the crystal shows it
    ic_phapbao.png    the icon of Pháp bảo, black on transparency like the other icons

    python tools/phap_bao.py            writes both into res/drawable-nodpi
    python tools/phap_bao.py out.png    a preview sheet only
"""
import math
import os
import sys

import numpy as np
from PIL import Image, ImageDraw

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import khoang_thach as K  # noqa: E402

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(ROOT, "app", "src", "main", "res", "drawable-nodpi")

# The sword on the 128 frame: from the pommel to the point, rising to the upper right.
P0 = (30.0, 86.0)
P1 = (110.0, 8.0)


def at(t, w):
    """A point `t` along the sword (0 pommel, 1 point), `w` across it (positive to the lower right)."""
    ux, uy = P1[0] - P0[0], P1[1] - P0[1]
    L = math.hypot(ux, uy)
    ux, uy = ux / L, uy / L
    px, py = -uy, ux
    return (P0[0] + ux * L * t + px * w, P0[1] + uy * L * t + py * w)


def cloud_pts(cx, cy, rx, ry):
    return [(cx + rx * math.cos(a), cy + ry * math.sin(a)) for a in np.linspace(0, 2 * math.pi, 40)]


def art():
    c = K.Canvas()
    # The qi turning round the blade: there and not there.
    qi = K.Layer(8)
    ring = K.line_mask([((at(0.62, 0)[0]) + 26 * math.cos(a), at(0.62, 0)[1] + 11 * math.sin(a)) for a in np.linspace(0, 2 * math.pi, 60)], 4)
    qi.put(ring, np.full((K.N, K.N), 0.75))
    c.paste(qi, line=False, ghost=True)
    # The cloud it rides.
    cl = K.Layer(6)
    for (x, y, rx, ry) in ((26, 104, 20, 10), (56, 100, 26, 13), (88, 106, 22, 10)):
        m = K.ellipse_mask(x, y, rx, ry)
        nx, ny = (K.XX + 0.5 - x) / rx, (K.YY + 0.5 - y) / ry
        nz = np.sqrt(np.clip(1 - nx ** 2 - ny ** 2, 0, 1))
        n = K.norm(np.stack([nx, ny, nz + 1e-3], -1))
        cl.put(m, np.clip(K.shade(n, 0.95, spec=0.2, shin=8, amb=0.45), 0, 1))
    c.paste(cl)
    # The blade: two faces and a bright ridge.
    blade = K.Layer(2)
    blade.facet([at(0.27, -9), at(0.9, -7), at(1.0, 0), at(0.27, 0)], K.norm([-0.5, -0.6, 0.65]), 0.95, spec=1.1, shin=30)
    blade.facet([at(0.27, 0), at(1.0, 0), at(0.9, 7), at(0.27, 9)], K.norm([0.55, 0.4, 0.7]), 0.55, spec=0.8, shin=30)
    blade.stroke([at(0.3, 0), at(0.96, 0)], 1.0, 4)
    blade.stroke([at(0.3, 3), at(0.88, 3)], 0.35)
    c.paste(blade)
    # The grip, wrapped, and the pommel.
    grip = K.Layer(11)
    grip.facet([at(0.04, -6), at(0.21, -6), at(0.21, 6), at(0.04, 6)], K.norm([-0.2, -0.3, 0.9]), 0.7, spec=0.3, shin=10)
    for t in (0.08, 0.125, 0.17):
        band = K.line_mask([at(t, -6), at(t, 6)], 4) & grip.a
        grip.put(band, np.full((K.N, K.N), 0.85), mat=9)
    c.paste(grip)
    gold = K.Layer(9)
    # The guard: a bar with cloud-curled ends.
    gold.facet([at(0.2, -22), at(0.27, -16), at(0.27, 16), at(0.2, 22), at(0.23, 0)], K.norm([-0.3, -0.5, 0.8]), 0.95, spec=1.0, shin=24)
    for side in (-1, 1):
        cx, cy = at(0.2, 23 * side)
        gold.put(K.ellipse_mask(cx, cy, 6, 6), np.full((K.N, K.N), 0.8))
    gx, gy = at(0.235, 0)
    gold.put(K.ellipse_mask(gx, gy, 5, 5), np.full((K.N, K.N), 0.98))
    px, py = at(0.0, 0)
    gold.put(K.ellipse_mask(px, py, 7, 7), np.full((K.N, K.N), 0.85))
    c.paste(gold)
    # The tassel from the pommel.
    tas = K.Layer(3)
    tas.put(K.line_mask([(px - 2, py + 6), (px - 8, py + 12)], 4), np.full((K.N, K.N), 0.6))
    tas.put(K.poly_mask([(px - 16, py + 10), (px - 6, py + 12), (px - 10, py + 22), (px - 18, py + 20)]), np.full((K.N, K.N), 0.75))
    c.paste(tas)
    c.sparkle(108, 46, 6, mat=8)
    c.sparkle(20, 30, 4, mat=8)
    return c.image()


def icon(size=128):
    """Black on transparency: the sword and the cloud in outline, the blade's ridge cut out."""
    S = 4
    s = size * S
    im = Image.new("L", (s, s), 0)
    d = ImageDraw.Draw(im)
    k = s / 128.0
    P = lambda p: (p[0] * k, p[1] * k)
    w = int(5 * k)
    # The blade, outlined, with its ridge.
    blade = [at(0.27, -9), at(0.9, -7), at(1.0, 0), at(0.9, 7), at(0.27, 9)]
    d.polygon([P(p) for p in blade], fill=255)
    d.line([P(at(0.32, 0)), P(at(0.88, 0))], fill=0, width=int(2.5 * k))
    # The guard, filled, its ends curled.
    d.polygon([P(p) for p in (at(0.2, -22), at(0.27, -16), at(0.27, 16), at(0.2, 22), at(0.23, 0))], fill=255)
    for side in (-1, 1):
        cx, cy = at(0.2, 23 * side)
        d.ellipse([P((cx - 6, cy - 6)), P((cx + 6, cy + 6))], fill=255)
    gx, gy = at(0.235, 0)
    d.ellipse([P((gx - 3, gy - 3)), P((gx + 3, gy + 3))], fill=0)
    # The grip and the pommel.
    d.polygon([P(p) for p in (at(0.04, -5), at(0.2, -5), at(0.2, 5), at(0.04, 5))], fill=255)
    px, py = at(0.0, 0)
    d.ellipse([P((px - 6, py - 6)), P((px + 6, py + 6))], outline=255, width=int(4 * k))
    # The cloud below, a single line with two curls.
    pts = []
    for (x, y, r, a0, a1) in ((34, 112, 9, 180, 360), (54, 108, 11, 180, 360), (76, 110, 10, 180, 340)):
        pts += [(x + r * math.cos(math.radians(a)), y + r * math.sin(math.radians(a))) for a in np.linspace(a0, a1, 12)]
    d.line([P(p) for p in pts], fill=255, width=int(4 * k), joint="curve")
    d.line([P((24, 112)), P((92, 112))], fill=255, width=int(4 * k))
    # Two sparkles of qi.
    for (x, y, r) in ((108, 48, 7), (22, 34, 5)):
        d.polygon([P(p) for p in ((x, y - r), (x + r * 0.25, y - r * 0.25), (x + r, y), (x + r * 0.25, y + r * 0.25),
                                  (x, y + r), (x - r * 0.25, y + r * 0.25), (x - r, y), (x - r * 0.25, y - r * 0.25))], fill=255)
    alpha = im.resize((size, size), Image.LANCZOS)
    out = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    out.putalpha(alpha)
    return out


def mono(im):
    """The drawing in black and white: its lightness in six grays, the outline black."""
    g = np.array(im.convert("L"), dtype=float) / 255
    # Lightness, a little darker so the gold and the steel keep their form, in six grays.
    q = np.round(np.clip(g ** 1.35, 0, 1) * 5) / 5
    v = (q * 255).astype(np.uint8)
    out = Image.merge("RGBA", (Image.fromarray(v), Image.fromarray(v), Image.fromarray(v), im.getchannel("A")))
    return out


def main():
    a = art()
    ic = icon()
    if len(sys.argv) > 1:
        sheet = Image.new("RGB", (256 + 24 + 256 + 24 + 64, 256), (238, 238, 232))
        sheet.paste(a.resize((256, 256), Image.NEAREST), (0, 0), a.resize((256, 256), Image.NEAREST))
        bw = mono(a).resize((256, 256), Image.NEAREST)
        sheet.paste(bw, (280, 0), bw)
        sm = mono(a).resize((64, 64), Image.LANCZOS)
        sheet.paste(sm, (560, 96), sm)
        sheet.save(sys.argv[1])
        return
    a.save(os.path.join(RES, "art_crystal.png"))
    # 3.6: the icon is this very drawing in black and white (six grays), drawn untinted; the plain
    # ic_ is its silhouette, for the places that tint an icon white.
    bw = mono(a)
    bw.save(os.path.join(RES, "icart_phapbao.png"))
    sil = Image.new("RGBA", a.size, (0, 0, 0, 0))
    sil.putalpha(a.getchannel("A"))
    sil.save(os.path.join(RES, "ic_phapbao.png"))
    print("phap bao")


if __name__ == "__main__":
    main()
