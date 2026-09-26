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
    "heaven_sword": "dao", "vajra_palm": "dao", "wind_dragon": "wind", "golden_body": "dao", "thunder_dragon": "lightning",
    "heaven_hand": "dao",
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


def icon_heaven_hand():
    """A colossal palm descending on a domain: eight-trigram ring above, palm with fingers spread,
    pressure lines below and a tiny ground ring at the bottom."""
    col, col2 = ELEMENT["dao"]
    img = icon_bg(col, col2)
    # dark domain sky
    img.alpha_composite(T.radial(32, 32, (40, 24, 60, 170), (10, 6, 20, 0), power=1.2, radius=15, center=(16, 12)))
    # formation ring in the sky
    T.ellipse_glow(img, (3, 2, 29, 8), (255, 235, 170), glow=1.2, fill_alpha=0)
    for k in range(8):
        a = k * math.pi / 4
        x, y = 16 + 12.5 * math.cos(a), 5 + 2.6 * math.sin(a)
        T.ellipse_glow(img, (x - 0.9, y - 0.9, x + 0.9, y + 0.9), WHITE, glow=0.8)
    # palm (seen from below: broad, fingers spread downward)
    palm = [(9, 10), (23, 10), (24, 18), (20, 21), (12, 21), (8, 18)]
    T.polygon_glow(img, palm, (255, 226, 150), glow=1.8, outline=(255, 250, 220))
    for (x0, y0, x1, y1, w) in [(11, 20, 9, 27, 2.6), (14, 21, 13.5, 29, 2.8), (17.5, 21, 18, 29.5, 2.8), (21, 20, 23, 27, 2.6), (8.5, 14, 4, 18, 2.4)]:
        T.polyline_glow(img, [(x0, y0), (x1, y1)], (255, 232, 160), w, glow=1.2)
    # dao seal on the palm
    T.ellipse_glow(img, (13, 12.5, 19, 18.5), (255, 250, 230), glow=1.0, fill_alpha=0)
    T.polyline_glow(img, [(16, 13), (16, 18)], WHITE, 1.2, glow=0.6)
    T.polyline_glow(img, [(13.5, 15.5), (18.5, 15.5)], WHITE, 1.2, glow=0.6)
    # pressure ring on the ground
    T.ellipse_glow(img, (2, 27, 30, 31), (255, 215, 110), glow=1.4, fill_alpha=0)
    icon_finish(img, "heaven_hand")


def effect_icon(name, draw_fn):
    """32x32 status-effect icon (textures/mob_effect/<name>.png): dark rounded plate + glowing symbol."""
    img = T.new(32, 32)
    d = ImageDraw.Draw(img)
    d.rounded_rectangle([1 * S, 1 * S, 31 * S - 1, 31 * S - 1], radius=6 * S, fill=(14, 10, 24, 235))
    draw_fn(img)
    d = ImageDraw.Draw(img)
    d.rounded_rectangle([1 * S, 1 * S, 31 * S - 1, 31 * S - 1], radius=6 * S, outline=(120, 100, 150, 200), width=S)
    save(img, os.path.join(TEX, "mob_effect"), name, 32, 32)


