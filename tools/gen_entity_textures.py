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
    def __init__(self, w, h):
        self.w, self.h = w, h
        self.arr = np.zeros((h, w, 4), dtype=np.float32)

    def face_regions(self, u, v, w, h, d):
        return {
            "top": (u + d, v, w, d),
            "bottom": (u + d + w, v, w, d),
            "right": (u, v + d, d, h),
            "front": (u + d, v + d, w, h),
            "left": (u + d + w, v + d, d, h),
            "back": (u + 2 * d + w, v + d, w, h),
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


def main():
    sword_qi(); ice_shard(); fire_lotus(); spirit_sword(); flying_sword(); rock_spike(); heaven_sword()


if __name__ == "__main__":
    main()
