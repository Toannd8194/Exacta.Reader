"""Vẽ lại đồ họa của Vạn Quyển Tiên Đồ (2.1).

    python tools/ve_do_hoa.py            # ghi đè vào app/src/main/res/drawable-nodpi
    python tools/ve_do_hoa.py --preview  # chỉ vẽ bảng xem thử vào build/do_hoa.png

Vẽ ba nhóm:
  - art_ore_0..5: sáu khoáng thạch, từ hòn đá thô (Hoàng) tới tinh cầu hỗn độn (Hỗn Độn);
  - realm_0..9: mười biểu tượng cảnh giới, hình đen trên nền trong;
  - badge_48..59: khung Âm Dương phẩm của pháp bảo (giữ hình pháp bảo bên trong, nét pixel).
Tất cả là đồ họa vẽ bằng hình học, xám trên nền trong, hợp màn e-ink.
"""
import math
import os
import sys

from PIL import Image, ImageChops, ImageDraw

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(ROOT, "app", "src", "main", "res", "drawable-nodpi")

BLACK, DARK, MID, LIGHT, WHITE = 0, 110, 170, 205, 255


# --- Helpers ---------------------------------------------------------------------------------------

class Pen:
    """Draws grayscale shapes on an L canvas with an alpha mask, at 4× for smooth edges."""

    def __init__(self, size, scale=4):
        self.s = scale
        self.n = size * scale
        self.g = Image.new("L", (self.n, self.n), 255)
        self.a = Image.new("L", (self.n, self.n), 0)
        self.dg = ImageDraw.Draw(self.g)
        self.da = ImageDraw.Draw(self.a)

    def p(self, pts):
        return [(x * self.s, y * self.s) for x, y in pts]

    def poly(self, pts, fill, outline=None, width=0):
        q = self.p(pts)
        self.dg.polygon(q, fill=fill)
        self.da.polygon(q, fill=255)
        if outline is not None and width:
            self.line(pts + [pts[0]], outline, width)

    def line(self, pts, fill, width, joint=True):
        q = self.p(pts)
        w = int(width * self.s)
        self.dg.line(q, fill=fill, width=w, joint="curve" if joint else None)
        self.da.line(q, fill=255, width=w, joint="curve" if joint else None)
        r = w / 2
        for x, y in q:
            self.dg.ellipse([x - r, y - r, x + r, y + r], fill=fill)
            self.da.ellipse([x - r, y - r, x + r, y + r], fill=255)

    def circle(self, cx, cy, r, fill, outline=None, width=0):
        s = self.s
        box = [(cx - r) * s, (cy - r) * s, (cx + r) * s, (cy + r) * s]
        if outline is not None and width:
            self.dg.ellipse(box, fill=outline)
            self.da.ellipse(box, fill=255)
            r2 = r - width
            box = [(cx - r2) * s, (cy - r2) * s, (cx + r2) * s, (cy + r2) * s]
        self.dg.ellipse(box, fill=fill)
        self.da.ellipse(box, fill=255)

    def ring(self, cx, cy, r, w, fill):
        s = self.s
        self.dg.ellipse([(cx - r) * s, (cy - r) * s, (cx + r) * s, (cy + r) * s], outline=fill, width=int(w * s))
        self.da.ellipse([(cx - r) * s, (cy - r) * s, (cx + r) * s, (cy + r) * s], outline=255, width=int(w * s))

    def cut(self, pts=None, circle=None):
        """Clears the alpha (a hole through the drawing)."""
        if pts is not None:
            self.da.polygon(self.p(pts), fill=0)
        if circle is not None:
            cx, cy, r = circle
            s = self.s
            self.da.ellipse([(cx - r) * s, (cy - r) * s, (cx + r) * s, (cy + r) * s], fill=0)

    def image(self, size):
        rgba = Image.merge("RGBA", (self.g, self.g, self.g, self.a))
        return rgba.resize((size, size), Image.LANCZOS)


def star(cx, cy, r1, r2, n, rot=-90):
    pts = []
    for i in range(2 * n):
        r = r1 if i % 2 == 0 else r2
        t = math.radians(rot + i * 180 / n)
        pts.append((cx + r * math.cos(t), cy + r * math.sin(t)))
    return pts


def arcpts(cx, cy, r, a0, a1, steps=40):
    return [(cx + r * math.cos(math.radians(a0 + (a1 - a0) * i / steps)),
             cy + r * math.sin(math.radians(a0 + (a1 - a0) * i / steps))) for i in range(steps + 1)]


def petal(cx, cy, length, width, angle):
    """A leaf from (cx, cy) towards `angle` (degrees)."""
    t = math.radians(angle)
    ux, uy = math.cos(t), math.sin(t)
    vx, vy = -uy, ux
    pts = []
    for i in range(25):
        s = i / 24
        w = width * math.sin(math.pi * s) * (1 - 0.25 * s)
        pts.append((cx + ux * length * s + vx * w, cy + uy * length * s + vy * w))
    for i in range(24, -1, -1):
        s = i / 24
        w = width * math.sin(math.pi * s) * (1 - 0.25 * s)
        pts.append((cx + ux * length * s - vx * w, cy + uy * length * s - vy * w))
    return pts