def effect_icons():
    def frozen(img):
        col = ELEMENT["ice"][0]
        img.alpha_composite(T.radial(32, 32, col + (120,), col + (0,), power=1.3, radius=13))
        for k in range(3):
            a = k * math.pi / 3
            dx, dy = 10 * math.cos(a), 10 * math.sin(a)
            T.polyline_glow(img, [(16 - dx, 16 - dy), (16 + dx, 16 + dy)], WHITE, 1.8, glow=1.2)
            for sgn in (-1, 1):
                bx, by = 16 + sgn * dx * 0.55, 16 + sgn * dy * 0.55
                for rot in (-0.6, 0.6):
                    ex, ey = bx + sgn * 3.5 * math.cos(a + rot), by + sgn * 3.5 * math.sin(a + rot)
                    T.polyline_glow(img, [(bx, by), (ex, ey)], col, 1.4, glow=0.8)

    def qi_burn(img):
        col, col2 = ELEMENT["fire"]
        img.alpha_composite(T.radial(32, 32, col + (110,), col + (0,), power=1.3, radius=13, center=(16, 19)))
        T.polygon_glow(img, [(16, 4), (22, 12), (24, 20), (20, 27), (12, 27), (8, 20), (11, 12)], col, glow=1.6)
        T.polygon_glow(img, [(16, 11), (19.5, 17), (20, 22), (16, 26), (12, 22), (12.5, 17)], col2, glow=1.2)
        T.polygon_glow(img, [(16, 16), (18, 20), (16, 24), (14, 20)], WHITE, glow=0.8)

    def sword_intent(img):
        col, col2 = ELEMENT["sword"]
        img.alpha_composite(T.radial(32, 32, col + (110,), col + (0,), power=1.3, radius=13))
        T.polygon_glow(img, [(15, 27), (16, 4), (17, 27)], col2, glow=1.8)
        T.polyline_glow(img, [(10, 23), (22, 23)], (200, 235, 255), 2.0, glow=1.0)
        T.polyline_glow(img, [(16, 23), (16, 29)], (110, 140, 190), 2.0, glow=0.6)
        T.polygon_glow(img, T.star_points(16, 6, 4, 1, 4), WHITE, glow=1.4, fill_alpha=220)

    def suppressed(img):
        col, col2 = ELEMENT["dao"]
        img.alpha_composite(T.radial(32, 32, (255, 216, 107, 120), (255, 216, 107, 0), power=1.3, radius=13, center=(16, 10)))
        # small palm pressing down
        T.polygon_glow(img, [(11, 6), (21, 6), (22, 12), (19, 14), (13, 14), (10, 12)], (255, 226, 150), glow=1.4)
        for (x0, x1) in [(12, 11), (14.5, 14.5), (17.5, 17.5), (20, 21)]:
            T.polyline_glow(img, [(x0, 13.5), (x1, 18.5)], (255, 232, 160), 2.0, glow=1.0)
        # pressure chevrons
        for y in (20, 24):
            T.polyline_glow(img, [(9, y), (16, y + 3.5), (23, y)], col, 1.8, glow=1.0)
        # crushed figure / ground line
        T.polyline_glow(img, [(6, 29), (26, 29)], (200, 170, 110), 1.6, glow=0.8)

    effect_icon("frozen", frozen)
    effect_icon("qi_burn", qi_burn)
    effect_icon("sword_intent", sword_intent)
    effect_icon("suppressed", suppressed)


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
    """256x256 texture; the panel occupies 256x222 at the top (layout mirrors SkillBookScreen)."""
    W, H = 256, 256
    PH = 222
    img = Image.new("RGBA", (W, H))
    # parchment background with subtle noise
    n = T.noise_layer(256, PH, 77, scale=6, octaves=3, ss=False)
    paper = Image.new("RGBA", (256, PH), (58, 46, 78, 255))
    tint = T.colorize(n, (95, 78, 125), 0.5)
    paper.alpha_composite(tint)
    img.paste(paper, (0, 0))
    d = ImageDraw.Draw(img)
    # outer frame
    d.rectangle([0, 0, 255, PH - 1], outline=(230, 195, 110, 255), width=2)
    d.rectangle([3, 3, 252, PH - 4], outline=(120, 95, 50, 255), width=1)
    # title bar
    d.rectangle([6, 6, 249, 24], fill=(30, 22, 44, 220), outline=(230, 195, 110, 255))
    # skill grid area (labels are drawn by the screen at y=27, grid starts at 40)
    d.rectangle([10, 38, 157, 185], fill=(24, 18, 36, 200), outline=(160, 130, 80, 255))
    for i in range(4):
        for j in range(4):
            x = 12 + i * 36
            y = 40 + j * 36
            d.rectangle([x, y, x + 33, y + 33], fill=(40, 32, 58, 255), outline=(90, 75, 110, 255))
    # slot list area
    d.rectangle([172, 38, 249, 172], fill=(24, 18, 36, 200), outline=(160, 130, 80, 255))
    for i in range(6):
        y = 40 + i * 22
        d.rounded_rectangle([176, y, 245, y + 19], radius=2, fill=(44, 36, 62, 255), outline=(110, 90, 130, 255))
    # breakthrough area (right) and status area (left)
    d.rectangle([160, 176, 249, 216], fill=(24, 18, 36, 200), outline=(160, 130, 80, 255))
    d.rectangle([10, 188, 157, 218], fill=(24, 18, 36, 200), outline=(160, 130, 80, 255))
    # corner ornaments
    gold = (230, 195, 110, 255)
    for (cx, cy, sx, sy) in [(6, 6, 1, 1), (249, 6, -1, 1), (6, PH - 7, 1, -1), (249, PH - 7, -1, -1)]:
        d.line([(cx, cy), (cx + sx * 10, cy)], fill=gold, width=2)
        d.line([(cx, cy), (cx, cy + sy * 10)], fill=gold, width=2)
        d.point((cx + sx * 3, cy + sy * 3), fill=gold)
    T.save(img, os.path.join(GUI, "skill_book.png"))
    print("gui/skill_book.png")


