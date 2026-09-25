#!/usr/bin/env python3
"""Generates item textures, skill icons, GUI atlases and the mod icon.

    python3 tools/gen_gui_textures.py
"""
import math
import os
import random
import sys

sys.path.insert(0, os.path.dirname(__file__))
from PIL import Image, ImageDraw, ImageFilter, ImageChops  # noqa: E402
import texlib as T  # noqa: E402

ASSETS = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "celestialarts")
TEX = os.path.join(ASSETS, "textures")
ITEM = os.path.join(TEX, "item")
GUI = os.path.join(TEX, "gui")
SKILLS = os.path.join(GUI, "skills")

S = T.SS
WHITE = (255, 255, 255)

ELEMENT = {
    "sword": ((0x9F, 0xE8, 0xFF), (0xFF, 0xFF, 0xFF)),
    "fire": ((0xFF, 0x7A, 0x1A), (0xFF, 0xE0, 0x7A)),
    "ice": ((0x9B, 0xE4, 0xFF), (0xE8, 0xFB, 0xFF)),
    "lightning": ((0xB5, 0x7B, 0xFF), (0xF2, 0xE6, 0xFF)),
    "wind": ((0xB8, 0xFF, 0xD9), (0xF0, 0xFF, 0xF6)),
    "earth": ((0xC6, 0x9C, 0x5B), (0xF2, 0xD9, 0xA6)),
    "void": ((0x6A, 0x1F, 0xB0), (0xD2, 0x4B, 0xFF)),
    "dao": ((0xFF, 0xE9, 0xA8), (0xFF, 0xFF, 0xFF)),
}

SKILL_ELEMENT = {
    "sword_qi_slash": "sword", "flame_claw": "fire", "ice_arrows": "ice", "lightning_step": "lightning",
    "wind_blade_dance": "wind", "tortoise_shield": "earth", "earth_shatter": "earth", "fire_lotus": "fire",
    "taiji_formation": "dao", "sword_flight": "sword", "frozen_domain": "ice", "thousand_swords": "sword",
    "purple_thunder_beam": "lightning", "devouring_vortex": "void", "nine_tribulations": "lightning",
    "heaven_sword": "dao",
}


def save(img, folder, name, w, h):
    T.save(T.finish(img, w, h), os.path.join(folder, name + ".png"))
    print(os.path.relpath(os.path.join(folder, name + ".png"), TEX))


def dk(c, f):
    return tuple(int(v * f) for v in c)


def lt(c, f):
    return tuple(int(v + (255 - v) * f) for v in c)


# ------------------------------------------------------------------------------- items

def item_crystal(name, col, col2):
    img = T.new(32, 32)
    d = ImageDraw.Draw(img)
    # hexagonal crystal cluster
    def gem(cx, cy, r, rot):
        pts = [(cx + math.cos(rot + i * math.pi / 3) * r, cy + math.sin(rot + i * math.pi / 3) * r) for i in range(6)]
        T.polygon_glow(img, pts, col, glow=1.5, fill_alpha=250, outline=lt(col, 0.6))
        # facets
        dd = ImageDraw.Draw(img)
        for i in range(0, 6, 2):
            dd.polygon([(cx * S, cy * S), (pts[i][0] * S, pts[i][1] * S), (pts[(i + 1) % 6][0] * S, pts[(i + 1) % 6][1] * S)], fill=lt(col, 0.35) + (255,))
        dd.polygon([(cx * S, cy * S), (pts[1][0] * S, pts[1][1] * S), (pts[2][0] * S, pts[2][1] * S)], fill=col2 + (255,))
    gem(11, 20, 7, 0.3)
    gem(21, 18, 6, -0.4)
    gem(16, 12, 7.5, 0.1)
    T.polygon_glow(img, T.star_points(9, 9, 4, 0.8, 4), WHITE, glow=1.0)
    save(img, ITEM, name, 32, 32)


def item_pill(name, col):
    img = T.new(32, 32)
    img.alpha_composite(T.radial(32, 32, col + (120,), col + (0,), power=1.2, radius=14))
    T.ellipse_glow(img, (7, 7, 25, 25), col, glow=1.0)
    d = ImageDraw.Draw(img)
    d.ellipse([9 * S, 9 * S, 23 * S, 23 * S], fill=lt(col, 0.25) + (255,))
    d.ellipse([11 * S, 10 * S, 17 * S, 15 * S], fill=lt(col, 0.7) + (255,))
    # swirl mark
    d.arc([12 * S, 12 * S, 21 * S, 21 * S], 20, 250, fill=dk(col, 0.55) + (255,), width=S)
    T.polygon_glow(img, T.star_points(24, 8, 3.5, 0.7, 4), WHITE, glow=1.0)
    save(img, ITEM, name, 32, 32)


