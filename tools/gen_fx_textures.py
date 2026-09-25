#!/usr/bin/env python3
"""Generates the procedural effect textures (assets/celestialarts/textures/fx) and particle
sprites (assets/celestialarts/textures/particle). Run from the repository root:

    python3 tools/gen_fx_textures.py
"""
import math
import os
import random
import sys

sys.path.insert(0, os.path.dirname(__file__))
from PIL import Image, ImageDraw, ImageFilter, ImageChops  # noqa: E402
import texlib as T  # noqa: E402

ROOT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "celestialarts", "textures")
FX = os.path.join(ROOT, "fx")
PART = os.path.join(ROOT, "particle")

WHITE = (255, 255, 255)


def out_fx(name, img, w, h):
    T.save(T.finish(img, w, h), os.path.join(FX, name + ".png"))
    print("fx/" + name)


def out_particle(name, img, w=32, h=32):
    T.save(T.finish(img, w, h), os.path.join(PART, name + ".png"))
    print("particle/" + name)


# ---------------------------------------------------------------------------- FX textures

def gen_ring():
    img = T.ring_gradient(128, 128, 0.70, 0.80, WHITE, 0.30, 0.14)
    core = T.ring_gradient(128, 128, 0.74, 0.79, WHITE, 0.05, 0.05)
    img.alpha_composite(core)
    out_fx("ring", img, 128, 128)


def gen_glow_soft():
    img = T.radial(64, 64, (255, 255, 255, 255), (255, 255, 255, 0), power=1.6)
    out_fx("glow_soft", img, 64, 64)


def gen_sparkle():
    img = T.new(64, 64)
    pts = T.star_points(32, 32, 30, 3, 4)
    T.polygon_glow(img, pts, WHITE, glow=3.0)
    pts = T.star_points(32, 32, 16, 2, 4, math.pi / 4)
    T.polygon_glow(img, pts, WHITE, glow=2.0)
    img.alpha_composite(T.radial(64, 64, (255, 255, 255, 200), (255, 255, 255, 0), power=2.0))
    out_fx("sparkle", img, 64, 64)


def circle_base(size, rings, color):
    """Concentric rings used by every magic circle."""
    img = T.new(size, size)
    for r_in, r_out, feather in rings:
        img.alpha_composite(T.ring_gradient(size, size, r_in, r_out, color, feather, feather))
    return img


def glyph_stroke(d, cx, cy, r, kind, color, width):
    """Small abstract dao glyphs made of strokes."""
    S = T.SS
    c = color + (255,)
    if kind == 0:
        d.line([(cx - r, cy) , (cx + r, cy)], fill=c, width=width)
        d.line([(cx, cy - r), (cx, cy + r)], fill=c, width=width)
        d.ellipse([cx - r * 0.45, cy - r * 0.45, cx + r * 0.45, cy + r * 0.45], outline=c, width=width)
    elif kind == 1:
        d.polygon([(cx, cy - r), (cx + r * 0.9, cy + r * 0.6), (cx - r * 0.9, cy + r * 0.6)], outline=c, width=width)
        d.line([(cx, cy - r * 0.3), (cx, cy + r * 0.6)], fill=c, width=width)
    elif kind == 2:
        d.rectangle([cx - r * 0.7, cy - r * 0.7, cx + r * 0.7, cy + r * 0.7], outline=c, width=width)
        d.line([(cx - r * 0.7, cy - r * 0.7), (cx + r * 0.7, cy + r * 0.7)], fill=c, width=width)
    elif kind == 3:
        d.line([(cx - r, cy + r * 0.5), (cx, cy - r), (cx + r, cy + r * 0.5)], fill=c, width=width)
        d.line([(cx - r * 0.5, cy + r * 0.5), (cx + r * 0.5, cy + r * 0.5)], fill=c, width=width)
    elif kind == 4:
        d.arc([cx - r, cy - r, cx + r, cy + r], 200, 340, fill=c, width=width)
        d.line([(cx, cy - r * 0.6), (cx, cy + r)], fill=c, width=width)
        d.line([(cx - r * 0.6, cy + r * 0.2), (cx + r * 0.6, cy + r * 0.2)], fill=c, width=width)
    elif kind == 5:
        d.line([(cx - r, cy - r), (cx + r, cy - r), (cx - r, cy + r), (cx + r, cy + r)], fill=c, width=width)
    elif kind == 6:
        d.ellipse([cx - r, cy - r * 0.5, cx + r, cy + r * 0.5], outline=c, width=width)
        d.line([(cx, cy - r), (cx, cy + r)], fill=c, width=width)
    else:
        d.line([(cx - r, cy), (cx, cy - r), (cx + r, cy), (cx, cy + r), (cx - r, cy)], fill=c, width=width)
        d.line([(cx - r * 0.5, cy), (cx + r * 0.5, cy)], fill=c, width=width)