def advancement_bg():
    """32x32 tileable dark cloud pattern used behind the advancement tab."""
    n = T.tileable_noise(32, 32, 123, scale=2, octaves=3)
    img = T.colorize(n, (70, 55, 100), 0.45)
    base = Image.new("RGBA", img.size, (28, 20, 44, 255))
    base.alpha_composite(img)
    d = ImageDraw.Draw(base)
    rnd = random.Random(5)
    for _ in range(14):  # faint stars
        x, y = rnd.uniform(0, 32), rnd.uniform(0, 32)
        a = rnd.randint(60, 160)
        d.ellipse([(x - 0.35) * S, (y - 0.35) * S, (x + 0.35) * S, (y + 0.35) * S], fill=(220, 210, 255, a))
    save(base, GUI, "advancement_bg", 32, 32)


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


def icon_vajra_palm():
    col, col2 = ELEMENT["dao"]
    img = icon_bg(col, col2)
    # open palm: palm disc + five fingers, with a seal ring behind
    T.ellipse_glow(img, (5, 5, 27, 27), col, glow=1.5, fill_alpha=0)
    T.ellipse_glow(img, (11, 15, 21, 27), col2, glow=1.5)
    for (x0, y0, x1, y1) in [(9, 16, 6, 8), (12, 14, 11, 4), (16, 14, 16, 3), (20, 14, 21, 4), (22, 17, 27, 11)]:
        T.polyline_glow(img, [(x0, y0), (x1, y1)], col2, 2.4, glow=1.2)
    icon_finish(img, "vajra_palm")


def icon_wind_dragon():
    col, col2 = ELEMENT["wind"]
    img = icon_bg(col, col2)
    # tornado: stacked rings shrinking downward
    for i, (w, y) in enumerate([(12, 8), (9.5, 13), (7, 18), (4.5, 23), (2.5, 28)]):
        T.arc_glow(img, (16 - w, y - 2.2, 16 + w, y + 2.2), 0, 360, col2 if i % 2 == 0 else col, 1.6, glow=1.2)
    # dragon head at the rim
    T.polygon_glow(img, [(21, 2), (30, 5), (27, 10), (20, 8)], col2, glow=1.5)
    T.polyline_glow(img, [(22, 3), (20, 0.5)], col2, 1.2, glow=0.6)
    T.ellipse_glow(img, (25, 5, 27, 7), (40, 90, 70), glow=0.3)
    icon_finish(img, "wind_dragon")