def sparkle(pen, cx, cy, r, fill=BLACK):
    pen.poly(star(cx, cy, r, r * 0.28, 4), fill)


# --- Six ores (160 × 160) --------------------------------------------------------------------------

OL = 5  # outline width


def ore0():
    """Hoàng phẩm: a rough stone, three faces and a crack."""
    p = Pen(160)
    p.poly([(28, 126), (132, 126), (124, 136), (36, 136)], DARK)
    body = [(34, 100), (44, 70), (70, 52), (104, 54), (126, 76), (124, 106), (96, 122), (52, 120)]
    p.poly(body, MID, BLACK, OL)
    # Lit top, dark right, a mid front.
    p.poly([(44, 70), (70, 52), (104, 54), (90, 76), (56, 80)], LIGHT)
    p.poly([(104, 54), (126, 76), (124, 106), (100, 100), (90, 76)], DARK)
    p.line([(56, 80), (90, 76), (104, 54)], BLACK, 3)
    p.line([(90, 76), (100, 100), (96, 122)], BLACK, 3)
    p.line(body + [body[0]], BLACK, OL)
    # A crack and a few specks.
    p.line([(62, 92), (70, 100), (66, 110)], BLACK, 2.5)
    for x, y in [(78, 96), (54, 102), (84, 110)]:
        p.circle(x, y, 2, DARK)
    p.line([(60, 62), (76, 58)], WHITE, 3)
    return p.image(160)


def ore1():
    """Huyền phẩm: one clear crystal, a facet lit, on a small rock."""
    p = Pen(160)
    p.poly([(44, 132), (58, 114), (102, 112), (118, 132)], DARK, BLACK, 4)
    c = [(66, 118), (62, 52), (80, 22), (98, 52), (94, 118)]
    p.poly(c, LIGHT, BLACK, OL)
    p.poly([(62, 52), (80, 22), (80, 118), (66, 118)], WHITE)
    p.poly([(80, 22), (98, 52), (94, 118), (80, 118)], MID)
    p.line([(80, 22), (80, 118)], BLACK, 2.5)
    p.line([(62, 52), (98, 52)], BLACK, 2.5)
    p.line(c + [c[0]], BLACK, OL)
    p.line([(70, 60), (70, 100)], WHITE, 3)
    sparkle(p, 112, 36, 9)
    return p.image(160)


def ore2():
    """Địa phẩm: three crystals rise from a rock, the middle one tallest."""
    p = Pen(160)
    p.poly([(26, 136), (40, 112), (120, 110), (136, 136)], DARK, BLACK, 4)

    def crystal(x, top, w, base, light):
        pts = [(x - w, base), (x - w, top + w * 1.3), (x, top), (x + w, top + w * 1.3), (x + w, base)]
        p.poly(pts, MID, BLACK, 4)
        p.poly([(x - w, base), (x - w, top + w * 1.3), (x, top), (x, base)], light)
        p.line([(x, top), (x, base)], BLACK, 2)
        p.line(pts + [pts[0]], BLACK, 4)

    crystal(52, 54, 13, 118, LIGHT)
    crystal(110, 60, 12, 116, LIGHT)
    crystal(80, 18, 17, 120, WHITE)
    p.line([(74, 46), (74, 104)], WHITE, 3)
    sparkle(p, 128, 30, 9)
    sparkle(p, 30, 40, 7)
    return p.image(160)


def ore3():
    """Thiên phẩm: a cut gem with its facets, light around it."""
    p = Pen(160)
    for a in range(0, 360, 30):
        t = math.radians(a)
        p.line([(80 + 58 * math.cos(t), 78 + 58 * math.sin(t)), (80 + 70 * math.cos(t), 78 + 70 * math.sin(t))], BLACK, 3.5)
    crown = [(36, 64), (54, 38), (106, 38), (124, 64)]
    gem = crown + [(80, 128)]
    p.poly(gem, LIGHT, BLACK, OL)
    p.poly([(36, 64), (54, 38), (68, 64)], WHITE)
    p.poly([(68, 64), (80, 38), (92, 64)], WHITE)
    p.poly([(92, 64), (106, 38), (124, 64)], MID)
    p.poly([(92, 64), (124, 64), (80, 128)], MID)
    p.poly([(36, 64), (68, 64), (80, 128)], WHITE)
    for a, b in [((36, 64), (124, 64)), ((54, 38), (68, 64)), ((68, 64), (80, 38)), ((80, 38), (92, 64)), ((92, 64), (106, 38)),
                 ((68, 64), (80, 128)), ((92, 64), (80, 128))]:
        p.line([a, b], BLACK, 2.5)
    p.line(gem + [gem[0]], BLACK, OL)
    sparkle(p, 128, 28, 11)
    sparkle(p, 28, 118, 8)
    return p.image(160)


