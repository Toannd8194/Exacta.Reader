"""Khoáng thạch (3.2.1): six ores in pixel art on a 128 grid (128 px, about 1:1 on the device). Each
material is a ramp of six colours (shadows cooler, lights warmer), chosen so that the e-ink screen's
gray still shows the form; linh thạch stays in sixteen grays. Each facet carries a normal and
takes the light from the upper left: diffuse, a specular glint, and for the stones that let light
through, a glow on the side away from it. A 2-pixel outline round the whole, 1 pixel between parts.

    0 Cấp 1 · Hoàng    a stone, and no more: angular, broken into flat faces
    1 Cấp 2 · Huyền    tinh thiết: a chunk of iron shining like steel, clods of earth clinging to it
    2 Cấp 3 · Địa      hồng ngọc hidden in stone: a rock split open, a ruby looking out of the break
    3 Cấp 4 · Thiên    a block of sky iron falling, its fire streaming back
    4 Cấp 5 · Âm Dương the Thái Cực as a pearl, its breath rising round it in two streams
    5 Cấp 6 · Hỗn Độn  half real, half dream: a pearl of chaos in a cradle of golden petals; on one side
                       solid, on the other there and not there, an echo drifting off in motes
   linh thạch     chunky spirit crystals on a mound of earth, clear, lit within

Used by tools/ve_do_hoa.py (--khoangthach); run alone to preview: python tools/khoang_thach.py out.png
"""
import math
import random
import sys

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

# 3.2.1: drawn on a 32 grid, true pixel art, then shown ×4 (128 px). The shapes are written in the
# coordinates of a 128 frame; SP maps them onto the grid.
N = 32
SP = 128 / N
SCALE = 4
OUT = 0.07                                        # outline
LIGHT = np.array([-0.5, -0.72, 0.85])
LIGHT = LIGHT / np.linalg.norm(LIGHT)
YY, XX = (np.mgrid[0:N, 0:N] * SP + (SP - 1) / 2).astype(float)

# Materials (3.2.1): 0 is plain gray in sixteen tones; the others are ramps of six colours, dark to
# light, the shadows cooler and the lights warmer, as pixel art is painted.
def _hex(h):
    return tuple(int(h[i:i + 2], 16) for i in (1, 3, 5))


RAMPS = {
    1: ["#2b2630", "#4a4248", "#6b6260", "#8f857c", "#b3a899", "#d8cfc0"],   # đá
    2: ["#10111c", "#1f2233", "#343a54", "#525d7e", "#8796b8", "#d6e2f6"],   # huyền thiết
    3: ["#3a0b1e", "#6e1230", "#a51d3d", "#d8344c", "#f2717a", "#ffd6d2"],   # hồng ngọc
    4: ["#1e1418", "#38262a", "#57393a", "#7a544c", "#9f7463", "#c49f88"],   # đá trời
    5: ["#6a1606", "#a8290c", "#e0521a", "#f8942a", "#ffd35a", "#fff7cc"],   # lửa
    6: ["#4c4766", "#7d7999", "#aeacc4", "#d6d5e4", "#efeef6", "#ffffff"],   # dương
    7: ["#06050c", "#121020", "#201d33", "#332f4c", "#4d4869", "#7f79a6"],   # âm
    8: ["#2a2466", "#4b44a0", "#6f72cf", "#9aa6ec", "#c9d6ff", "#f4f8ff"],   # khí tức
    9: ["#3d2208", "#6e3f10", "#a8661c", "#d9952c", "#f4c75a", "#fff1b8"],   # vàng
    10: ["#2a0a2a", "#5c1446", "#962562", "#cf3d78", "#f07aa4", "#ffdcea"],  # ngọc hỗn độn
    11: ["#1f140e", "#3b2418", "#5a3a24", "#7c5534", "#a0744a", "#c49a6a"],  # đất
}
RAMPS = {k: [_hex(c) for c in v] for k, v in RAMPS.items()}
LINE_RGB = (27, 19, 36)


def norm(v):
    v = np.asarray(v, dtype=float)
    return v / np.linalg.norm(v, axis=-1, keepdims=True)


def _g(p):
    """A point of the 128 frame on the grid."""
    return (p[0] / SP - 0.5, p[1] / SP - 0.5)


def poly_mask(pts):
    m = Image.new("L", (N, N), 0)
    ImageDraw.Draw(m).polygon([_g(p) for p in pts], fill=255)
    return np.array(m) > 0