def item_dao_manual():
    img = T.new(32, 32)
    d = ImageDraw.Draw(img)
    cover = (40, 60, 130)
    d.rounded_rectangle([5 * S, 3 * S, 27 * S, 29 * S], radius=2 * S, fill=cover + (255,), outline=(20, 30, 80, 255), width=S)
    d.rectangle([5 * S, 3 * S, 8 * S, 29 * S], fill=(25, 40, 100, 255))
    d.rectangle([9 * S, 4 * S, 26 * S, 28 * S], outline=(230, 200, 110, 255), width=S)
    # golden taiji
    c = (17.5 * S, 15 * S)
    r = 6 * S
    gold = (240, 205, 110, 255)
    d.pieslice([c[0] - r, c[1] - r, c[0] + r, c[1] + r], 90, 270, fill=gold)
    d.pieslice([c[0] - r, c[1] - r, c[0] + r, c[1] + r], 270, 450, fill=cover + (255,))
    d.ellipse([c[0] - r / 2, c[1] - r, c[0] + r / 2, c[1]], fill=gold)
    d.ellipse([c[0] - r / 2, c[1], c[0] + r / 2, c[1] + r], fill=cover + (255,))
    d.ellipse([c[0] - r / 6, c[1] - r / 2 - r / 6, c[0] + r / 6, c[1] - r / 2 + r / 6], fill=cover + (255,))
    d.ellipse([c[0] - r / 6, c[1] + r / 2 - r / 6, c[0] + r / 6, c[1] + r / 2 + r / 6], fill=gold)
    d.ellipse([c[0] - r, c[1] - r, c[0] + r, c[1] + r], outline=gold, width=S)
    d.rectangle([11 * S, 24 * S, 24 * S, 25 * S], fill=gold)
    # pages
    d.rectangle([8 * S, 29 * S, 27 * S, 30 * S], fill=(235, 225, 200, 255))
    save(img, ITEM, "dao_manual", 32, 32)


def item_immortal_sword():
    img = T.new(32, 32)
    d = ImageDraw.Draw(img)
    # diagonal from bottom-left (hilt) to top-right (tip), like vanilla handheld items.
    def P(x, y):
        return (x * S, y * S)
    blade = [P(11, 21), P(28, 4), P(30, 2), P(29, 6), P(13, 23)]
    d.polygon(blade, fill=(230, 240, 255, 255))
    d.line([P(12, 22), P(29, 5)], fill=(150, 220, 255, 255), width=S)
    d.line([P(13, 23), P(29, 6)], fill=(120, 160, 200, 255), width=S)
    # guard
    d.line([P(8, 19), P(14, 25)], fill=(240, 200, 100, 255), width=3 * S)
    # handle
    d.line([P(10, 22), P(5, 27)], fill=(70, 40, 100, 255), width=2 * S)
    for i in range(3):
        x = 9 - i * 1.6
        y = 23 + i * 1.6
        d.line([P(x - 0.6, y - 0.6), P(x + 0.6, y + 0.6)], fill=(120, 80, 160, 255), width=S)
    d.ellipse([P(3, 26)[0], P(3, 26)[1], P(6, 29)[0], P(6, 29)[1]], fill=(240, 200, 100, 255))
    # glow
    glow = img.filter(ImageFilter.GaussianBlur(1.5 * S))
    glow.alpha_composite(img)
    T.polygon_glow(glow, T.star_points(26, 6, 4, 0.8, 4), WHITE, glow=1.0)
    save(glow, ITEM, "immortal_sword", 32, 32)


