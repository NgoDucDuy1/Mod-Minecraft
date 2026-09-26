#!/usr/bin/env python3
"""Single source of truth for the 78 linh dược (herbs).

Emits, from the table below:
  * src/main/java/.../alchemy/HerbList.java                (the Java data table)
  * assets: blockstates, block models (3 growth stages), item models, lang (vi/en, merged)
  * data:   loot tables, worldgen configured/placed features (one common + one rare patch per habitat)
  * textures/block/herb/<key>_{0,1,2}.png  (32x32, procedural plants; stage 2 doubles as the item icon)

    python3 tools/gen_herbs.py
"""
import json
import math
import os
import random
import sys
from collections import OrderedDict

sys.path.insert(0, os.path.dirname(__file__))
from PIL import Image, ImageDraw  # noqa: E402
import texlib as T  # noqa: E402

ROOT = os.path.join(os.path.dirname(__file__), "..", "src", "main")
JAVA = os.path.join(ROOT, "java", "com", "ngoducduy", "celestialarts", "alchemy", "HerbList.java")
ASSETS = os.path.join(ROOT, "resources", "assets", "celestialarts")
DATA = os.path.join(ROOT, "resources", "data", "celestialarts")
TEX = os.path.join(ASSETS, "textures", "block", "herb")

S = T.SS