def icon_golden_body():
    col, col2 = ELEMENT["dao"]
    img = icon_bg(col, col2)
    # sun disc behind a meditating silhouette
    img.alpha_composite(T.radial(32, 32, col + (200,), col + (0,), power=1.6, radius=13))
    T.ellipse_glow(img, (12, 5, 20, 13), (90, 60, 20), glow=1.5)                    # head
    T.polygon_glow(img, [(9, 14), (23, 14), (27, 26), (5, 26)], (90, 60, 20), glow=1.5)  # body
    T.ellipse_glow(img, (13, 6, 19, 12), col2, glow=0.5)
    T.polygon_glow(img, [(10, 15), (22, 15), (25.5, 25), (6.5, 25)], col2, glow=0.5)
    T.polyline_glow(img, [(3, 26), (29, 26)], col2, 2.0, glow=1.0)          # crossed legs line
    for k in range(8):
        a = k * math.pi / 4
        T.polyline_glow(img, [(16 + math.cos(a) * 13, 16 + math.sin(a) * 13), (16 + math.cos(a) * 15.5, 16 + math.sin(a) * 15.5)], col, 1.5, glow=1.0)
    icon_finish(img, "golden_body")


def icon_thunder_dragon():
    col, col2 = ELEMENT["lightning"]
    img = icon_bg(col, col2)
    # serpentine lightning body
    pts = [(3, 26), (8, 20), (7, 15), (13, 12), (14, 7), (21, 6)]
    T.polyline_glow(img, pts, col, 4.0, glow=2.5, core=False)
    T.polyline_glow(img, pts, col2, 1.8, glow=1.2)
    # head with horns
    T.polygon_glow(img, [(20, 3), (29, 5), (27, 11), (20, 10)], col2, glow=1.5)
    T.polyline_glow(img, [(22, 4), (19, 1)], col2, 1.2, glow=0.8)
    T.polyline_glow(img, [(26, 4), (27, 1)], col2, 1.2, glow=0.8)
    T.ellipse_glow(img, (25, 6, 27, 8), (255, 255, 255), glow=0.6)
    icon_finish(img, "thunder_dragon")


# ---------------------------------------------------------------- 1.4.0 cultivation GUI

ROOT_COLORS = {
    "metal": ((0xE8, 0xEC, 0xF2), (0xFF, 0xFF, 0xFF)),
    "wood": ((0x7C, 0xDC, 0x7C), (0xD8, 0xFF, 0xC0)),
    "water": ((0x6F, 0xB8, 0xFF), (0xD8, 0xF2, 0xFF)),
    "fire": ((0xFF, 0x7A, 0x3A), (0xFF, 0xE0, 0x9A)),
    "earth": ((0xD9, 0xB3, 0x6A), (0xFF, 0xF0, 0xC0)),
    "thunder": ((0xC9, 0x8B, 0xFF), (0xF2, 0xE6, 0xFF)),
    "dark": ((0x8A, 0x3B, 0xD6), (0xD2, 0x8B, 0xFF)),
}

RARITY_COLORS = {
    "cursed": ((0x55, 0x55, 0x60), (0x90, 0x90, 0xA0)),
    "common": ((0xD8, 0xD8, 0xD8), (0xFF, 0xFF, 0xFF)),
    "uncommon": ((0x6C, 0xE0, 0x6C), (0xD0, 0xFF, 0xC0)),
    "rare": ((0x5A, 0xD8, 0xFF), (0xD0, 0xF8, 0xFF)),
    "legendary": ((0xFF, 0xC8, 0x40), (0xFF, 0xF0, 0xB0)),
}