def item_scroll_base():
    img = T.new(32, 32)
    d = ImageDraw.Draw(img)
    paper = (232, 214, 170)
    dark = (170, 140, 90)
    # rolled ends
    d.rounded_rectangle([4 * S, 6 * S, 28 * S, 26 * S], radius=S, fill=paper + (255,), outline=dark + (255,), width=S)
    d.rectangle([4 * S, 6 * S, 7 * S, 26 * S], fill=(210, 190, 140, 255))
    d.rectangle([25 * S, 6 * S, 28 * S, 26 * S], fill=(210, 190, 140, 255))
    d.rectangle([3 * S, 4 * S, 8 * S, 28 * S], fill=(120, 60, 40, 255), outline=(80, 40, 30, 255), width=S)
    d.rectangle([24 * S, 4 * S, 29 * S, 28 * S], fill=(120, 60, 40, 255), outline=(80, 40, 30, 255), width=S)
    d.ellipse([3 * S, 3 * S, 8 * S, 6 * S], fill=(230, 200, 120, 255))
    d.ellipse([24 * S, 3 * S, 29 * S, 6 * S], fill=(230, 200, 120, 255))
    d.ellipse([3 * S, 26 * S, 8 * S, 29 * S], fill=(230, 200, 120, 255))
    d.ellipse([24 * S, 26 * S, 29 * S, 29 * S], fill=(230, 200, 120, 255))
    # faint text lines
    for i in range(4):
        d.line([(10 * S, (10 + i * 3.5) * S), (22 * S, (10 + i * 3.5) * S)], fill=dark + (110,), width=S)
    save(img, ITEM, "scroll_base", 32, 32)


def item_scroll_seal(skill):
    col, col2 = ELEMENT[SKILL_ELEMENT[skill]]
    img = T.new(32, 32)
    d = ImageDraw.Draw(img)
    c = 16 * S
    r = 6 * S
    d.ellipse([c - r, c - r, c + r, c + r], fill=col + (255,), outline=dk(col, 0.6) + (255,), width=S)
    d.ellipse([c - r * 0.65, c - r * 0.65, c + r * 0.65, c + r * 0.65], outline=col2 + (255,), width=S)
    from gen_fx_textures import glyph_stroke
    idx = list(SKILL_ELEMENT.keys()).index(skill) % 8
    glyph_stroke(d, c, c, 2.4 * S, idx, col2, S)
    glow = img.filter(ImageFilter.GaussianBlur(1.0 * S))
    glow.alpha_composite(img)
    save(glow, ITEM, "scroll_seal_" + skill, 32, 32)


# ------------------------------------------------------------------------ skill icons

def icon_bg(col, col2):
    img = T.new(32, 32)
    d = ImageDraw.Draw(img)
    d.rounded_rectangle([0, 0, 32 * S - 1, 32 * S - 1], radius=3 * S, fill=dk(col, 0.25) + (255,))
    img.alpha_composite(T.radial(32, 32, col + (150,), dk(col, 0.4) + (0,), power=1.4))
    d = ImageDraw.Draw(img)
    d.rounded_rectangle([0, 0, 32 * S - 1, 32 * S - 1], radius=3 * S, outline=col + (255,), width=S)
    return img


def icon_finish(img, name):
    save(img, SKILLS, name, 32, 32)


def icon_sword_qi_slash():
    col, col2 = ELEMENT["sword"]
    img = icon_bg(col, col2)
    T.polygon_glow(img, T.crescent(17, 15, 11, 4, -40, 210), col2, glow=1.5)
    T.polygon_glow(img, T.crescent(15, 17, 7, 2.5, -40, 180), col, glow=1.0)
    icon_finish(img, "sword_qi_slash")


def icon_flame_claw():
    col, col2 = ELEMENT["fire"]
    img = icon_bg(col, col2)
    for i in range(3):
        ox = 6 + i * 7
        pts = [(ox + 2, 26), (ox + 6, 20), (ox + 7, 8), (ox + 3, 14)]
        T.polygon_glow(img, pts, col2, glow=1.5)
    T.polygon_glow(img, [(4, 30), (16, 12), (28, 30)], col, glow=2.0, fill_alpha=90)
    icon_finish(img, "flame_claw")


def icon_ice_arrows():
    col, col2 = ELEMENT["ice"]
    img = icon_bg(col, col2)
    for i, (x0, y0) in enumerate([(6, 26), (13, 28), (20, 26)]):
        T.polyline_glow(img, [(x0, y0), (x0 + 6, 6)], col2, 1.8, glow=1.2)
        T.polygon_glow(img, [(x0 + 6, 3), (x0 + 8.5, 9), (x0 + 3.5, 9)], col2, glow=1.0)
    icon_finish(img, "ice_arrows")