def ore4():
    """Âm Dương phẩm: a sphere of yin and yang in a ring of light, two stars of sun and moon."""
    p = Pen(160)
    cx, cy, r = 80, 80, 40
    # Rays, long and short.
    for i, a in enumerate(range(0, 360, 22)):
        t = math.radians(a + 11)
        l = 76 if i % 2 == 0 else 64
        p.poly([(cx + (r + 10) * math.cos(t - 0.07), cy + (r + 10) * math.sin(t - 0.07)),
                (cx + l * math.cos(t), cy + l * math.sin(t)),
                (cx + (r + 10) * math.cos(t + 0.07), cy + (r + 10) * math.sin(t + 0.07))], DARK if i % 2 else BLACK)
    p.ring(cx, cy, r + 9, 3, BLACK)
    # Taiji: white on the left, black on the right, the S between.
    p.circle(cx, cy, r, WHITE, BLACK, OL)
    right = arcpts(cx, cy, r - 2, -90, 90) + arcpts(cx, cy + (r - 2) / 2, (r - 2) / 2, 90, 270) + arcpts(cx, cy - (r - 2) / 2, (r - 2) / 2, 90, -90)
    p.poly(right, BLACK)
    p.circle(cx, cy - r / 2, 6, BLACK)
    p.circle(cx, cy + r / 2, 6, WHITE)
    # A shine on the white half.
    p.line(arcpts(cx, cy, r - 9, 200, 240, 10), MID, 4)
    p.ring(cx, cy, r, OL, BLACK)
    return p.image(160)


def ore5():
    """Hỗn Độn phẩm: a dark star of chaos, a spiral inside, an orbit, an eight-point burst behind."""
    p = Pen(160)
    cx, cy = 80, 80
    p.poly(star(cx, cy, 78, 46, 8, -90), LIGHT, BLACK, 3)
    p.poly(star(cx, cy, 66, 50, 8, -67.5), MID, BLACK, 2.5)
    p.circle(cx, cy, 42, BLACK)
    # A spiral of light.
    sp = []
    for i in range(120):
        t = i / 119 * 3.4 * math.pi
        rr = 3 + 32 * i / 119
        sp.append((cx + rr * math.cos(t), cy + rr * math.sin(t)))
    p.line(sp, LIGHT, 3.5)
    sp2 = [(2 * cx - x, 2 * cy - y) for x, y in sp]
    p.line(sp2, DARK, 3)
    p.circle(cx, cy, 6, WHITE)
    for x, y, s in [(62, 64, 2.2), (100, 70, 2.6), (92, 104, 2), (66, 96, 1.8), (104, 92, 1.6), (76, 54, 1.6)]:
        p.circle(x, y, s, WHITE)
    # The orbit, broken where it passes behind.
    s = p.s
    box = [(cx - 72) * s, (cy - 22) * s, (cx + 72) * s, (cy + 22) * s]
    p.dg.arc(box, -10, 190, fill=BLACK, width=int(7 * s))
    p.da.arc(box, -10, 190, fill=255, width=int(7 * s))
    p.dg.arc(box, -8, 188, fill=WHITE, width=int(3 * s))
    p.circle(cx + 70, cy + 6, 6, WHITE, BLACK, 3)
    sparkle(p, 26, 26, 9)
    sparkle(p, 136, 132, 8)
    return p.image(160)


ORES = [ore0, ore1, ore2, ore3, ore4, ore5]


# --- Ten realms (160 × 160, black) -----------------------------------------------------------------

def realm0():
    """Luyện Khí: qi drawn in, a spiral of three turns and the motes around it."""
    p = Pen(160)
    cx, cy = 80, 80
    sp = []
    for i in range(200):
        t = i / 199 * 4.6 * math.pi
        r = 6 + 52 * i / 199
        sp.append((cx + r * math.cos(t), cy + r * math.sin(t)))
    p.line(sp, BLACK, 9)
    p.circle(cx, cy, 9, BLACK)
    for a, r, s in [(20, 70, 5), (130, 70, 4), (250, 72, 5.5), (310, 64, 3.5), (200, 66, 3.5)]:
        t = math.radians(a)
        p.circle(cx + r * math.cos(t), cy + r * math.sin(t), s, BLACK)
    return p.image(160)


def realm1():
    """Trúc Cơ: a stone altar of three steps, two pillars, the foundation's light on top."""
    p = Pen(160)
    p.poly([(14, 132), (146, 132), (146, 148), (14, 148)], BLACK)
    p.poly([(30, 112), (130, 112), (130, 126), (30, 126)], BLACK)
    p.poly([(46, 94), (114, 94), (114, 106), (46, 106)], BLACK)
    p.poly([(38, 40), (50, 40), (50, 88), (38, 88)], BLACK)
    p.poly([(110, 40), (122, 40), (122, 88), (110, 88)], BLACK)
    p.poly([(30, 30), (130, 30), (124, 40), (36, 40)], BLACK)
    p.poly([(56, 20), (104, 20), (110, 30), (50, 30)], BLACK)
    p.circle(80, 66, 16, BLACK)
    p.cut(circle=(80, 66, 7))
    for a in range(0, 360, 45):
        t = math.radians(a)
        p.line([(80 + 21 * math.cos(t), 66 + 21 * math.sin(t)), (80 + 26 * math.cos(t), 66 + 26 * math.sin(t))], BLACK, 3.5)
    return p.image(160)