def root_icon(kind):
    """32x32 spirit-root icon (textures/gui/roots/<kind>.png): round jade plate + element glyph."""
    col, col2 = ROOT_COLORS[kind]
    img = T.new(32, 32)
    d = ImageDraw.Draw(img)
    d.ellipse([1 * S, 1 * S, 31 * S - 1, 31 * S - 1], fill=dk(col, 0.22) + (255,))
    img.alpha_composite(T.radial(32, 32, col + (140,), dk(col, 0.35) + (0,), power=1.5, radius=14))
    if kind == "metal":
        # a straight sword blade with a short cross guard
        T.polygon_glow(img, [(15, 26), (16, 5), (17, 26)], col2, glow=1.6)
        T.polyline_glow(img, [(11, 22), (21, 22)], (210, 220, 235), 2.0, glow=1.0)
        T.polyline_glow(img, [(16, 22), (16, 28)], (120, 130, 150), 2.0, glow=0.6)
    elif kind == "wood":
        # a sprouting tree: trunk and three leaves
        T.polyline_glow(img, [(16, 28), (16, 14)], (120, 90, 50), 2.4, glow=0.6)
        for (cx, cy, rot) in [(16, 8, 90), (10, 14, 150), (22, 14, 30)]:
            a = math.radians(rot)
            pts = [(cx + 5 * math.cos(a), cy - 5 * math.sin(a)), (cx + 4 * math.cos(a + 1.7), cy - 4 * math.sin(a + 1.7)),
                   (cx - 5 * math.cos(a), cy + 5 * math.sin(a)), (cx + 4 * math.cos(a - 1.7), cy - 4 * math.sin(a - 1.7))]
            T.polygon_glow(img, pts, col, glow=1.2)
        T.polygon_glow(img, [(16, 4), (18, 8), (16, 12), (14, 8)], col2, glow=1.0)
    elif kind == "water":
        # a droplet with two ripples underneath
        T.polygon_glow(img, [(16, 4), (21, 12), (22, 17), (19, 21), (13, 21), (10, 17), (11, 12)], col, glow=1.6)
        T.polygon_glow(img, [(15, 12), (17.5, 15.5), (17, 18.5), (14, 18), (13.5, 15)], col2, glow=1.0, fill_alpha=200)
        for y, w in ((24, 8), (27, 12)):
            T.arc_glow(img, [16 - w, y - 2, 16 + w, y + 2], 0, 180, col, 1.6, glow=0.8)
    elif kind == "fire":
        T.polygon_glow(img, [(16, 3), (22, 12), (25, 20), (21, 28), (11, 28), (7, 20), (10, 12)], col, glow=1.8)
        T.polygon_glow(img, [(16, 11), (20, 17), (20.5, 22.5), (16, 27), (11.5, 22.5), (12, 17)], col2, glow=1.2)
        T.polygon_glow(img, [(16, 17), (18, 21), (16, 25.5), (14, 21)], WHITE, glow=0.8)
    elif kind == "earth":
        # a mountain with a sun disc
        T.polygon_glow(img, [(4, 27), (12, 12), (16, 18), (21, 9), (28, 27)], col, glow=1.4)
        T.polygon_glow(img, [(12, 12), (14, 16), (10, 16)], col2, glow=0.8)
        T.polygon_glow(img, [(21, 9), (23.5, 14), (18.5, 14)], col2, glow=0.8)
        T.polyline_glow(img, [(3, 28), (29, 28)], (200, 170, 110), 1.8, glow=0.8)
    elif kind == "thunder":
        rnd = random.Random(11)
        T.polyline_glow(img, [(18, 3), (12, 15), (17, 15), (13, 29)], col2, 2.4, glow=2.0)
        T.polyline_glow(img, [(18, 3), (12, 15), (17, 15), (13, 29)], WHITE, 1.0, glow=0.5)
        for k in range(3):
            pts = T.lightning_points(15 + rnd.uniform(-4, 4), 10 + k * 6, 24 + rnd.uniform(-3, 3), 12 + k * 6, 3, 1.5, rnd)
            T.polyline_glow(img, pts, col, 1.0, glow=1.0)
    elif kind == "dark":
        # a crescent moon with a swallowing vortex
        T.polygon_glow(img, T.crescent(17, 15, 10, 4.5, 40), col2, glow=1.8)
        for k in range(3):
            a0 = 60 + k * 120
            T.arc_glow(img, [8, 8, 24, 24], a0, a0 + 70, col, 1.6, glow=1.4)
        T.polygon_glow(img, T.star_points(9, 9, 2.5, 1, 4), WHITE, glow=1.0, fill_alpha=220)
    d = ImageDraw.Draw(img)
    d.ellipse([1 * S, 1 * S, 31 * S - 1, 31 * S - 1], outline=col + (255,), width=S)
    save(img, os.path.join(GUI, "roots"), kind, 32, 32)