def icon_lightning_step():
    col, col2 = ELEMENT["lightning"]
    img = icon_bg(col, col2)
    T.polygon_glow(img, [(18, 3), (8, 17), (15, 17), (12, 29), (24, 13), (17, 13), (21, 3)], col2, glow=2.0)
    T.polyline_glow(img, [(4, 24), (10, 24)], col, 1.5, glow=1.0)
    T.polyline_glow(img, [(3, 27), (9, 27)], col, 1.5, glow=1.0)
    icon_finish(img, "lightning_step")


def icon_wind_blade_dance():
    col, col2 = ELEMENT["wind"]
    img = icon_bg(col, col2)
    for k in range(3):
        rot = 90 + k * 120
        T.polygon_glow(img, T.crescent(16, 16, 12, 3.5, rot, 110), col2, glow=1.5)
    T.ellipse_glow(img, (13, 13, 19, 19), col, glow=1.5)
    icon_finish(img, "wind_blade_dance")


def icon_tortoise_shield():
    col, col2 = ELEMENT["earth"]
    img = icon_bg(col, col2)
    pts = [(16 + math.cos(math.pi / 6 + i * math.pi / 3) * 12, 16 + math.sin(math.pi / 6 + i * math.pi / 3) * 12) for i in range(6)]
    T.polygon_glow(img, pts, col, glow=1.5, fill_alpha=200, outline=col2)
    inner = [(16 + math.cos(math.pi / 6 + i * math.pi / 3) * 6, 16 + math.sin(math.pi / 6 + i * math.pi / 3) * 6) for i in range(6)]
    T.polygon_glow(img, inner, col2, glow=1.0, fill_alpha=120, outline=col2)
    d = ImageDraw.Draw(img)
    for p, q in zip(pts, inner):
        d.line([(p[0] * S, p[1] * S), (q[0] * S, q[1] * S)], fill=col2 + (255,), width=S)
    icon_finish(img, "tortoise_shield")


def icon_earth_shatter():
    col, col2 = ELEMENT["earth"]
    img = icon_bg(col, col2)
    for (x, h) in [(6, 10), (12, 20), (18, 15), (24, 24)]:
        T.polygon_glow(img, [(x - 3, 30), (x, 30 - h), (x + 3, 30)], col, glow=1.0, fill_alpha=255, outline=col2)
    rnd = random.Random(2)
    T.polyline_glow(img, T.lightning_points(2, 30, 30, 30, 8, 2, rnd), (255, 200, 120), 1.5, glow=1.5)
    icon_finish(img, "earth_shatter")


def icon_fire_lotus():
    col, col2 = ELEMENT["fire"]
    img = icon_bg(col, col2)
    for i in range(8):
        a = i * math.pi / 4
        tip = (16 + math.cos(a) * 13, 16 + math.sin(a) * 13)
        l = (16 + math.cos(a + 0.35) * 6, 16 + math.sin(a + 0.35) * 6)
        r = (16 + math.cos(a - 0.35) * 6, 16 + math.sin(a - 0.35) * 6)
        T.polygon_glow(img, [(16, 16), l, tip, r], (255, 120, 50), glow=1.0, fill_alpha=230, outline=col2)
    T.ellipse_glow(img, (12, 12, 20, 20), col2, glow=1.5)
    icon_finish(img, "fire_lotus")


def icon_taiji_formation():
    col, col2 = ELEMENT["dao"]
    img = icon_bg(col, col2)
    d = ImageDraw.Draw(img)
    c = 16 * S
    r = 11 * S
    gold = col2 + (255,)
    dark = dk(col, 0.25) + (255,)
    d.pieslice([c - r, c - r, c + r, c + r], 90, 270, fill=gold)
    d.pieslice([c - r, c - r, c + r, c + r], 270, 450, fill=dark)
    d.ellipse([c - r / 2, c - r, c + r / 2, c], fill=gold)
    d.ellipse([c - r / 2, c, c + r / 2, c + r], fill=dark)
    d.ellipse([c - r / 5, c - r / 2 - r / 5, c + r / 5, c - r / 2 + r / 5], fill=dark)
    d.ellipse([c - r / 5, c + r / 2 - r / 5, c + r / 5, c + r / 2 + r / 5], fill=gold)
    d.ellipse([c - r, c - r, c + r, c + r], outline=col + (255,), width=S)
    icon_finish(img, "taiji_formation")