def realm2():
    """Kim Đan: the golden core, round and lit, in a ring with eight rays."""
    p = Pen(160)
    cx, cy = 80, 80
    for a in range(0, 360, 45):
        t = math.radians(a)
        l = 74 if a % 90 == 0 else 64
        p.poly([(cx + 52 * math.cos(t - 0.12), cy + 52 * math.sin(t - 0.12)), (cx + l * math.cos(t), cy + l * math.sin(t)),
                (cx + 52 * math.cos(t + 0.12), cy + 52 * math.sin(t + 0.12))], BLACK)
    p.ring(cx, cy, 48, 6, BLACK)
    p.circle(cx, cy, 36, BLACK)
    p.cut(pts=arcpts(cx, cy, 27, 200, 260, 16) + arcpts(cx, cy, 20, 260, 200, 16))
    p.cut(circle=(cx - 18, cy - 18, 5))
    return p.image(160)


def realm3():
    """Nguyên Anh: the infant soul sitting on a lotus, a halo behind its head."""
    p = Pen(160)
    cx = 80
    p.ring(cx, 46, 26, 5, BLACK)
    p.circle(cx, 46, 15, BLACK)
    # Body: shoulders down to crossed legs.
    p.poly([(56, 96), (62, 70), (72, 62), (88, 62), (98, 70), (104, 96)], BLACK)
    p.poly([(40, 106), (56, 92), (104, 92), (120, 106), (100, 112), (60, 112)], BLACK)
    p.cut(pts=[(68, 92), (80, 84), (92, 92), (80, 96)])
    # A lotus under it: five petals, a gap between each.
    for a in (-160, -125, -90, -55, -20):
        p.poly(petal(cx, 140, 34, 10, a), BLACK)
    for a in (-142, -107, -73, -38):
        t = math.radians(a)
        p.cut(pts=[(cx, 140), (cx + 40 * math.cos(t - 0.03), 140 + 40 * math.sin(t - 0.03)), (cx + 40 * math.cos(t + 0.03), 140 + 40 * math.sin(t + 0.03))])
    p.poly([(34, 140), (126, 140), (116, 150), (44, 150)], BLACK)
    return p.image(160)


def realm4():
    """Hóa Thần: the spirit become flame, a flame within the flame, on a lotus."""
    p = Pen(160)
    cx = 80
    flame = [(80, 10), (98, 38), (116, 62), (120, 90), (108, 114), (80, 126), (52, 114), (40, 90), (46, 64), (60, 48), (66, 64),
             (74, 40)]
    p.poly(flame, BLACK)
    p.cut(pts=[(82, 50), (94, 72), (100, 94), (92, 112), (80, 116), (68, 112), (62, 96), (70, 76), (76, 86)])
    p.poly([(82, 72), (90, 90), (88, 104), (80, 108), (72, 104), (72, 92)], BLACK)
    for a, l in [(180, 40), (0, 40)]:
        p.poly(petal(cx, 140, l, 9, a), BLACK)
    p.poly([(44, 140), (116, 140), (108, 150), (52, 150)], BLACK)
    return p.image(160)


def realm5():
    """Luyện Hư: a gate of the void, a broken ring around a point, stars in the gap."""
    p = Pen(160)
    cx, cy = 80, 82
    p.line(arcpts(cx, cy, 52, 40, 320, 60), BLACK, 12, joint=True)
    p.line(arcpts(cx, cy, 32, 220, 500, 60), BLACK, 6)
    p.circle(cx, cy, 11, BLACK)
    sparkle(p, 130, 50, 13)
    sparkle(p, 138, 108, 8)
    sparkle(p, 116, 80, 6)
    return p.image(160)


def realm6():
    """Hợp Thể: body and spirit one, the taiji in a ring of eight marks."""
    p = Pen(160)
    cx, cy, r = 80, 80, 54
    p.circle(cx, cy, r, BLACK)
    q = r - 7
    p.cut(pts=arcpts(cx, cy, q, 90, 270) + arcpts(cx, cy - q / 2, q / 2, 270, 450) + arcpts(cx, cy + q / 2, q / 2, 270, 90))
    p.circle(cx, cy + q / 2, 8, BLACK)
    p.cut(circle=(cx, cy - q / 2, 8))
    for a in range(0, 360, 45):
        t = math.radians(a + 22.5)
        p.line([(cx + 62 * math.cos(t), cy + 62 * math.sin(t)), (cx + 72 * math.cos(t), cy + 72 * math.sin(t))], BLACK, 5)
    return p.image(160)