def talent_seal(rarity):
    """32x32 talent seal (textures/gui/talent_<rarity>.png): square seal with as many stars as the rank."""
    col, col2 = RARITY_COLORS[rarity]
    rank = ["cursed", "common", "uncommon", "rare", "legendary"].index(rarity)
    img = T.new(32, 32)
    d = ImageDraw.Draw(img)
    d.rounded_rectangle([2 * S, 2 * S, 30 * S - 1, 30 * S - 1], radius=4 * S, fill=dk(col, 0.2) + (255,))
    img.alpha_composite(T.radial(32, 32, col + (110 + 25 * rank,), dk(col, 0.3) + (0,), power=1.5, radius=14))
    # inner seal ring
    T.arc_glow(img, [6, 6, 26, 26], 0, 360, col, 1.4, glow=1.0 + 0.3 * rank)
    if rarity == "cursed":
        # a cracked seal: an X of two dark fissures
        T.polyline_glow(img, [(10, 10), (22, 22)], (30, 20, 40), 2.2, glow=0.4)
        T.polyline_glow(img, [(22, 10), (10, 22)], (30, 20, 40), 2.2, glow=0.4)
        T.polyline_glow(img, [(10, 10), (22, 22)], col2, 0.9, glow=0.8)
        T.polyline_glow(img, [(22, 10), (10, 22)], col2, 0.9, glow=0.8)
    else:
        # central big star, plus smaller satellite stars for higher ranks
        T.polygon_glow(img, T.star_points(16, 16, 6.5, 2.8, 5, rot=-math.pi / 2), col2, glow=1.4 + 0.4 * rank, fill_alpha=240)
        satellites = {1: [], 2: [(16, 6)], 3: [(9, 8), (23, 8)], 4: [(8, 9), (24, 9), (16, 26)]}[rank]
        for (sx, sy) in satellites:
            T.polygon_glow(img, T.star_points(sx, sy, 2.4, 1.0, 4), WHITE, glow=1.0, fill_alpha=230)
    if rarity == "legendary":
        # golden corner flourishes
        for (cx, cy, sx, sy) in [(4, 4, 1, 1), (28, 4, -1, 1), (4, 28, 1, -1), (28, 28, -1, -1)]:
            T.polyline_glow(img, [(cx, cy + sy * 5), (cx, cy), (cx + sx * 5, cy)], col2, 1.4, glow=1.2)
    d = ImageDraw.Draw(img)
    d.rounded_rectangle([2 * S, 2 * S, 30 * S - 1, 30 * S - 1], radius=4 * S, outline=col + (255,), width=S)
    save(img, GUI, "talent_" + rarity, 32, 32)


def effect_icon_qi_deviation():
    def qi_deviation(img):
        red = (220, 30, 40)
        img.alpha_composite(T.radial(32, 32, (120, 0, 10, 170), (40, 0, 5, 0), power=1.2, radius=14))
        # a cracked, inverted qi swirl: two black arcs, red fissures bleeding out
        for a0 in (20, 200):
            T.arc_glow(img, [7, 7, 25, 25], a0, a0 + 130, (20, 6, 10), 3.0, glow=0.4)
            T.arc_glow(img, [7, 7, 25, 25], a0, a0 + 130, red, 1.2, glow=1.4)
        rnd = random.Random(4)
        for (x1, y1) in [(4, 5), (28, 6), (26, 28), (5, 27)]:
            pts = T.lightning_points(16, 16, x1, y1, 4, 1.4, rnd)
            T.polyline_glow(img, pts, red, 1.0, glow=1.2)
        T.polygon_glow(img, T.star_points(16, 16, 3.2, 1.3, 4), (255, 90, 90), glow=1.2, fill_alpha=240)
    effect_icon("qi_deviation", qi_deviation)