def glyph_ring(img, size, radius_frac, count, color, glyph_r, width, rot=0.0):
    S = T.SS
    d = ImageDraw.Draw(img)
    cx = cy = size * S / 2
    R = size * S / 2 * radius_frac
    for i in range(count):
        a = rot + i * 2 * math.pi / count
        gx = cx + math.cos(a) * R
        gy = cy + math.sin(a) * R
        glyph_stroke(d, gx, gy, glyph_r * S, i % 8, color, max(1, int(width * S)))


def gen_circle_taiji():
    size = 256
    col = (255, 236, 190)
    img = circle_base(size, [(0.95, 0.985, 0.01), (0.86, 0.88, 0.01), (0.60, 0.615, 0.01)], col)
    S = T.SS
    d = ImageDraw.Draw(img)
    c = size * S / 2
    # Eight trigrams (bagua) between the outer rings.
    R = c * 0.735
    for i in range(8):
        a = i * math.pi / 4
        pattern = [(1, 1, 1), (0, 1, 1), (1, 0, 1), (0, 0, 1), (1, 1, 0), (0, 1, 0), (1, 0, 0), (0, 0, 0)][i]
        for k, solid in enumerate(pattern):
            rr = R - (k - 1) * 9 * S
            # line perpendicular to radius
            px = c + math.cos(a) * rr
            py = c + math.sin(a) * rr
            tx, ty = -math.sin(a), math.cos(a)
            L = 18 * S
            if solid:
                d.line([(px - tx * L, py - ty * L), (px + tx * L, py + ty * L)], fill=col + (255,), width=3 * S)
            else:
                d.line([(px - tx * L, py - ty * L), (px - tx * L * 0.2, py - ty * L * 0.2)], fill=col + (255,), width=3 * S)
                d.line([(px + tx * L * 0.2, py + ty * L * 0.2), (px + tx * L, py + ty * L)], fill=col + (255,), width=3 * S)
    # Taiji symbol in the middle.
    r = c * 0.5
    d.pieslice([c - r, c - r, c + r, c + r], 90, 270, fill=col + (255,))
    d.pieslice([c - r, c - r, c + r, c + r], 270, 450, fill=col + (60,))
    d.ellipse([c - r / 2, c - r, c + r / 2, c], fill=col + (255,))
    d.ellipse([c - r / 2, c, c + r / 2, c + r], fill=col + (60,))
    d.ellipse([c - r / 6, c - r / 2 - r / 6, c + r / 6, c - r / 2 + r / 6], fill=col + (60,))
    d.ellipse([c - r / 6, c + r / 2 - r / 6, c + r / 6, c + r / 2 + r / 6], fill=col + (255,))
    d.ellipse([c - r, c - r, c + r, c + r], outline=col + (255,), width=3 * S)
    glow = img.filter(ImageFilter.GaussianBlur(3 * S))
    glow.alpha_composite(img)
    out_fx("circle_taiji", glow, size, size)


def gen_circle_runes():
    size = 256
    col = (255, 236, 190)
    img = circle_base(size, [(0.95, 0.985, 0.01), (0.80, 0.815, 0.01), (0.56, 0.575, 0.01), (0.20, 0.215, 0.01)], col)
    S = T.SS
    d = ImageDraw.Draw(img)
    c = size * S / 2
    # Hexagram.
    for k in range(2):
        pts = [(c + math.cos(math.pi / 2 + k * math.pi / 3 + i * 2 * math.pi / 3) * c * 0.56,
                c + math.sin(math.pi / 2 + k * math.pi / 3 + i * 2 * math.pi / 3) * c * 0.56) for i in range(3)]
        d.polygon(pts, outline=col + (255,), width=3 * S)
    glyph_ring(img, size, 0.875, 16, col, 5.5, 1.6)
    glyph_ring(img, size, 0.38, 8, col, 6, 1.6, rot=math.pi / 8)
    # inner sun
    d.ellipse([c - c * 0.14, c - c * 0.14, c + c * 0.14, c + c * 0.14], outline=col + (255,), width=3 * S)
    glow = img.filter(ImageFilter.GaussianBlur(3 * S))
    glow.alpha_composite(img)
    out_fx("circle_runes", glow, size, size)