def ellipse_mask(cx, cy, rx, ry):
    return ((XX + 0.5 - cx) / rx) ** 2 + ((YY + 0.5 - cy) / ry) ** 2 <= 1


def line_mask(pts, width=1):
    m = Image.new("L", (N, N), 0)
    ImageDraw.Draw(m).line([_g(p) for p in pts], fill=255, width=max(1, int(round(width / SP))))
    return np.array(m) > 0


def ring(mask):
    r = np.zeros_like(mask)
    r[1:, :] |= mask[:-1, :]
    r[:-1, :] |= mask[1:, :]
    r[:, 1:] |= mask[:, :-1]
    r[:, :-1] |= mask[:, 1:]
    return r & ~mask


def noise(seed, scale=8, octaves=3):
    rnd = np.random.default_rng(seed)
    out = np.zeros((N, N))
    amp, total = 1.0, 0.0
    for o in range(octaves):
        s = max(2, N // (scale * 2 ** o) + 1)
        g = Image.fromarray((rnd.random((s, s)) * 255).astype(np.uint8)).resize((N, N), Image.BICUBIC)
        out += amp * (np.array(g) / 255.0 - 0.5)
        total += amp
        amp *= 0.5
    return out / total


def shade(n, albedo, spec=0.5, shin=24, trans=0.0, amb=0.17):
    """Light on a normal field: ambient, diffuse, a specular glint, and light passing through."""
    ndl = n @ LIGHT
    diff = np.clip(ndl, 0, None)
    r = 2 * ndl[..., None] * n - LIGHT
    sp = np.clip(r[..., 2], 0, None) ** shin
    back = np.clip(-ndl, 0, None) * trans + trans * 0.35 * (1 - np.abs(n[..., 2]))
    return amb * albedo + 0.8 * albedo * diff + spec * sp + back * albedo


class Layer:
    def __init__(self, mat=0):
        self.v = np.zeros((N, N))
        self.a = np.zeros((N, N), dtype=bool)
        self.m = np.zeros((N, N), dtype=int)
        self.cur = mat

    def put(self, mask, value, mat=None):
        value = np.broadcast_to(value, (N, N)) if np.ndim(value) == 0 else value
        self.v[mask] = value[mask]
        self.m[mask] = self.cur if mat is None else mat
        self.a |= mask
        return self

    def facet(self, pts, n, albedo, spec=0.5, shin=24, trans=0.0, curv=0.12):
        """A flat facet, bowed a little so the light runs across it."""
        m = poly_mask(pts)
        if not m.any():
            return m
        p = np.array(pts, dtype=float)
        cx, cy = p[:, 0].mean(), p[:, 1].mean()
        R = max(6.0, np.ptp(p[:, 0]), np.ptp(p[:, 1]))
        nn = np.zeros((N, N, 3))
        nn[...] = n
        nn[..., 0] += curv * (XX - cx) / R
        nn[..., 1] += curv * (YY - cy) / R
        self.put(m, shade(norm(nn), albedo, spec, shin, trans))
        return m

    def stroke(self, pts, value, width=1, inside=True):
        m = line_mask(pts, width)
        if inside:
            m &= self.a
        self.v[m] = value if np.ndim(value) == 0 else value[m]
        return self


class Canvas:
    def __init__(self):
        self.v = np.zeros((N, N))
        self.a = np.zeros((N, N), dtype=bool)
        self.m = np.zeros((N, N), dtype=int)
        self.line = np.zeros((N, N), dtype=bool)
        self.ghost = np.zeros((N, N), dtype=bool)

    def paste(self, layer, line=True, ghost=False):
        """Back to front: a 1-pixel line round each part keeps it apart from what lies behind. A
        ghost part is drawn on every other pixel only: there and not there."""
        if line:
            r = ring(layer.a)
            self.v[r] = OUT
            self.line[r] = True
            self.ghost[r] = ghost
            self.a |= r
        self.v[layer.a] = layer.v[layer.a]
        self.m[layer.a] = layer.m[layer.a]
        self.line[layer.a] = False
        self.ghost[layer.a] = ghost
        self.a |= layer.a

    def dot(self, x, y, v, mat=0):
        x, y = int(x / SP), int(y / SP)
        if 0 <= x < N and 0 <= y < N:
            self.v[y, x] = v
            self.m[y, x] = mat
            self.line[y, x] = mat == 0 and v <= OUT
            self.a[y, x] = True

    def sparkle(self, x, y, r=6, mat=0):
        """A four-point star beside the stone, with a dark edge so it shows on paper."""
        gx, gy = int(x / SP), int(y / SP)
        arm = 2 if r >= 6 else 1
        m = np.zeros((N, N), dtype=bool)
        for d in range(-arm, arm + 1):
            for px, py in ((gx + d, gy), (gx, gy + d)):
                if 0 <= px < N and 0 <= py < N:
                    m[py, px] = True
        m &= ~self.a
        rr = ring(m) & ~self.a
        self.v[rr] = OUT
        self.line[rr] = True
        self.v[m] = 0.62 if mat == 0 else 0.75
        self.m[m] = mat
        self.a |= m | rr
        if 0 <= gx < N and 0 <= gy < N and m[gy, gx]:
            self.v[gy, gx] = 1.0

    def image(self):
        # On a fine grid the silhouette takes a second pixel of line; on the pixel grid one is enough.
        r = ring(self.a) if SP <= 1 else np.zeros((N, N), dtype=bool)
        v = self.v.copy()
        line = self.line | r
        ghost = self.ghost.copy()
        ghost[r] = False
        a = self.a | r
        coloured = (self.m[self.a] > 0).any()
        rgb = np.zeros((N, N, 3))
        q = np.floor(np.clip(v, 0, 1) * 15 + 0.5) / 15
        rgb[...] = (q * 255)[..., None]
        for k, ramp in RAMPS.items():
            mk = (self.m == k) & ~line
            if mk.any():
                idx = np.clip((np.clip(v, 0, 1) * 6).astype(int), 0, 5)
                pal = np.array(ramp, dtype=float)
                rgb[mk] = pal[idx[mk]]
        rgb[line] = LINE_RGB if coloured else (OUT * 255,) * 3
        # A ghost is there on every other pixel.
        a = a & ~(ghost & (((XX + YY).astype(int) % 2) == 0))
        out = np.dstack([np.clip(rgb, 0, 255), a[..., None] * 255.0]).astype(np.uint8)
        return Image.fromarray(out, "RGBA").resize((N * SCALE, N * SCALE), Image.NEAREST)


# --- Parts ------------------------------------------------------------------------------------------

def rock(pts, albedo=0.55, seed=1, cells=14, tilt=0.45, cracks=0.5, dome=22.0, mat=0):
    """A lump of rock: domed, broken into flat facets (Voronoi cells), grained, cracked along some
    facet edges."""
    lay = Layer(mat)
    m = poly_mask(pts)
    blur = np.array(Image.fromarray((m * 255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(max(1, 9 / SP)))) / 255.0
    gy, gx = np.gradient(blur, SP)
    rnd = random.Random(seed)
    ys, xs = np.nonzero(m)
    seeds = [(xs[j], ys[j]) for j in (rnd.randrange(len(xs)) for _ in range(cells))]
    d = np.stack([(XX - sx) ** 2 + (YY - sy) ** 2 for sx, sy in seeds])
    cell = d.argmin(0)
    tilts = np.array([(rnd.uniform(-tilt, tilt), rnd.uniform(-tilt, tilt)) for _ in seeds])
    n = np.zeros((N, N, 3))
    n[..., 0] = -gx * dome + tilts[cell, 0]
    n[..., 1] = -gy * dome + tilts[cell, 1]
    n[..., 2] = 1
    grain = noise(seed, 6, 3)
    alb = albedo * (1 + 0.35 * grain)
    v = shade(norm(n), alb, spec=0.12, shin=10)
    # Cracks along some facet edges, a lit lip under them.
    edge = np.zeros_like(m)
    edge[:, 1:] |= cell[:, 1:] != cell[:, :-1]
    edge[1:, :] |= cell[1:, :] != cell[:-1, :]
    keep = np.array([rnd.random() < cracks for _ in seeds])
    edge &= keep[cell] & m
    v[edge] *= 0.55
    lip = np.zeros_like(edge)
    lip[1:, 1:] = edge[:-1, :-1]
    lip &= m & ~edge
    v[lip] = np.minimum(1, v[lip] * 1.18)
    lay.put(m, v)
    lay.normals = norm(n)
    return lay


def prism(lay, base, tip, w, h, albedo=0.8, spec=0.75, shin=30, trans=0.4, skew=0.0, phantom=True, glow=None):
    """A hexagonal crystal lying in the picture: three faces along its length and a tip of three
    facets. `skew` breaks the tip off to one side."""
    bx, by = base
    tx, ty = tip
    L = math.hypot(tx - bx, ty - by)
    ux, uy = (tx - bx) / L, (ty - by) / L
    px, py = -uy, ux
    if px < 0:
        px, py = -px, -py
    sx, sy = tx - ux * h, ty - uy * h
    tx2, ty2 = tx + px * skew * w, ty + py * skew * w
    offs = [-w, -0.42 * w, 0.42 * w, w]
    at = lambda cx, cy, o: (cx + px * o, cy + py * o)
    p3 = np.array([px, py, 0.0])
    u3 = np.array([ux, uy, 0.0])
    z = np.array([0.0, 0.0, 1.0])
    normals = [-0.87 * p3 + 0.5 * z, z, 0.87 * p3 + 0.5 * z]
    masks = []
    for i in range(3):
        o1, o2 = offs[i], offs[i + 1]
        body = [at(bx, by, o1), at(sx, sy, o1), at(sx, sy, o2), at(bx, by, o2)]
        m = lay.facet(body, normals[i], albedo, spec, shin, trans, curv=0.05)
        # Deeper toward the root, where the crystal leaves the rock.
        f = np.clip(((XX - bx) * ux + (YY - by) * uy) / L, 0, 1)
        lay.v[m] *= (0.62 + 0.38 * f[m])
        masks.append(m)
        tipn = norm(normals[i] * 0.62 + u3 * 0.78)
        lay.facet([at(sx, sy, o1), (tx2, ty2), at(sx, sy, o2)], tipn, albedo, spec, shin, trans, curv=0.04)
    # The edges: the one between the lit faces catches the light, the far one falls dark.
    lay.stroke([at(bx, by, offs[1]), at(sx, sy, offs[1]), (tx2, ty2)], 0.97)
    lay.stroke([at(bx, by, offs[2]), at(sx, sy, offs[2]), (tx2, ty2)], 0.3)
    lay.stroke([at(sx, sy, offs[0]), at(sx, sy, offs[2])], 0.9)
    lay.stroke([at(sx, sy, offs[2]), at(sx, sy, offs[3])], 0.25)
    if phantom and L > 40:
        # A phantom: the shape of an older tip, ghosted inside.
        k = 0.55
        qx, qy = bx + ux * L * k, by + uy * L * k
        a1, a2 = at(qx - ux * h * 0.8, qy - uy * h * 0.8, -0.8 * w), at(qx - ux * h * 0.8, qy - uy * h * 0.8, 0.8 * w)
        lay.stroke([a1, (qx, qy), a2], 0.82 if glow is None else glow)
    if glow is not None:
        # Light inside: a streak down the front face.
        lay.stroke([at(bx + ux * L * 0.2, by + uy * L * 0.2, -0.1 * w), at(sx, sy, -0.1 * w)], glow, 2)
    return lay


# --- The six ----------------------------------------------------------------------------------------

def _sphere(cx, cy, r, flat=1.0):
    m = ellipse_mask(cx, cy, r, r)
    nx, ny = (XX + 0.5 - cx) / r, (YY + 0.5 - cy) / r
    nz = np.sqrt(np.clip(1 - nx ** 2 - ny ** 2, 0, 1)) * flat
    return m, nx, ny, nz, norm(np.stack([nx, ny, nz + 1e-3], -1))


def _octagon(x0, y0, x1, y1, cut):
    return [(x0 + cut, y0), (x1 - cut, y0), (x1, y0 + cut), (x1, y1 - cut), (x1 - cut, y1), (x0 + cut, y1), (x0, y1 - cut), (x0, y0 + cut)]


def ore0():
    """Cấp 1 · Hoàng phẩm: a stone, and no more: angular, broken into flat faces, a pit or two."""
    c = Canvas()
    pts = [(12, 78), (22, 44), (48, 22), (84, 20), (110, 40), (118, 74), (100, 102), (62, 112), (28, 104)]
    lay = rock(pts, albedo=0.8, seed=31, cells=7, tilt=1.25, cracks=0.95, dome=10, mat=1)
    for (x, y, r) in ((50, 66, 3.0), (86, 82, 2.6)):
        pm = ellipse_mask(x, y, r, r) & lay.a
        lay.v[pm] *= 0.55
    c.paste(lay)
    return c.image()


def _clod(cx, cy, rx, ry, seed):
    """A clod of earth: a lumpy blob, domed, flecked with grit."""
    lay = Layer(11)
    m = ellipse_mask(cx, cy, rx, ry)
    m &= (noise(seed, 3, 2) * 30 + ((XX - cx) / rx) ** 2 * 10 + ((YY - cy) / ry) ** 2 * 10) < 9
    b = np.array(Image.fromarray((m * 255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(max(1, 5 / SP)))) / 255.0
    gy, gx = np.gradient(b, SP)
    n = norm(np.stack([-gx * 16, -gy * 16, np.ones((N, N))], -1))
    v = shade(n, 0.8, spec=0.05, shin=8, amb=0.3)
    rnd = np.random.default_rng(seed)
    v = np.where(rnd.random((N, N)) < 0.12, v * 0.7, v)
    lay.put(m, np.clip(v, 0, 1))
    return lay


def ore1():
    """Cấp 2 · Huyền phẩm: tinh thiết, refined iron dug from the ground: a hard chunk shining like
    steel, clods of earth still clinging to it."""
    c = Canvas()
    lay = Layer(2)
    # Six faces, each turned its own way, so each takes its own step of light like worked steel.
    for pts, n in (
        ([(32, 36), (60, 20), (84, 28), (68, 52), (40, 56)], (-0.45, -0.65, 0.6)),
        ([(84, 28), (108, 44), (116, 72), (88, 64), (68, 52)], (0.55, -0.45, 0.7)),
        ([(40, 56), (68, 52), (88, 64), (76, 88), (44, 84)], (-0.1, -0.1, 1.0)),
        ([(12, 68), (32, 36), (40, 56), (44, 84), (20, 92)], (-0.85, 0.05, 0.5)),
        ([(20, 92), (44, 84), (76, 88), (64, 112), (32, 108)], (0.0, 0.85, 0.5)),
        ([(88, 64), (116, 72), (104, 100), (64, 112), (76, 88)], (0.75, 0.45, 0.45)),
    ):
        lay.facet(pts, norm(n), 0.72, spec=1.1, shin=24, curv=0.08)
    # Bright edges where the lit faces meet.
    lay.stroke([(32, 36), (60, 20), (84, 28)], 0.98)
    lay.stroke([(40, 56), (68, 52), (88, 64)], 0.9)
    c.paste(lay)
    for (x, y, rx, ry, sd) in ((38, 82, 15, 11, 61), (86, 92, 13, 9, 62), (104, 62, 7, 10, 63), (62, 100, 10, 6, 64)):
        c.paste(_clod(x, y, rx, ry, sd))
    c.sparkle(50, 30, 5, mat=2)
    c.sparkle(116, 16, 6, mat=2)
    return c.image()


def ore2():
    """Cấp 3 · Địa phẩm: hồng ngọc hidden in stone: a plain rock split open, a ruby crystal looking
    out of the break, a fleck or two of red in the rock."""
    c = Canvas()
    back = rock([(10, 76), (20, 42), (46, 24), (82, 22), (110, 38), (120, 70), (108, 100), (70, 112), (30, 106)],
                albedo=0.72, seed=52, cells=8, tilt=0.6, cracks=0.3, dome=18, mat=1)
    # The break: a dark hollow where the ruby sits.
    hollow = poly_mask([(40, 64), (54, 40), (84, 36), (96, 56), (84, 80), (56, 84)]) & back.a
    back.v[hollow] = 0.12
    back.v[ring(hollow) & back.a] *= 0.7
    c.paste(back)
    gem = Layer(3)
    prism(gem, (58, 84), (80, 30), 13, 12, albedo=0.85, spec=0.9, shin=30, trans=0.6, phantom=False, glow=1.0)
    gem.a &= hollow | poly_mask([(70, 20), (96, 30), (88, 48), (66, 40)])
    c.paste(gem)
    lip = rock([(18, 96), (34, 78), (60, 86), (84, 82), (104, 94), (96, 112), (58, 118), (26, 112)],
               albedo=0.78, seed=53, cells=5, tilt=0.5, cracks=0.2, dome=14, mat=1)
    c.paste(lip)
    for (x, y) in ((30, 60), (100, 76)):
        fl = Layer(3)
        fl.put(ellipse_mask(x, y, 3, 3), 0.8)
        c.paste(fl, line=False)
    c.sparkle(112, 14, 6, mat=3)
    return c.image()


def ore3():
    """Cấp 4 · Thiên phẩm: a block of sky iron falling, cratered and hot, fire streaming back from it."""
    c = Canvas()
    cx, cy, r = 46, 84, 25
    d = norm([1.0, -1.0])
    px, py = -d[1], d[0]
    t = ((XX + 0.5 - cx) * d[0] + (YY + 0.5 - cy) * d[1]) / 96
    lat = ((XX + 0.5 - cx) * px + (YY + 0.5 - cy) * py)
    jag = np.clip(1 + (0.42 * np.sin(t * 26 + lat * 0.3) + 0.5 * noise(52, 4, 2)) * np.clip(t, 0, 1) * 1.4, 0.55, 1.6)
    width = r * 1.3 * np.clip(1 - t, 0, 1) ** 0.45 * jag
    width = np.where(t < 0, r * 1.05, width)
    ahead = t > -0.12
    for k, (frac, until, v) in enumerate(((1.0, 1.0, 0.3), (0.7, 0.8, 0.55), (0.45, 0.6, 0.75), (0.24, 0.36, 0.95))):
        lay = Layer(5)
        lay.put((np.abs(lat) < width * frac) & (t < until) & ahead, v)
        c.paste(lay, line=(k == 0))
    for (x, y, s, v) in ((114, 12, 3, 0.75), (122, 30, 2, 0.55), (100, 6, 2, 0.95), (124, 48, 2, 0.75), (90, 14, 2, 0.55)):
        sp = Layer(5)
        sp.put(poly_mask([(x - s, y - s), (x + s, y - s), (x + s, y + s), (x - s, y + s)]), v)
        c.paste(sp)
    # The block: angular, not round; its faces cratered; the face meeting the air glows.
    st = rock([(22, 80), (28, 64), (44, 58), (62, 62), (72, 76), (70, 96), (58, 108), (38, 110), (24, 100)],
              albedo=0.85, seed=21, cells=7, tilt=0.55, cracks=0.0, dome=20, mat=4)
    for (ax, ay, ar) in ((38, 86, 4.5), (56, 96, 3.2), (48, 72, 2.6), (32, 100, 2.4)):
        pm = ellipse_mask(ax, ay, ar, ar) & st.a
        st.v[pm] *= 0.5
        lip = np.roll(np.roll(pm, 1, 0), 1, 1) & ~pm & st.a
        st.v[lip] = np.minimum(1, st.v[lip] * 1.3)
    hot = st.a & (((XX - cx) * d[0] + (YY - cy) * d[1]) > 6)
    st.put(hot, np.clip(0.55 + 0.04 * (((XX - cx) * d[0] + (YY - cy) * d[1]) - 6), 0, 0.95), mat=5)
    c.paste(st)
    return c.image()


def ore4():
    """Cấp 5 · Âm Dương: the Thái Cực as a pearl, day and night in one, its breath (khí tức) turning
    round it in two streams, one light and one dark."""
    c = Canvas()
    cx, cy, r = 64, 64, 34

    def stream(front, phase, mat, width, turn=1):
        """A breath of khí rising from the foot of the pearl, wrapping round it and curling up off
        the top like smoke, thick at its root, thinning to a wisp."""
        lay = Layer(mat)
        T = 0.85 * math.pi
        for k in range(160):
            t = k / 160 * T
            ang = phase + turn * t
            rr = r + 4 + 10 * t / T
            x = cx + rr * math.cos(ang) + turn * 8 * (t / T) ** 3
            y = cy + rr * 0.8 * math.sin(ang) - 14 * (t / T) ** 2
            if (math.sin(ang) > -0.15) != front:
                continue
            w = width * (1 - t / T) + 1
            mm = ellipse_mask(x, y, w, w * 0.8)
            lay.put(mm & ~lay.a, np.full((N, N), 0.95 - 0.55 * t / T))
        return lay

    halo = Layer(8)
    hm = ellipse_mask(cx, cy, r + 6, r + 6) & ~ellipse_mask(cx, cy, r, r)
    halo.put(hm, np.full((N, N), 0.7))
    c.paste(halo, line=False, ghost=True)
    c.paste(stream(False, math.pi / 2, 8, 7, 1))
    c.paste(stream(False, math.pi / 2, 7, 7, -1))
    m, nx, ny, nz, n = _sphere(cx, cy, r)
    a = math.radians(24)
    X = nx * math.cos(a) + ny * math.sin(a)
    Y = -nx * math.sin(a) + ny * math.cos(a)
    up = X ** 2 + (Y + 0.5) ** 2 < 0.25
    down = X ** 2 + (Y - 0.5) ** 2 < 0.25
    dark = ((X > 0) & ~down) | up
    dark = np.where(X ** 2 + (Y + 0.5) ** 2 < 0.045, False, dark)
    dark = np.where(X ** 2 + (Y - 0.5) ** 2 < 0.045, True, dark)
    v = shade(n, 0.85, spec=0.9, shin=60, amb=0.3) + 0.25 * (1 - nz) ** 3
    v = np.where(dark, v * 0.55 + 0.12, np.maximum(v, 0.5))
    pearl = Layer()
    pearl.put(m & dark, np.clip(v, 0, 1), mat=7)
    pearl.put(m & ~dark, np.clip(v, 0, 1), mat=6)
    c.paste(pearl)
    c.paste(stream(True, math.pi / 2, 8, 7, 1))
    c.paste(stream(True, math.pi / 2, 7, 7, -1))
    for (x, y) in ((16, 22), (112, 104), (22, 110), (108, 18)):
        c.dot(x, y, 0.8, 8)
    c.sparkle(112, 18, 5, mat=8)
    return c.image()


def _petal(base, tip, w, curl=0.0):
    """A petal of the cradle: a leaf from base to tip, curling outward near the tip."""
    bx, by = base
    tx, ty = tip
    L = math.hypot(tx - bx, ty - by)
    ux, uy = (tx - bx) / L, (ty - by) / L
    px, py = -uy, ux
    left, right = [], []
    for k in range(21):
        t = k / 20
        cxp = bx + ux * L * t + px * curl * L * t * t
        cyp = by + uy * L * t + py * curl * L * t * t
        ww = w * math.sin(math.pi * min(1, t * 1.05)) ** 0.7 * (1 - 0.55 * t)
        left.append((cxp - px * ww, cyp - py * ww))
        right.append((cxp + px * ww, cyp + py * ww))
    return left + right[::-1]


def _gold(pts, rib=None):
    lay = Layer(9)
    m = poly_mask(pts)
    b = np.array(Image.fromarray((m * 255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(max(1, 4 / SP)))) / 255.0
    gy, gx = np.gradient(b, SP)
    n = norm(np.stack([-gx * 14, -gy * 14, np.ones((N, N))], -1))
    lay.put(m, np.clip(shade(n, 0.85, spec=1.0, shin=20, amb=0.25), 0, 1))
    if rib is not None:
        # The rib down the petal: a bright ridge with a shadow beside it.
        r1 = line_mask(rib, 1) & m
        lay.v[np.roll(r1, 1, 1) & m & ~r1] *= 0.6
        lay.v[r1] = 0.97
    return lay


def ore5():
    """Cấp 6 · Hỗn Độn: nửa ảo nửa thật. A pearl of chaos held in a cradle of golden petals; on one
    side the petals and the pearl are solid, on the other they are there and not there, an echo
    of the pearl drifting off, and the petals breaking into motes."""
    c = Canvas()
    cx, cy, r = 62, 58, 22
    back = [((50, 80), (34, 30), 17, -0.18), ((74, 80), (92, 30), 17, 0.18), ((62, 78), (62, 24), 15, 0.0)]
    front = [((52, 96), (20, 60), 19, -0.22), ((76, 96), (106, 60), 19, 0.22), ((58, 100), (40, 78), 15, -0.15), ((70, 100), (86, 78), 15, 0.15)]
    real = lambda base: base[0] < 64
    rib = lambda b, t: [(b[0] + (t[0] - b[0]) * 0.15, b[1] + (t[1] - b[1]) * 0.15), (b[0] + (t[0] - b[0]) * 0.85, b[1] + (t[1] - b[1]) * 0.85)]
    for base, tip, w, curl in back:
        c.paste(_gold(_petal(base, tip, w, curl), rib(base, tip)), ghost=not real(tip))
    # The echo of the pearl, drifting off: there and not there.
    em, enx, eny, enz, en = _sphere(cx + 16, cy - 8, r * 0.9)
    echo = Layer(10)
    echo.put(em, np.clip(shade(en, 0.7, spec=0.8, shin=40, trans=0.5), 0, 1))
    c.paste(echo, ghost=True)
    m, nx, ny, nz, n = _sphere(cx, cy, r)
    v = shade(n, 0.82, spec=1.0, shin=50, trans=0.6, amb=0.3)
    # A swirl inside the pearl: chaos turning.
    rr = np.hypot(XX + 0.5 - cx, YY + 0.5 - cy)
    th = np.arctan2(YY + 0.5 - cy, XX + 0.5 - cx)
    sw = (np.cos(2 * (th - 1.4 * np.log(rr + 1))) > 0.6) & (rr < r * 0.75)
    v = np.where(sw, v * 0.7, v)
    pearl = Layer(10)
    pearl.put(m, np.clip(v, 0, 1))
    c.paste(pearl)
    for base, tip, w, curl in front:
        c.paste(_gold(_petal(base, tip, w, curl), rib(base, tip)), ghost=not real(tip))
    # The stem and the base of the cradle, solid: this much is real.
    c.paste(_gold([(52, 100), (72, 100), (70, 110), (80, 116), (44, 116), (54, 110)]))
    # The ghost side breaks into motes of gold and of the pearl.
    rnd = random.Random(7)
    for _ in range(14):
        x, y = rnd.randrange(84, 124), rnd.randrange(14, 100)
        if not c.a[int(y / SP), int(x / SP)]:
            s = rnd.choice((1, 1, 2))
            mote = Layer(rnd.choice((9, 10)))
            mote.put(poly_mask([(x - s, y - s), (x + s, y - s), (x + s, y + s), (x - s, y + s)]), 0.85)
            c.paste(mote, line=False, ghost=rnd.random() < 0.4)
    c.sparkle(18, 20, 6, mat=9)
    c.sparkle(40, 10, 3, mat=10)
    return c.image()


def linhthach():
    """Linh thạch: a cluster of chunky spirit crystals on a mound of earth, clear, lit within."""
    c = Canvas()
    back = Layer()
    prism(back, (42, 98), (20, 56), 13, 8, albedo=0.9, trans=0.6, phantom=False, glow=0.95)
    prism(back, (86, 98), (108, 58), 13, 8, albedo=0.9, trans=0.6, phantom=False, glow=0.95)
    c.paste(back)
    main = Layer()
    prism(main, (64, 102), (60, 22), 21, 12, albedo=0.92, trans=0.6, glow=1.0)
    c.paste(main)
    front = Layer()
    prism(front, (46, 108), (36, 82), 9, 6, albedo=0.9, trans=0.6, phantom=False)
    prism(front, (82, 108), (94, 84), 9, 6, albedo=0.9, trans=0.6, phantom=False)
    c.paste(front)
    mound = rock([(10, 110), (22, 100), (44, 98), (64, 102), (86, 98), (108, 100), (120, 110), (110, 120), (70, 124), (28, 122)],
                 albedo=0.3, seed=81, cells=12, tilt=0.35, cracks=0.3, dome=10)
    # Pebbles in the earth.
    for (x, y, s) in ((24, 112, 3), (50, 118, 2), (92, 116, 3), (108, 110, 2), (70, 120, 2)):
        pm = ellipse_mask(x, y, s + 0.6, s) & mound.a
        mound.v[pm] = 0.62
        mound.v[ring(pm) & mound.a] = 0.15
    c.paste(mound)
    c.sparkle(104, 20, 7)
    c.sparkle(18, 30, 5)
    for x, y in ((30, 14), (118, 44), (8, 76)):
        c.dot(x, y, 0.45)
    return c.image()


ORES = [ore0, ore1, ore2, ore3, ore4, ore5]


if __name__ == "__main__":
    out = sys.argv[1]
    ims = [f() for f in ORES] + [linhthach()]
    sheet = Image.new("RGB", (7 * 264, 400), (238, 238, 232))
    for i, im in enumerate(ims):
        sheet.paste(im.resize((256, 256), Image.NEAREST), (i * 264 + 4, 4), im.resize((256, 256), Image.NEAREST))
        small = im.resize((96, 96), Image.LANCZOS)
        sheet.paste(small, (i * 264 + 72, 272), small)
    sheet.save(out)
