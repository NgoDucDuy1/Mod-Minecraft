"""Shared drawing helpers for the Celestial Arts texture generators.

Everything is drawn at a super-sampling factor (SS) and downscaled with LANCZOS so
curves and glows stay smooth even at 32x32.
"""
import math
import random

from PIL import Image, ImageDraw, ImageFilter, ImageChops

SS = 4


def new(w, h, color=(0, 0, 0, 0)):
    return Image.new("RGBA", (w * SS, h * SS), color)


def finish(img, w, h):
    return img.resize((w, h), Image.LANCZOS)


def hexrgb(rgb):
    return ((rgb >> 16) & 255, (rgb >> 8) & 255, rgb & 255)


def lerp(a, b, t):
    return a + (b - a) * t


def lerp_rgb(c1, c2, t):
    return tuple(int(round(lerp(c1[i], c2[i], t))) for i in range(3))


def _dist_grid(W, H, center=None):
    import numpy as np
    cx, cy = center if center else (W / 2.0, H / 2.0)
    ys, xs = np.mgrid[0:H, 0:W].astype(np.float32)
    return np.hypot(xs + 0.5 - cx, ys + 0.5 - cy)


def _rgba_from_alpha_t(t, inner, outer):
    import numpy as np
    t = np.clip(t, 0.0, 1.0)[..., None]
    a = np.array(inner, dtype=np.float32)
    b = np.array(outer, dtype=np.float32)
    arr = a + (b - a) * t
    return Image.fromarray(np.clip(arr + 0.5, 0, 255).astype(np.uint8), "RGBA")


def radial(w, h, inner, outer, power=1.0, radius=None, center=None, ss=True):
    """Radial gradient from inner colour (RGBA) at the centre to outer (RGBA) at radius."""
    import numpy as np
    W, H = (w * SS, h * SS) if ss else (w, h)
    R = radius * (SS if ss else 1) if radius else min(W, H) / 2.0
    c = (center[0] * (SS if ss else 1), center[1] * (SS if ss else 1)) if center else None
    d = _dist_grid(W, H, c) / R
    t = np.minimum(1.0, d) ** power
    return _rgba_from_alpha_t(t, inner, outer)


def ring_gradient(w, h, r_in, r_out, color, feather_in, feather_out, max_alpha=255):
    """Soft ring (annulus) with feathered edges. Radii are fractions of the half size."""
    import numpy as np
    W, H = w * SS, h * SS
    R = min(W, H) / 2.0
    d = _dist_grid(W, H) / R
    a = np.ones_like(d)
    a = np.where(d < r_in, (d - (r_in - feather_in)) / max(1e-6, feather_in), a)
    a = np.where(d > r_out, 1.0 - (d - r_out) / max(1e-6, feather_out), a)
    a = np.clip(a, 0.0, 1.0)
    arr = np.zeros((H, W, 4), dtype=np.float32)
    arr[..., 0] = color[0]
    arr[..., 1] = color[1]
    arr[..., 2] = color[2]
    arr[..., 3] = a * max_alpha
    return Image.fromarray(np.clip(arr + 0.5, 0, 255).astype(np.uint8), "RGBA")


def glow_blur(img, radius):
    return img.filter(ImageFilter.GaussianBlur(radius * SS))


def add(base, layer):
    """Additive-ish composite: alpha composite after brightening."""
    return Image.alpha_composite(base, layer)


def _grid_noise(cells, seed, tile):
    rnd = random.Random(seed)
    g = Image.new("L", (cells, cells))
    g.putdata([int(rnd.random() * 255) for _ in range(cells * cells)])
    if tile:
        big = Image.new("L", (cells * 3, cells * 3))
        for i in range(3):
            for j in range(3):
                big.paste(g, (i * cells, j * cells))
        return big
    return g


def noise_layer(w, h, seed, scale=4, octaves=3, ss=True, tile=False):
    """Smooth value noise in [0,255] as a greyscale image (super-sampled unless ss=False)."""
    import numpy as np
    W, H = (w * SS, h * SS) if ss else (w, h)
    acc = np.zeros((H, W), dtype=np.float32)
    amp = 1.0
    total = 0.0
    for o in range(octaves):
        cells = max(2, scale * (2 ** o))
        g = _grid_noise(cells, seed + o * 7919, tile)
        if tile:
            up = g.resize((W * 3, H * 3), Image.BICUBIC).crop((W, H, 2 * W, 2 * H))
        else:
            up = g.resize((W, H), Image.BICUBIC)
        acc += np.asarray(up, dtype=np.float32) * amp
        total += amp
        amp *= 0.5
    acc = np.clip(acc / total, 0, 255).astype(np.uint8)
    return Image.fromarray(acc, "L")


