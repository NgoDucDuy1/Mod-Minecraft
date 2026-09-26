#!/usr/bin/env python3
"""Generates entity textures matching the UV layouts of the models in
src/client/java/com/ngoducduy/celestialarts/client/render/entity/model.

    python3 tools/gen_entity_textures.py
"""
import math
import os
import random
import sys

import numpy as np
from PIL import Image, ImageDraw

sys.path.insert(0, os.path.dirname(__file__))

ROOT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "celestialarts", "textures", "entity")

SHADE = {"top": 1.0, "bottom": 0.62, "front": 0.88, "back": 0.80, "left": 0.72, "right": 0.72}


class Tex:
    def __init__(self, w, h, density=1):
        """w x h is the UV space of the model; density multiplies the pixel resolution (a
        density-4 256x128 skin is a 1024x512 PNG with the same UV layout – Minecraft samples
        normalised UVs, so world-scale props get four times the detail per model unit)."""
        self.w, self.h = w, h
        self.density = density
        self.arr = np.zeros((h * density, w * density, 4), dtype=np.float32)

    def face_regions(self, u, v, w, h, d):
        k = self.density
        return {
            "top": ((u + d) * k, v * k, w * k, d * k),
            "bottom": ((u + d + w) * k, v * k, w * k, d * k),
            "right": (u * k, (v + d) * k, d * k, h * k),
            "front": ((u + d) * k, (v + d) * k, w * k, h * k),
            "left": ((u + d + w) * k, (v + d) * k, d * k, h * k),
            "back": ((u + 2 * d + w) * k, (v + d) * k, w * k, h * k),
        }

    def cuboid(self, u, v, w, h, d, painter, shade=True):
        """painter(face, fx, fy) -> (r,g,b,a) with fx, fy in [0,1] across the face."""
        for face, (x0, y0, fw, fh) in self.face_regions(u, v, w, h, d).items():
            if fw <= 0 or fh <= 0:
                continue
            s = SHADE[face] if shade else 1.0
            for yy in range(fh):
                for xx in range(fw):
                    fx = (xx + 0.5) / fw
                    fy = (yy + 0.5) / fh
                    r, g, b, a = painter(face, fx, fy)
                    self.arr[y0 + yy, x0 + xx] = (r * s, g * s, b * s, a)

    def save(self, name):
        img = Image.fromarray(np.clip(self.arr + 0.5, 0, 255).astype(np.uint8), "RGBA")
        os.makedirs(ROOT, exist_ok=True)
        img.save(os.path.join(ROOT, name + ".png"))
        print("entity/" + name)


def lerp(a, b, t):
    return a + (b - a) * t


def mix(c1, c2, t):
    t = max(0.0, min(1.0, t))
    return tuple(lerp(c1[i], c2[i], t) for i in range(3))


def with_a(c, a=255):
    return (c[0], c[1], c[2], a)


def hash_noise(x, y, seed=0):
    n = math.sin(x * 127.1 + y * 311.7 + seed * 74.7) * 43758.5453
    return n - math.floor(n)


# ------------------------------------------------------------------- painters

def energy_blade(base, hot, along="fy", edge_axis="fx", alpha=255):
    """Bright core line along the blade with hot colour, fading to base at the sides."""
    def p(face, fx, fy):
        e = {"fx": fx, "fy": fy}[edge_axis]
        c = mix(base, hot, math.exp(-((e - 0.5) / 0.22) ** 2))
        return with_a(c, alpha)
    return p


def sword_qi():
    t = Tex(64, 32)
    cyan = (120, 220, 255)
    white = (255, 255, 255)
    # core: horizontal (w=24 along x), edge is along z (d). On top face fx = along w, fy = along d.
    def core(face, fx, fy):
        if face in ("top", "bottom"):
            c = mix(cyan, white, math.exp(-((fy - 0.75) / 0.25) ** 2))  # hot line near the cutting edge (+z)
            c = mix(c, (60, 170, 255), abs(fx - 0.5) * 0.8)
            return with_a(c, 235)
        return with_a(mix(cyan, white, 0.4), 220)
    t.cuboid(0, 0, 24, 2, 6, core)

    def wing(face, fx, fy):
        if face in ("top", "bottom"):
            fade = 1.0 - abs(fx - 0.5) * 0.4
            c = mix(cyan, white, math.exp(-((fy - 0.7) / 0.3) ** 2) * 0.7)
            return with_a(c, 210 * fade)
        return with_a(cyan, 190)
    t.cuboid(0, 8, 12, 2, 4, wing)
    t.cuboid(32, 8, 12, 2, 4, wing)

    def edge(face, fx, fy):
        return with_a(white, 255)
    t.cuboid(0, 14, 26, 1, 1, edge, shade=False)
    t.save("sword_qi")