# key, vi, en, element, grade, habitat, shape, nature
HERBS = [
    # ---- Kim (SWORD)
    ("bach_kim_thao", "Bạch Kim Thảo", "Platinum Grass", "SWORD", 1, "MOUNTAIN", "grass", 1),
    ("ngan_diep_hoa", "Ngân Diệp Hoa", "Silverleaf Flower", "SWORD", 1, "MEADOW", "flower", 0),
    ("thiet_can_thao", "Thiết Căn Thảo", "Ironroot Grass", "SWORD", 1, "CAVE", "grass", 1),
    ("kim_tuyen_lan", "Kim Tuyến Lan", "Goldthread Orchid", "SWORD", 2, "FOREST", "flower", 0),
    ("bach_nhan_cuc", "Bạch Nhận Cúc", "White Blade Chrysanthemum", "SWORD", 2, "MOUNTAIN", "flower", 1),
    ("kim_cuong_chi", "Kim Cương Chi", "Diamond Lingzhi", "SWORD", 2, "CAVE", "mushroom", 1),
    ("ngan_long_tu", "Ngân Long Tu", "Silver Dragon Whisker", "SWORD", 3, "MOUNTAIN", "fern", 1),
    ("canh_kim_nha", "Canh Kim Nha", "Geng-Metal Sprout", "SWORD", 3, "DEEP", "crystal", 2),
    ("thai_bach_tinh_hoa", "Thái Bạch Tinh Hoa", "Venus Star Flower", "SWORD", 4, "SNOW", "flower", 0),
    ("thien_kim_lien", "Thiên Kim Liên", "Heavenly Gold Lotus", "SWORD", 5, "END", "lotus", 1),
    # ---- Mộc (WIND)
    ("thanh_diep_thao", "Thanh Diệp Thảo", "Greenleaf Grass", "WIND", 1, "FOREST", "grass", 0),
    ("luc_tam_thao", "Lục Tâm Thảo", "Greenheart Grass", "WIND", 1, "MEADOW", "grass", -1),
    ("xuan_phong_hoa", "Xuân Phong Hoa", "Spring Breeze Blossom", "WIND", 1, "CHERRY", "flower", 0),
    ("bich_ngoc_dang", "Bích Ngọc Đằng", "Jade Vine", "WIND", 2, "JUNGLE", "vine", -1),
    ("thanh_moc_chi", "Thanh Mộc Chi", "Azure Wood Lingzhi", "WIND", 2, "FOREST", "mushroom", 0),
    ("phong_linh_thao", "Phong Linh Thảo", "Windchime Grass", "WIND", 2, "MOUNTAIN", "grass", 0),
    ("van_nien_duong_sam", "Vạn Niên Dương Sâm", "Ten-Thousand-Year Ginseng", "WIND", 3, "FOREST", "bush", 1),
    ("thuy_luc_tam_lan", "Thúy Lục Tâm Lan", "Emerald Heart Orchid", "WIND", 3, "JUNGLE", "flower", 0),
    ("thanh_long_moc_nha", "Thanh Long Mộc Nha", "Azure Dragon Sprout", "WIND", 4, "LUSH", "bush", 0),
    ("kien_moc_linh_nha", "Kiến Mộc Linh Nha", "Jianmu Spirit Sprout", "WIND", 5, "JUNGLE", "bush", 0),
    # ---- Thủy (ICE)
    ("han_thuy_thao", "Hàn Thủy Thảo", "Coldwater Grass", "ICE", 1, "SNOW", "grass", -2),
    ("thanh_thuy_lien", "Thanh Thủy Liên", "Clearwater Lotus", "ICE", 1, "SWAMP", "lotus", -1),
    ("bang_tam_hoa", "Băng Tâm Hoa", "Iceheart Flower", "ICE", 1, "SNOW", "flower", -2),
    ("hai_nguyet_thao", "Hải Nguyệt Thảo", "Sea Moon Grass", "ICE", 2, "BEACH", "grass", -1),
    ("tuyet_lien", "Tuyết Liên", "Snow Lotus", "ICE", 2, "SNOW", "lotus", -2),
    ("huyen_thuy_chi", "Huyền Thủy Chi", "Dark Water Lingzhi", "ICE", 2, "SWAMP", "mushroom", -1),
    ("bang_phach_lan", "Băng Phách Lan", "Ice Soul Orchid", "ICE", 3, "SNOW", "flower", -3),
    ("thien_son_tuyet_lien", "Thiên Sơn Tuyết Liên", "Tianshan Snow Lotus", "ICE", 3, "MOUNTAIN", "lotus", -2),
    ("cuu_u_huyen_bang_thao", "Cửu U Huyền Băng Thảo", "Nine-Nether Black Ice Grass", "ICE", 4, "DEEP", "crystal", -3),
    ("thai_am_thuy_tinh_hoa", "Thái Âm Thủy Tinh Hoa", "Lunar Crystal Flower", "ICE", 5, "SNOW", "crystal", -3),
    # ---- Hỏa (FIRE)
    ("xich_diem_thao", "Xích Diễm Thảo", "Crimson Flame Grass", "FIRE", 1, "DESERT", "grass", 2),
    ("hoa_lien_tu", "Hỏa Liên Tử", "Fire Lotus Seedling", "FIRE", 1, "DESERT", "flower", 2),
    ("dan_sa_thao", "Đan Sa Thảo", "Cinnabar Grass", "FIRE", 1, "CAVE", "grass", 1),
    ("liet_duong_hoa", "Liệt Dương Hoa", "Blazing Sun Flower", "FIRE", 2, "DESERT", "flower", 3),
    ("diem_tam_chi", "Diễm Tâm Chi", "Flameheart Lingzhi", "FIRE", 2, "NETHER", "fungus", 2),
    ("xich_ha_dang", "Xích Hà Đằng", "Red Mist Vine", "FIRE", 2, "NETHER", "vine", 2),
    ("hoa_long_thao", "Hỏa Long Thảo", "Fire Dragon Grass", "FIRE", 3, "NETHER", "spike", 3),
    ("dia_tam_hoa_lien", "Địa Tâm Hỏa Liên", "Earth-Core Fire Lotus", "FIRE", 3, "DEEP", "lotus", 3),
    ("chu_tuoc_vu_hoa", "Chu Tước Vũ Hoa", "Vermilion Bird Plume", "FIRE", 4, "NETHER", "flower", 3),
    ("thai_duong_kim_lien", "Thái Dương Kim Liên", "Solar Golden Lotus", "FIRE", 5, "NETHER", "lotus", 3),
    # ---- Thổ (EARTH)
    ("hoang_tinh_thao", "Hoàng Tinh Thảo", "Yellow Essence Grass", "EARTH", 1, "MEADOW", "grass", 0),
    ("tho_linh_chi", "Thổ Linh Chi", "Earth Lingzhi", "EARTH", 1, "CAVE", "mushroom", 0),
    ("son_can_thao", "Sơn Căn Thảo", "Mountain Root Grass", "EARTH", 1, "MOUNTAIN", "bush", 0),
    ("hoang_ngoc_sam", "Hoàng Ngọc Sâm", "Topaz Ginseng", "EARTH", 2, "FOREST", "bush", 1),
    ("thach_nhu_hoa", "Thạch Nhũ Hoa", "Stalactite Flower", "EARTH", 2, "CAVE", "crystal", 0),
    ("hau_tho_thao", "Hậu Thổ Thảo", "Thick Earth Grass", "EARTH", 2, "DESERT", "grass", 1),
    ("dia_long_sam", "Địa Long Sâm", "Earth Dragon Ginseng", "EARTH", 3, "DEEP", "bush", 0),
    ("huyen_nham_chi", "Huyền Nham Chi", "Black Rock Lingzhi", "EARTH", 3, "DEEP", "mushroom", 0),
    ("tu_ngoc_sam_vuong", "Tử Ngọc Sâm Vương", "Amethyst Ginseng King", "EARTH", 4, "LUSH", "bush", 1),
    ("tho_tam_cuu_chuyen_lien", "Thổ Tâm Cửu Chuyển Liên", "Nine-Turn Earthheart Lotus", "EARTH", 5, "DEEP", "lotus", 0),
    # ---- Lôi (LIGHTNING)
    ("tu_dien_thao", "Tử Điện Thảo", "Purple Lightning Grass", "LIGHTNING", 1, "MOUNTAIN", "grass", 1),
    ("loi_minh_hoa", "Lôi Minh Hoa", "Thunderclap Flower", "LIGHTNING", 1, "MEADOW", "flower", 1),
    ("tu_quang_chi", "Tử Quang Chi", "Violet Light Lingzhi", "LIGHTNING", 1, "CAVE", "mushroom", 0),
    ("tich_loi_moc_nha", "Tích Lôi Mộc Nha", "Thunderstruck Sprout", "LIGHTNING", 2, "FOREST", "bush", 1),
    ("loi_trach_thao", "Lôi Trạch Thảo", "Thunder Marsh Grass", "LIGHTNING", 2, "SWAMP", "grass", 0),
    ("tu_tieu_lan", "Tử Tiêu Lan", "Purple Firmament Orchid", "LIGHTNING", 2, "MOUNTAIN", "flower", 1),
    ("cuu_thien_loi_truc", "Cửu Thiên Lôi Trúc", "Nine-Heaven Thunder Bamboo", "LIGHTNING", 3, "MOUNTAIN", "spike", 2),
    ("tu_loi_tinh_thao", "Tử Lôi Tinh Thảo", "Purple Thunder Star Grass", "LIGHTNING", 3, "END", "crystal", 1),
    ("loi_kiep_hoa", "Lôi Kiếp Hoa", "Tribulation Flower", "LIGHTNING", 4, "MOUNTAIN", "flower", 2),
    ("thien_loi_tu_lien", "Thiên Lôi Tử Liên", "Heavenly Thunder Purple Lotus", "LIGHTNING", 5, "END", "lotus", 2),
    # ---- Ám (VOID)
    ("u_minh_thao", "U Minh Thảo", "Netherworld Grass", "VOID", 1, "SWAMP", "grass", -1),
    ("hac_tam_chi", "Hắc Tâm Chi", "Blackheart Lingzhi", "VOID", 1, "DEEP", "mushroom", -1),
    ("da_anh_hoa", "Dạ Ảnh Hoa", "Night Shadow Flower", "VOID", 1, "FOREST", "flower", -1),
    ("am_ngoc_dang", "Âm Ngọc Đằng", "Yin Jade Vine", "VOID", 2, "SWAMP", "vine", -2),
    ("quy_dien_thao", "Quỷ Diện Thảo", "Ghostface Grass", "VOID", 2, "DEEP", "grass", -1),
    ("hu_khong_chi", "Hư Không Chi", "Void Lingzhi", "VOID", 2, "END", "mushroom", 0),
    ("vong_xuyen_hoa", "Vong Xuyên Hoa", "Flower of Oblivion", "VOID", 3, "DEEP", "flower", -2),
    ("u_hon_lan", "U Hồn Lan", "Wraith Orchid", "VOID", 3, "NETHER", "flower", 1),
    ("minh_ha_thao", "Minh Hà Thảo", "River Styx Grass", "VOID", 4, "END", "grass", -2),
    ("hon_don_hac_lien", "Hỗn Độn Hắc Liên", "Chaos Black Lotus", "VOID", 5, "END", "lotus", -1),
    # ---- Đạo (DAO)
    ("linh_khi_thao", "Linh Khí Thảo", "Spirit Qi Grass", "DAO", 2, "MEADOW", "grass", 0),
    ("thanh_tam_lien", "Thanh Tâm Liên", "Clear Heart Lotus", "DAO", 3, "LUSH", "lotus", -1),
    ("ngo_dao_hoa", "Ngộ Đạo Hoa", "Enlightenment Blossom", "DAO", 3, "CHERRY", "flower", 0),
    ("truc_co_thao", "Trúc Cơ Thảo", "Foundation Grass", "DAO", 4, "MOUNTAIN", "bush", 0),
    ("nguyen_anh_thao", "Nguyên Anh Thảo", "Nascent Soul Grass", "DAO", 4, "LUSH", "bush", 0),
    ("thien_dao_kim_lien", "Thiên Đạo Kim Liên", "Heavenly Dao Golden Lotus", "DAO", 5, "END", "lotus", 1),
    ("bat_tu_thao", "Bất Tử Thảo", "Undying Grass", "DAO", 5, "CHERRY", "grass", 0),
    ("hon_don_thanh_lien", "Hỗn Độn Thanh Liên", "Chaos Green Lotus", "DAO", 5, "LUSH", "lotus", 0),
]