def realm7():
    """Đại Thừa: the wheel of the great vehicle, eight spokes with jewels."""
    p = Pen(160)
    cx, cy = 80, 80
    p.ring(cx, cy, 46, 9, BLACK)
    p.ring(cx, cy, 30, 3, BLACK)
    p.circle(cx, cy, 12, BLACK)
    p.cut(circle=(cx, cy, 5))
    for a in range(0, 360, 45):
        t = math.radians(a)
        p.line([(cx + 12 * math.cos(t), cy + 12 * math.sin(t)), (cx + 44 * math.cos(t), cy + 44 * math.sin(t))], BLACK, 5)
        p.poly(star(cx + 60 * math.cos(t), cy + 60 * math.sin(t), 10, 4, 4, a), BLACK)
    return p.image(160)


def cloud(p, x, y, w, fill=BLACK):
    """A cloud of three puffs on a flat base, `w` wide from x."""
    h = w * 0.42
    p.circle(x + w * 0.28, y + h * 0.55, w * 0.2, fill)
    p.circle(x + w * 0.52, y + h * 0.36, w * 0.26, fill)
    p.circle(x + w * 0.76, y + h * 0.58, w * 0.18, fill)
    p.poly([(x + w * 0.1, y + h * 0.6), (x + w * 0.92, y + h * 0.6), (x + w * 0.94, y + h), (x + w * 0.06, y + h)], fill)
    p.circle(x + w * 0.08, y + h * 0.8, h * 0.2, fill)
    p.circle(x + w * 0.92, y + h * 0.8, h * 0.2, fill)


def realm8():
    """Độ Kiếp: the storm cloud of the tribulation and its bolt."""
    p = Pen(160)
    cloud(p, 14, 18, 132)
    p.poly([(88, 66), (58, 108), (78, 108), (62, 150), (110, 96), (88, 96), (104, 66)], BLACK)
    p.cut(pts=[(88, 66), (58, 108), (78, 108), (62, 150), (110, 96), (88, 96), (104, 66)])
    p.poly([(86, 62), (54, 110), (74, 110), (58, 154), (114, 94), (92, 94), (108, 62)], BLACK)
    p.line([(36, 76), (30, 92)], BLACK, 4)
    p.line([(124, 76), (118, 92)], BLACK, 4)
    return p.image(160)


def realm9():
    """Tiên nhân: a star of the immortals over the clouds, rays above."""
    p = Pen(160)
    p.poly(star(80, 56, 38, 13, 4), BLACK)
    p.poly(star(80, 56, 22, 8, 4, -45), BLACK)
    p.cut(circle=(80, 56, 6))
    for a in range(-160, -10, 25):
        t = math.radians(a)
        p.line([(80 + 46 * math.cos(t), 56 + 46 * math.sin(t)), (80 + 58 * math.cos(t), 56 + 58 * math.sin(t))], BLACK, 4)
    cloud(p, 20, 100, 120)
    p.cut(pts=[(24, 136), (136, 136), (136, 140), (24, 140)])
    return p.image(160)


REALMS = [realm0, realm1, realm2, realm3, realm4, realm5, realm6, realm7, realm8, realm9]



# --- Icons of the screens (128 × 128, black) --------------------------------------------------------

def icon_congphap():
    """Công pháp: a scroll unrolled on two rods, lines of writing and a seal."""
    p = Pen(128)
    p.poly([(26, 26), (102, 26), (102, 104), (26, 104)], BLACK)
    p.cut(pts=[(32, 32), (96, 32), (96, 98), (32, 98)])
    for x in (18, 110):
        p.poly([(x - 7, 18), (x + 7, 18), (x + 7, 112), (x - 7, 112)], BLACK)
        p.circle(x, 16, 7, BLACK)
        p.circle(x, 114, 7, BLACK)
    for i, y in enumerate((42, 56, 70, 84)):
        p.poly([(40, y), (40 + (44 if i % 2 == 0 else 34), y), (40 + (44 if i % 2 == 0 else 34), y + 6), (40, y + 6)], BLACK)
    p.poly([(78, 74), (92, 74), (92, 92), (78, 92)], BLACK)
    p.cut(pts=[(82, 78), (88, 78), (88, 88), (82, 88)])
    return p.image(128)


def icon_dongphu():
    """Động phủ: a mountain, an arched door in it, steps up to the door."""
    p = Pen(128)
    p.poly([(4, 112), (38, 44), (52, 60), (78, 14), (124, 112)], BLACK)
    p.cut(pts=arcpts(64, 78, 20, 180, 360, 24) + [(84, 108), (44, 108)])
    p.poly([(52, 98), (76, 98), (76, 102), (52, 102)], BLACK)
    p.poly([(40, 112), (88, 112), (88, 118), (40, 118)], BLACK)
    p.cut(pts=[(70, 30), (78, 18), (86, 30), (78, 26)])
    p.circle(64, 72, 3.5, BLACK)
    return p.image(128)