def ice_shard():
    t = Tex(32, 32)
    ice = (200, 236, 255)
    deep = (130, 195, 245)
    white = (245, 252, 255)

    def body(face, fx, fy):
        n = hash_noise(int(fx * 10), int(fy * 10), 1)
        c = mix(deep, ice, 0.5 + 0.5 * n)
        if face in ("top", "bottom", "left", "right", "front", "back"):
            c = mix(c, white, math.exp(-((fy - 0.85) / 0.12) ** 2) * 0.8)  # brighter towards the tip
        return with_a(c, 235)
    t.cuboid(0, 0, 2, 2, 10, body)
    t.cuboid(0, 12, 1, 1, 3, lambda f, fx, fy: with_a(white, 255), shade=False)

    def facet(face, fx, fy):
        c = mix(deep, white, 0.35 + 0.65 * abs(fx - 0.5) * 2)
        return with_a(c, 170)
    t.cuboid(0, 16, 4, 1, 6, facet)
    t.cuboid(0, 23, 4, 1, 6, facet)
    t.save("ice_shard")


def fire_lotus():
    t = Tex(64, 64)
    yellow = (255, 240, 170)
    orange = (255, 140, 40)
    red = (220, 40, 20)

    def core(face, fx, fy):
        d = math.hypot(fx - 0.5, fy - 0.5) * 2
        return with_a(mix((255, 255, 240), yellow, d), 255)
    t.cuboid(0, 0, 6, 6, 6, core, shade=False)

    def petal(face, fx, fy):
        # top/bottom: fx across the petal, fy along its length (base at fy=0, tip fy=1).
        if face in ("top", "bottom"):
            c = mix(yellow, orange, fy * 1.4)
            c = mix(c, red, max(0.0, fy - 0.55) * 2.2)
            c = mix(c, red, abs(fx - 0.5) * 1.2)  # darker rims
            vein = math.exp(-((fx - 0.5) / 0.09) ** 2)
            c = mix(c, yellow, vein * (1 - fy) * 0.8)
            return with_a(c, 255)
        return with_a(mix(orange, red, 0.5), 255)
    t.cuboid(0, 12, 6, 1, 9, petal, shade=False)
    t.cuboid(0, 22, 4, 1, 6, petal, shade=False)
    t.save("fire_lotus")


def sword_painter(blade_light, blade_dark, glow, guard, handle, pommel, wrap_period=3):
    def blade(face, fx, fy):
        if face in ("top", "bottom"):
            # fx across blade width, fy along length (0 = hilt, 1 = tip)
            e = abs(fx - 0.5) * 2
            c = mix(blade_light, blade_dark, e * 0.7)
            c = mix(c, glow, math.exp(-((fx - 0.5) / 0.16) ** 2) * 0.9)
            c = mix(c, (255, 255, 255), max(0.0, fy - 0.9) * 8)  # white tip
            return with_a(c, 255)
        return with_a(mix(blade_light, (255, 255, 255), 0.4), 255)

    def fuller(face, fx, fy):
        return with_a(mix(glow, (255, 255, 255), 0.35), 255)

    def guard_p(face, fx, fy):
        c = mix(guard, (255, 255, 255), math.exp(-((fy - 0.35) / 0.25) ** 2) * 0.35)
        c = mix(c, (0, 0, 0), max(0.0, fy - 0.7) * 0.8)
        return with_a(c, 255)

    def handle_p(face, fx, fy):
        band = (math.sin(fy * math.pi * wrap_period * 2) * 0.5 + 0.5)
        c = mix(handle, (0, 0, 0), band * 0.45)
        return with_a(c, 255)

    def pommel_p(face, fx, fy):
        d = math.hypot(fx - 0.5, fy - 0.5) * 2
        return with_a(mix((255, 255, 255), pommel, min(1.0, d * 1.3 + 0.2)), 255)
    return blade, fuller, guard_p, handle_p, pommel_p


