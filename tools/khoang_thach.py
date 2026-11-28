"""Khoáng thạch (3.1.1): six gems in pixel art, on a 32 grid ×6 (192 px), grayscale on transparency
for the e-ink screen. Chunky gem icons: a black outline, flat facets in four tones lit from the upper
left, white glints on the lit facets, a sparkle or two beside the finer ones.

    0 Hoàng phẩm   a raw nugget, broken into rough facets, flecked
    1 Huyền phẩm   a cluster of three crystals on a bit of rock
    2 Địa phẩm     a step-cut stone: four bevelled bands round a flat table
    3 Thiên phẩm   a brilliant-cut gem: crown, table, pavilion, sparkling
    4 Âm Dương     an orb split by the Thái Cực, ringed by motes
    5 Hỗn Độn      a dark gem grown into a rock, a seam of light through it

Used by tools/ve_do_hoa.py (--khoangthach); run alone to preview: python tools/khoang_thach.py out.png
"""
import math
import sys

import numpy as np
from PIL import Image, ImageDraw

N = 32
SCALE = 6
O = 24          # outline
D = 70          # shade
M = 128         # mid
L = 182         # lit
H = 228         # bright
W = 255         # glint


class Gem:
    def __init__(self):
        self.g = Image.new("L", (N, N), 0)
        self.a = Image.new("L", (N, N), 0)
        self.dg = ImageDraw.Draw(self.g)
        self.da = ImageDraw.Draw(self.a)

    def face(self, pts, v):
        self.dg.polygon(pts, fill=v)
        self.da.polygon(pts, fill=255)
        return self

    def px(self, pts, v):
        for x, y in pts:
            if 0 <= x < N and 0 <= y < N:
                self.g.putpixel((x, y), v)
                self.a.putpixel((x, y), 255)
        return self

    def line(self, pts, v):
        self.dg.line(pts, fill=v)
        self.da.line(pts, fill=255)
        return self

    def outline(self):
        a = np.array(self.a) > 0
        ring = np.zeros_like(a)
        ring[1:, :] |= a[:-1, :]
        ring[:-1, :] |= a[1:, :]
        ring[:, 1:] |= a[:, :-1]
        ring[:, :-1] |= a[:, 1:]
        ring &= ~a
        g = np.array(self.g)
        g[ring] = O
        self.g = Image.fromarray(g)
        self.a = Image.fromarray(((a | ring) * 255).astype(np.uint8))
        self.dg = ImageDraw.Draw(self.g)
        self.da = ImageDraw.Draw(self.a)
        return self

    def sparkle(self, x, y, r=2):
        """A four-point sparkle beside the gem, dark so it shows on paper."""
        pts = [(x, y, O)] + [(x + i, y, 120) for i in (-r, r)] + [(x, y + i, 120) for i in (-r, r)]
        if r >= 2:
            pts += [(x + i, y, 70) for i in (-1, 1)] + [(x, y + i, 70) for i in (-1, 1)]
        for px, py, v in pts:
            if 0 <= px < N and 0 <= py < N and not self.a.getpixel((px, py)):
                self.g.putpixel((px, py), v)
                self.a.putpixel((px, py), 255)
        return self

    def image(self):
        return Image.merge("RGBA", (self.g, self.g, self.g, self.a)).resize((N * SCALE, N * SCALE), Image.NEAREST)


# --- The six ----------------------------------------------------------------------------------------

def ore0():
    """Hoàng phẩm: a raw nugget, broken into rough facets, flecked."""
    g = Gem()
    g.face([(8, 9), (15, 5), (21, 7), (17, 13), (10, 14)], H)                 # the top, in the light
    g.face([(21, 7), (27, 11), (29, 18), (22, 16), (17, 13)], M + 16)         # upper right
    g.face([(10, 14), (17, 13), (22, 16), (19, 22), (11, 21)], L)             # the front
    g.face([(3, 17), (8, 9), (10, 14), (11, 21), (5, 23)], M + 4)             # left
    g.face([(5, 23), (11, 21), (19, 22), (16, 28), (8, 27)], D)               # under
    g.face([(22, 16), (29, 18), (26, 25), (16, 28), (19, 22)], D + 24)        # lower right
    # Flecks of rock in the ore, and the light on the top.
    g.px([(14, 18), (7, 18), (24, 13), (22, 22)], D - 14)
    g.px([(11, 10), (12, 9), (13, 8), (12, 10)], W)
    return g.outline().image()