ELEMENT_COLORS = {
    "SWORD": ((0xE0, 0xE8, 0xF0), (0xC0, 0xCC, 0xD8), (0xFF, 0xFF, 0xFF)),
    "WIND": ((0x66, 0xC8, 0x6A), (0x3E, 0x92, 0x46), (0xC8, 0xFF, 0xD0)),
    "ICE": ((0x7C, 0xC4, 0xFF), (0x4A, 0x8C, 0xD8), (0xE0, 0xF6, 0xFF)),
    "FIRE": ((0xFF, 0x7A, 0x2A), (0xC8, 0x3A, 0x18), (0xFF, 0xE0, 0x7A)),
    "EARTH": ((0xD2, 0xA8, 0x5A), (0x8E, 0x6A, 0x36), (0xFF, 0xF0, 0xB0)),
    "LIGHTNING": ((0xB5, 0x7B, 0xFF), (0x7A, 0x48, 0xC8), (0xF2, 0xE6, 0xFF)),
    "VOID": ((0x7A, 0x3A, 0xB8), (0x3A, 0x18, 0x5A), (0xD2, 0x8B, 0xFF)),
    "DAO": ((0xFF, 0xE0, 0x8A), (0xD8, 0xA8, 0x40), (0xFF, 0xFF, 0xFF)),
}

