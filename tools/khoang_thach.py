"""Khoáng thạch (3.1): six ores in pixel art, on a 48 grid ×4 (192 px), grayscale on transparency,
light from the upper left, every piece outlined in black, in the style of the linh thạch.

    0 Hoàng phẩm   a raw lump of ore, veins of linh khí showing through the cracks
    1 Huyền phẩm   a crystal breaking out of its rock
    2 Địa phẩm     a geode split open, crystals turned inward around a bright heart
    3 Thiên phẩm   a cut gem, crown and pavilion, glinting
    4 Âm Dương     an orb of yin and yang, ringed
    5 Hỗn Độn      a dark crystal of chaos in a burst of shards, motes orbiting

Used by tools/ve_do_hoa.py; run alone to preview: python tools/khoang_thach.py out.png
"""
import math
import sys

from PIL import Image, ImageDraw

N = 48
RIM = 22


def layer():
    return Image.new("L", (N, N), 0), Image.new("L", (N, N), 0)


def rim(g, a, v=RIM):
    """Black outline on every empty pixel touching the shape (4-neighbours)."""
    pts = []
    for y in range(N):
        for x in range(N):
            if a.getpixel((x, y)) == 0 and any(0 <= x + dx < N and 0 <= y + dy < N and a.getpixel((x + dx, y + dy))
                                               for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                pts.append((x, y))
    for p in pts:
        g.putpixel(p, v)
        a.putpixel(p, 255)


def paste(cg, ca, g, a):
    cg.paste(g, (0, 0), a)
    ca.paste(255, (0, 0), a)


def poly(g, a, pts, v):
    ImageDraw.Draw(g).polygon(pts, fill=v)
    ImageDraw.Draw(a).polygon(pts, fill=255)


def dot(cg, ca, x, y, v):
    if 0 <= x < N and 0 <= y < N:
        cg.putpixel((x, y), v)
        ca.putpixel((x, y), 255)


def glint(cg, ca, x, y, r=3):
    """A four-point glint of linh khí, fading out."""
    for i in range(-r, r + 1):
        v = 255 if i == 0 else max(30, 110 - 30 * abs(i))
        for px, py in ((x + i, y), (x, y + i)):
            if not ca.getpixel((px, py)):
                dot(cg, ca, px, py, v)


def motes(cg, ca, pts, v=120):
    for x, y in pts:
        if not ca.getpixel((x, y)):
            dot(cg, ca, x, y, v)


def crystal(base, tip, w, shades=(232, 170, 96), tips=(250, 205, 130), streak=True):
    """A hexagonal crystal: left face lit, front face, right face in shade; the point in three facets;
    the faces glow toward the point."""
    g, a = layer()
    d = ImageDraw.Draw(g)
    da = ImageDraw.Draw(a)
    bx, by = base
    tx, ty = tip
    L = math.hypot(tx - bx, ty - by)
    ux, uy = (tx - bx) / L, (ty - by) / L
    px, py = -uy, ux
    if px < 0:
        px, py = -px, -py
    h = 1.25 * w
    sx, sy = tx - ux * h, ty - uy * h
    offs = [-w, -0.38 * w, 0.38 * w, w]

    def at(cx, cy, o):
        return (cx + px * o, cy + py * o)

    for i in range(3):
        o1, o2 = offs[i], offs[i + 1]
        body = [at(bx, by, o1), at(sx, sy, o1), at(sx, sy, o2), at(bx, by, o2)]
        d.polygon(body, fill=shades[i]); da.polygon(body, fill=255)
        point = [at(sx, sy, o1), (tx, ty), at(sx, sy, o2)]
        d.polygon(point, fill=tips[i]); da.polygon(point, fill=255)
    for o, v in ((offs[1], 252), (offs[2], 70)):
        d.line([at(bx, by, o), at(sx, sy, o)], fill=v)
        d.line([at(sx, sy, o), (tx, ty)], fill=v)
    d.line([at(sx, sy, offs[0]), at(sx, sy, offs[2])], fill=245)
    d.line([at(sx, sy, offs[2]), at(sx, sy, offs[3])], fill=60)
    gp = g.load()
    for y in range(N):
        for x in range(N):
            v = gp[x, y]
            if a.getpixel((x, y)) and v < 250:
                f = ((x - bx) * ux + (y - by) * uy) / L
                gp[x, y] = max(0, min(250, v - int(34 * (1 - f)) + 8))
    if streak:
        o = offs[1] + 0.25 * w
        for t in range(int(L * 0.15), int(L * 0.55)):
            x, y = at(bx + ux * t, by + uy * t, o)
            if a.getpixel((int(x), int(y))):
                g.putpixel((int(x), int(y)), 255)
    rim(g, a)
    return g, a


def rock(shape, base=96, lit=160, dark=66, seed=7):
    """A lump of rock: lit along its top edge, darker below and to the right, flecked."""
    g, a = layer()
    poly(g, a, shape, base)
    gp = g.load()
    xs = [p[0] for p in shape]
    cx = (min(xs) + max(xs)) / 2
    bottom = max(p[1] for p in shape)
    for y in range(N):
        for x in range(N):
            if not a.getpixel((x, y)):
                continue
            if y == 0 or not a.getpixel((x, y - 1)):
                gp[x, y] = lit
            elif y >= bottom - 1 or (x + 1 < N and not a.getpixel((x + 1, y)) and x > cx):
                gp[x, y] = dark
            elif (x * 7 + y * 5 + seed) % 13 == 0:
                gp[x, y] = dark + 4
            elif (x * 3 + y * 11 + seed) % 17 == 0:
                gp[x, y] = base + 40
    rim(g, a)
    return g, a


def finish(cg, ca):
    return Image.merge("RGBA", (cg, cg, cg, ca)).resize((N * 4, N * 4), Image.NEAREST)


# --- The six ----------------------------------------------------------------------------------------

def ore0():
    """Hoàng phẩm: a raw lump of ore, facets roughly broken, veins of linh khí in the cracks."""
    cg, ca = layer()
    outer = [(9, 30), (13, 20), (21, 15), (31, 15), (39, 21), (42, 31), (37, 39), (24, 42), (12, 39)]
    g, a = layer()
    poly(g, a, outer, 120)
    # Facets: the top plane lit, the left lit less, the right and the bottom in shade.
    poly(g, a, [(13, 20), (21, 15), (31, 15), (34, 22), (22, 25)], 196)
    poly(g, a, [(9, 30), (13, 20), (22, 25), (20, 34), (12, 39)], 158)
    poly(g, a, [(34, 22), (39, 21), (42, 31), (37, 39), (30, 33)], 86)
    poly(g, a, [(20, 34), (30, 33), (37, 39), (24, 42), (12, 39)], 104)
    d = ImageDraw.Draw(g)
    for p, q, v in [((22, 25), (34, 22), 225), ((22, 25), (20, 34), 70), ((20, 34), (30, 33), 70), ((30, 33), (34, 22), 60)]:
        d.line([p, q], fill=v)
    # Veins of linh khí: bright cracks.
    for pts in ([(16, 24), (18, 27), (17, 30)], [(26, 18), (28, 20), (30, 19)], [(32, 28), (35, 31)], [(22, 37), (25, 38)]):
        d.line(pts, fill=250)
    gp = g.load()
    for x, y in [(15, 33), (27, 29), (36, 26), (19, 21)]:
        gp[x, y] = 240
    rim(g, a)
    paste(cg, ca, g, a)
    # Rubble at its foot.
    for shape in ([(5, 41), (8, 39), (10, 41), (8, 43)], [(40, 41), (43, 40), (44, 43), (41, 44)]):
        paste(cg, ca, *rock(shape, 110, 150, 80))
    glint(cg, ca, 35, 10, 3)
    motes(cg, ca, [(28, 8), (12, 12), (43, 15)])
    return finish(cg, ca)


def ore1():
    """Huyền phẩm: one crystal breaking out of its rock, a small one beside."""
    cg, ca = layer()
    paste(cg, ca, *crystal((27, 37), (31, 5), 5.6))
    paste(cg, ca, *crystal((18, 38), (12, 24), 3.0, streak=False))
    paste(cg, ca, *rock([(7, 40), (11, 34), (18, 33), (24, 35), (31, 32), (37, 34), (41, 39), (39, 44), (10, 44)]))
    glint(cg, ca, 39, 7, 3)
    motes(cg, ca, [(20, 9), (42, 20), (6, 22), (24, 14)])
    return finish(cg, ca)


def ore2():
    """Địa phẩm: a geode split open: a shell of dark rock, a band of agate, crystals turned inward
    around a bright heart."""
    cg, ca = layer()
    cx, cy, R = 24, 25, 19
    g, a = layer()
    gp = g.load()
    for y in range(N):
        for x in range(N):
            # A slightly squashed, lumpy round.
            ang = math.atan2(y - cy, x - cx)
            r = math.hypot((x - cx) * 0.95, (y - cy) * 1.08)
            edge = R + 0.7 * math.sin(5 * ang) + 0.5 * math.cos(9 * ang)
            if r <= edge:
                a.putpixel((x, y), 255)
                light = -math.cos(ang + math.pi / 4)        # lit from the upper left
                if r > edge - 4:
                    v = 110 + int(40 * light)                  # the shell
                    if (x * 5 + y * 3) % 11 == 0:
                        v -= 30
                elif r > edge - 6:
                    v = 205 + int(25 * light)                  # the band of agate
                elif r > edge - 7:
                    v = 120
                else:
                    v = 70                                     # the hollow
                gp[x, y] = max(0, min(255, v))
    # Crystals around the hollow, pointing to the heart.
    inner = R - 8
    for k in range(14):
        ang = 2 * math.pi * k / 14 + 0.2
        bx, by = cx + inner * math.cos(ang), cy + inner * math.sin(ang)
        tx, ty = cx + (inner - 6) * math.cos(ang), cy + (inner - 6) * math.sin(ang)
        px, py = -math.sin(ang) * 2.2, math.cos(ang) * 2.2
        lit = math.cos(ang - 3 * math.pi / 4) > 0
        ImageDraw.Draw(g).polygon([(bx + px, by + py), (tx, ty), (bx - px, by - py)], fill=230 if lit else 150)
        ImageDraw.Draw(g).line([(bx, by), (tx, ty)], fill=250 if lit else 95)
    # The heart of linh khí.
    for y in range(N):
        for x in range(N):
            r = math.hypot(x - cx, y - cy)
            if r <= 2.5:
                gp[x, y] = 255
            elif r <= 4 and gp[x, y] < 100:
                gp[x, y] = 175
    rim(g, a)
    paste(cg, ca, g, a)
    glint(cg, ca, 41, 6, 3)
    motes(cg, ca, [(6, 6), (44, 30), (3, 38), (30, 2)])
    return finish(cg, ca)


def ore3():
    """Thiên phẩm: a cut gem, crown and pavilion, facets catching the light."""
    cg, ca = layer()
    g, a = layer()
    # Girdle at y 17, from x 5 to 43; the table above, the culet below.
    tl, tr, ty = 15, 33, 8
    gl, gr, gy = 5, 43, 18
    cul = (24, 43)
    crown = [(tl, ty), (tr, ty), (gr, gy), (gl, gy)]
    poly(g, a, crown, 200)
    # Crown facets: table bright, left lit, right shaded.
    poly(g, a, [(tl, ty), (tr, ty), (29, gy), (19, gy)], 238)
    poly(g, a, [(gl, gy), (tl, ty), (19, gy)], 212)
    poly(g, a, [(tr, ty), (gr, gy), (29, gy)], 130)
    # Pavilion facets to the culet.
    poly(g, a, [(gl, gy), (14, gy), cul], 175)
    poly(g, a, [(14, gy), (24, gy), cul], 215)
    poly(g, a, [(24, gy), (34, gy), cul], 140)
    poly(g, a, [(34, gy), (gr, gy), cul], 88)
    d = ImageDraw.Draw(g)
    d.line([(gl, gy), (gr, gy)], fill=250)
    for x0, v in ((14, 245), (24, 120), (34, 70)):
        d.line([(x0, gy), cul], fill=v)
    d.line([(19, gy), (tl, ty)], fill=252)
    d.line([(29, gy), (tr, ty)], fill=110)
    # A streak across the table.
    for i in range(5):
        g.putpixel((19 + i, 13 - i // 2), 255)
    rim(g, a)
    paste(cg, ca, g, a)
    # Rays of light around it.
    for ang in range(0, 360, 45):
        t = math.radians(ang)
        for r in (22, 23):
            x, y = int(24 + r * math.cos(t)), int(24 + r * math.sin(t))
            motes(cg, ca, [(x, y)], 150 if r == 22 else 90)
    glint(cg, ca, 40, 5, 3)
    glint(cg, ca, 9, 29, 2)
    return finish(cg, ca)


def ore4():
    """Âm Dương phẩm: an orb, yin and yang turning in it, a ring of light around."""
    cg, ca = layer()
    cx, cy, R = 24, 24, 15
    g, a = layer()
    gp = g.load()
    for y in range(N):
        for x in range(N):
            dx, dy = x - cx + 0.5, y - cy + 0.5
            r = math.hypot(dx, dy)
            if r > R:
                continue
            a.putpixel((x, y), 255)
            # The S of yin and yang: the upper half-disc belongs to the left side's colour.
            yang = dx < 0
            if math.hypot(dx, dy + R / 2) < R / 2:
                yang = True
            if math.hypot(dx, dy - R / 2) < R / 2:
                yang = False
            # Sphere shading, lit from the upper left.
            nz = math.sqrt(max(0.0, 1 - (r / R) ** 2))
            lum = 0.55 + 0.45 * (-dx * 0.55 - dy * 0.55 + nz * 0.6) / R * R / max(R, 1)
            base = 235 if yang else 60
            v = int(base * (0.75 + 0.35 * lum)) if yang else int(base + 50 * max(0.0, lum - 0.4))
            gp[x, y] = max(0, min(255, v))
    # The eyes.
    for ex, ey, v in ((cx, cy - R / 2, 50), (cx, cy + R / 2, 235)):
        for y in range(N):
            for x in range(N):
                if math.hypot(x - ex + 0.5, y - ey + 0.5) <= 2.4:
                    gp[x, y] = v
    # A highlight on the glass.
    for x, y in ((17, 15), (18, 14), (19, 14), (16, 16), (17, 16)):
        gp[x, y] = 255
    rim(g, a)
    # The ring: an ellipse around the orb, behind at the top, in front at the bottom.
    ring_g, ring_a = layer()
    rp = ring_g.load()
    for k in range(720):
        t = 2 * math.pi * k / 720
        x, y = cx + 22 * math.cos(t), cy + 6 * math.sin(t) + 0.3 * 22 * math.cos(t) * 0.3
        xi, yi = int(round(x)), int(round(y))
        if 0 <= xi < N and 0 <= yi < N:
            front = math.sin(t) > 0
            if front or not a.getpixel((xi, yi)):
                rp[xi, yi] = 230 if math.cos(t) < 0 else 160
                ring_a.putpixel((xi, yi), 255)
    paste(cg, ca, g, a)
    rim(ring_g, ring_a, 40)
    # Draw the ring's back part under the orb, the front over it.
    cg2, ca2 = cg.copy(), ca.copy()
    paste(cg2, ca2, ring_g, ring_a)
    for y in range(N):
        for x in range(N):
            if ring_a.getpixel((x, y)) and (y >= cy or not a.getpixel((x, y))):
                cg.putpixel((x, y), cg2.getpixel((x, y)))
                ca.putpixel((x, y), 255)
    glint(cg, ca, 41, 8, 3)
    motes(cg, ca, [(7, 8), (44, 40), (4, 36), (30, 3)])
    return finish(cg, ca)


def ore5():
    """Hỗn Độn phẩm: a dark crystal of chaos, a swirl in it, a burst of shards behind, motes orbiting."""
    cg, ca = layer()
    cx, cy = 24, 24
    # The burst: eight shards behind.
    for k in range(8):
        t = math.radians(22.5 + 45 * k)
        tip = (cx + 22 * math.cos(t), cy + 22 * math.sin(t))
        side = 4.5
        pts = [(cx + 9 * math.cos(t) - side * math.sin(t), cy + 9 * math.sin(t) + side * math.cos(t)), tip,
               (cx + 9 * math.cos(t) + side * math.sin(t), cy + 9 * math.sin(t) - side * math.cos(t))]
        g, a = layer()
        poly(g, a, pts, 190 if math.cos(t - 3 * math.pi / 4) > 0 else 120)
        rim(g, a)
        paste(cg, ca, g, a)
    # The crystal: an octahedron seen from the front, four facets.
    g, a = layer()
    top, bot, left, right, mid = (cx, 5), (cx, 43), (cx - 12, cy), (cx + 12, cy), (cx + 2, cy + 1)
    poly(g, a, [top, left, mid], 92)
    poly(g, a, [top, mid, right], 54)
    poly(g, a, [left, bot, mid], 64)
    poly(g, a, [mid, bot, right], 36)
    d = ImageDraw.Draw(g)
    d.line([top, mid], fill=150)
    d.line([left, mid], fill=120)
    d.line([top, left], fill=200)
    # The swirl of chaos inside.
    gp = g.load()
    for k in range(260):
        t = k / 260 * 4 * math.pi
        r = 1 + 7.5 * k / 260
        x, y = int(round(cx + 1 + r * math.cos(t))), int(round(cy + r * math.sin(t) * 1.4))
        if 0 <= x < N and 0 <= y < N and a.getpixel((x, y)):
            gp[x, y] = 235 if k > 40 else 255
    rim(g, a)
    paste(cg, ca, g, a)
    # Motes orbiting.
    for k in range(10):
        t = 2 * math.pi * k / 10 + 0.3
        x, y = int(cx + 21 * math.cos(t)), int(cy + 9 * math.sin(t))
        if not ca.getpixel((x, y)):
            dot(cg, ca, x, y, 230 if k % 3 == 0 else 140)
    glint(cg, ca, 39, 6, 3)
    return finish(cg, ca)


ORES = [ore0, ore1, ore2, ore3, ore4, ore5]


if __name__ == "__main__":
    out = sys.argv[1]
    ims = [f() for f in ORES]
    sheet = Image.new("RGB", (6 * 200, 290), "white")
    for i, im in enumerate(ims):
        sheet.paste(im, (i * 200 + 4, 4), im)
        small = im.resize((64, 64), Image.LANCZOS)
        sheet.paste(small, (i * 200 + 68, 212), small)
    sheet.save(out)