def spirit_sword():
    t = Tex(64, 32)
    blade, fuller, guard, handle, pommel = sword_painter((235, 245, 255), (150, 190, 220), (150, 240, 255), (230, 190, 90), (60, 40, 90), (150, 240, 255))
    t.cuboid(0, 0, 3, 1, 14, blade)
    t.cuboid(0, 16, 6, 2, 2, guard)
    t.cuboid(16, 16, 1, 1, 5, handle)
    t.cuboid(34, 0, 2, 2, 2, pommel)
    t.save("spirit_sword")


def flying_sword():
    t = Tex(64, 64)
    blade, fuller, guard, handle, pommel = sword_painter((240, 246, 255), (140, 180, 215), (160, 235, 255), (240, 200, 100), (40, 30, 70), (120, 220, 255), wrap_period=4)
    t.cuboid(0, 0, 6, 1, 26, blade)
    t.cuboid(0, 27, 2, 2, 20, fuller)
    t.cuboid(0, 49, 12, 3, 3, guard)
    t.cuboid(30, 49, 2, 2, 6, handle)
    t.cuboid(46, 49, 3, 3, 2, pommel)
    t.save("flying_sword")

    g = Tex(64, 64)
    def glow_blade(face, fx, fy):
        if face in ("top", "bottom"):
            a = math.exp(-((fx - 0.5) / 0.3) ** 2) * 255
            return (160, 235, 255, a)
        return (160, 235, 255, 180)
    g.cuboid(0, 0, 6, 1, 26, glow_blade, shade=False)
    g.cuboid(0, 27, 2, 2, 20, lambda f, fx, fy: (220, 250, 255, 255), shade=False)
    g.cuboid(46, 49, 3, 3, 2, lambda f, fx, fy: (160, 235, 255, 200), shade=False)
    g.save("flying_sword_glow")


def rock_spike():
    t = Tex(64, 64)
    stone = (128, 108, 82)
    dark = (70, 56, 42)
    light = (170, 150, 118)
    rnd = random.Random(5)

    def rock(face, fx, fy):
        n = hash_noise(int(fx * 12), int(fy * 12), 3)
        n2 = hash_noise(int(fx * 4), int(fy * 4), 9)
        c = mix(dark, light, 0.35 + 0.4 * n + 0.25 * n2)
        crack = hash_noise(int(fx * 12) * 3, int(fy * 12) * 5, 11) > 0.93
        if crack:
            c = mix(c, (255, 190, 90), 0.75)  # glowing molten cracks
        return with_a(c, 255)
    t.cuboid(0, 0, 12, 8, 12, rock)
    t.cuboid(0, 20, 8, 10, 8, rock)
    t.cuboid(32, 20, 5, 10, 5, rock)

    def tip(face, fx, fy):
        c = mix(light, (255, 220, 150), fy * 0.4)
        return with_a(c, 255)
    t.cuboid(0, 38, 2, 8, 2, tip)
    t.save("rock_spike")


def heaven_sword():
    t = Tex(128, 128)
    blade, fuller, guard, handle, pommel = sword_painter((255, 250, 230), (230, 200, 130), (255, 240, 190), (255, 210, 90), (140, 30, 40), (255, 235, 150), wrap_period=6)
    t.cuboid(0, 0, 6, 1, 48, blade)
    t.cuboid(0, 49, 2, 2, 40, fuller)
    t.cuboid(0, 91, 16, 3, 3, guard)
    t.cuboid(38, 91, 2, 2, 9, handle)
    t.cuboid(60, 91, 4, 4, 3, pommel)
    t.save("heaven_sword")