def icon_sword_flight():
    col, col2 = ELEMENT["sword"]
    img = icon_bg(col, col2)
    T.polygon_glow(img, [(3, 24), (24, 6), (28, 4), (26, 8), (6, 26)], col2, glow=1.5)
    T.polyline_glow(img, [(4, 19), (9, 24)], (240, 200, 100), 2.0, glow=0.8)
    for i in range(3):
        T.polyline_glow(img, [(3 + i * 2, 12 + i * 5), (10 + i * 2, 12 + i * 5)], col, 1.0, glow=1.0, core=False)
    icon_finish(img, "sword_flight")


def icon_frozen_domain():
    col, col2 = ELEMENT["ice"]
    img = icon_bg(col, col2)
    d = ImageDraw.Draw(img)
    c = 16 * S
    for i in range(6):
        a = i * math.pi / 3
        d.line([(c, c), (c + math.cos(a) * 12 * S, c + math.sin(a) * 12 * S)], fill=col2 + (255,), width=2 * S)
        for f in (0.5, 0.78):
            bx, by = c + math.cos(a) * 12 * S * f, c + math.sin(a) * 12 * S * f
            for sgn in (-1, 1):
                b = a + sgn * math.pi / 3
                d.line([(bx, by), (bx + math.cos(b) * 3.5 * S, by + math.sin(b) * 3.5 * S)], fill=col2 + (255,), width=S)
    glow = img.filter(ImageFilter.GaussianBlur(1.0 * S))
    glow.alpha_composite(img)
    icon_finish(glow, "frozen_domain")


def icon_thousand_swords():
    col, col2 = ELEMENT["sword"]
    img = icon_bg(col, col2)
    for (x, top) in [(8, 4), (16, 2), (24, 4), (12, 9), (20, 9)]:
        T.polygon_glow(img, [(x - 1.6, top + 4), (x, top), (x + 1.6, top + 4), (x + 1.2, 26), (x - 1.2, 26)], col2, glow=1.0)
        T.polyline_glow(img, [(x - 3, 24), (x + 3, 24)], (240, 200, 100), 1.2, glow=0.5, core=False)
    icon_finish(img, "thousand_swords")


def icon_purple_thunder_beam():
    col, col2 = ELEMENT["lightning"]
    img = icon_bg(col, col2)
    T.polyline_glow(img, [(3, 16), (29, 16)], col, 7.0, glow=3.0, core=False)
    T.polyline_glow(img, [(3, 16), (29, 16)], col2, 3.0, glow=1.5)
    rnd = random.Random(8)
    for k in range(2):
        T.polyline_glow(img, T.lightning_points(6, 16 + (k * 2 - 1) * 3, 27, 16 + (k * 2 - 1) * 8, 6, 3, rnd), col2, 1.0, glow=1.0)
    T.ellipse_glow(img, (1, 12, 9, 20), col2, glow=2.0)
    icon_finish(img, "purple_thunder_beam")


def icon_devouring_vortex():
    col, col2 = ELEMENT["void"]
    img = icon_bg(col, col2)
    d = ImageDraw.Draw(img)
    for arm in range(3):
        pts = []
        for i in range(30):
            t = i / 29
            a = arm * 2 * math.pi / 3 + t * 4.0
            r = 2 + t * 12
            pts.append((16 + math.cos(a) * r, 16 + math.sin(a) * r))
        T.polyline_glow(img, pts, col2, 2.0, glow=1.5)
    T.ellipse_glow(img, (12, 12, 20, 20), (20, 5, 40), glow=1.5)
    icon_finish(img, "devouring_vortex")


def icon_nine_tribulations():
    col, col2 = ELEMENT["lightning"]
    img = icon_bg(col, col2)
    T.ellipse_glow(img, (3, 3, 29, 13), (90, 60, 130), glow=2.0, fill_alpha=230)
    T.ellipse_glow(img, (8, 1, 24, 11), (120, 90, 160), glow=1.0, fill_alpha=230)
    rnd = random.Random(19)
    for x in (8, 16, 24):
        T.polyline_glow(img, T.lightning_points(x, 9, x + rnd.uniform(-3, 3), 30, 6, 3, rnd), col2, 1.6, glow=1.5)
    icon_finish(img, "nine_tribulations")