HABITAT_VI = {
    "MEADOW": "đồng cỏ", "FOREST": "rừng", "JUNGLE": "rừng rậm", "SWAMP": "đầm lầy", "MOUNTAIN": "núi cao",
    "SNOW": "tuyết sơn", "DESERT": "sa mạc / hoang mạc", "BEACH": "bờ biển", "CHERRY": "rừng anh đào",
    "CAVE": "hang động", "DEEP": "vực sâu", "LUSH": "hang rêu", "NETHER": "Địa Ngục", "END": "Tận Thế",
}
HABITAT_EN = {
    "MEADOW": "meadows", "FOREST": "forests", "JUNGLE": "jungles", "SWAMP": "swamps", "MOUNTAIN": "mountains",
    "SNOW": "snowy peaks", "DESERT": "deserts / badlands", "BEACH": "shores", "CHERRY": "cherry groves",
    "CAVE": "caves", "DEEP": "the deep", "LUSH": "lush caves", "NETHER": "the Nether", "END": "the End",
}


def potency(grade, shape):
    base = {1: 2, 2: 4, 3: 6, 4: 8, 5: 11}[grade]
    return base + (1 if shape in ("lotus", "bush") else 0)


def toxicity(element, grade, nature):
    t = 1 if element == "VOID" else 0
    if abs(nature) == 3:
        t += 1
    if element == "FIRE" and grade >= 3:
        t += 1
    return min(5, t)


def rgb_hex(c):
    return "0x%02X%02X%02X" % c


# ------------------------------------------------------------------ textures