def heaven_hand():
    """1024x512 skin (UV space 256x128, density 4) for HeavenHandModel (Thiên Đạo Chi Thủ): pale
    luminous golden jade with dark mineral veins, bevelled joints, bright dao seams, a scripture seal
    in the palm and a sun seal on the back of the hand. The model is drawn in world orientation, so
    the 'top' UV region (-Y) is the palm surface that faces the ground."""
    import texlib as T
    t = Tex(256, 128, density=4)
    base = (244, 202, 112)
    deep = (160, 104, 36)
    dark = (98, 58, 18)
    hot = (255, 240, 190)
    seam = (255, 226, 130)
    # Painters also accumulate an emissive amount in EMIT[0]; a second pass writes only that into
    # heaven_hand_glow.png (the seams/seals that burn from within on the client).
    EMIT = [0.0]
    mode = {"emit": False}

    def out(c, e):
        if mode["emit"]:
            return (255, 236, 170, int(255 * max(0.0, min(1.0, e))))
        return with_a(c, 255)
    # Smooth noise fields sampled by normalised face coordinates: body tone, veins, fine grain.
    tone = np.asarray(T.noise_layer(256, 256, 41, scale=3, octaves=4, ss=False), dtype=np.float32) / 255.0
    vein = np.asarray(T.noise_layer(256, 256, 87, scale=5, octaves=3, ss=False), dtype=np.float32) / 255.0
    grain = np.asarray(T.noise_layer(256, 256, 19, scale=24, octaves=2, ss=False), dtype=np.float32) / 255.0

    def sample(field, fx, fy, ox, oy, rep=1.0):
        x = int(((fx * rep + ox) % 1.0) * 255)
        y = int(((fy * rep + oy) % 1.0) * 255)
        return float(field[y, x])

    def jade(fx, fy, seed):
        ox, oy = (seed * 0.173) % 1.0, (seed * 0.371) % 1.0
        n = sample(tone, fx, fy, ox, oy)
        g = sample(grain, fx, fy, ox, oy, 2.0)
        c = mix(mix(base, deep, 0.28), mix(base, hot, 0.35), n)
        c = mix(c, deep, (g - 0.5) * 0.18 + 0.09)
        # Ridged veins: thin dark mineral lines with a faint bright halo (jade inclusions).
        v = abs(sample(vein, fx, fy, ox, oy, 1.5) - 0.5)
        if v < 0.012:
            c = mix(c, dark, 0.7 * (1 - v / 0.012))
        elif v < 0.03:
            c = mix(c, hot, 0.12 * (1 - (v - 0.012) / 0.018))
        return c

    def bevel(c, fx, fy, k=10.0, strength=1.0):
        rim = min(fx, 1 - fx, fy, 1 - fy)
        return mix(mix(dark, c, 0.35), c, min(1.0, rim * k)) if strength > 0 else c

    def palm(face, fx, fy):
        c = jade(fx, fy, 1)
        e = 0.0
        if face == "top":  # palm surface (-Y): palm lines, central scripture seal, bevelled rim
            c = bevel(c, fx, fy, 14.0)
            for (cx, cy, r, w) in [(0.05, 0.2, 0.62, 0.014), (0.1, -0.05, 0.85, 0.012), (0.9, 0.35, 0.55, 0.011)]:
                d = abs(math.hypot(fx - cx, fy - cy) - r)
                if d < w:
                    c = mix(seam, c, d / w)
                    e = max(e, 0.7 * (1 - d / w))
            d = math.hypot((fx - 0.5) * 1.1, fy - 0.5)
            # Seal: double ring, eight-spoke wheel, inner ring, centre dot.
            if abs(d - 0.22) < 0.009 or abs(d - 0.30) < 0.005 or abs(d - 0.10) < 0.006:
                c = hot
                e = 1.0
            a = math.atan2(fy - 0.5, (fx - 0.5) * 1.1)
            if 0.10 < d < 0.22:
                spoke = abs(((a / (math.pi / 4)) + 0.5) % 1.0 - 0.5)
                if spoke * d < 0.006:
                    c = mix(c, hot, 0.85)
                    e = max(e, 0.85)
            if d < 0.035:
                c = hot
                e = 1.0
            # 24 scripture strokes between the rings.
            if 0.235 < d < 0.29:
                cell = ((a + math.pi) / (2 * math.pi) * 24) % 1.0
                if 0.2 < cell < 0.5 and abs(d - 0.262) < 0.018 * (0.5 + 0.5 * math.sin(cell * math.pi * 3)):
                    c = mix(c, seam, 0.9)
                    e = max(e, 0.9)
            return out(c, e)
        if face == "bottom":  # back of the hand: tendon ridges + sun seal near the wrist
            ridge = 0.5 + 0.5 * math.cos((fx - 0.5) * 4 * 2 * math.pi)
            c = mix(c, hot, ridge * 0.14 * fy)
            c = bevel(c, fx, fy, 14.0)
            d = math.hypot((fx - 0.5) * 1.1, (fy - 0.30) * 1.0)
            if abs(d - 0.16) < 0.007 or abs(d - 0.05) < 0.02:
                c = hot
                e = 1.0
            a = math.atan2(fy - 0.30, (fx - 0.5) * 1.1)
            if 0.16 < d < 0.24 and abs(((a / (math.pi / 8)) + 0.5) % 1.0 - 0.5) * d < 0.004:
                c = mix(c, seam, 0.9)
                e = max(e, 0.9)
            # Four dashed scripture lines running along the tendons towards the fingers.
            for k in range(4):
                lx = 0.125 + 0.25 * k
                if abs(fx - lx) < 0.006 and 0.52 < fy < 0.94 and (int(fy * 40) % 3) != 0:
                    c = mix(c, seam, 0.85)
                    e = max(e, 0.85)
            return out(c, e)
        # side walls: darker, bevelled, one bright seam line halfway
        c = mix(c, deep, 0.3)
        c = bevel(c, fx, fy, 12.0)
        if abs(fy - 0.5) < 0.06:
            k = 1 - abs(fy - 0.5) / 0.06
            c = mix(c, seam, 0.55 * k)
            e = 0.8 * k
        return out(c, e)

    def finger(seed, last=False):
        def p(face, fx, fy):
            c = jade(fx, fy, seed)
            e = 0.0
            if face in ("top", "bottom"):
                # dark joints at both ends, bevelled sides, bright nail seam on the tip segment
                j = min(fy, 1 - fy)
                c = mix(dark, c, min(1.0, j * 9))
                c = bevel(c, fx, fy, 9.0)
                if face == "top":
                    c = mix(c, hot, math.exp(-((fx - 0.5) / 0.28) ** 2) * 0.2)
                    # a single glyph stroke on each finger pad
                    if abs(fx - 0.5) < 0.05 and 0.3 < fy < 0.7:
                        c = mix(c, seam, 0.6)
                        e = 0.9
                if last and face == "bottom" and fy > 0.72:
                    c = mix(c, hot, 0.6)
                    e = 0.5
                    if fy > 0.78 and abs(fx - 0.5) < 0.3:
                        c = mix(c, (255, 255, 255), 0.35)
                        e = 0.8
            elif face in ("left", "right"):
                j = min(fx, 1 - fx)
                c = mix(dark, c, min(1.0, j * 9))
                c = bevel(c, fx, fy, 9.0)
                if abs(fy - 0.5) < 0.08:
                    k = 1 - abs(fy - 0.5) / 0.08
                    c = mix(c, seam, 0.5 * k)
                    e = 0.8 * k
            else:
                c = mix(c, deep, 0.4)
                c = bevel(c, fx, fy, 8.0)
            return out(c, e)
        return p

    t.cuboid(0, 0, 32, 6, 36, palm)
    # (u, v, w, h, d, seed, last) – must match HeavenHandModel.getTexturedModelData()
    segs = [
        (0, 44, 7, 5, 14, 11, False), (44, 44, 7, 5, 15, 12, False), (90, 44, 7, 5, 14, 13, False), (134, 44, 6, 5, 11, 14, False), (170, 44, 7, 6, 12, 15, False),
        (0, 66, 7, 5, 12, 21, False), (40, 66, 7, 5, 13, 22, False), (82, 66, 7, 5, 12, 23, False), (122, 66, 6, 5, 9, 24, False), (154, 66, 7, 6, 9, 25, True),
        (0, 86, 7, 5, 8, 31, True), (32, 86, 7, 5, 9, 32, True), (66, 86, 7, 5, 8, 33, True), (98, 86, 6, 5, 7, 34, True),
    ]
    for (u, v, w, h, d, seed, last) in segs:
        t.cuboid(u, v, w, h, d, finger(seed, last))
    t.save("heaven_hand")
    # Emissive mask: same layout, only the seams/seals, unshaded.
    mode["emit"] = True
    g = Tex(256, 128, density=4)
    g.cuboid(0, 0, 32, 6, 36, palm, shade=False)
    for (u, v, w, h, d, seed, last) in segs:
        g.cuboid(u, v, w, h, d, finger(seed, last), shade=False)
    g.save("heaven_hand_glow")