def icon_heaven_sword():
    col, col2 = ELEMENT["dao"]
    img = icon_bg(col, col2)
    img.alpha_composite(T.radial(32, 32, (255, 240, 200, 160), (255, 220, 120, 0), power=1.5, radius=12))
    T.polygon_glow(img, [(14, 26), (16, 2), (18, 26)], col2, glow=2.0)
    T.polyline_glow(img, [(10, 24), (22, 24)], (255, 210, 90), 2.0, glow=1.0)
    T.polyline_glow(img, [(16, 24), (16, 30)], (180, 40, 60), 2.0, glow=0.8)
    T.polygon_glow(img, T.star_points(16, 6, 5, 1, 4), WHITE, glow=1.5, fill_alpha=200)
    icon_finish(img, "heaven_sword")


# ------------------------------------------------------------------------ GUI atlases

def gui_hud():
    """256x64 atlas:
    qi frame     (0,0)   128x12
    qi fill      (0,12)  124x8
    exp fill     (0,20)  124x3
    slot frame   (128,0) 24x24
    active frame (152,0) 24x24
    empty slot   (176,0) 24x24
    """
    W, H = 256, 64
    img = Image.new("RGBA", (W, H))
    d = ImageDraw.Draw(img)
    # qi frame: dark rounded bar with golden border, transparent inside for the fill.
    d.rounded_rectangle([0, 0, 127, 11], radius=3, fill=(12, 10, 24, 200), outline=(215, 180, 100, 255), width=1)
    d.rectangle([2, 2, 125, 9], fill=(20, 18, 36, 255))
    # qi fill 124x8 gradient (cyan-blue) with highlight line
    for x in range(124):
        t = x / 123
        r = int(40 + 60 * t)
        g = int(150 + 80 * t)
        b = int(230 + 25 * t)
        d.line([(x, 12), (x, 19)], fill=(r, g, b, 255))
    d.line([(0, 13), (123, 13)], fill=(200, 245, 255, 255))
    d.line([(0, 19), (123, 19)], fill=(20, 80, 160, 255))
    # exp fill 124x3 golden
    for x in range(124):
        t = x / 123
        d.line([(x, 20), (x, 22)], fill=(int(220 + 30 * t), int(170 + 50 * t), 60, 255))
    d.line([(0, 20), (123, 20)], fill=(255, 240, 180, 255))
    # slot frame
    def frame(x0, border, inner, glow=None):
        d.rounded_rectangle([x0, 0, x0 + 23, 23], radius=3, fill=inner, outline=border, width=1)
        d.rounded_rectangle([x0 + 1, 1, x0 + 22, 22], radius=2, outline=(border[0] // 2, border[1] // 2, border[2] // 2, 255), width=1)
        if glow:
            d.rounded_rectangle([x0 + 2, 2, x0 + 21, 21], radius=2, outline=glow, width=1)
    frame(128, (200, 170, 100, 255), (0, 0, 0, 0))
    frame(152, (255, 250, 200, 255), (0, 0, 0, 0), glow=(255, 230, 150, 200))
    frame(176, (110, 95, 70, 255), (10, 10, 20, 170))
    # decorative corner studs
    for x0 in (128, 152):
        for (px, py) in [(x0 + 2, 2), (x0 + 21, 2), (x0 + 2, 21), (x0 + 21, 21)]:
            d.point((px, py), fill=(255, 240, 200, 255))
    T.save(img, os.path.join(GUI, "hud.png"))
    print("gui/hud.png")


def gui_skill_book():
    """256x256 texture; the panel occupies 256x200 at the top."""
    W, H = 256, 256
    img = Image.new("RGBA", (W, H))
    d = ImageDraw.Draw(img)
    # parchment background with subtle noise
    n = T.noise_layer(256, 200, 77, scale=6, octaves=3, ss=False)
    paper = Image.new("RGBA", (256, 200), (58, 46, 78, 255))
    tint = T.colorize(n, (95, 78, 125), 0.5)
    paper.alpha_composite(tint)
    img.paste(paper, (0, 0))
    d = ImageDraw.Draw(img)
    # outer frame
    d.rectangle([0, 0, 255, 199], outline=(230, 195, 110, 255), width=2)
    d.rectangle([3, 3, 252, 196], outline=(120, 95, 50, 255), width=1)
    # title bar
    d.rectangle([6, 6, 249, 24], fill=(30, 22, 44, 220), outline=(230, 195, 110, 255))
    # skill grid area
    d.rectangle([10, 28, 157, 175], fill=(24, 18, 36, 200), outline=(160, 130, 80, 255))
    for i in range(4):
        for j in range(4):
            x = 12 + i * 36
            y = 30 + j * 36
            d.rectangle([x, y, x + 33, y + 33], fill=(40, 32, 58, 255), outline=(90, 75, 110, 255))
    # slot list area
    d.rectangle([172, 28, 249, 160], fill=(24, 18, 36, 200), outline=(160, 130, 80, 255))
    for i in range(6):
        y = 30 + i * 22
        d.rounded_rectangle([176, y, 245, y + 19], radius=2, fill=(44, 36, 62, 255), outline=(110, 90, 130, 255))
    # status/breakthrough area
    d.rectangle([160, 164, 249, 194], fill=(24, 18, 36, 200), outline=(160, 130, 80, 255))
    d.rectangle([10, 178, 157, 194], fill=(24, 18, 36, 200), outline=(160, 130, 80, 255))
    # corner ornaments
    gold = (230, 195, 110, 255)
    for (cx, cy, sx, sy) in [(6, 6, 1, 1), (249, 6, -1, 1), (6, 193, 1, -1), (249, 193, -1, -1)]:
        d.line([(cx, cy), (cx + sx * 10, cy)], fill=gold, width=2)
        d.line([(cx, cy), (cx, cy + sy * 10)], fill=gold, width=2)
        d.point((cx + sx * 3, cy + sy * 3), fill=gold)
    # decorative glyph strip along the bottom of the panel (below 200: spare region)
    T.save(img, os.path.join(GUI, "skill_book.png"))
    print("gui/skill_book.png")


def mod_icon():
    size = 128
    img = T.new(size, size)
    img.alpha_composite(T.radial(size, size, (30, 20, 60, 255), (10, 6, 24, 255), power=1.0))
    img.alpha_composite(T.radial(size, size, (160, 120, 255, 120), (80, 40, 160, 0), power=1.3, radius=56))
    d = ImageDraw.Draw(img)
    c = 64 * S
    r = 40 * S
    gold = (245, 210, 120, 255)
    dark = (25, 15, 45, 255)
    d.pieslice([c - r, c - r, c + r, c + r], 90, 270, fill=gold)
    d.pieslice([c - r, c - r, c + r, c + r], 270, 450, fill=dark)
    d.ellipse([c - r / 2, c - r, c + r / 2, c], fill=gold)
    d.ellipse([c - r / 2, c, c + r / 2, c + r], fill=dark)
    d.ellipse([c - r / 5, c - r / 2 - r / 5, c + r / 5, c - r / 2 + r / 5], fill=dark)
    d.ellipse([c - r / 5, c + r / 2 - r / 5, c + r / 5, c + r / 2 + r / 5], fill=gold)
    d.ellipse([c - r, c - r, c + r, c + r], outline=(255, 240, 200, 255), width=2 * S)
    # sword crossing the symbol
    T.polygon_glow(img, [(24, 104), (100, 28), (108, 20), (104, 32), (30, 108)], (220, 245, 255), glow=3.0)
    T.polyline_glow(img, [(20, 92), (36, 108)], (245, 210, 120), 5.0, glow=1.5)
    T.polygon_glow(img, T.star_points(104, 24, 10, 2, 4), WHITE, glow=2.0)
    d = ImageDraw.Draw(img)
    d.rounded_rectangle([0, 0, size * S - 1, size * S - 1], radius=16 * S, outline=gold, width=3 * S)
    T.save(T.finish(img, size, size), os.path.join(ASSETS, "icon.png"))
    print("icon.png")


def main():
    for f in (ITEM, GUI, SKILLS):
        os.makedirs(f, exist_ok=True)
    item_crystal("spirit_stone", (110, 220, 255), (200, 250, 255))
    item_crystal("high_spirit_stone", (190, 110, 255), (255, 220, 140))
    item_pill("foundation_pill", (255, 150, 60))
    item_pill("nascent_pill", (190, 110, 255))
    item_dao_manual()
    item_immortal_sword()
    item_scroll_base()
    for skill in SKILL_ELEMENT:
        item_scroll_seal(skill)
    icon_sword_qi_slash(); icon_flame_claw(); icon_ice_arrows(); icon_lightning_step()
    icon_wind_blade_dance(); icon_tortoise_shield(); icon_earth_shatter(); icon_fire_lotus()
    icon_taiji_formation(); icon_sword_flight(); icon_frozen_domain(); icon_thousand_swords()
    icon_purple_thunder_beam(); icon_devouring_vortex(); icon_nine_tribulations(); icon_heaven_sword()
    gui_hud(); gui_skill_book(); mod_icon()


if __name__ == "__main__":
    main()