def draw_plant(img, shape, col, dark, light, rnd, scale, stage):
    """Draws one plant into a 32x32 (super-sampled) canvas. scale 0..1 shrinks it towards the ground."""
    d = ImageDraw.Draw(img)
    gx, gy = 16, 30  # ground anchor
    h = 22 * scale + 4

    def P(x, y):
        return (x, y)

    stem = dark if shape in ("bush", "vine", "spike") else (0x4E, 0x8A, 0x3C)
    if shape == "grass":
        blades = 5 if stage == 2 else 3
        for i in range(blades):
            ang = (i - (blades - 1) / 2) * 0.42 + rnd.uniform(-0.1, 0.1)
            L = h * rnd.uniform(0.7, 1.0)
            tipx = gx + math.sin(ang) * L * 0.9
            tipy = gy - math.cos(ang) * L
            midx = gx + math.sin(ang) * L * 0.45 + rnd.uniform(-1, 1)
            midy = gy - math.cos(ang) * L * 0.5
            T.polygon_glow(img, [P(gx - 1.2, gy), P(midx - 1.0, midy), P(tipx, tipy), P(midx + 1.0, midy), P(gx + 1.2, gy)], col if i % 2 else dark, glow=0.0)
            T.polyline_glow(img, [P(gx, gy), P(midx, midy), P(tipx, tipy)], light, 0.5, glow=0.3, core=True)
    elif shape == "flower":
        T.polyline_glow(img, [P(gx, gy), P(gx + 0.5, gy - h * 0.55), P(gx, gy - h * 0.8)], stem, 1.6, glow=0.0, core=False)
        for sgn in (-1, 1):
            ly = gy - h * rnd.uniform(0.3, 0.5)
            T.polygon_glow(img, [P(gx, ly), P(gx + sgn * 4 * scale, ly - 3 * scale), P(gx + sgn * 7 * scale, ly), P(gx + sgn * 4 * scale, ly + 2 * scale)], (0x4E, 0x8A, 0x3C), glow=0.0)
        cx, cy = gx, gy - h * 0.82
        petals = rnd.choice([5, 6, 8])
        pr = 5.5 * scale + 1
        for i in range(petals):
            a = i * 2 * math.pi / petals + rnd.uniform(-0.1, 0.1)
            px, py = cx + math.cos(a) * pr, cy + math.sin(a) * pr * 0.8
            T.ellipse_glow(img, [px - 2.6 * scale - 0.5, py - 1.8 * scale - 0.5, px + 2.6 * scale + 0.5, py + 1.8 * scale + 0.5], col if i % 2 else light, glow=0.2)
        T.ellipse_glow(img, [cx - 2.2, cy - 2.2, cx + 2.2, cy + 2.2], light if rnd.random() < 0.5 else (0xFF, 0xE4, 0x7A), glow=0.8)
    elif shape == "lotus":
        pad = 9 * scale + 2
        T.ellipse_glow(img, [gx - pad, gy - 3.5, gx + pad, gy + 1.5], (0x3E, 0x92, 0x46), glow=0.0)
        T.ellipse_glow(img, [gx - pad * 0.7, gy - 2.5, gx + pad * 0.7, gy + 0.5], (0x5A, 0xB0, 0x5E), glow=0.0)
        cx, cy = gx, gy - h * 0.55 - 2
        T.polyline_glow(img, [P(gx, gy - 1), P(cx, cy + 2)], (0x4E, 0x8A, 0x3C), 1.4, glow=0.0, core=False)
        for i in range(7):
            a = math.pi + i * math.pi / 6
            px, py = cx + math.cos(a) * 6 * scale, cy + math.sin(a) * 6 * scale
            T.polygon_glow(img, [P(cx, cy + 3), P(px - 2, py - 1), P(px, py - 4 * scale), P(px + 2, py - 1)], col if i % 2 else light, glow=0.3)
        T.ellipse_glow(img, [cx - 2.5, cy - 1, cx + 2.5, cy + 3], (0xFF, 0xE0, 0x7A), glow=0.9)
    elif shape == "bush":
        for i in range(3):
            ang = (i - 1) * 0.55
            L = h * 0.85
            T.polyline_glow(img, [P(gx, gy), P(gx + math.sin(ang) * L, gy - math.cos(ang) * L)], stem, 1.5, glow=0.0, core=False)
        for i in range(9 if stage == 2 else 5):
            a = rnd.uniform(-1.4, 1.4)
            r = rnd.uniform(0.3, 1.0) * h * 0.9
            px, py = gx + math.sin(a) * r * 0.9, gy - math.cos(a) * r - 1
            T.ellipse_glow(img, [px - 3 * scale - 0.8, py - 2.2 * scale - 0.6, px + 3 * scale + 0.8, py + 2.2 * scale + 0.6], col if i % 3 else dark, glow=0.0)
        for i in range(4 if stage == 2 else 1):
            px, py = gx + rnd.uniform(-6, 6) * scale, gy - h * rnd.uniform(0.4, 0.9)
            T.ellipse_glow(img, [px - 1.6, py - 1.6, px + 1.6, py + 1.6], light, glow=0.9)
    elif shape == "mushroom" or shape == "fungus":
        capw = 9 * scale + 2
        caph = 5 * scale + 1.5
        cy = gy - h * 0.7
        T.polygon_glow(img, [P(gx - 2.2, gy), P(gx - 1.6, cy), P(gx + 1.6, cy), P(gx + 2.2, gy)], (0xE8, 0xDC, 0xC0) if shape == "mushroom" else dark, glow=0.0)
        T.ellipse_glow(img, [gx - capw, cy - caph, gx + capw, cy + caph * 0.6], dark, glow=0.0)
        T.ellipse_glow(img, [gx - capw * 0.85, cy - caph * 0.9, gx + capw * 0.85, cy + caph * 0.2], col, glow=0.0)
        for i in range(4 if stage == 2 else 2):
            px = gx + rnd.uniform(-capw * 0.7, capw * 0.7)
            py = cy - caph * 0.3 + rnd.uniform(-1, 1)
            T.ellipse_glow(img, [px - 1.2, py - 0.9, px + 1.2, py + 0.9], light, glow=0.6)
        if shape == "fungus":
            for i in range(3):
                px = gx + rnd.uniform(-capw, capw)
                T.polyline_glow(img, [P(px, cy), P(px + rnd.uniform(-1, 1), cy + 4 * scale)], light, 0.8, glow=0.6, core=False)
    elif shape == "crystal":
        for i in range(5 if stage == 2 else 3):
            ang = (i - 2) * 0.35 + rnd.uniform(-0.1, 0.1)
            L = h * rnd.uniform(0.55, 1.0)
            tipx, tipy = gx + math.sin(ang) * L * 0.8, gy - math.cos(ang) * L
            w = 2.4 * scale + 0.8
            T.polygon_glow(img, [P(gx - w + i * 0.4, gy), P(tipx - w * 0.3, tipy + 2), P(tipx, tipy), P(tipx + w * 0.3, tipy + 2), P(gx + w + i * 0.4, gy)], col if i % 2 else light, glow=1.0, fill_alpha=230)
            T.polyline_glow(img, [P(gx + i * 0.4, gy), P(tipx, tipy)], (255, 255, 255), 0.6, glow=0.2)
        T.ellipse_glow(img, [gx - 7, gy - 2, gx + 7, gy + 2], dark, glow=0.0)
    elif shape == "vine":
        pts = [P(gx, gy)]
        for k in range(1, 7):
            pts.append(P(gx + math.sin(k * 1.3) * 5 * scale, gy - k * h / 6))
        T.polyline_glow(img, pts, stem, 1.4, glow=0.0, core=False)
        for k in range(1, 7, 1 if stage == 2 else 2):
            px, py = pts[k]
            sgn = 1 if k % 2 else -1
            T.polygon_glow(img, [P(px, py), P(px + sgn * 4 * scale, py - 2.5 * scale), P(px + sgn * 6.5 * scale, py + 0.5), P(px + sgn * 3.5 * scale, py + 2 * scale)], col if k % 3 else light, glow=0.0)
        for k in (2, 5):
            px, py = pts[k]
            T.ellipse_glow(img, [px - 1.4, py - 1.4, px + 1.4, py + 1.4], light, glow=0.8)
    elif shape == "spike":
        for i in range(3 if stage == 2 else 2):
            off = (i - 1) * 5 * scale
            L = h * (1.0 if i == 1 else 0.75)
            T.polygon_glow(img, [P(gx + off - 2.2, gy), P(gx + off - 0.6, gy - L), P(gx + off + 0.6, gy - L), P(gx + off + 2.2, gy)], dark, glow=0.0)
            for seg in range(1, 4):
                sy = gy - L * seg / 4
                T.polyline_glow(img, [P(gx + off - 2.0, sy), P(gx + off + 2.0, sy)], col, 0.9, glow=0.0, core=False)
            T.polygon_glow(img, [P(gx + off - 0.5, gy - L + 1), P(gx + off, gy - L - 3 * scale), P(gx + off + 0.5, gy - L + 1)], light, glow=0.8)
    elif shape == "fern":
        for i in range(4 if stage == 2 else 2):
            ang = (i - 1.5) * 0.5
            L = h * rnd.uniform(0.75, 1.0)
            pts = [P(gx + math.sin(ang) * L * t, gy - math.cos(ang) * L * t + 3 * t * t) for t in (0, 0.25, 0.5, 0.75, 1.0)]
            T.polyline_glow(img, pts, stem, 1.0, glow=0.0, core=False)
            for t in (0.3, 0.5, 0.7, 0.85):
                px, py = gx + math.sin(ang) * L * t, gy - math.cos(ang) * L * t + 3 * t * t
                for sgn in (-1, 1):
                    T.polygon_glow(img, [P(px, py), P(px + sgn * 3 * scale, py - 1.2 * scale), P(px + sgn * 4 * scale, py + 0.5)], col if int(t * 10) % 2 else light, glow=0.0)