def gen_circle_thunder():
    size = 256
    col = (230, 210, 255)
    img = circle_base(size, [(0.95, 0.99, 0.01), (0.70, 0.72, 0.01), (0.30, 0.33, 0.01)], col)
    S = T.SS
    d = ImageDraw.Draw(img)
    c = size * S / 2
    # Nine-pointed star of straight lines.
    n = 9
    pts = [(c + math.cos(-math.pi / 2 + i * 2 * math.pi / n) * c * 0.70, c + math.sin(-math.pi / 2 + i * 2 * math.pi / n) * c * 0.70) for i in range(n)]
    for i in range(n):
        d.line([pts[i], pts[(i + 4) % n]], fill=col + (255,), width=3 * S)
    # Zig-zag lightning glyphs around the outer band.
    rnd = random.Random(7)
    for i in range(12):
        a = i * math.pi / 6
        x0 = c + math.cos(a) * c * 0.74
        y0 = c + math.sin(a) * c * 0.74
        x1 = c + math.cos(a) * c * 0.93
        y1 = c + math.sin(a) * c * 0.93
        lp = T.lightning_points(x0 / S, y0 / S, x1 / S, y1 / S, 5, 5, rnd)
        T.polyline_glow(img, lp, col, 1.8, glow=1.5)
    d.ellipse([c - c * 0.1, c - c * 0.1, c + c * 0.1, c + c * 0.1], fill=col + (255,))
    glow = img.filter(ImageFilter.GaussianBlur(3 * S))
    glow.alpha_composite(img)
    out_fx("circle_thunder", glow, size, size)


def gen_circle_ice():
    size = 256
    col = (225, 248, 255)
    img = circle_base(size, [(0.95, 0.985, 0.01), (0.62, 0.64, 0.01)], col)
    S = T.SS
    d = ImageDraw.Draw(img)
    c = size * S / 2
    # Six-fold snowflake.
    for i in range(6):
        a = i * math.pi / 3
        ex = c + math.cos(a) * c * 0.92
        ey = c + math.sin(a) * c * 0.92
        d.line([(c, c), (ex, ey)], fill=col + (255,), width=4 * S)
        for f in (0.35, 0.55, 0.75):
            bx = c + math.cos(a) * c * f
            by = c + math.sin(a) * c * f
            L = c * 0.12 * (1.2 - f)
            for sgn in (-1, 1):
                b = a + sgn * math.pi / 3
                d.line([(bx, by), (bx + math.cos(b) * L, by + math.sin(b) * L)], fill=col + (255,), width=3 * S)
    hexpts = [(c + math.cos(math.pi / 6 + i * math.pi / 3) * c * 0.35, c + math.sin(math.pi / 6 + i * math.pi / 3) * c * 0.35) for i in range(6)]
    d.polygon(hexpts, outline=col + (255,), width=3 * S)
    hexpts = [(c + math.cos(i * math.pi / 3) * c * 0.78, c + math.sin(i * math.pi / 3) * c * 0.78) for i in range(6)]
    d.polygon(hexpts, outline=col + (255,), width=3 * S)
    glow = img.filter(ImageFilter.GaussianBlur(3 * S))
    glow.alpha_composite(img)
    out_fx("circle_ice", glow, size, size)


