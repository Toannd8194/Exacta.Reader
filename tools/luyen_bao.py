"""Luyện bảo (4.1): the six stages of a treasure in the furnace, in the pixel art of the ores and the
pháp bảo (the 32 grid, ramps of six colours), the flying sword of tools/phap_bao.py taking form:

    1 Phôi liệu      two chunks of iron over the fire
    2 Luyện hỏa      the iron molten into one glowing drop
    3 Đúc phôi       the blank of a blade, red hot, a tang below
    4 Khắc trận văn  the blade in grey steel, runes of qi cut along it, a rough guard
    5 Khai phong     the edge ground bright, guard and grip in place, sparks flying
    6 Thành hình     the sword whole, waiting for its furnace to be opened

    python tools/luyen_bao.py            writes art_stage_1 … art_stage_6 into res/drawable-nodpi
    python tools/luyen_bao.py out.png    a preview sheet only
"""
import math
import os
import sys

import numpy as np
from PIL import Image

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import khoang_thach as K  # noqa: E402
from phap_bao import at  # noqa: E402

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(ROOT, "app", "src", "main", "res", "drawable-nodpi")


def tongue(x, b, hh, ww, lean=0.0):
    return [(x - ww, b), (x - ww * 0.7, b - hh * 0.45), (x + lean * ww, b - hh), (x + ww * 0.55, b - hh * 0.5), (x + ww, b)]


def fire(c, cx=64, base=124, h=30, w=40):
    """A bed of coals and the fire on it: three tongues, each with a hotter core."""
    bed = K.Layer(4)
    m = K.ellipse_mask(cx, base - 2, w * 0.62, 6)
    bed.put(m, 0.42)
    for (x, y) in ((cx - 14, base - 3), (cx + 4, base - 1), (cx + 18, base - 4)):
        bed.put(K.ellipse_mask(x, y, 3, 2) & m, 0.8, mat=5)
    c.paste(bed)
    for (dx, hh, ww, lean) in ((-w * 0.32, h * 0.75, w * 0.22, -0.3), (w * 0.3, h * 0.7, w * 0.2, 0.3), (0, h, w * 0.28, 0.1)):
        x = cx + dx
        outer = K.Layer(5)
        outer.put(K.poly_mask(tongue(x, base - 4, hh, ww, lean)), 0.45)
        c.paste(outer)
        mid = K.Layer(5)
        mid.put(K.poly_mask(tongue(x, base - 4, hh * 0.68, ww * 0.62, lean)) & outer.a, 0.72)
        c.paste(mid, line=False)
        core = K.Layer(5)
        core.put(K.poly_mask(tongue(x, base - 4, hh * 0.38, ww * 0.32, lean)) & outer.a, 0.97)
        c.paste(core, line=False)


def sphere(cx, cy, rx, ry, albedo, spec=0.6, shin=18, amb=0.35):
    nx, ny = (K.XX + 0.5 - cx) / rx, (K.YY + 0.5 - cy) / ry
    nz = np.sqrt(np.clip(1 - nx ** 2 - ny ** 2, 0, 1))
    n = K.norm(np.stack([nx, ny, nz + 1e-3], -1))
    return np.clip(K.shade(n, albedo, spec=spec, shin=shin, amb=amb), 0, 1)


def blade(c, mat=2, albedo=(0.95, 0.55), ridge=True, point=True, half=9):
    lay = K.Layer(mat)
    tip = [at(1.0, 0)] if point else [at(0.92, -half * 0.8), at(0.92, half * 0.8)]
    if point:
        lay.facet([at(0.27, -half), at(0.9, -half * 0.78), at(1.0, 0), at(0.27, 0)], K.norm([-0.5, -0.6, 0.65]), albedo[0], spec=1.1, shin=30)
        lay.facet([at(0.27, 0), at(1.0, 0), at(0.9, half * 0.78), at(0.27, half)], K.norm([0.55, 0.4, 0.7]), albedo[1], spec=0.8, shin=30)
    else:
        lay.facet([at(0.16, -half), tip[0], at(0.94, 0), at(0.16, 0)], K.norm([-0.5, -0.6, 0.65]), albedo[0], spec=0.6, shin=14)
        lay.facet([at(0.16, 0), at(0.94, 0), tip[1], at(0.16, half)], K.norm([0.55, 0.4, 0.7]), albedo[1], spec=0.5, shin=14)
    if ridge:
        lay.stroke([at(0.3, 0), at(0.96, 0)], 1.0, 4)
        lay.stroke([at(0.3, 3), at(0.88, 3)], 0.35)
    c.paste(lay)
    return lay


def guard(c, mat=9, curls=True, value=0.95):
    g = K.Layer(mat)
    g.facet([at(0.2, -22), at(0.27, -16), at(0.27, 16), at(0.2, 22), at(0.23, 0)], K.norm([-0.3, -0.5, 0.8]), value, spec=1.0 if mat == 9 else 0.3, shin=24)
    if curls:
        for side in (-1, 1):
            cx, cy = at(0.2, 23 * side)
            g.put(K.ellipse_mask(cx, cy, 6, 6), 0.8)
        gx, gy = at(0.235, 0)
        g.put(K.ellipse_mask(gx, gy, 5, 5), 0.98)
    c.paste(g)