def gui_cultivation():
    """256x256 texture; the panel occupies 256x222 at the top (layout mirrors CultivationScreen)."""
    W, H = 256, 256
    PH = 222
    img = Image.new("RGBA", (W, H))
    n = T.noise_layer(256, PH, 91, scale=6, octaves=3, ss=False)
    paper = Image.new("RGBA", (256, PH), (46, 40, 70, 255))
    tint = T.colorize(n, (80, 70, 120), 0.5)
    paper.alpha_composite(tint)
    img.paste(paper, (0, 0))
    # faint taiji / bagua watermark behind the left column
    wm = T.new(120, 120)
    T.arc_glow(wm, [8, 8, 112, 112], 0, 360, (150, 130, 190), 1.2, glow=1.0)
    for k in range(8):
        a = math.radians(k * 45)
        x0, y0 = 60 + 46 * math.cos(a), 60 + 46 * math.sin(a)
        x1, y1 = 60 + 54 * math.cos(a), 60 + 54 * math.sin(a)
        T.polyline_glow(wm, [(x0, y0), (x1, y1)], (150, 130, 190), 1.4, glow=0.8)
    T.arc_glow(wm, [30, 30, 90, 90], 0, 360, (150, 130, 190), 1.0, glow=0.8)
    wm = T.finish(wm, 120, 120)
    a = wm.split()[3].point(lambda v: int(v * 0.22))
    wm.putalpha(a)
    img.alpha_composite(wm, (8, 60))
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, 255, PH - 1], outline=(230, 195, 110, 255), width=2)
    d.rectangle([3, 3, 252, PH - 4], outline=(120, 95, 50, 255), width=1)
    # title bar
    d.rectangle([6, 6, 249, 24], fill=(30, 22, 44, 220), outline=(230, 195, 110, 255))
    # left column (realm / progress / spirit qi) and right column (root / talent / aptitude)
    d.rectangle([8, 27, 127, 190], fill=(24, 18, 36, 190), outline=(160, 130, 80, 255))
    d.rectangle([130, 27, 249, 190], fill=(24, 18, 36, 190), outline=(160, 130, 80, 255))
    # column header strips
    d.rectangle([9, 28, 126, 39], fill=(40, 32, 58, 255))
    d.rectangle([131, 28, 248, 39], fill=(40, 32, 58, 255))
    # button row
    d.rectangle([8, 193, 249, 218], fill=(24, 18, 36, 200), outline=(160, 130, 80, 255))
    gold = (230, 195, 110, 255)
    for (cx, cy, sx, sy) in [(6, 6, 1, 1), (249, 6, -1, 1), (6, PH - 7, 1, -1), (249, PH - 7, -1, -1)]:
        d.line([(cx, cy), (cx + sx * 10, cy)], fill=gold, width=2)
        d.line([(cx, cy), (cx, cy + sy * 10)], fill=gold, width=2)
        d.point((cx + sx * 3, cy + sy * 3), fill=gold)
    T.save(img, os.path.join(GUI, "cultivation.png"))
    print("gui/cultivation.png")


def cultivation_assets():
    os.makedirs(os.path.join(GUI, "roots"), exist_ok=True)
    for kind in ROOT_COLORS:
        root_icon(kind)
    for rarity in RARITY_COLORS:
        talent_seal(rarity)
    effect_icon_qi_deviation()
    gui_cultivation()


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
    icon_vajra_palm(); icon_wind_dragon(); icon_golden_body(); icon_thunder_dragon()
    icon_heaven_hand(); effect_icons()
    item_pill("qi_pill", (80, 220, 200))
    item_pill("heaven_pill", (255, 215, 90))
    gui_hud(); gui_skill_book(); advancement_bg(); mod_icon()
    cultivation_assets()


if __name__ == "__main__":
    main()