def tileable_noise(w, h, seed, scale=4, octaves=3):
    return noise_layer(w, h, seed, scale, octaves, tile=True)


def colorize(mask, color, alpha_scale=1.0):
    """Greyscale mask -> RGBA with the given colour and alpha = mask * alpha_scale."""
    W, H = mask.size
    img = Image.new("RGBA", (W, H), color + (0,))
    a = mask.point(lambda v: int(v * alpha_scale))
    img.putalpha(a)
    return img


def polyline_glow(img, pts, color, width, glow=2.0, core=True):
    """Draw a glowing polyline (super-sampled coordinates expected in texture units)."""
    W, H = img.size
    layer = Image.new("RGBA", (W, H))
    d = ImageDraw.Draw(layer)
    spts = [(x * SS, y * SS) for x, y in pts]
    d.line(spts, fill=color + (255,), width=int(width * SS), joint="curve")
    blurred = layer.filter(ImageFilter.GaussianBlur(glow * SS))
    img.alpha_composite(blurred)
    if core:
        core_layer = Image.new("RGBA", (W, H))
        dc = ImageDraw.Draw(core_layer)
        dc.line(spts, fill=(255, 255, 255, 255), width=max(1, int(width * SS * 0.4)), joint="curve")
        img.alpha_composite(core_layer)
    return img


def polygon_glow(img, pts, color, glow=2.0, fill_alpha=255, outline=None):
    W, H = img.size
    layer = Image.new("RGBA", (W, H))
    d = ImageDraw.Draw(layer)
    spts = [(x * SS, y * SS) for x, y in pts]
    d.polygon(spts, fill=color + (fill_alpha,))
    if glow > 0:
        img.alpha_composite(layer.filter(ImageFilter.GaussianBlur(glow * SS)))
    img.alpha_composite(layer)
    if outline:
        ol = Image.new("RGBA", (W, H))
        ImageDraw.Draw(ol).line(spts + [spts[0]], fill=outline + (255,), width=SS)
        img.alpha_composite(ol)
    return img


def ellipse_glow(img, box, color, glow=2.0, fill_alpha=255):
    W, H = img.size
    layer = Image.new("RGBA", (W, H))
    d = ImageDraw.Draw(layer)
    sbox = [box[0] * SS, box[1] * SS, box[2] * SS, box[3] * SS]
    d.ellipse(sbox, fill=color + (fill_alpha,))
    if glow > 0:
        img.alpha_composite(layer.filter(ImageFilter.GaussianBlur(glow * SS)))
    img.alpha_composite(layer)
    return img


def arc_glow(img, box, start, end, color, width, glow=2.0):
    W, H = img.size
    layer = Image.new("RGBA", (W, H))
    d = ImageDraw.Draw(layer)
    sbox = [box[0] * SS, box[1] * SS, box[2] * SS, box[3] * SS]
    d.arc(sbox, start, end, fill=color + (255,), width=int(width * SS))
    if glow > 0:
        img.alpha_composite(layer.filter(ImageFilter.GaussianBlur(glow * SS)))
    img.alpha_composite(layer)
    return img


def star_points(cx, cy, r_out, r_in, n, rot=0.0):
    pts = []
    for i in range(n * 2):
        r = r_out if i % 2 == 0 else r_in
        a = rot + i * math.pi / n
        pts.append((cx + math.cos(a) * r, cy + math.sin(a) * r))
    return pts


def crescent(cx, cy, r, thickness, rot, sweep=200):
    """Crescent polygon points (outer arc + inner arc offset)."""
    pts = []
    n = 32
    a0 = math.radians(rot - sweep / 2)
    a1 = math.radians(rot + sweep / 2)
    for i in range(n + 1):
        a = a0 + (a1 - a0) * i / n
        pts.append((cx + math.cos(a) * r, cy + math.sin(a) * r))
    for i in range(n, -1, -1):
        a = a0 + (a1 - a0) * i / n
        t = math.sin((i / n) * math.pi)  # thick in the middle, thin at the tips
        rr = r - thickness * t
        pts.append((cx + math.cos(a) * rr, cy + math.sin(a) * rr))
    return pts


def lightning_points(x0, y0, x1, y1, segments, jitter, rnd):
    pts = [(x0, y0)]
    dx, dy = x1 - x0, y1 - y0
    L = math.hypot(dx, dy)
    nx, ny = -dy / L, dx / L
    for i in range(1, segments):
        t = i / segments
        off = (rnd.random() - 0.5) * 2 * jitter * math.sin(t * math.pi) ** 0.5
        pts.append((x0 + dx * t + nx * off, y0 + dy * t + ny * off))
    pts.append((x1, y1))
    return pts


def save(img, path):
    import os
    os.makedirs(os.path.dirname(path), exist_ok=True)
    img.save(path)