def _prism(g, x, top, w, bottom, tip, shades):
    """An upright crystal: left face lit, right face in shade, a pointed tip of two facets."""
    m = x + w // 2
    g.face([(x, bottom), (x, top), (m, top), (m, bottom)], shades[0])
    g.face([(m, bottom), (m, top), (x + w, top), (x + w, bottom)], shades[1])
    g.face([(x, top), (m, tip), (m, top)], shades[2])
    g.face([(m, top), (m, tip), (x + w, top)], shades[3])


def ore1():
    """Huyền phẩm: a cluster of three crystals on a bit of rock."""
    g = Gem()
    _prism(g, 4, 15, 7, 26, 9, (M + 10, D, H, M))
    _prism(g, 21, 13, 7, 26, 7, (M + 10, D, H, M))
    _prism(g, 11, 9, 10, 27, 2, (L, M, W - 10, L - 20))
    # Light down the tall one, and on the tips.
    g.line([(13, 11), (13, 22)], W)
    g.px([(6, 17), (6, 18), (23, 15), (23, 16)], H)
    g.face([(3, 26), (8, 24), (16, 25), (25, 24), (29, 27), (26, 29), (6, 29)], D - 10)   # the rock
    g.px([(9, 26), (20, 27), (14, 28)], M - 20)
    g.outline()
    g.sparkle(28, 4)
    return g.image()


def ore2():
    """Địa phẩm: a step-cut stone, four bevelled bands round a flat table."""
    g = Gem()
    outer = [(8, 6), (24, 6), (27, 9), (27, 23), (24, 26), (8, 26), (5, 23), (5, 9)]
    inner = [(10, 10), (22, 10), (23, 11), (23, 21), (22, 22), (10, 22), (9, 21), (9, 11)]
    g.face(outer, M)
    g.face([outer[0], outer[1], inner[1], inner[0]], H)                       # top band
    g.face([outer[7], outer[0], inner[0], inner[7]], L)
    g.face([outer[6], outer[7], inner[7], inner[6]], L)                       # left band
    g.face([outer[1], outer[2], inner[2], inner[1]], M + 20)
    g.face([outer[2], outer[3], inner[3], inner[2]], D + 10)                  # right band
    g.face([outer[3], outer[4], inner[4], inner[3]], D)
    g.face([outer[4], outer[5], inner[5], inner[4]], D)                       # bottom band
    g.face([outer[5], outer[6], inner[6], inner[5]], M - 20)
    g.face(inner, M + 14)                                                     # the table
    # A second step inside the table, and a streak of light across it.
    g.line([(11, 12), (21, 12)], L)
    g.line([(11, 20), (21, 20)], M - 14)
    g.line([(12, 18), (17, 13)], W)
    g.line([(13, 19), (18, 14)], H)
    g.px([(9, 7), (10, 7), (6, 10)], W)
    g.outline()
    g.sparkle(29, 3, 1)
    return g.image()


def ore3():
    """Thiên phẩm: a brilliant-cut gem, crown, table and pavilion, sparkling."""
    g = Gem()
    tl, tr, gl, gr, pt = (9, 7), (22, 7), (3, 12), (28, 12), (15, 28)
    a, b = (12, 7), (19, 7)                                                   # the table's top corners
    c, d = (11, 12), (20, 12)                                                 # where the crown meets the girdle
    g.face([tl, a, c, gl], L)
    g.face([a, b, d, c], H)
    g.face([b, tr, gr, d], M)
    g.face([gl, c, (12, 16), pt], M + 12)
    g.face([c, d, (19, 16), pt, (12, 16)], L + 10)
    g.face([d, gr, pt, (19, 16)], D)
    # The facet lines of the pavilion, catching light on the lit side.
    g.line([gl, (12, 16)], H)
    g.line([(19, 16), gr], D - 20)
    g.line([c, (12, 16)], W - 30)
    g.line([d, (19, 16)], M - 20)
    g.px([(13, 8), (14, 8), (13, 9), (6, 11), (7, 10)], W)
    g.outline()
    g.sparkle(27, 4)
    g.sparkle(4, 25, 1)
    return g.image()