def gen_beam():
    # 64 wide (wraps around), 256 tall (scrolls). Streaks of energy.
    w, h = 64, 256
    noise = T.tileable_noise(w, h, 21, scale=2, octaves=3)
    # stretch vertically: streaks
    streak = noise.resize((w * T.SS, h * T.SS // 8), Image.BILINEAR).resize((w * T.SS, h * T.SS), Image.BILINEAR)
    img = T.colorize(streak, WHITE, 0.85)
    base = Image.new("RGBA", img.size, WHITE + (110,))
    base.alpha_composite(img)
    out_fx("beam", base, w, h)


def gen_beam_core():
    w, h = 32, 128
    noise = T.tileable_noise(w, h, 5, scale=1, octaves=2)
    streak = noise.resize((w * T.SS, h * T.SS // 6), Image.BILINEAR).resize((w * T.SS, h * T.SS), Image.BILINEAR)
    base = Image.new("RGBA", (w * T.SS, h * T.SS), WHITE + (200,))
    base.alpha_composite(T.colorize(streak, WHITE, 0.5))
    out_fx("beam_core", base, w, h)


def gen_slash():
    # u along the arc (x), v radial (y): bright band near the outer edge (top), fading inward.
    import numpy as np
    w, h = 128, 64
    W, H = w * T.SS, h * T.SS
    ys = np.linspace(0, 1, H, dtype=np.float32)[:, None]
    xs = np.linspace(0, 1, W, dtype=np.float32)[None, :]
    edge = np.exp(-((ys - 0.10) / 0.09) ** 2)
    body = np.clip(1.0 - (ys - 0.1) / 0.9, 0, 1) ** 2.2 * 0.75
    taper = np.clip(np.sin(xs * np.pi), 0, 1) ** 0.35
    a = np.clip((edge + body) * taper, 0, 1)
    arr = np.zeros((H, W, 4), dtype=np.float32)
    arr[..., :3] = 255
    arr[..., 3] = a * 255
    img = Image.fromarray(arr.astype(np.uint8), "RGBA")
    out_fx("slash", img, w, h)


def gen_wind_blade():
    import numpy as np
    w, h = 128, 64
    W, H = w * T.SS, h * T.SS
    ys = np.linspace(0, 1, H, dtype=np.float32)[:, None]
    xs = np.linspace(0, 1, W, dtype=np.float32)[None, :]
    edge = np.exp(-((ys - 0.12) / 0.07) ** 2)
    streaks = (np.sin(ys * 60 + xs * 8) * 0.5 + 0.5) ** 3 * 0.5
    body = np.clip(1.0 - (ys - 0.12) / 0.7, 0, 1) ** 1.5
    a = np.clip((edge + body * streaks) * np.clip(np.sin(xs * np.pi), 0, 1) ** 0.6, 0, 1)
    arr = np.zeros((H, W, 4), dtype=np.float32)
    arr[..., :3] = 255
    arr[..., 3] = a * 255
    out_fx("wind_blade", Image.fromarray(arr.astype(np.uint8), "RGBA"), w, h)


def gen_hex_shield():
    # tileable hexagon grid with glowing edges, 128x128.
    size = 128
    S = T.SS
    img = T.new(size, size)
    d = ImageDraw.Draw(img)
    W = size * S
    cols = 4
    r = W / cols / math.sqrt(3)  # hex radius so that 4 hexes fit horizontally
    hx = math.sqrt(3) * r
    hy = 1.5 * r
    rows = int(round(W / hy))
    r = W / rows / 1.5  # make it tile vertically exactly
    hx = math.sqrt(3) * r
    hy = 1.5 * r
    cols = int(round(W / hx))
    r = W / cols / math.sqrt(3)
    hx = math.sqrt(3) * r
    hy = 1.5 * r
    for row in range(-1, rows + 2):
        for col in range(-1, cols + 2):
            cx = col * hx + (hx / 2 if row % 2 else 0)
            cy = row * hy
            pts = [(cx + math.cos(math.pi / 6 + i * math.pi / 3) * r * 0.92, cy + math.sin(math.pi / 6 + i * math.pi / 3) * r * 0.92) for i in range(6)]
            d.polygon(pts, outline=WHITE + (255,), width=2 * S)
            d.polygon([(cx + (p[0] - cx) * 0.55, cy + (p[1] - cy) * 0.55) for p in pts], fill=WHITE + (35,))
    glow = img.filter(ImageFilter.GaussianBlur(2 * S))
    glow.alpha_composite(img)
    out_fx("hex_shield", glow, size, size)


def gen_flame_column():
    import numpy as np
    w, h = 64, 128
    n = T.tileable_noise(w, h, 33, scale=3, octaves=4)
    arr = np.asarray(n, dtype=np.float32) / 255.0
    H, W = arr.shape
    ys = np.linspace(0, 1, H, dtype=np.float32)[:, None]
    # tongues: threshold noise, denser near bottom (v=1)
    a = np.clip((arr - 0.35 + (1 - ys) * 0.15) * 2.2, 0, 1)
    rgba = np.zeros((H, W, 4), dtype=np.float32)
    rgba[..., 0] = 255
    rgba[..., 1] = 150 + 105 * a
    rgba[..., 2] = 60 + 160 * a * a
    rgba[..., 3] = a * 255
    out_fx("flame_column", Image.fromarray(rgba.astype(np.uint8), "RGBA"), w, h)


def gen_crack():
    # horizontal strip 128x32, glowing fissure in the middle, tileable horizontally.
    w, h = 128, 32
    S = T.SS
    img = T.new(w, h)
    rnd = random.Random(4)
    pts = T.lightning_points(0, h / 2, w, h / 2, 14, 5, rnd)
    pts[0] = (0, h / 2)
    pts[-1] = (w, h / 2)
    T.polyline_glow(img, pts, (255, 200, 120), 4.5, glow=5.0)
    for i in range(6):
        i0 = rnd.randint(2, len(pts) - 3)
        x0, y0 = pts[i0]
        L = rnd.uniform(5, 10)
        a = rnd.uniform(-1.2, 1.2) + (math.pi / 2 if rnd.random() < 0.5 else -math.pi / 2)
        T.polyline_glow(img, [(x0, y0), (x0 + math.cos(a) * L, y0 + math.sin(a) * L)], (255, 200, 120), 1.6, glow=2.0)
    out_fx("crack", img, w, h)


def gen_vortex():
    import numpy as np
    size = 128
    W = size * T.SS
    d = T._dist_grid(W, W) / (W / 2)
    ys, xs = np.mgrid[0:W, 0:W].astype(np.float32)
    ang = np.arctan2(ys - W / 2, xs - W / 2)
    arms = 3
    spiral = np.sin(ang * arms + d * 14.0) * 0.5 + 0.5
    a = spiral ** 2.5 * np.clip(1 - d, 0, 1) ** 0.7 * np.clip(d * 6, 0, 1)
    a = np.clip(a * 1.4, 0, 1)
    arr = np.zeros((W, W, 4), dtype=np.float32)
    arr[..., :3] = 255
    arr[..., 3] = a * 255
    out_fx("vortex", Image.fromarray(arr.astype(np.uint8), "RGBA"), size, size)


def gen_cloud():
    import numpy as np
    size = 256
    n = T.noise_layer(size, size, 61, scale=3, octaves=5)
    arr = np.asarray(n, dtype=np.float32) / 255.0
    W = arr.shape[0]
    d = T._dist_grid(W, W) / (W / 2)
    a = np.clip((arr * 0.9 + 0.45 - d * 1.05) * 2.0, 0, 1) * np.clip((1.0 - d) * 4, 0, 1)
    rgba = np.zeros((W, W, 4), dtype=np.float32)
    shade = 0.55 + 0.45 * arr
    rgba[..., 0] = 255 * shade
    rgba[..., 1] = 255 * shade
    rgba[..., 2] = 255 * shade
    rgba[..., 3] = a * 255
    out_fx("cloud", Image.fromarray(rgba.astype(np.uint8), "RGBA"), size, size)


def gen_glyph_strip():
    # 8 glyphs, each 32x32, in a 256x32 strip.
    w, h = 256, 32
    S = T.SS
    img = T.new(w, h)
    d = ImageDraw.Draw(img)
    for i in range(8):
        glyph_stroke(d, (i * 32 + 16) * S, 16 * S, 11 * S, i, WHITE, 3 * S)
    glow = img.filter(ImageFilter.GaussianBlur(2 * S))
    glow.alpha_composite(img)
    out_fx("glyph_strip", glow, w, h)


def gen_petal():
    # vertical petal: pointed top, round bottom, bright veins. 64x64 (u across, v top->bottom).
    import numpy as np
    size = 64
    W = size * T.SS
    ys, xs = np.mgrid[0:W, 0:W].astype(np.float32) / W
    cx = 0.5
    half = 0.42 * np.sin(np.clip(ys, 0, 1) * np.pi) ** 0.8 * (1 - ys * 0.25)
    inside = np.abs(xs - cx) < half
    edge = 1 - np.clip(np.abs(xs - cx) / np.maximum(half, 1e-4), 0, 1)
    vein = np.exp(-((xs - cx) / 0.02) ** 2)
    a = np.where(inside, 1.0, 0.0)
    shade = 0.55 + 0.45 * edge
    rgba = np.zeros((W, W, 4), dtype=np.float32)
    rgba[..., 0] = 255
    rgba[..., 1] = (140 + 100 * edge + 60 * vein).clip(0, 255)
    rgba[..., 2] = (80 + 60 * edge ** 2 + 80 * vein).clip(0, 255)
    rgba[..., 3] = a * 255 * (0.85 + 0.15 * shade)
    img = Image.fromarray(rgba.astype(np.uint8), "RGBA").filter(ImageFilter.GaussianBlur(T.SS * 0.6))
    out_fx("petal", img, size, size)


def gen_frost():
    # tileable ice pattern: crystalline cracks over a frosty noise.
    size = 128
    S = T.SS
    n = T.tileable_noise(size, size, 71, scale=4, octaves=3)
    img = T.colorize(n, (225, 245, 255), 0.55)
    base = Image.new("RGBA", img.size, (200, 235, 255, 90))
    base.alpha_composite(img)
    rnd = random.Random(9)
    for k in range(10):
        x0, y0 = rnd.uniform(0, size), rnd.uniform(0, size)
        a = rnd.uniform(0, math.pi)
        L = rnd.uniform(25, 60)
        x1, y1 = x0 + math.cos(a) * L, y0 + math.sin(a) * L
        pts = T.lightning_points(x0, y0, x1, y1, 5, 3, rnd)
        for dx in (-size, 0, size):
            for dy in (-size, 0, size):
                T.polyline_glow(base, [(x + dx, y + dy) for x, y in pts], (240, 252, 255), 1.4, glow=1.2, core=False)
    out_fx("frost", base, size, size)


def gen_pillar():
    # 64x256 tileable light streaks (soft vertical bands).
    import numpy as np
    w, h = 64, 256
    n = T.tileable_noise(w, h, 91, scale=2, octaves=2)
    streak = n.resize((w * T.SS, h * T.SS // 16), Image.BILINEAR).resize((w * T.SS, h * T.SS), Image.BILINEAR)
    arr = np.asarray(streak, dtype=np.float32) / 255.0
    a = np.clip((arr - 0.3) * 1.8, 0, 1) ** 1.3
    rgba = np.zeros(arr.shape + (4,), dtype=np.float32)
    rgba[..., :3] = 255
    rgba[..., 3] = (0.25 + 0.75 * a) * 255
    out_fx("pillar", Image.fromarray(rgba.astype(np.uint8), "RGBA"), w, h)


def gen_ice_spike():
    # crystal facet: tapered spike, bright edges. 64x64 (v: 0 top = tip, 1 bottom = base)
    import numpy as np
    size = 64
    W = size * T.SS
    ys, xs = np.mgrid[0:W, 0:W].astype(np.float32) / W
    half = 0.02 + 0.44 * ys ** 0.9
    inside = np.abs(xs - 0.5) < half
    edge = 1 - np.clip(np.abs(xs - 0.5) / np.maximum(half, 1e-4), 0, 1)
    facet = (np.sin((xs - 0.5) / np.maximum(half, 1e-4) * math.pi * 1.5) * 0.5 + 0.5) * 0.35
    rgba = np.zeros((W, W, 4), dtype=np.float32)
    shade = 0.6 + 0.4 * (edge ** 0.5) + facet
    rgba[..., 0] = (190 + 60 * shade).clip(0, 255)
    rgba[..., 1] = (230 + 25 * shade).clip(0, 255)
    rgba[..., 2] = 255
    rgba[..., 3] = np.where(inside, 255, 0)
    out_fx("ice_spike", Image.fromarray(rgba.astype(np.uint8), "RGBA"), size, size)


# ------------------------------------------------------------------------ particles (32x32)

def p_glow():
    img = T.radial(32, 32, (255, 255, 255, 255), (255, 255, 255, 0), power=1.4)
    out_particle("glow", img)


def p_spark():
    img = T.new(32, 32)
    T.polygon_glow(img, T.star_points(16, 16, 15, 2.5, 4), WHITE, glow=2.0)
    img.alpha_composite(T.radial(32, 32, (255, 255, 255, 255), (255, 255, 255, 0), power=2.5, radius=8))
    out_particle("spark", img)


def p_flame_wisp():
    rnd = random.Random(12)
    for f in range(8):
        img = T.new(32, 32)
        t = f / 7.0
        # flame tongue: base ellipse + wobbling tip
        h = 26 - 8 * t
        pts = []
        n = 24
        for i in range(n + 1):
            u = i / n
            a = math.pi * u
            wobble = math.sin(u * 6 + f * 0.9) * 1.8 * (1 - t)
            x = 16 + math.cos(a) * (9 - 4 * t) * (1 - u * 0.15) + wobble * (1 - abs(u - 0.5) * 2)
            y = 28 - math.sin(a) * h
            pts.append((x, y))
        col = T.lerp_rgb((255, 200, 90), (255, 90, 30), t)
        T.polygon_glow(img, pts, col, glow=2.5, fill_alpha=230)
        inner = [(16 + (x - 16) * 0.5, 28 + (y - 28) * 0.6) for x, y in pts]
        T.polygon_glow(img, inner, (255, 240, 180), glow=1.0, fill_alpha=200)
        out_particle("flame_wisp_%d" % f, img)


def p_ember():
    img = T.radial(32, 32, (255, 220, 160, 255), (255, 120, 40, 0), power=1.2, radius=10)
    core = T.radial(32, 32, (255, 255, 255, 255), (255, 255, 255, 0), power=2.0, radius=4)
    img.alpha_composite(core)
    out_particle("ember", img)


def p_ice_crystal():
    img = T.new(32, 32)
    pts = [(16, 2), (22, 12), (19, 30), (13, 30), (10, 12)]
    T.polygon_glow(img, pts, (210, 240, 255), glow=1.5, fill_alpha=235, outline=(255, 255, 255))
    d = ImageDraw.Draw(img)
    S = T.SS
    d.line([(16 * S, 2 * S), (16 * S, 30 * S)], fill=(255, 255, 255, 200), width=S)
    d.line([(10 * S, 12 * S), (22 * S, 12 * S)], fill=(255, 255, 255, 150), width=S)
    out_particle("ice_crystal", img)


def p_snowflake():
    img = T.new(32, 32)
    d = ImageDraw.Draw(img)
    S = T.SS
    c = 16 * S
    for i in range(6):
        a = i * math.pi / 3
        ex, ey = c + math.cos(a) * 14 * S, c + math.sin(a) * 14 * S
        d.line([(c, c), (ex, ey)], fill=(240, 250, 255, 255), width=2 * S)
        for f in (0.5, 0.75):
            bx, by = c + math.cos(a) * 14 * S * f, c + math.sin(a) * 14 * S * f
            for sgn in (-1, 1):
                b = a + sgn * math.pi / 3
                d.line([(bx, by), (bx + math.cos(b) * 4 * S, by + math.sin(b) * 4 * S)], fill=(240, 250, 255, 255), width=int(1.5 * S))
    glow = img.filter(ImageFilter.GaussianBlur(1.2 * S))
    glow.alpha_composite(img)
    out_particle("snowflake", glow)


def p_frost_mist():
    n = T.noise_layer(32, 32, 44, scale=2, octaves=3)
    img = T.colorize(n, (220, 240, 255), 0.9)
    mask = T.radial(32, 32, (0, 0, 0, 255), (0, 0, 0, 0), power=1.3)
    img.putalpha(ImageChops.multiply(img.getchannel("A"), mask.getchannel("A")))
    out_particle("frost_mist", img)


def p_lightning_arc():
    rnd = random.Random(99)
    for f in range(4):
        img = T.new(32, 32)
        pts = T.lightning_points(3, 16 + rnd.uniform(-6, 6), 29, 16 + rnd.uniform(-6, 6), 7, 6, rnd)
        T.polyline_glow(img, pts, (200, 170, 255), 3.0, glow=2.5)
        i0 = rnd.randint(2, 4)
        x0, y0 = pts[i0]
        T.polyline_glow(img, [(x0, y0), (x0 + rnd.uniform(-8, 8), y0 + rnd.uniform(-9, 9))], (200, 170, 255), 1.8, glow=1.5)
        out_particle("lightning_arc_%d" % f, img)


def p_wind_streak():
    import numpy as np
    W = 32 * T.SS
    ys, xs = np.mgrid[0:W, 0:W].astype(np.float32) / W
    a = np.exp(-((ys - 0.5) / 0.12) ** 2) * np.clip(np.sin(xs * math.pi), 0, 1) ** 0.7
    arr = np.zeros((W, W, 4), dtype=np.float32)
    arr[..., :3] = 255
    arr[..., 3] = a * 255
    out_particle("wind_streak", Image.fromarray(arr.astype(np.uint8), "RGBA"))


def p_void_smoke():
    for f in range(6):
        n = T.noise_layer(32, 32, 200 + f * 3, scale=2, octaves=3)
        img = T.colorize(n, (120, 70, 180), 1.0)
        mask = T.radial(32, 32, (0, 0, 0, 255), (0, 0, 0, 0), power=1.0 + f * 0.15)
        img.putalpha(ImageChops.multiply(img.getchannel("A"), mask.getchannel("A")))
        dark = T.radial(32, 32, (20, 5, 40, 200), (20, 5, 40, 0), power=1.8, radius=8)
        img.alpha_composite(dark)
        out_particle("void_smoke_%d" % f, img)


def p_lotus_petal():
    img = T.new(32, 32)
    pts = [(16, 2), (24, 10), (26, 20), (16, 30), (6, 20), (8, 10)]
    T.polygon_glow(img, pts, (255, 110, 60), glow=1.5, fill_alpha=240, outline=(255, 200, 120))
    d = ImageDraw.Draw(img)
    S = T.SS
    d.line([(16 * S, 4 * S), (16 * S, 28 * S)], fill=(255, 220, 150, 220), width=S)
    out_particle("lotus_petal", img)


def p_rune():
    for i in range(8):
        img = T.new(32, 32)
        d = ImageDraw.Draw(img)
        glyph_stroke(d, 16 * T.SS, 16 * T.SS, 11 * T.SS, i, (255, 235, 180), 3 * T.SS)
        glow = img.filter(ImageFilter.GaussianBlur(1.5 * T.SS))
        glow.alpha_composite(img)
        out_particle("rune_%d" % i, glow)


def p_sword_glint():
    img = T.new(32, 32)
    T.polygon_glow(img, T.star_points(16, 16, 15, 1.5, 4), (220, 250, 255), glow=1.5)
    T.polygon_glow(img, T.star_points(16, 16, 7, 1.2, 4, math.pi / 4), (255, 255, 255), glow=1.0)
    out_particle("sword_glint", img)


def p_rock_debris():
    img = T.new(32, 32)
    rnd = random.Random(3)
    pts = [(16 + math.cos(a) * rnd.uniform(9, 14), 16 + math.sin(a) * rnd.uniform(9, 14)) for a in [i * math.pi / 3.5 for i in range(7)]]
    T.polygon_glow(img, pts, (120, 100, 78), glow=0, fill_alpha=255, outline=(70, 58, 45))
    n = T.noise_layer(32, 32, 8, scale=3, octaves=2)
    shade = T.colorize(n, (60, 48, 36), 0.5)
    shade.putalpha(ImageChops.multiply(shade.getchannel("A"), img.getchannel("A")))
    img.alpha_composite(shade)
    out_particle("rock_debris", img)


def p_golden_light():
    img = T.radial(32, 32, (255, 240, 190, 255), (255, 210, 110, 0), power=1.3)
    T.polygon_glow(img, T.star_points(16, 16, 13, 2, 4), (255, 250, 220), glow=1.5, fill_alpha=200)
    out_particle("golden_light", img)


def main():
    os.makedirs(FX, exist_ok=True)
    os.makedirs(PART, exist_ok=True)
    gen_ring(); gen_glow_soft(); gen_sparkle()
    gen_circle_taiji(); gen_circle_runes(); gen_circle_thunder(); gen_circle_ice()
    gen_beam(); gen_beam_core(); gen_slash(); gen_wind_blade(); gen_hex_shield()
    gen_flame_column(); gen_crack(); gen_vortex(); gen_cloud(); gen_glyph_strip()
    gen_petal(); gen_frost(); gen_pillar(); gen_ice_spike()
    p_glow(); p_spark(); p_flame_wisp(); p_ember(); p_ice_crystal(); p_snowflake(); p_frost_mist()
    p_lightning_arc(); p_wind_streak(); p_void_smoke(); p_lotus_petal(); p_rune(); p_sword_glint()
    p_rock_debris(); p_golden_light()


if __name__ == "__main__":
    main()
