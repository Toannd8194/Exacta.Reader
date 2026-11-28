"""Khoáng thạch (3.2): six ores in pixel art on a 128 grid (128 px, about 1:1 on the device), for the 300 ppi e-ink screen.
Grayscale on transparency, sixteen tones with a light ordered dither. Each facet carries a normal and
takes the light from the upper left: diffuse, a specular glint, and for the stones that let light
through, a glow on the side away from it. A 2-pixel outline round the whole, 1 pixel between parts.

    0 Hoàng phẩm   no more than a stone: worn rock, a pale vein of quartz, no shine
    1 Huyền phẩm   a stone split open: banded agate round a hollow of small crystals
    2 Địa phẩm     seven clear crystals growing from a dark rock
    3 Thiên phẩm   a stone of the sky: a cratered meteor, its tail of fire streaming back
    4 Âm Dương     a world, day and night parted by the Thái Cực, a ring of stones round it
    5 Hỗn Độn      hư vô mà thật chất: a whole universe in a hard bright shell, a galaxy turning in the
                   void; at one side the shell breaks and drifts away
   linh thạch     chunky spirit crystals on a mound of earth, clear, lit within

Used by tools/ve_do_hoa.py (--khoangthach); run alone to preview: python tools/khoang_thach.py out.png
"""
import math
import random
import sys

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

N = 128
SCALE = 1
OUT = 0.07                                        # outline
LIGHT = np.array([-0.5, -0.72, 0.85])
LIGHT = LIGHT / np.linalg.norm(LIGHT)
YY, XX = np.mgrid[0:N, 0:N].astype(float)
BAYER = (np.array([[0, 8, 2, 10], [12, 4, 14, 6], [3, 11, 1, 9], [15, 7, 13, 5]]) / 16.0 - 0.47)


def norm(v):
    v = np.asarray(v, dtype=float)
    return v / np.linalg.norm(v, axis=-1, keepdims=True)


def poly_mask(pts):
    m = Image.new("L", (N, N), 0)
    ImageDraw.Draw(m).polygon([tuple(p) for p in pts], fill=255)
    return np.array(m) > 0


def ellipse_mask(cx, cy, rx, ry):
    return ((XX + 0.5 - cx) / rx) ** 2 + ((YY + 0.5 - cy) / ry) ** 2 <= 1


def line_mask(pts, width=1):
    m = Image.new("L", (N, N), 0)
    ImageDraw.Draw(m).line([tuple(p) for p in pts], fill=255, width=width)
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
    def __init__(self):
        self.v = np.zeros((N, N))
        self.a = np.zeros((N, N), dtype=bool)

    def put(self, mask, value):
        value = np.broadcast_to(value, (N, N)) if np.ndim(value) == 0 else value
        self.v[mask] = value[mask]
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

    def paste(self, layer, line=True):
        """Back to front: a 1-pixel line round each part keeps it apart from what lies behind."""
        if line:
            r = ring(layer.a)
            self.v[r] = OUT
            self.a |= r
        self.v[layer.a] = layer.v[layer.a]
        self.a |= layer.a

    def dot(self, x, y, v):
        if 0 <= x < N and 0 <= y < N:
            self.v[y, x] = v
            self.a[y, x] = True

    def sparkle(self, x, y, r=6):
        """A four-point star beside the stone, gray with a dark edge so it shows on paper."""
        pts = [(x, y - r), (x + 1.4, y - 1.4), (x + r, y), (x + 1.4, y + 1.4), (x, y + r), (x - 1.4, y + 1.4), (x - r, y), (x - 1.4, y - 1.4)]
        m = poly_mask(pts) & ~self.a
        rr = ring(m) & ~self.a
        self.v[rr] = OUT
        self.v[m] = 0.62
        self.a |= m | rr
        self.dot(x, y, 1.0)

    def image(self):
        # The silhouette's outer line: a second pixel.
        r = ring(self.a)
        v = self.v.copy()
        v[r] = OUT
        a = self.a | r
        q = np.floor(np.clip(v, 0, 1) * 15 + BAYER[YY.astype(int) % 4, XX.astype(int) % 4] * 0.9 + 0.5) / 15
        q[v <= OUT + 1e-6] = OUT
        g = Image.fromarray((np.clip(q, 0, 1) * 255).astype(np.uint8), "L")
        al = Image.fromarray((a * 255).astype(np.uint8), "L")
        return Image.merge("RGBA", (g, g, g, al)).resize((N * SCALE, N * SCALE), Image.NEAREST)


