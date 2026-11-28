"""Logo of Vạn Quyển Tiên Đồ (3.6), in the pixel art of the ores and the pháp bảo: three books (red,
blue, gold, their pages showing) laid as steps climbing to a full moon, a cloud under them, two
sparkles; on a black tile. Vạn quyển, the books; tiên đồ, the way up they make.

    python tools/logo.py            writes res/drawable-nodpi/logo.png and every launcher icon
    python tools/logo.py out.png    a preview sheet only
"""
import math
import os
import sys

import numpy as np
from PIL import Image, ImageDraw

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import khoang_thach as K  # noqa: E402

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(ROOT, "app", "src", "main", "res")

# Paper, for the pages.
K.RAMPS[12] = [K._hex(c) for c in ["#4a3c30", "#7a654f", "#a99073", "#d3bd98", "#ecdfc2", "#fffaee"]]


def book(c, x0, y0, w, h, depth, mat):
    """A book lying flat: its cover on top, the pages along the front, the spine to the right."""
    dx, dy = depth, -depth * 0.8
    lay = K.Layer(mat)
    # The cover, seen from above.
    lay.facet([(x0, y0), (x0 + w, y0), (x0 + w + dx, y0 + dy), (x0 + dx, y0 + dy)], K.norm([-0.2, -0.75, 0.6]), 0.9, spec=0.6, shin=16)
    # The spine, on the right.
    lay.facet([(x0 + w, y0), (x0 + w + dx, y0 + dy), (x0 + w + dx, y0 + dy + h), (x0 + w, y0 + h)], K.norm([0.8, 0.1, 0.55]), 0.7, spec=0.3, shin=10)
    # The front edge: a strip of cover above and below the pages.
    lay.facet([(x0, y0), (x0 + w, y0), (x0 + w, y0 + h), (x0, y0 + h)], K.norm([0.0, 0.2, 0.98]), 0.62, spec=0.2, shin=10)
    c.paste(lay)
    pages = K.Layer(12)
    pm = K.poly_mask([(x0 + 4, y0 + 4), (x0 + w - 1, y0 + 4), (x0 + w - 1, y0 + h - 4), (x0 + 4, y0 + h - 4)])
    v = np.where(((K.YY - y0).astype(int) // 4) % 2 == 0, 0.95, 0.78)
    pages.put(pm, v)
    c.paste(pages, line=False)
    # A band of gold on the spine.
    gold = K.Layer(9)
    gold.put(K.line_mask([(x0 + w + dx * 0.5, y0 + dy * 0.5 + 2), (x0 + w + dx * 0.5, y0 + dy * 0.5 + h - 2)], 4), np.full((K.N, K.N), 0.9))
    c.paste(gold, line=False)


def motif():
    """The books, the moon and the cloud on a transparent 128 frame (the 32 grid ×4)."""
    c = K.Canvas()
    # The moon, glowing, and its halo of qi.
    halo = K.Layer(8)
    halo.put(K.ellipse_mask(100, 24, 20, 20) & ~K.ellipse_mask(100, 24, 16, 16), np.full((K.N, K.N), 0.7))
    c.paste(halo, line=False, ghost=True)
    moon = K.Layer(9)
    mm = K.ellipse_mask(100, 24, 13, 13)
    nx, ny = (K.XX + 0.5 - 100) / 13, (K.YY + 0.5 - 24) / 13
    nz = np.sqrt(np.clip(1 - nx ** 2 - ny ** 2, 0, 1))
    n = K.norm(np.stack([nx, ny, nz + 1e-3], -1))
    mv = np.clip(K.shade(n, 1.0, spec=0.4, shin=10, amb=0.55), 0, 1)
    for (cx, cy, r) in ((95, 21, 3), (105, 28, 2.4), (101, 17, 1.8)):
        mv = np.where(K.ellipse_mask(cx, cy, r, r), mv * 0.8, mv)
    moon.put(mm, mv)
    c.paste(moon)
    # The cloud under the steps.
    cl = K.Layer(6)
    for (x, y, rx, ry) in ((24, 108, 15, 7), (46, 105, 19, 10), (70, 110, 16, 7)):
        m = K.ellipse_mask(x, y, rx, ry)
        nx, ny = (K.XX + 0.5 - x) / rx, (K.YY + 0.5 - y) / ry
        nz = np.sqrt(np.clip(1 - nx ** 2 - ny ** 2, 0, 1))
        cl.put(m, np.clip(K.shade(K.norm(np.stack([nx, ny, nz + 1e-3], -1)), 0.95, spec=0.2, shin=8, amb=0.45), 0, 1))
    c.paste(cl)
    # Three books as steps, the lowest first.
    book(c, 16, 90, 62, 14, 10, 3)       # red
    book(c, 32, 70, 56, 14, 10, 8)       # blue
    book(c, 48, 50, 50, 14, 10, 9)       # gold
    c.sparkle(22, 26, 6, mat=8)
    c.sparkle(62, 20, 4, mat=8)
    return c.image()


def tile(size, shape="square"):
    """The motif on its black tile, scaled by whole steps of the grid so the pixels stay square."""
    im = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    if shape == "circle":
        d.ellipse([0, 0, size - 1, size - 1], fill=(0, 0, 0, 255))
        room = size * 0.74
    else:
        d.rounded_rectangle([0, 0, size - 1, size - 1], radius=size * 0.22, fill=(0, 0, 0, 255))
        room = size * 0.86
    m = motif()
    small = m.resize((32, 32), Image.NEAREST)
    step = max(1, int(room // 32))
    art = small.resize((32 * step, 32 * step), Image.NEAREST)
    if 32 * step < room * 0.8:
        art = small.resize((int(room), int(room)), Image.NEAREST)
    o = (size - art.width) // 2
    im.alpha_composite(art, (o, o))
    return im


def foreground(size):
    """The adaptive icon's foreground: the motif inside the safe circle, on transparency."""
    im = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    room = size * 0.6
    small = motif().resize((32, 32), Image.NEAREST)
    step = max(1, int(room // 32))
    art = small.resize((32 * step, 32 * step), Image.NEAREST)
    o = (size - art.width) // 2
    im.alpha_composite(art, (o, o))
    return im


DENSITIES = {"mdpi": 1.0, "hdpi": 1.5, "xhdpi": 2.0, "xxhdpi": 3.0, "xxxhdpi": 4.0}


def main():
    if len(sys.argv) > 1:
        tiles = [tile(512), tile(192), tile(96, "circle"), tile(48)]
        sheet = Image.new("RGB", (sum(t.width for t in tiles) + 24 * len(tiles), 520), (238, 238, 232))
        x = 0
        for t in tiles:
            sheet.paste(t, (x, 0), t)
            x += t.width + 24
        sheet.save(sys.argv[1])
        return
    tile(512).save(os.path.join(RES, "drawable-nodpi", "logo.png"))
    for name, f in DENSITIES.items():
        folder = os.path.join(RES, "mipmap-" + name)
        tile(int(48 * f)).save(os.path.join(folder, "ic_launcher.png"))
        tile(int(48 * f), "circle").save(os.path.join(folder, "ic_launcher_round.png"))
        foreground(int(108 * f)).save(os.path.join(folder, "ic_launcher_foreground.png"))
    print("logo")


if __name__ == "__main__":
    main()