def herb_textures(idx, key, element, shape, grade):
    col, dark, light = ELEMENT_COLORS[element]
    os.makedirs(TEX, exist_ok=True)
    for stage in range(3):
        rnd = random.Random(idx * 101 + 7)
        img = T.new(32, 32)
        scale = (0.35, 0.65, 1.0)[stage]
        if stage == 0:
            # A sprout: two small leaves, whatever the species.
            T.polyline_glow(img, [(16, 30), (16, 24)], (0x4E, 0x8A, 0x3C), 1.4, glow=0.0, core=False)
            for sgn in (-1, 1):
                T.polygon_glow(img, [(16, 26), (16 + sgn * 4, 23), (16 + sgn * 6, 26), (16 + sgn * 3, 27)], col, glow=0.0)
        else:
            draw_plant(img, shape, col, dark, light, rnd, scale, stage)
        if stage == 2 and grade >= 4:
            # A faint spirit-light halo marks the precious species.
            halo = T.radial(32, 32, light + (70,), light + (0,), power=1.6, radius=10, center=(16, 16))
            base = T.new(32, 32)
            base.alpha_composite(halo)
            base.alpha_composite(img)
            img = base
        T.save(T.finish(img, 32, 32), os.path.join(TEX, f"{key}_{stage}.png"))


# ------------------------------------------------------------------- emitters