# --- Parts ------------------------------------------------------------------------------------------

def rock(pts, albedo=0.55, seed=1, cells=14, tilt=0.45, cracks=0.5, dome=22.0):
    """A lump of rock: domed, broken into flat facets (Voronoi cells), grained, cracked along some
    facet edges."""
    lay = Layer()
    m = poly_mask(pts)
    blur = np.array(Image.fromarray((m * 255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(9))) / 255.0
    gy, gx = np.gradient(blur)
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

def ore0():
    """Hoàng phẩm: no more than a stone, a little linh khí sleeping in it: a worn lump of rock, grained,
    a pale vein of quartz across it, no shine."""
    c = Canvas()
    pts = [(16, 72), (20, 52), (34, 34), (56, 24), (80, 26), (100, 38), (112, 58), (110, 82), (96, 100), (72, 108), (46, 106), (26, 94)]
    lay = rock(pts, albedo=0.66, seed=5, cells=7, tilt=0.22, cracks=0.25, dome=40)
    # A pale vein of quartz, soft at its edges: the one sign of what sleeps inside.
    vein = line_mask([(22, 80), (40, 70), (60, 74), (78, 62), (104, 58)], 5) & lay.a
    soft = ring(vein) & lay.a
    lay.v[vein] = np.minimum(1, lay.v[vein] + 0.16)
    lay.v[soft] = np.minimum(1, lay.v[soft] + 0.07)
    # Specks of dark mineral in the grain.
    r = random.Random(3)
    for _ in range(40):
        x, y = r.randrange(20, 110), r.randrange(28, 106)
        if lay.a[y, x]:
            lay.v[y, x] *= 0.6
    c.paste(lay)
    return c.image()


def ore1():
    """Huyền phẩm: a stone split open: banded agate round a hollow lined with small crystals, the
    first light of linh khí."""
    c = Canvas()
    pts = [(14, 66), (22, 40), (42, 22), (68, 16), (94, 24), (112, 46), (116, 74), (104, 98), (80, 112), (50, 112), (26, 98)]
    lay = rock(pts, albedo=0.5, seed=8, cells=10, tilt=0.4, cracks=0.4, dome=30)
    # The cut face: flat, polished, banded like agate.
    cx, cy = 62, 62
    face = ellipse_mask(cx, cy, 42, 36) & lay.a
    d = np.sqrt(((XX + 0.5 - cx) / 42) ** 2 + ((YY + 0.5 - cy) / 36) ** 2)
    band = np.where((np.floor(d * 22) % 2) == 0, 0.8, 0.66) - 0.15 * (d > 0.93)
    lay.put(face, band + 0.06 * noise(31, 4, 2))
    # The hollow: dark at its heart, lined with crystal points turned inward.
    hollow = ellipse_mask(cx, cy, 30, 25)
    hv = 0.18 + 0.2 * np.clip(1 - np.sqrt(((XX + 0.5 - cx) / 30) ** 2 + ((YY + 0.5 - cy) / 25) ** 2), 0, 1)
    lay.put(hollow, hv)
    r = random.Random(17)
    for k in range(46):
        t = r.uniform(0, 2 * math.pi)
        rr = r.uniform(0.55, 1.0)
        bx, by = cx + 30 * rr * math.cos(t), cy + 25 * rr * math.sin(t)
        ln = r.uniform(5, 10) * (1.2 - rr * 0.4)
        tx, ty = bx - ln * math.cos(t), by - ln * math.sin(t)
        w = r.uniform(2.2, 3.6)
        px, py = -math.sin(t) * w, math.cos(t) * w
        lit = [(bx + px, by + py), (tx, ty), (bx, by)]
        dark = [(bx, by), (tx, ty), (bx - px, by - py)]
        m1 = poly_mask(lit) & hollow
        m2 = poly_mask(dark) & hollow
        # The facet that faces the light glints; the other stays in the hollow's shadow.
        f = 0.5 + 0.5 * (-(math.cos(t + 0.9)))
        lay.v[m1] = 0.55 + 0.42 * f
        lay.v[m2] = 0.3 + 0.25 * f
        lay.v[ring(m1 | m2) & hollow & ~(m1 | m2)] *= 0.7
    lay.v[ring(hollow) & lay.a] = 0.22
    c.paste(lay)
    c.sparkle(52, 50, 5)
    return c.image()


def ore2():
    """Địa phẩm: the earth gives up its crystals: seven of them, clear, growing from a dark rock."""
    c = Canvas()
    back = Layer()
    prism(back, (40, 98), (14, 50), 9, 12)
    prism(back, (90, 98), (116, 46), 9, 12)
    c.paste(back)
    mid = Layer()
    prism(mid, (52, 100), (42, 28), 12, 16)
    prism(mid, (78, 100), (90, 32), 11, 15)
    c.paste(mid)
    front = Layer()
    prism(front, (64, 104), (63, 6), 16, 22, albedo=0.86, glow=1.0)
    c.paste(front)
    small = Layer()
    prism(small, (34, 108), (22, 84), 6, 8, phantom=False)
    prism(small, (96, 108), (110, 86), 6, 8, phantom=False)
    c.paste(small)
    base = rock([(12, 108), (24, 96), (46, 94), (64, 98), (84, 94), (106, 96), (118, 106), (110, 118), (76, 122), (36, 122), (16, 118)],
                albedo=0.38, seed=7, cells=10, tilt=0.4, dome=14)
    c.paste(base)
    c.sparkle(108, 18, 6)
    c.sparkle(20, 30, 4)
    return c.image()


def ore3():
    """Thiên phẩm: a stone of the sky: a meteor falling, cratered, its tail of fire streaming back."""
    c = Canvas()
    cx, cy, r = 46, 84, 24
    d = norm([1.0, -1.0])                          # the tail streams to the upper right
    px, py = -d[1], d[0]

    # The fire: three layers along the tail, wide at the stone, licking out into tongues.
    t = ((XX + 0.5 - cx) * d[0] + (YY + 0.5 - cy) * d[1]) / 96
    lat = ((XX + 0.5 - cx) * px + (YY + 0.5 - cy) * py)
    jag = np.clip(1 + (0.42 * np.sin(t * 26 + lat * 0.3) + 0.5 * noise(52, 4, 2)) * np.clip(t, 0, 1) * 1.4, 0.55, 1.6)
    width = r * 1.3 * np.clip(1 - t, 0, 1) ** 0.45 * jag
    width = np.where(t < 0, r * 1.05, width)
    ahead = t > -0.12
    for k, (frac, until, v) in enumerate(((1.0, 1.0, 0.5), (0.66, 0.78, 0.78), (0.38, 0.5, 0.98))):
        lay = Layer()
        lay.put((np.abs(lat) < width * frac) & (t < until) & ahead, v)
        c.paste(lay, line=(k == 0))
    # Sparks thrown off the tail.
    for (x, y, s, v) in ((114, 12, 3, 0.75), (122, 30, 2, 0.52), (100, 6, 2, 0.97), (124, 48, 2, 0.75), (90, 14, 2, 0.52)):
        sp = Layer()
        sp.put(poly_mask([(x - s, y - s), (x + s, y - s), (x + s, y + s), (x - s, y + s)]), v)
        c.paste(sp)
    # The stone: a sphere, cratered, hot on the side that meets the air.
    st = Layer()
    sm = ellipse_mask(cx, cy, r, r)
    nx, ny = (XX + 0.5 - cx) / r, (YY + 0.5 - cy) / r
    nz = np.sqrt(np.clip(1 - nx ** 2 - ny ** 2, 0, 1))
    n = norm(np.stack([nx, ny, nz + 1e-3], -1))
    alb = 0.55 * (1 + 0.25 * noise(51, 5, 3))
    for (ax, ay, ar) in ((-0.3, 0.1, 0.22), (0.25, 0.35, 0.16), (-0.05, -0.4, 0.13), (0.4, -0.1, 0.1), (-0.45, 0.5, 0.1)):
        dd = np.hypot(nx - ax, ny - ay)
        alb = np.where(dd < ar, alb * 0.62, alb)
        rim = (dd >= ar) & (dd < ar + 0.06) & ((nx - ax) + (ny - ay) > 0)
        alb = np.where(rim, alb * 1.35, alb)
    v = shade(n, alb, spec=0.15, shin=10)
    heat = np.clip((nx * d[0] + ny * d[1]), 0, 1) ** 2
    v = np.clip(v + 0.45 * heat, 0, 1)
    st.put(sm, v)
    c.paste(st)
    return c.image()


def ore4():
    """Âm Dương: a world: day and night parted by the Thái Cực, a ring of stones round it."""
    c = Canvas()
    cx, cy, r = 64, 62, 38
    tilt = math.radians(-18)
    RX, RY = 60, 15

    def ring_layer(front):
        lay = Layer()
        m = np.zeros((N, N), dtype=bool)
        band = np.zeros((N, N))
        for k, (rx, ry, v) in enumerate(((RX, RY, 0.62), (RX - 5, RY - 1.2, 0.82), (RX - 10, RY - 2.5, 0.5))):
            pts = [(cx + rx * math.cos(t) * math.cos(tilt) - ry * math.sin(t) * math.sin(tilt),
                    cy + rx * math.cos(t) * math.sin(tilt) + ry * math.sin(t) * math.cos(tilt)) for t in np.linspace(0, 2 * math.pi, 241)]
            seg = [p for t, p in zip(np.linspace(0, 2 * math.pi, 241), pts) if (math.sin(t) > 0) == front]
            mm = line_mask(seg, 3)
            band[mm & ~m] = v
            m |= mm
        lay.put(m, band)
        return lay

    c.paste(ring_layer(False))
    w = Layer()
    wm = ellipse_mask(cx, cy, r, r)
    nx, ny = (XX + 0.5 - cx) / r, (YY + 0.5 - cy) / r
    nz = np.sqrt(np.clip(1 - nx ** 2 - ny ** 2, 0, 1))
    n = norm(np.stack([nx, ny, nz + 1e-3], -1))
    a = math.radians(28)
    X = nx * math.cos(a) + ny * math.sin(a)
    Y = -nx * math.sin(a) + ny * math.cos(a)
    up = X ** 2 + (Y + 0.5) ** 2 < 0.25
    down = X ** 2 + (Y - 0.5) ** 2 < 0.25
    night = ((X > 0) & ~down) | up
    # Day: seas, lands, bands of cloud. Night: dark, lit with the lamps of cities.
    land = noise(61, 4, 3) > 0.04
    alb = np.where(land, 0.62, 0.8) + 0.1 * np.sin(ny * 16 + 3 * noise(62, 6, 2))
    day = shade(n, alb, spec=0.5, shin=30, amb=0.35)
    rnd = np.random.default_rng(63)
    lamps = (rnd.random((N, N)) < 0.05) & land
    nv = np.where(lamps, 0.62, 0.1 + 0.08 * (1 - nz))
    v = np.where(night, nv, day)
    # The eyes: a dark storm in the day, a bright moonlit sea in the night.
    v = np.where(X ** 2 + (Y - 0.5) ** 2 < 0.02, 0.86, v)
    v = np.where(X ** 2 + (Y + 0.5) ** 2 < 0.02, 0.2, v)
    v = v + 0.2 * (1 - nz) ** 4                    # the air glows at the rim
    w.put(wm, np.clip(v, 0, 1))
    c.paste(w)
    c.paste(ring_layer(True))
    for x, y in ((14, 16), (110, 108), (20, 106), (104, 10)):
        c.dot(x, y, OUT)
    c.sparkle(114, 22, 5)
    return c.image()


def ore5():
    """Hỗn Độn: hư vô mà thật chất. A whole universe held in a shell: a galaxy turning in the void,
    stars, far galaxies; the shell is hard and bright, and at one side it breaks and drifts away."""
    c = Canvas()
    cx, cy, r = 62, 60, 48
    lay = Layer()
    m = ellipse_mask(cx, cy, r, r)
    dx, dy = XX + 0.5 - cx, YY + 0.5 - cy
    # The galaxy, tilted: an ellipse of arms round a bright heart.
    a = math.radians(-28)
    gx = dx * math.cos(a) + dy * math.sin(a)
    gy = (-dx * math.sin(a) + dy * math.cos(a)) / 0.5
    rr = np.hypot(gx, gy)
    th = np.arctan2(gy, gx)
    arm = (np.cos(2 * (th - 1.7 * np.log(rr + 1.5))) + 1) / 2
    gal = arm ** 2.5 * np.exp(-rr / 22) * (0.7 + 0.6 * noise(71, 6, 3))
    v = 0.03 + 0.95 * gal + 1.0 * np.exp(-(rr / 5) ** 2)
    rnd = random.Random(72)
    for _ in range(90):
        x, y = rnd.randrange(16, 110), rnd.randrange(12, 108)
        if m[y, x]:
            v[y, x] = max(v[y, x], rnd.choice((0.45, 0.6, 0.8, 1.0)))
    for (x, y) in ((36, 34), (88, 86), (30, 80)):
        for ddx in (-2, -1, 0, 1, 2):
            v[y, x + ddx] = max(v[y, x + ddx], 0.7 - 0.15 * abs(ddx))
        v[y - 1, x] = v[y + 1, x] = 0.6
    # The shell: hard and bright at its edge on the lit side, a window of light, dark below.
    dist = np.hypot(dx, dy) / r
    nz = np.sqrt(np.clip(1 - dist ** 2, 0, 1))
    lit = -(dx * 0.6 + dy * 0.8) / r
    shell = (dist > 0.92) & m
    v = np.where(shell, np.where(lit > 0.2, 0.92, np.where(lit > -0.3, 0.6, 0.32)), v)
    v = v + 0.18 * (1 - nz) ** 3 * (lit > 0)
    win = ellipse_mask(cx - 22, cy - 26, 11, 6) & ~ellipse_mask(cx - 17, cy - 22, 9, 5) & m
    v = np.where(win, 0.97, v)
    lay.put(m, np.clip(v, 0, 1))
    # At its lower right the shell breaks: the edge is gone there, and the pieces drift away.
    gone = m & (dist > 0.78) & ((dx + dy) + 20 * noise(73, 5, 2) > 50)
    lay.a &= ~gone
    c.paste(lay)
    for (x, y, s) in ((104, 96, 6), (114, 108, 4), (96, 114, 4), (122, 92, 3), (110, 122, 2)):
        frag = Layer()
        fm = poly_mask([(x - s, y - s + 1), (x + s, y - s), (x + s - 1, y + s), (x - s, y + s - 1)])
        frag.put(fm, 0.06)
        frag.stroke([(x - s, y - s + 1), (x + s, y - s)], 0.9)
        frag.v[y, x] = 0.8
        c.paste(frag)
    for x, y in ((8, 20), (120, 14), (14, 112), (124, 62), (4, 64)):
        c.dot(x, y, OUT)
    c.sparkle(112, 22, 6)
    c.sparkle(10, 40, 4)
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