def dragon_head(name, scale_dark, scale_light, belly, horn, eye, glow):
    """Eastern dragon head for DragonHeadModel (64x32): scales with a bright ridge, pale
    jaw/belly plates, ivory horns, glowing eyes and translucent fins."""
    t = Tex(64, 32)

    def scales(face, fx, fy):
        # diamond scale pattern
        u, v = fx * 6, fy * 5
        d = abs((u % 1) - 0.5) + abs((v % 1) - 0.5)
        n = hash_noise(int(u), int(v), 21)
        c = mix(scale_dark, scale_light, 0.35 + 0.45 * n)
        c = mix(c, scale_dark, max(0.0, d - 0.55) * 2.2)  # dark scale edges
        if face == "top":
            c = mix(c, glow, math.exp(-((fx - 0.5) / 0.14) ** 2) * 0.8)  # glowing dorsal ridge
        if face == "bottom":
            c = mix(c, belly, 0.7)
        return with_a(c, 255)

    def snout(face, fx, fy):
        r, g, b, a = scales(face, fx, fy)
        if face == "front":  # nostrils
            for nx in (0.3, 0.7):
                if math.hypot((fx - nx) * 1.4, fy - 0.45) < 0.14:
                    return with_a((25, 15, 35), 255)
        if face in ("left", "right") and fy > 0.75:
            return with_a(mix(belly, (255, 255, 255), 0.2), 255)  # teeth line
        return (r, g, b, a)

    def head(face, fx, fy):
        r, g, b, a = scales(face, fx, fy)
        if face in ("left", "right"):
            # eye near the front-top
            ex = 0.72 if face == "left" else 0.28
            d = math.hypot((fx - ex) * 1.3, (fy - 0.38) * 1.0)
            if d < 0.16:
                return with_a(mix(eye, (255, 255, 255), max(0.0, 1 - d / 0.08)), 255)
            if d < 0.21:
                return with_a((20, 12, 30), 255)
        return (r, g, b, a)

    def jaw(face, fx, fy):
        c = mix(belly, scale_light, 0.25)
        if face == "top":
            c = mix(c, (255, 255, 255), 0.5 if (int(fx * 8) % 2 == 0 and fy < 0.3) else 0.0)  # teeth
        return with_a(c, 255)

    def horn_p(face, fx, fy):
        c = mix(horn, (255, 255, 255), fy * 0.5)
        c = mix(c, (0, 0, 0), (math.sin(fy * 18) * 0.5 + 0.5) * 0.15)
        return with_a(c, 255)

    def fin(face, fx, fy):
        c = mix(glow, scale_light, 0.4)
        edge = 1.0 - abs(fy - 0.5) * 2
        rays = (math.sin(fx * 14) * 0.5 + 0.5)
        c = mix(c, (255, 255, 255), rays * 0.35)
        return with_a(c, int(120 + 110 * edge))

    t.cuboid(0, 0, 4, 3, 5, snout)
    t.cuboid(0, 8, 6, 5, 6, head)
    t.cuboid(24, 8, 4, 1, 5, jaw)
    t.cuboid(18, 0, 1, 4, 1, horn_p)
    t.cuboid(22, 0, 1, 4, 1, horn_p)
    t.cuboid(42, 8, 1, 3, 4, fin, shade=False)
    t.cuboid(52, 8, 1, 3, 4, fin, shade=False)
    t.cuboid(0, 19, 5, 5, 3, scales)
    t.save(name)


def wind_dragon():
    dragon_head("wind_dragon", (60, 140, 110), (150, 235, 200), (225, 255, 240), (240, 245, 230), (120, 255, 200), (200, 255, 230))


def thunder_dragon():
    dragon_head("thunder_dragon", (70, 30, 130), (170, 120, 255), (225, 205, 255), (235, 225, 255), (255, 240, 255), (210, 190, 255))


def main():
    sword_qi(); ice_shard(); fire_lotus(); spirit_sword(); flying_sword(); rock_spike(); heaven_sword()
    heaven_hand()
    wind_dragon(); thunder_dragon()


if __name__ == "__main__":
    main()
