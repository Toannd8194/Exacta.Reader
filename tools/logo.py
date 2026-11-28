"""Logo of Vạn Quyển Tiên Đồ (3.5): minimal. Three books laid as steps climbing to a full moon:
vạn quyển, the books; tiên đồ, the way up they make. White on black, two shapes and nothing else, so
it reads at launcher size and on e-ink.

    python tools/logo.py            writes res/drawable-nodpi/logo.png and every launcher icon
    python tools/logo.py out.png    a preview sheet only
"""
import os
import sys

from PIL import Image, ImageDraw

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(ROOT, "app", "src", "main", "res")
SS = 4                                   # supersampling


def motif(d, s, ox=0.0, oy=0.0, scale=1.0, ink=255, bg=0):
    """The books and the moon on a 512 frame, scaled and moved; `d` draws at supersampled size `s`/512."""
    k = s / 512.0 * scale
    X = lambda x: (ox + x) * k
    Y = lambda y: (oy + y) * k
    # Three books, each a slab with the line of its pages, climbing to the right.
    books = [(88, 360, 300, 58), (152, 288, 258, 58), (216, 216, 208, 58)]
    for (x, y, w, h) in books:
        d.rounded_rectangle([X(x), Y(y), X(x + w), Y(y + h)], radius=12 * k, fill=ink)
        # The pages: a line along the slab, stopping short of the spine on the left.
        d.rounded_rectangle([X(x + 34), Y(y + h * 0.42), X(x + w - 14), Y(y + h * 0.42 + 9)], radius=4 * k, fill=bg)
    # The moon above the top step.
    cx, cy, r = 372, 136, 44
    d.ellipse([X(cx - r), Y(cy - r), X(cx + r), Y(cy + r)], fill=ink)


def tile(size, shape="square"):
    """The logo on its black tile: a rounded square, or a circle."""
    s = size * SS
    im = Image.new("RGBA", (s, s), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    if shape == "circle":
        d.ellipse([0, 0, s - 1, s - 1], fill=(0, 0, 0, 255))
        sc, off = 0.78, 512 * 0.11
    else:
        d.rounded_rectangle([0, 0, s - 1, s - 1], radius=s * 0.22, fill=(0, 0, 0, 255))
        sc, off = 0.86, 512 * 0.07
    layer = Image.new("L", (s, s), 0)
    motif(ImageDraw.Draw(layer), s, off / sc, off / sc, sc, ink=255, bg=0)
    white = Image.new("RGBA", (s, s), (255, 255, 255, 255))
    im.paste(white, (0, 0), layer)
    return im.resize((size, size), Image.LANCZOS)


def foreground(size):
    """The adaptive icon's foreground: the motif inside the safe circle, on transparency."""
    s = size * SS
    layer = Image.new("L", (s, s), 0)
    sc = 0.6
    off = 512 * (1 - sc) / 2
    motif(ImageDraw.Draw(layer), s, off / sc, off / sc, sc, ink=255, bg=0)
    im = Image.new("RGBA", (s, s), (255, 255, 255, 0))
    im.putalpha(layer)
    return im.resize((size, size), Image.LANCZOS)


DENSITIES = {"mdpi": 1.0, "hdpi": 1.5, "xhdpi": 2.0, "xxhdpi": 3.0, "xxxhdpi": 4.0}


def main():
    if len(sys.argv) > 1:
        sheet = Image.new("RGB", (512 + 24 + 192 + 24 + 96 + 24 + 48 + 24 + 432, 520), (238, 238, 232))
        x = 0
        for im in (tile(512), tile(192), tile(96, "circle"), tile(48)):
            sheet.paste(im, (x, 0), im)
            x += im.width + 24
        fg = foreground(432)
        back = Image.new("RGBA", (432, 432), (0, 0, 0, 255))
        back.paste(fg, (0, 0), fg)
        sheet.paste(back, (x, 0))
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