def icon_phapbao():
    """Pháp bảo: a flying sword pointing up in a ring of light."""
    p = Pen(128)
    p.ring(64, 64, 48, 5, BLACK)
    for a in range(0, 360, 45):
        t = math.radians(a + 22.5)
        p.line([(64 + 54 * math.cos(t), 64 + 54 * math.sin(t)), (64 + 61 * math.cos(t), 64 + 61 * math.sin(t))], BLACK, 4)
    p.poly([(64, 10), (73, 26), (71, 80), (57, 80), (55, 26)], BLACK)
    p.cut(pts=[(64, 20), (66, 28), (65, 74), (63, 74), (62, 28)])
    p.poly([(40, 80), (88, 80), (88, 88), (40, 88)], BLACK)
    p.poly([(59, 88), (69, 88), (69, 108), (59, 108)], BLACK)
    p.circle(64, 113, 6, BLACK)
    return p.image(128)


def icon_daohieu():
    """Đạo hiệu: a lệnh bài on a cord, a mark carved in it, a tassel below."""
    p = Pen(128)
    p.line([(64, 4), (64, 18)], BLACK, 4)
    p.circle(64, 18, 6, BLACK)
    body = [(36, 26), (92, 26), (100, 36), (100, 86), (64, 104), (28, 86), (28, 36)]
    p.poly(body, BLACK)
    p.cut(pts=[(38, 36), (90, 36), (90, 82), (64, 94), (38, 82)])
    p.poly(star(64, 60, 20, 8, 4), BLACK)
    p.circle(64, 60, 5, WHITE)
    for x in (58, 64, 70):
        p.line([(64, 104), (x, 124)], BLACK, 3)
    return p.image(128)


def icon_nienky():
    """Niên kỷ: a calendar of the year, two rings on top, a crescent moon and the days."""
    p = Pen(128)
    p.poly([(16, 22), (112, 22), (112, 114), (16, 114)], BLACK)
    p.cut(pts=[(22, 42), (106, 42), (106, 108), (22, 108)])
    for x in (40, 88):
        p.poly([(x - 5, 10), (x + 5, 10), (x + 5, 32), (x - 5, 32)], BLACK)
    p.circle(48, 72, 18, BLACK)
    p.cut(circle=(56, 66, 16))
    for i in range(3):
        for j in range(2):
            p.poly([(70 + i * 11, 56 + j * 18), (78 + i * 11, 56 + j * 18), (78 + i * 11, 64 + j * 18), (70 + i * 11, 64 + j * 18)], BLACK)
    return p.image(128)


def icon_tongmon():
    """Tông môn: the sơn môn, a gate of two pillars under two curved roofs."""
    p = Pen(128)
    roof = [(6, 30), (20, 22), (108, 22), (122, 30), (110, 34), (18, 34)]
    p.poly(roof, BLACK)
    p.poly([(14, 22), (64, 6), (114, 22)], BLACK)
    p.poly([(22, 50), (106, 50), (112, 58), (16, 58)], BLACK)
    for x in (30, 98):
        p.poly([(x - 7, 34), (x + 7, 34), (x + 7, 118), (x - 7, 118)], BLACK)
        p.poly([(x - 12, 114), (x + 12, 114), (x + 12, 122), (x - 12, 122)], BLACK)
    p.poly([(52, 36), (76, 36), (76, 48), (52, 48)], BLACK)
    p.cut(pts=[(56, 39), (72, 39), (72, 45), (56, 45)])
    return p.image(128)


def icon_daolo():
    """Đạo lộ: one road winding up from below, splitting into three, under a peak and a star."""
    p = Pen(128)
    p.poly([(38, 50), (64, 16), (90, 50)], BLACK)
    p.cut(pts=[(50, 45), (64, 27), (78, 45)])
    sparkle(p, 64, 8, 8)
    p.line([(64, 124), (54, 110), (72, 96), (64, 84)], BLACK, 9)
    p.line([(64, 84), (38, 70), (26, 54)], BLACK, 6)
    p.line([(64, 84), (64, 56)], BLACK, 6)
    p.line([(64, 84), (90, 70), (102, 54)], BLACK, 6)
    for x in (26, 102):
        p.circle(x, 54, 7, BLACK)
    return p.image(128)


def icon_dao_phap():
    """Pháp tu: an orb of linh lực in a ring, rays of a spell around it."""
    p = Pen(128)
    p.ring(64, 64, 34, 7, BLACK)
    p.circle(64, 64, 15, BLACK)
    p.cut(circle=(58, 58, 5))
    for a in range(0, 360, 45):
        t = math.radians(a + 22.5)
        p.line([(64 + 45 * math.cos(t), 64 + 45 * math.sin(t)), (64 + 58 * math.cos(t), 64 + 58 * math.sin(t))], BLACK, 6)
    return p.image(128)


def icon_dao_the():
    """Thể tu: a breastplate of the body, a spine and ribs of iron."""
    p = Pen(128)
    body = [(28, 26), (50, 16), (64, 26), (78, 16), (100, 26), (108, 58), (94, 106), (64, 122), (34, 106), (20, 58)]
    p.poly(body, BLACK)
    p.cut(pts=[(34, 34), (52, 26), (64, 34), (76, 26), (94, 34), (100, 58), (88, 98), (64, 112), (40, 98), (28, 58)])
    p.poly([(60, 34), (68, 34), (68, 110), (60, 110)], BLACK)
    for i, y in enumerate((48, 64, 80)):
        w = 28 - i * 4
        p.poly([(64 - w - 6, y), (58, y), (58, y + 8), (64 - w - 2, y + 8)], BLACK)
        p.poly([(70, y), (64 + w + 6, y), (64 + w + 2, y + 8), (70, y + 8)], BLACK)
    return p.image(128)