def ore4():
    """Âm Dương: an orb split by the Thái Cực, ringed by motes."""
    g = Gem()
    cx, cy, r = 15.5, 15.5, 11.5
    for y in range(N):
        for x in range(N):
            dx, dy = x + 0.5 - cx, y + 0.5 - cy
            dist = math.hypot(dx, dy)
            if dist > r:
                continue
            # Yin (dark) on the right, with the upper small circle; yang on the left, with the lower.
            up = math.hypot(dx, dy + r / 2) < r / 2
            down = math.hypot(dx, dy - r / 2) < r / 2
            dark = (dx > 0 and not down) or up
            if math.hypot(dx, dy + r / 2) < 1.8:
                dark = False
            if math.hypot(dx, dy - r / 2) < 1.8:
                dark = True
            # The sphere: light from the upper left, a darker rim below and to the right.
            lit = -(dx * 0.6 + dy * 0.8) / r
            if dark:
                v = D - 20 if lit < -0.45 else D if lit < 0.2 else M - 30
            else:
                v = M + 20 if lit < -0.45 else L + 10 if lit < 0.2 else H
            if dist > r - 1.2 and lit < 0:
                v = max(O + 16, v - 40)
            g.px([(x, y)], v)
    g.px([(9, 8), (10, 7), (8, 9), (11, 7)], W)
    g.outline()
    for k in range(8):
        t = 2 * math.pi * k / 8 + 0.35
        x, y = int(round(cx - 0.5 + 15 * math.cos(t))), int(round(cy - 0.5 + 15 * math.sin(t)))
        if 0 <= x < N and 0 <= y < N and not g.a.getpixel((x, y)):
            g.px([(x, y)], O if k % 2 == 0 else 120)
    return g.image()


def ore5():
    """Hỗn Độn: a dark gem grown into a rock, a seam of light through it."""
    g = Gem()
    rock = [(5, 11), (11, 4), (22, 4), (28, 11), (29, 22), (23, 29), (9, 29), (3, 22)]
    g.face(rock, M - 6)
    g.face([(5, 11), (11, 4), (22, 4), (16, 10)], L - 10)                     # the rock's lit top
    g.face([(29, 22), (23, 29), (9, 29), (18, 23)], D + 10)                   # its underside
    g.px([(7, 20), (25, 9), (26, 18), (12, 27), (20, 27), (6, 15)], D)
    g.px([(13, 6), (8, 10), (24, 24)], H)
    # The gem of chaos: a long rhombus, dark facets, edges catching the light.
    top, left, bot, right = (19, 6), (9, 14), (13, 26), (24, 18)
    mid = (16, 16)
    g.face([top, left, mid], D + 30)
    g.face([top, mid, right], O + 18)
    g.face([left, bot, mid], D - 10)
    g.face([mid, bot, right], O + 6)
    g.line([left, top], H)
    g.line([top, right], M)
    g.line([(12, 21), (20, 12)], W)                                           # the seam of light
    g.line([(13, 21), (21, 12)], L)
    g.px([(17, 8), (16, 9)], W)
    g.outline()
    g.sparkle(29, 3)
    g.sparkle(2, 4, 1)
    g.sparkle(30, 28, 1)
    return g.image()


ORES = [ore0, ore1, ore2, ore3, ore4, ore5]


if __name__ == "__main__":
    out = sys.argv[1]
    ims = [f() for f in ORES]
    sheet = Image.new("RGB", (6 * 200, 290), (238, 238, 232))
    for i, im in enumerate(ims):
        sheet.paste(im, (i * 200 + 4, 4), im)
        small = im.resize((64, 64), Image.LANCZOS)
        sheet.paste(small, (i * 200 + 68, 212), small)
    sheet.save(out)