def grip(c, wrapped=True):
    gr = K.Layer(11)
    gr.facet([at(0.04, -6), at(0.21, -6), at(0.21, 6), at(0.04, 6)], K.norm([-0.2, -0.3, 0.9]), 0.7, spec=0.3, shin=10)
    if wrapped:
        for t in (0.08, 0.125, 0.17):
            band = K.line_mask([at(t, -6), at(t, 6)], 4) & gr.a
            gr.put(band, np.full((K.N, K.N), 0.85), mat=9)
    c.paste(gr)


def pommel(c):
    g = K.Layer(9)
    px, py = at(0.0, 0)
    g.put(K.ellipse_mask(px, py, 7, 7), 0.85)
    c.paste(g)


def stage1():
    c = K.Canvas()
    fire(c, h=26)
    for pts, seed in (([(30, 70), (42, 50), (62, 46), (72, 62), (62, 84), (38, 86)], 11), ([(66, 78), (76, 58), (96, 54), (106, 72), (96, 92), (72, 92)], 12)):
        c.paste(K.rock(pts, albedo=1.0, seed=seed, cells=5, tilt=0.8, cracks=0.3, dome=10, mat=2))
    c.sparkle(100, 30, 5, mat=2)
    return c.image()


def stage2():
    c = K.Canvas()
    fire(c, h=34, w=46)
    drop = K.Layer(5)
    m = K.ellipse_mask(64, 66, 26, 20) | K.poly_mask([(56, 80), (72, 80), (66, 96), (62, 96)])
    v = sphere(64, 66, 26, 20, 0.95, spec=0.8)
    v = np.where(K.poly_mask([(56, 80), (72, 80), (66, 96), (62, 96)]) & ~K.ellipse_mask(64, 66, 26, 20), 0.85, v)
    drop.put(m, v)
    for (x, y, r) in ((52, 60, 4), (74, 72, 3), (66, 54, 2.5)):
        drop.put(K.ellipse_mask(x, y, r, r) & m, 0.38, mat=4)
    c.paste(drop)
    c.sparkle(102, 34, 5, mat=5)
    c.sparkle(28, 40, 4, mat=5)
    return c.image()


def stage3():
    c = K.Canvas()
    fire(c, cx=70, h=24)
    tang = K.Layer(4)
    tang.facet([at(0.02, -4), at(0.17, -4), at(0.17, 4), at(0.02, 4)], K.norm([-0.2, -0.3, 0.9]), 0.6, spec=0.2, shin=10)
    c.paste(tang)
    lay = blade(c, mat=5, albedo=(1.0, 0.85), ridge=False, point=False, half=11)
    cool = (K.XX > 96) & (K.YY < 26) & lay.a
    c.m[cool] = 2
    c.sparkle(22, 30, 4, mat=5)
    return c.image()


def stage4():
    c = K.Canvas()
    fire(c, cx=74, h=18, w=34)
    tang = K.Layer(4)
    tang.facet([at(0.04, -5), at(0.21, -5), at(0.21, 5), at(0.04, 5)], K.norm([-0.2, -0.3, 0.9]), 0.55, spec=0.2, shin=10)
    c.paste(tang)
    guard(c, mat=4, curls=False, value=0.7)
    blade(c, mat=2, albedo=(0.72, 0.42), ridge=False)
    runes = K.Layer(8)
    for t in (0.38, 0.5, 0.62, 0.74, 0.86):
        runes.put(K.line_mask([at(t, -4), at(t + 0.03, 0), at(t, 4)], 4), 0.95)
    runes.a &= c.a
    c.paste(runes, line=False)
    c.sparkle(112, 44, 5, mat=8)
    return c.image()


def stage5():
    c = K.Canvas()
    grip(c, wrapped=True)
    guard(c, mat=9, curls=False)
    blade(c, mat=2)
    pommel(c)
    for (x, y, s) in ((112, 54, 1.5), (120, 44, 1.5), (104, 64, 1.5)):
        sp = K.Layer(5)
        sp.put(K.poly_mask([(x - s, y - s), (x + s, y - s), (x + s, y + s), (x - s, y + s)]), 0.9)
        c.paste(sp)
    return c.image()


def stage6():
    c = K.Canvas()
    grip(c)
    guard(c)
    blade(c)
    pommel(c)
    tas = K.Layer(3)
    px, py = at(0.0, 0)
    tas.put(K.line_mask([(px - 2, py + 6), (px - 8, py + 12)], 4), np.full((K.N, K.N), 0.6))
    tas.put(K.poly_mask([(px - 16, py + 10), (px - 6, py + 12), (px - 10, py + 22), (px - 18, py + 20)]), np.full((K.N, K.N), 0.75))
    c.paste(tas)
    c.sparkle(108, 46, 6, mat=8)
    return c.image()


STAGES = [stage1, stage2, stage3, stage4, stage5, stage6]


def main():
    ims = [f() for f in STAGES]
    if len(sys.argv) > 1:
        S = 256
        sheet = Image.new("RGB", (6 * (S + 16), 2 * S + 16), (238, 238, 232))
        for i, im in enumerate(ims):
            big = im.resize((S, S), Image.NEAREST)
            sheet.paste(big, (i * (S + 16), 0), big)
            g = big.convert("LA").convert("RGBA")
            sheet.paste(g, (i * (S + 16), S + 16), g)
        sheet.save(sys.argv[1])
        return
    for i, im in enumerate(ims):
        im.save(os.path.join(RES, "art_stage_%d.png" % (i + 1)))
    print("luyen bao")


if __name__ == "__main__":
    main()