def write_json(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(obj, f, ensure_ascii=False, indent=2)
        f.write("\n")


def emit_java():
    lines = [
        "package com.ngoducduy.celestialarts.alchemy;",
        "",
        "import com.ngoducduy.celestialarts.skill.Element;",
        "",
        "import java.util.List;",
        "",
        "/** GENERATED by tools/gen_herbs.py – do not edit by hand. The 78 linh dược. */",
        "final class HerbList {",
        "\tprivate HerbList() {",
        "\t}",
        "",
        "\tstatic final List<Herb> ALL = List.of(",
    ]
    rows = []
    for (key, vi, en, element, grade, habitat, shape, nature) in HERBS:
        col = ELEMENT_COLORS[element][0]
        rows.append(f"\t\t\tnew Herb(\"{key}\", Element.{element}, {grade}, Herb.Habitat.{habitat}, {nature}, {potency(grade, shape)}, {toxicity(element, grade, nature)}, {rgb_hex(col)})")
    lines.append(",\n".join(rows))
    lines += ["\t);", "}", ""]
    os.makedirs(os.path.dirname(JAVA), exist_ok=True)
    with open(JAVA, "w", encoding="utf-8") as f:
        f.write("\n".join(lines))
    print("HerbList.java", len(HERBS))


def emit_assets():
    for (key, vi, en, element, grade, habitat, shape, nature) in HERBS:
        b = "herb_" + key
        write_json(os.path.join(ASSETS, "blockstates", b + ".json"), {
            "variants": {f"age={a}": {"model": f"celestialarts:block/herb/{key}_{a}"} for a in range(3)}
        })
        for a in range(3):
            write_json(os.path.join(ASSETS, "models", "block", "herb", f"{key}_{a}.json"), {
                "parent": "minecraft:block/cross",
                "textures": {"cross": f"celestialarts:block/herb/{key}_{a}"}
            })
        write_json(os.path.join(ASSETS, "models", "item", b + ".json"), {
            "parent": "minecraft:item/generated",
            "textures": {"layer0": f"celestialarts:block/herb/{key}_2"}
        })
    print("blockstates/models", len(HERBS))


def emit_lang():
    for lang, idx, hab in (("vi_vn", 1, HABITAT_VI), ("en_us", 2, HABITAT_EN)):
        p = os.path.join(ASSETS, "lang", lang + ".json")
        with open(p, encoding="utf-8") as f:
            d = json.load(f, object_pairs_hook=OrderedDict)
        for row in HERBS:
            d["block.celestialarts.herb_" + row[0]] = row[idx]
        for k, v in hab.items():
            d["habitat.celestialarts." + k.lower()] = v
        if lang == "vi_vn":
            d.update({
                "herb.celestialarts.grade.1": "Nhất phẩm", "herb.celestialarts.grade.2": "Nhị phẩm", "herb.celestialarts.grade.3": "Tam phẩm",
                "herb.celestialarts.grade.4": "Tứ phẩm", "herb.celestialarts.grade.5": "Ngũ phẩm",
                "herb.celestialarts.nature.cold": "Hàn", "herb.celestialarts.nature.cool": "Lương", "herb.celestialarts.nature.neutral": "Bình",
                "herb.celestialarts.nature.warm": "Ôn", "herb.celestialarts.nature.hot": "Nhiệt",
                "herb.celestialarts.tooltip.kind": "Linh dược · %s · hệ %s",
                "herb.celestialarts.tooltip.nature": "Tính %s · dược lực %s · độc tính %s",
                "herb.celestialarts.tooltip.habitat": "Mọc ở %s",
                "herb.celestialarts.tooltip.grow": "Trồng lên đất phù hợp; lớn nhanh hơn nơi linh khí dồi dào",
            })
        else:
            d.update({
                "herb.celestialarts.grade.1": "Grade 1", "herb.celestialarts.grade.2": "Grade 2", "herb.celestialarts.grade.3": "Grade 3",
                "herb.celestialarts.grade.4": "Grade 4", "herb.celestialarts.grade.5": "Grade 5",
                "herb.celestialarts.nature.cold": "Cold", "herb.celestialarts.nature.cool": "Cool", "herb.celestialarts.nature.neutral": "Neutral",
                "herb.celestialarts.nature.warm": "Warm", "herb.celestialarts.nature.hot": "Hot",
                "herb.celestialarts.tooltip.kind": "Spirit herb · %s · %s element",
                "herb.celestialarts.tooltip.nature": "Nature %s · potency %s · toxicity %s",
                "herb.celestialarts.tooltip.habitat": "Grows in %s",
                "herb.celestialarts.tooltip.grow": "Plant on suitable ground; grows faster where spiritual qi is rich",
            })
        with open(p, "w", encoding="utf-8") as f:
            json.dump(d, f, ensure_ascii=False, indent="\t")
            f.write("\n")
    print("lang merged")


def emit_loot():
    for (key, *_rest) in HERBS:
        b = "herb_" + key
        write_json(os.path.join(DATA, "loot_tables", "blocks", b + ".json"), {
            "type": "minecraft:block",
            "pools": [
                {
                    "rolls": 1,
                    "entries": [{
                        "type": "minecraft:alternatives",
                        "children": [
                            {
                                "type": "minecraft:item", "name": "celestialarts:" + b,
                                "conditions": [{"condition": "minecraft:block_state_property", "block": "celestialarts:" + b, "properties": {"age": "2"}}],
                                "functions": [
                                    {"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 1, "max": 2}},
                                    {"function": "minecraft:apply_bonus", "enchantment": "minecraft:fortune", "formula": "minecraft:uniform_bonus_count", "parameters": {"bonusMultiplier": 1}}
                                ]
                            },
                            {"type": "minecraft:item", "name": "celestialarts:" + b}
                        ]
                    }]
                }
            ]
        })
    print("loot tables", len(HERBS))


def _underground(rarity, count, y0, y1):
    mods = [{"type": "minecraft:count", "count": count}]
    if rarity > 1:
        mods.append({"type": "minecraft:rarity_filter", "chance": rarity})
    mods += [
        {"type": "minecraft:in_square"},
        {"type": "minecraft:height_range", "height": {"type": "minecraft:uniform", "min_inclusive": {"absolute": y0}, "max_inclusive": {"absolute": y1}}},
        {"type": "minecraft:environment_scan", "direction_of_search": "down", "target_condition": {"type": "minecraft:solid"},
         "allowed_search_condition": {"type": "minecraft:matching_blocks", "blocks": "minecraft:air"}, "max_steps": 12},
        {"type": "minecraft:random_offset", "xz_spread": 0, "y_spread": 1},
        {"type": "minecraft:biome"},
    ]
    return mods


PLACEMENT = {
    "SURFACE": lambda tier: [
        {"type": "minecraft:rarity_filter", "chance": 4 if tier == "common" else 22},
        {"type": "minecraft:in_square"},
        {"type": "minecraft:heightmap", "heightmap": "MOTION_BLOCKING"},
        {"type": "minecraft:biome"},
    ],
    "CAVE": lambda tier: _underground(1 if tier == "common" else 12, 2 if tier == "common" else 1, 0, 60),
    "DEEP": lambda tier: _underground(1 if tier == "common" else 12, 2 if tier == "common" else 1, -58, 4),
    "NETHER": lambda tier: _underground(1 if tier == "common" else 10, 3 if tier == "common" else 1, 31, 110),
}

HABITAT_PLACEMENT = {
    "MEADOW": "SURFACE", "FOREST": "SURFACE", "JUNGLE": "SURFACE", "SWAMP": "SURFACE", "MOUNTAIN": "SURFACE", "SNOW": "SURFACE",
    "DESERT": "SURFACE", "BEACH": "SURFACE", "CHERRY": "SURFACE", "END": "SURFACE",
    "CAVE": "CAVE", "LUSH": "CAVE", "DEEP": "DEEP", "NETHER": "NETHER",
}
WEIGHT = {1: 12, 2: 7, 3: 3, 4: 2, 5: 1}


def emit_worldgen():
    by_hab = {}
    for row in HERBS:
        by_hab.setdefault(row[5], []).append(row)
    names = []
    for hab, rows in by_hab.items():
        for tier, tries in (("common", 20), ("rare", 8)):
            picks = [r for r in rows if (r[4] >= 4) == (tier == "rare")]
            if not picks:
                continue
            name = f"herbs_{hab.lower()}_{tier}"
            entries = [{"weight": WEIGHT[r[4]], "data": {"Name": "celestialarts:herb_" + r[0], "Properties": {"age": "2"}}} for r in picks]
            write_json(os.path.join(DATA, "worldgen", "configured_feature", name + ".json"), {
                "type": "minecraft:random_patch",
                "config": {
                    "tries": tries, "xz_spread": 4, "y_spread": 2,
                    "feature": {
                        "feature": {"type": "minecraft:simple_block", "config": {"to_place": {"type": "minecraft:weighted_state_provider", "entries": entries}}},
                        "placement": [{"type": "minecraft:block_predicate_filter", "predicate": {"type": "minecraft:matching_blocks", "blocks": "minecraft:air"}}]
                    }
                }
            })
            write_json(os.path.join(DATA, "worldgen", "placed_feature", name + ".json"), {
                "feature": "celestialarts:" + name,
                "placement": PLACEMENT[HABITAT_PLACEMENT[hab]](tier)
            })
            names.append(name)
    print("worldgen features", len(names))


def main():
    emit_java()
    emit_assets()
    emit_lang()
    emit_loot()
    emit_worldgen()
    for i, (key, vi, en, element, grade, habitat, shape, nature) in enumerate(HERBS):
        herb_textures(i, key, element, shape, grade)
    print("textures", len(HERBS) * 3)


if __name__ == "__main__":
    main()