def icon_dao_than():
    """Thần tu: the eye of the mind open, rays of thần thức above it."""
    p = Pen(128)
    top = arcpts(64, 118, 70, 222, 318, 30)
    bot = arcpts(64, 10, 70, 42, 138, 30)
    p.poly(top + bot, BLACK)
    p.cut(pts=arcpts(64, 122, 64, 226, 314, 30) + arcpts(64, 6, 64, 46, 134, 30))
    p.circle(64, 64, 17, BLACK)
    p.cut(circle=(64, 64, 6))
    for a in (-150, -120, -90, -60, -30):
        t = math.radians(a)
        p.line([(64 + 40 * math.cos(t), 56 + 40 * math.sin(t)), (64 + 54 * math.cos(t), 56 + 54 * math.sin(t))], BLACK, 5)
    return p.image(128)


def icon_dao_kiem():
    """Kiếm tu: a straight sword, point up, a guard and a tassel."""
    p = Pen(128)
    p.poly([(64, 4), (74, 20), (72, 86), (56, 86), (54, 20)], BLACK)
    p.cut(pts=[(64, 14), (66, 22), (65, 80), (63, 80), (62, 22)])
    p.poly([(36, 86), (92, 86), (86, 96), (42, 96)], BLACK)
    p.poly([(59, 96), (69, 96), (69, 114), (59, 114)], BLACK)
    p.circle(64, 118, 7, BLACK)
    p.line([(64, 118), (80, 126)], BLACK, 3)
    return p.image(128)


def icon_dao_bach():
    """Bách nghệ: a đan đỉnh on three legs, two handles, the smoke of a pill rising."""
    p = Pen(128)
    p.poly([(26, 56), (102, 56), (98, 90), (82, 104), (46, 104), (30, 90)], BLACK)
    p.poly([(20, 50), (108, 50), (108, 58), (20, 58)], BLACK)
    for x in (16, 112):
        p.ring(x, 66, 9, 5, BLACK)
    for x0, x1 in ((44, 38), (64, 64), (84, 90)):
        p.line([(x0, 104), (x1, 120)], BLACK, 6)
    p.poly([(52, 70), (76, 70), (76, 78), (52, 78)], WHITE)
    p.line([(54, 44), (48, 32), (58, 22), (52, 10)], BLACK, 5)
    p.line([(76, 44), (82, 32), (72, 22), (78, 10)], BLACK, 5)
    return p.image(128)


ICONS = {'congphap': icon_congphap, 'dongphu': icon_dongphu, 'phapbao': icon_phapbao, 'daohieu': icon_daohieu,
         'nienky': icon_nienky, 'tongmon': icon_tongmon, 'daolo': icon_daolo, 'dao_phap': icon_dao_phap,
         'dao_the': icon_dao_the, 'dao_than': icon_dao_than, 'dao_kiem': icon_dao_kiem, 'dao_bach': icon_dao_bach}

# --- Âm Dương phẩm frames (pixel art, 128 logical × 2) ---------------------------------------------

def yin_yang_frame(src):
    """The badge `src` (256 × 256) with a frame of yin and yang in place of its round one."""
    base = src.convert("RGBA").resize((128, 128), Image.NEAREST)
    cx, cy = 63.5, 56.5
    out = Image.new("RGBA", (128, 128), (0, 0, 0, 0))
    px = out.load()

    def put(x, y, v):
        if 0 <= x < 128 and 0 <= y < 128:
            px[x, y] = (v, v, v, 255)

    # Rays of light behind the ring: twelve, long and short, dark with black edges.
    for y in range(128):
        for x in range(128):
            dx, dy = x - cx, y - cy
            d = math.hypot(dx, dy)
            ang = (math.degrees(math.atan2(dy, dx)) + 360) % 360
            k = ang / 30.0
            off = abs(k - round(k)) * 30  # degrees from the nearest ray
            long = round(k) % 2 == 0
            reach = 64 if long else 61
            width = 9.0 * (1 - (d - 53) / (reach - 53 + 0.01)) if d > 53 else 0
            if 55 < d <= reach and off < width:
                put(x, y, BLACK if off > width - 1.8 or d > reach - 1.2 else (WHITE if long else LIGHT))
    # The ring: white on the left, dark on the right, bounded in black; the S where they meet.
    for y in range(128):
        for x in range(128):
            dx, dy = x - cx, y - cy
            d = math.hypot(dx, dy)
            if 45.5 <= d <= 56:
                if d < 47.5 or d > 54:
                    put(x, y, BLACK)
                    continue
                # The divide sways: it leans right at the top and left at the bottom.
                yang = dx + (6 if dy < 0 else -6) * (abs(dy) / 50) < 0

                put(x, y, WHITE if yang else BLACK)
    # The divide itself, top and bottom.
    for y in range(128):
        for x in range(128):
            dx, dy = x - cx, y - cy
            d = math.hypot(dx, dy)
            if 47.5 <= d <= 54 and abs(dx + (6 if dy < 0 else -6) * (abs(dy) / 50)) < 1.0:
                put(x, y, BLACK)
    # The two eyes: dark on the white side, white on the dark side.
    for ex, ev, edge in [(cx - 51, BLACK, BLACK), (cx + 51, WHITE, WHITE)]:
        for y in range(128):
            for x in range(128):
                d = math.hypot(x - ex, y - cy)
                if d <= 4.6:
                    put(x, y, edge if d > 3.2 else ev)
    # Four diamonds on the diagonals.
    for a in (45, 135, 225, 315):
        t = math.radians(a)
        mx, my = cx + 51 * math.cos(t), cy + 51 * math.sin(t)
        for y in range(128):
            for x in range(128):
                dd = abs(x - mx) + abs(y - my)
                if dd <= 4.5:
                    put(x, y, BLACK if dd > 3.2 else LIGHT)
    # The sun and the moon on top.
    sx, sy = cx, 6.5
    for y in range(128):
        for x in range(128):
            d = math.hypot(x - sx, y - sy)
            if d <= 7:
                put(x, y, BLACK if d > 5.6 else WHITE)
            elif d <= 10 and abs(((math.degrees(math.atan2(y - sy, x - sx)) + 360) % 45) - 22.5) > 16 and y < sy + 3:
                put(x, y, BLACK)
    for y in range(int(sy) - 4, int(sy) + 5):
        for x in range(int(sx) - 4, int(sx) + 5):
            if math.hypot(x - sx, y - sy) <= 3.6 and math.hypot(x - sx - 2, y - sy + 1) > 3.0:
                put(x, y, DARK)
    # The treasure inside, as it was.
    bp = base.load()
    for y in range(128):
        for x in range(128):
            if math.hypot(x - cx, y - cy) < 45.5 and bp[x, y][3]:
                v = bp[x, y][0]
                # The ground of the treasure: light on the yang side.
                if v == MID and x < cx + (6 if y < cy else -6) * (abs(y - cy) / 50):
                    v = LIGHT
                put(x, y, v)
    # The ribbon, as it was.
    for y in range(101, 128):
        for x in range(128):
            if bp[x, y][3]:
                px[x, y] = bp[x, y]
    return out.resize((256, 256), Image.NEAREST)


# --- Main ----------------------------------------------------------------------------------------

def main():
    preview = "--preview" in sys.argv
    if "--icons" in sys.argv:
        for name, f in ICONS.items():
            f().save(os.path.join(RES, f"ic_{name}.png"))
        sheet = Image.new("RGB", (len(ICONS) * 140, 140), "white")
        for i, f in enumerate(ICONS.values()):
            im = f()
            sheet.paste(im, (i * 140 + 6, 6), im)
        os.makedirs(os.path.join(ROOT, "build"), exist_ok=True)
        sheet.save(os.path.join(ROOT, "build", "icons.png"))
        print("icons")
        return
    ores = [f() for f in ORES]
    realms = [f() for f in REALMS]
    badges = {i: yin_yang_frame(Image.open(os.path.join(RES, f"badge_{i:02d}.png"))) for i in range(48, 60)}
    if preview:
        out = os.path.join(ROOT, "build")
        os.makedirs(out, exist_ok=True)
        sheet = Image.new("RGB", (10 * 160, 3 * 160 + 2 * 128 + 256), "white")
        for i, im in enumerate(ores):
            sheet.paste(im, (i * 160, 0), im)
        for i, im in enumerate(realms):
            sheet.paste(im, (i * 160, 160), im)
        for i, im in enumerate(realms):
            small = im.resize((64, 64), Image.LANCZOS)
            sheet.paste(small, (i * 160 + 48, 330), small)
        tiers = [Image.open(os.path.join(RES, f"badge_{t * 12 + 1:02d}.png")).convert("RGBA") for t in range(4)] + \
            [badges[49], Image.open(os.path.join(RES, "badge_61.png")).convert("RGBA")]
        for i, im in enumerate(tiers):
            sheet.paste(im, (i * 260, 410), im)
        sheet.save(os.path.join(out, "do_hoa.png"))
        print(os.path.join(out, "do_hoa.png"))
        return
    for i, im in enumerate(ores):
        im.save(os.path.join(RES, f"art_ore_{i}.png"))
    for i, im in enumerate(realms):
        im.save(os.path.join(RES, f"realm_{i}.png"))
    for i, im in badges.items():
        im.save(os.path.join(RES, f"badge_{i:02d}.png"))
    for name, f in ICONS.items():
        f().save(os.path.join(RES, f"ic_{name}.png"))
    print("ok")


if __name__ == "__main__":
    main()
