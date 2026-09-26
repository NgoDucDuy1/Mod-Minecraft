#!/usr/bin/env python3
"""Single source of truth for the alchemy content: pills, recipes, flames, furnaces.

Emits
  * alchemy/PillList.java                         – 15 archetypes x 5 grades = 75 pills
  * data/celestialarts/recipes/alchemy/*.json      – 4 recipe variants per pill (300), each with its own
                                                      heat curve / qi window / volatility
  * item textures + models for pills and flames, furnace textures/blockstates/models/item models,
    crafting recipes for the 12 furnaces and Phàm Hỏa, the furnace GUI background, lang (merged)

    python3 tools/gen_alchemy.py
"""
import json
import math
import os
import random
import sys
from collections import OrderedDict

sys.path.insert(0, os.path.dirname(__file__))
from PIL import Image, ImageDraw, ImageFilter  # noqa: E402
import texlib as T  # noqa: E402
import gen_herbs as H  # noqa: E402

ROOT = os.path.join(os.path.dirname(__file__), "..", "src", "main")
JAVA = os.path.join(ROOT, "java", "com", "ngoducduy", "celestialarts", "alchemy", "PillList.java")
ASSETS = os.path.join(ROOT, "resources", "assets", "celestialarts")
DATA = os.path.join(ROOT, "resources", "data", "celestialarts")
S = T.SS

GRADE_VI = {1: "Nhất phẩm", 2: "Nhị phẩm", 3: "Tam phẩm", 4: "Tứ phẩm", 5: "Ngũ phẩm"}
GRADE_EN = {1: "Grade 1", 2: "Grade 2", 3: "Grade 3", 4: "Grade 4", 5: "Grade 5"}

# key, vi, en, element, curve shape, qi window (start, end, min), base tolerance, volatility, desc vi, desc en
PILLS = [
    ("hoi_khi", "Hồi Khí Đan", "Qi Restoring Pill", "WIND", [(0.3, 40), (0.5, 55), (0.2, 35)], (0.55, 0.85, 35), 16, 0.25,
     "Hồi phục linh khí tức thì", "Instantly restores spirit qi"),
    ("tu_khi", "Tụ Khí Đan", "Qi Gathering Pill", "DAO", [(0.25, 50), (0.5, 65), (0.25, 45)], (0.5, 0.9, 45), 14, 0.35,
     "Tăng tu vi", "Grants cultivation experience"),
    ("truc_co", "Trúc Cơ Đan", "Foundation Pill", "DAO", [(0.2, 45), (0.2, 75), (0.2, 45), (0.2, 75), (0.2, 50)], (0.7, 1.0, 55), 12, 0.45,
     "Tăng tỉ lệ đột phá trong một thời gian", "Raises breakthrough chance for a while"),
    ("hoi_xuan", "Hồi Xuân Đan", "Rejuvenation Pill", "WIND", [(0.4, 35), (0.6, 50)], (0.3, 0.6, 30), 17, 0.2,
     "Hồi máu và tái sinh", "Heals and regenerates"),
    ("giai_doc", "Giải Độc Đan", "Detoxifying Pill", "ICE", [(0.3, 60), (0.4, 30), (0.3, 55)], (0.35, 0.65, 40), 14, 0.4,
     "Giải mọi hiệu ứng xấu", "Cleanses harmful effects"),
    ("kim_cuong", "Kim Cương Đan", "Diamond Body Pill", "EARTH", [(0.5, 80), (0.5, 70)], (0.6, 0.95, 50), 13, 0.3,
     "Kháng sát thương", "Damage resistance"),
    ("liet_hoa", "Liệt Hỏa Đan", "Raging Fire Pill", "FIRE", [(0.2, 50), (0.6, 90), (0.2, 60)], (0.4, 0.8, 45), 12, 0.55,
     "Miễn nhiễm lửa, tăng sức mạnh", "Fire immunity and strength"),
    ("bang_tam", "Băng Tâm Đan", "Ice Heart Pill", "ICE", [(0.5, 20), (0.5, 30)], (0.2, 0.7, 40), 11, 0.3,
     "Trấn định tâm thần, chữa tẩu hỏa nhập ma", "Calms the mind and cures qi deviation"),
    ("than_hanh", "Thần Hành Đan", "Divine Stride Pill", "LIGHTNING", [(0.15, 70), (0.15, 40), (0.15, 70), (0.15, 40), (0.4, 55)], (0.6, 0.9, 40), 13, 0.6,
     "Tốc độ và nhảy cao", "Speed and jump boost"),
    ("kim_than", "Kim Thân Đan", "Golden Body Pill", "SWORD", [(0.3, 60), (0.4, 85), (0.3, 65)], (0.5, 0.85, 50), 13, 0.4,
     "Sức mạnh và nhanh tay", "Strength and haste"),
    ("ngung_than", "Ngưng Thần Đan", "Spirit Focus Pill", "LIGHTNING", [(0.6, 45), (0.4, 70)], (0.75, 1.0, 60), 12, 0.35,
     "Tăng uy lực công pháp", "Boosts technique power"),
    ("tay_tuy", "Tẩy Tủy Đan", "Marrow Cleansing Pill", "DAO", [(0.25, 30), (0.25, 60), (0.25, 90), (0.25, 40)], (0.5, 1.0, 65), 10, 0.5,
     "Có cơ hội nâng phẩm linh căn", "Chance to raise spirit-root grade"),
    ("ngo_dao", "Ngộ Đạo Đan", "Enlightenment Pill", "DAO", [(0.5, 50), (0.5, 50)], (0.2, 1.0, 50), 8, 0.3,
     "Đại lượng tu vi và may mắn", "Large cultivation gain and luck"),
    ("do_kiep", "Độ Kiếp Đan", "Tribulation Pill", "LIGHTNING", [(0.2, 40), (0.3, 95), (0.5, 60)], (0.45, 0.75, 55), 11, 0.5,
     "Chống chịu lôi kiếp, tăng tỉ lệ đột phá", "Endure tribulation lightning, raises breakthrough chance"),
    ("boi_nguyen", "Bồi Nguyên Đan", "Origin Nourishing Pill", "EARTH", [(0.35, 55), (0.3, 40), (0.35, 65)], (0.4, 0.8, 45), 14, 0.3,
     "Tăng máu tối đa và hồi linh khí", "Max health boost and qi"),
]

# Effects per archetype as Java expressions, g = grade (1..5)
def effects(key, g):
    e = []
    if key == "hoi_khi":
        e.append(f"PillEffect.qi({int(60 * g ** 1.3)}F)")
        e.append(f"PillEffect.heal({2 * g}F)")
    elif key == "tu_khi":
        e.append(f"PillEffect.exp({int(80 * 2.2 ** (g - 1))})")
    elif key == "truc_co":
        e.append(f"PillEffect.status(() -> ModEffects.PILL_FORTUNE, {2400 + 600 * g}, {g - 1})")
    elif key == "hoi_xuan":
        e.append(f"PillEffect.heal({4 + 4 * g}F)")
        e.append(f"PillEffect.status(() -> StatusEffects.REGENERATION, {200 * g}, {min(g // 2, 2)})")
    elif key == "giai_doc":
        e.append("PillEffect.cleanse()")
        if g >= 3:
            e.append("PillEffect.cureDeviation()")
        e.append(f"PillEffect.heal({2 * g}F)")
    elif key == "kim_cuong":
        e.append(f"PillEffect.status(() -> StatusEffects.RESISTANCE, {1200 * g}, {(g - 1) // 2})")
    elif key == "liet_hoa":
        e.append(f"PillEffect.status(() -> StatusEffects.FIRE_RESISTANCE, {2400 * g}, 0)")
        e.append(f"PillEffect.status(() -> StatusEffects.STRENGTH, {600 * g}, {(g - 1) // 2})")
    elif key == "bang_tam":
        e.append("PillEffect.cureDeviation()")
        e.append(f"PillEffect.status(() -> StatusEffects.ABSORPTION, {1200 * g}, {g - 1})")
        e.append(f"PillEffect.qi({30 * g}F)")
    elif key == "than_hanh":
        e.append(f"PillEffect.status(() -> StatusEffects.SPEED, {1800 * g}, {min(g - 1, 3)})")
        e.append(f"PillEffect.status(() -> StatusEffects.JUMP_BOOST, {1800 * g}, {g // 2})")
    elif key == "kim_than":
        e.append(f"PillEffect.status(() -> StatusEffects.STRENGTH, {1200 * g}, {(g - 1) // 2})")
        e.append(f"PillEffect.status(() -> StatusEffects.HASTE, {1200 * g}, {(g - 1) // 2})")
    elif key == "ngung_than":
        e.append(f"PillEffect.status(() -> ModEffects.SPIRIT_POWER, {1800 * g}, {g - 1})")
    elif key == "tay_tuy":
        e.append(f"PillEffect.rootUpgrade({[0.08, 0.15, 0.25, 0.4, 0.6][g - 1]}F)")
        e.append(f"PillEffect.exp({int(60 * 2.0 ** (g - 1))})")
    elif key == "ngo_dao":
        e.append(f"PillEffect.exp({int(200 * 2.4 ** (g - 1))})")
        e.append(f"PillEffect.status(() -> StatusEffects.LUCK, {3600 * g}, {g - 1})")
    elif key == "do_kiep":
        e.append(f"PillEffect.status(() -> StatusEffects.RESISTANCE, {1200 + 600 * g}, {min(1 + g // 2, 3)})")
        e.append(f"PillEffect.status(() -> ModEffects.PILL_FORTUNE, {1200 + 600 * g}, {max(0, g - 2)})")
    elif key == "boi_nguyen":
        e.append(f"PillEffect.status(() -> StatusEffects.HEALTH_BOOST, {3600 * g}, {g - 1})")
        e.append(f"PillEffect.heal({4 * g}F)")
        e.append(f"PillEffect.qi({40 * g}F)")
    return e


FLAMES = [
    # key, vi, en, tier, power, volatility, color, desc vi, desc en
    ("flame_mortal", "Phàm Hỏa", "Mortal Flame", 1, 0.30, 1.0, (0xFF, 0x9A, 0x3A), "Lửa phàm trần, luyện được đan nhất – nhị phẩm", "Mundane fire; refines grade 1–2 pills"),
    ("flame_earth", "Địa Hỏa", "Earth Flame", 2, 0.40, 0.9, (0xFF, 0x5A, 0x1A), "Lửa lòng đất, nóng và ổn định", "Fire of the deep earth, hot and steady"),
    ("flame_spirit", "Linh Hỏa", "Spirit Flame", 3, 0.48, 0.8, (0x7A, 0xE8, 0xFF), "Hỏa chủng có linh tính, nghe theo linh khí", "A flame with a spirit, answers to qi"),
    ("flame_ghost", "Quỷ Hỏa", "Ghost Flame", 3, 0.42, 0.7, (0x8A, 0xFF, 0xB8), "Âm hỏa lạnh lẽo, hợp với đan hệ Ám và hàn dược", "Cold yin fire, suits void pills and cold herbs"),
    ("flame_karmic", "Nghiệp Hỏa", "Karmic Flame", 4, 0.56, 1.1, (0xFF, 0x2A, 0x2A), "Lửa nghiệp thiêu đốt tội nghiệt, bạo liệt khó thuần", "Karma fire, violent and hard to tame"),
    ("flame_strange", "Dị Hỏa", "Strange Fire", 4, 0.60, 0.75, (0xD2, 0x6B, 0xFF), "Dị hỏa hiếm có, tăng mạnh tỉ lệ thành đan", "Rare strange fire, greatly raises pill success"),
    ("flame_samadhi", "Tam Muội Chân Hỏa", "Samadhi True Fire", 5, 0.70, 0.6, (0xFF, 0xD8, 0x5A), "Chân hỏa tu thành, tinh thuần vô cùng", "True fire of cultivation, utterly pure"),
    ("flame_heaven", "Thiên Hỏa", "Heavenly Flame", 6, 0.85, 0.5, (0xFF, 0xFF, 0xE0), "Hỏa chủng từ trời giáng, thiêu vạn vật", "Fire fallen from the heavens"),
]

FURNACE_TYPES = [
    # key, vi, en, accent colour, desc
    ("steady", "Ổn Hỏa Lô", "Steady Fire Furnace", (0x66, 0xC8, 0x6A), "Hỏa hầu êm, sai số được nới rộng", "Gentle heat: wider tolerance"),
    ("fierce", "Liệt Diễm Lô", "Fierce Flame Furnace", (0xFF, 0x5A, 0x2A), "Lửa mạnh, lên nhiệt nhanh, đan chất cao hơn", "Strong fire: fast heat, higher quality"),
    ("spirit", "Tụ Linh Lô", "Spirit Gathering Furnace", (0x7A, 0xE8, 0xFF), "Tụ linh khí, chú linh hiệu quả hơn", "Gathers qi: injecting qi is more effective"),
]
FURNACE_GRADES = [
    # key, vi, en, metal colours (base, dark, light), max pill grade, crafting core
    ("mortal", "Phàm cấp", "Mortal", ((0x8A, 0x8E, 0x96), (0x4A, 0x4E, 0x56), (0xC8, 0xCC, 0xD4)), 2, "minecraft:iron_block"),
    ("spirit", "Linh cấp", "Spirit", ((0xB8, 0x7A, 0x3E), (0x6A, 0x40, 0x1E), (0xF0, 0xB8, 0x70)), 3, "minecraft:copper_block"),
    ("treasure", "Bảo cấp", "Treasure", ((0xE8, 0xC0, 0x4A), (0x8A, 0x6A, 0x1A), (0xFF, 0xF0, 0xA0)), 4, "minecraft:gold_block"),
    ("immortal", "Tiên cấp", "Immortal", ((0xD8, 0xF0, 0xE0), (0x6A, 0xA0, 0x88), (0xFF, 0xFF, 0xFF)), 5, "minecraft:diamond_block"),
]

HERBS = H.HERBS
HERB_BY_KEY = {h[0]: h for h in HERBS}
NATURE = {h[0]: h[7] for h in HERBS}
TOX = {h[0]: H.toxicity(h[3], h[4], h[7]) for h in HERBS}


def write_json(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(obj, f, ensure_ascii=False, indent=2)
        f.write("\n")


def herbs_of(element, grade):
    out = [h for h in HERBS if h[3] == element and h[4] == grade]
    return out


def pool(element, grade):
    """Grade-g herbs of the element; if none (DAO has no grade 1) the next grade up."""
    for g in (grade, grade + 1, grade - 1):
        if 1 <= g <= 5 and herbs_of(element, g):
            return herbs_of(element, g)
    return []


ELEMENT_FRIENDS = {
    "WIND": ["EARTH", "ICE", "DAO"], "DAO": ["WIND", "EARTH", "SWORD"], "ICE": ["WIND", "VOID", "DAO"],
    "EARTH": ["FIRE", "SWORD", "DAO"], "FIRE": ["EARTH", "LIGHTNING", "SWORD"], "LIGHTNING": ["FIRE", "SWORD", "VOID"],
    "SWORD": ["EARTH", "LIGHTNING", "DAO"], "VOID": ["ICE", "LIGHTNING", "DAO"],
}


def build_recipe(pill, g, variant, rnd):
    key, vi, en, element, curve, qiwin, tol, vol = pill[:8]
    main = pool(element, g)
    ing = OrderedDict()
    m = main[(variant + g) % len(main)]
    ing[m[0]] = 2 + (1 if g >= 3 else 0)
    # Secondary: same element, one grade down (or same grade, other species)
    sec = pool(element, max(1, g - 1)) if g > 1 else main
    sec = [h for h in sec if h[0] != m[0]] or [h for h in main if h[0] != m[0]] or main
    s = sec[(variant * 3 + g) % len(sec)]
    ing[s[0]] = ing.get(s[0], 0) + (1 + variant % 2)
    # Filler from a friendly element; its nature shifts the heat curve.
    fe = ELEMENT_FRIENDS[element][variant % 3]
    fl = pool(fe, max(1, g - 1 if variant % 2 == 0 else g))
    f = fl[(variant + g * 2) % len(fl)]
    ing[f[0]] = ing.get(f[0], 0) + 1
    # High grades add a Đạo herb as the binding agent.
    if g >= 4 and element != "DAO":
        dao = pool("DAO", g - 1)
        d = dao[(variant + 1) % len(dao)]
        ing[d[0]] = ing.get(d[0], 0) + 1
    # Curve: baked per recipe. Hot herbs push the required heat up, cold herbs down.
    nature_sum = sum(NATURE[k] * n for k, n in ing.items())
    tox_sum = sum(TOX[k] * n for k, n in ing.items())
    shift = nature_sum * 1.5
    tolerance = max(5, tol - 2 * (g - 1))
    total = 800 + 300 * g  # ticks: 55 s .. 115 s
    heat = []
    for span, target in curve:
        t = int(round(min(95, max(10, target + shift))))
        heat.append({"span": span, "target": t, "tol": tolerance})
    # Small deterministic per-variant wobble so the four variants of a pill feel different.
    if variant % 2 == 1 and len(heat) >= 2:
        heat[-1]["target"] = int(min(95, max(10, heat[-1]["target"] - 8)))
    if variant >= 2:
        heat[0]["span"] = round(heat[0]["span"] * 0.8, 3)
        heat[-1]["span"] = round(heat[-1]["span"] + heat[0]["span"] * 0.25, 3)
    ssum = sum(h["span"] for h in heat)
    for h in heat:
        h["span"] = round(h["span"] / ssum, 4)
    volatility = round(min(1.0, vol + tox_sum * 0.05 + 0.05 * (g - 1)), 3)
    fire_tier = [1, 1, 2, 3, 4][g - 1]
    return {
        "type": "celestialarts:alchemy",
        "ingredients": [{"item": "celestialarts:herb_" + k, "count": n} for k, n in ing.items()],
        "result": {"item": f"celestialarts:pill_{key}_{g}", "count": 1},
        "grade": g,
        "fire_tier": fire_tier,
        "time": total,
        "heat": heat,
        "qi": {"start": qiwin[0], "end": qiwin[1], "min": min(90, qiwin[2] + 4 * (g - 1))},
        "volatility": volatility,
    }


def emit_recipes():
    n = 0
    rnd = random.Random(7)
    for pill in PILLS:
        for g in range(1, 6):
            for v in range(4):
                r = build_recipe(pill, g, v, rnd)
                write_json(os.path.join(DATA, "recipes", "alchemy", f"{pill[0]}_{g}_{v + 1}.json"), r)
                n += 1
    print("alchemy recipes", n)


def emit_java():
    lines = [
        "package com.ngoducduy.celestialarts.alchemy;",
        "",
        "import com.ngoducduy.celestialarts.registry.ModEffects;",
        "import com.ngoducduy.celestialarts.skill.Element;",
        "import net.minecraft.entity.effect.StatusEffects;",
        "",
        "import java.util.List;",
        "",
        "/** GENERATED by tools/gen_alchemy.py – do not edit by hand. 15 pill archetypes x 5 grades. */",
        "final class PillList {",
        "\tprivate PillList() {",
        "\t}",
        "",
        "\tstatic final List<Pill> ALL = List.of(",
    ]
    rows = []
    for pill in PILLS:
        key, vi, en, element = pill[:4]
        col = H.ELEMENT_COLORS[element][0]
        for g in range(1, 6):
            eff = ", ".join(effects(key, g))
            rows.append(f"\t\t\tnew Pill(\"{key}\", {g}, Element.{element}, {H.rgb_hex(col)}, List.of({eff}))")
    lines.append(",\n".join(rows))
    lines += ["\t);", "}", ""]
    with open(JAVA, "w", encoding="utf-8") as f:
        f.write("\n".join(lines))
    print("PillList.java", len(rows))


# --------------------------------------------------------------------------- textures

def pill_texture(path, element, grade):
    col, dark, light = H.ELEMENT_COLORS[element]
    img = T.new(32, 32)
    cx, cy, r = 16, 17, 9.5
    # shadow
    T.ellipse_glow(img, [cx - r, cy + r * 0.55, cx + r, cy + r * 0.95], (0, 0, 0), glow=1.2, fill_alpha=90)
    # body: dark sphere with element gradient
    T.ellipse_glow(img, [cx - r, cy - r, cx + r, cy + r], dark, glow=0.0)
    grad = T.radial(32, 32, col + (255,), dark + (255,), power=1.4, radius=r * 1.1, center=(cx - 2.5, cy - 3))
    mask = Image.new("L", img.size, 0)
    ImageDraw.Draw(mask).ellipse([(cx - r) * S, (cy - r) * S, (cx + r) * S, (cy + r) * S], fill=255)
    img.paste(grad, (0, 0), mask)
    # marbled pattern: swirl lines
    rnd = random.Random(hash(element) & 0xFFFF)
    for i in range(3):
        a0 = rnd.uniform(0, 360)
        T.arc_glow(img, [cx - r * 0.7, cy - r * 0.7, cx + r * 0.7, cy + r * 0.7], a0, a0 + 90 + i * 20, light, 0.9, glow=0.3)
    # glossy highlight
    T.ellipse_glow(img, [cx - 5.5, cy - 7, cx - 1.5, cy - 4.5], (255, 255, 255), glow=0.8, fill_alpha=210)
    # grade marks: small gold stars along the bottom
    for i in range(grade):
        px = cx + (i - (grade - 1) / 2) * 4.2
        py = 29.0
        T.polygon_glow(img, T.star_points(px, py, 1.8, 0.8, 4), (0xFF, 0xE0, 0x7A), glow=0.6)
    if grade >= 4:
        halo = T.radial(32, 32, light + (90,), light + (0,), power=1.6, radius=14, center=(cx, cy))
        base = T.new(32, 32)
        base.alpha_composite(halo)
        base.alpha_composite(img)
        img = base
    T.save(T.finish(img, 32, 32), path)


def flame_texture(path, color, tier):
    img = T.new(32, 32)
    # jade vessel
    body = (0x5A, 0x8C, 0x7A)
    T.polygon_glow(img, [(9, 30), (7, 18), (10, 14), (22, 14), (25, 18), (23, 30)], body, glow=0.0)
    T.polygon_glow(img, [(10, 30), (8.5, 19), (11, 15.5), (13, 15.5), (12, 30)], (0x8A, 0xC0, 0xA8), glow=0.0)
    T.polygon_glow(img, [(6, 13), (26, 13), (26, 15.5), (6, 15.5)], (0xC8, 0xA0, 0x4A), glow=0.0)
    T.polygon_glow(img, [(8, 29), (24, 29), (24, 31), (8, 31)], (0xC8, 0xA0, 0x4A), glow=0.0)
    # flame above
    dark = tuple(int(c * 0.55) for c in color)
    light = tuple(min(255, int(c * 0.4 + 255 * 0.6)) for c in color)
    T.polygon_glow(img, [(16, 1), (11, 7), (10.5, 11), (12, 14), (20, 14), (21.5, 11), (21, 7)], dark, glow=2.0, fill_alpha=230)
    T.polygon_glow(img, [(16, 3), (12.5, 8), (12.5, 11.5), (14, 14), (18, 14), (19.5, 11.5), (19.5, 8)], color, glow=1.0)
    T.polygon_glow(img, [(16, 6.5), (14.2, 9.5), (14.5, 12), (16, 14), (17.5, 12), (17.8, 9.5)], light, glow=0.8)
    # tier ticks on the vessel
    for i in range(tier):
        x = 10 + i * 2.1
        T.polygon_glow(img, [(x, 21), (x + 1.2, 21), (x + 1.2, 25), (x, 25)], (0xFF, 0xE8, 0xA0), glow=0.3)
    T.save(T.finish(img, 32, 32), path)


def metal_texture(size, base, dark, light, seed, accent, band=False, runes=False, lit=False, top=False):
    """32x32 (drawn at SS) metal plate with rivets, optional rune band."""
    rnd = random.Random(seed)
    img = T.new(size, size, base + (255,))
    noise = T.noise_layer(size, size, seed, scale=6, octaves=3)
    shade = T.colorize(noise, dark, alpha_scale=0.55)
    img.alpha_composite(shade)
    d = ImageDraw.Draw(img)
    if top:
        # lid: concentric rings + centre knob shadow
        for r in (14, 10, 6):
            T.arc_glow(img, [16 - r, 16 - r, 16 + r, 16 + r], 0, 360, dark, 0.8, glow=0.0)
            T.arc_glow(img, [16 - r + 0.7, 16 - r + 0.7, 16 + r + 0.7, 16 + r + 0.7], 200, 340, light, 0.5, glow=0.0)
        T.polygon_glow(img, T.star_points(16, 16, 4.5, 2.0, 8), accent, glow=1.0 if lit else 0.0)
    else:
        # plate seams
        for y in (8, 24):
            d.line([(0, y * S), (size * S, y * S)], fill=dark + (255,), width=S)
            d.line([(0, (y + 1) * S), (size * S, (y + 1) * S)], fill=light + (150,), width=max(1, S // 2))
        for x in (4, 28):
            for y in (4, 12, 20, 28):
                T.ellipse_glow(img, [x - 1.1, y - 1.1, x + 1.1, y + 1.1], light, glow=0.0)
                T.ellipse_glow(img, [x - 0.5, y - 0.3, x + 0.7, y + 0.9], dark, glow=0.0, fill_alpha=160)
        if band:
            T.polygon_glow(img, [(6, 12), (26, 12), (26, 20), (6, 20)], dark, glow=0.0)
            for i in range(5):
                x = 8 + i * 3.8
                pts = [(x, 13.5), (x + 2.2, 13.5), (x + 1.1, 18.5)] if i % 2 else [(x, 18.5), (x + 2.2, 18.5), (x + 1.1, 13.5)]
                T.polygon_glow(img, pts, accent, glow=1.4 if lit else 0.0, fill_alpha=255 if lit else 170)
    return T.finish(img, size, size)


def emit_furnaces():
    tex_dir = os.path.join(ASSETS, "textures", "block", "furnace")
    os.makedirs(tex_dir, exist_ok=True)
    seed = 100
    for (tk, tvi, ten, accent, _dv, _de) in FURNACE_TYPES:
        for (gk, gvi, gen, (base, dark, light), maxg, core) in FURNACE_GRADES:
            name = f"{tk}_{gk}"
            seed += 1
            metal_texture(32, base, dark, light, seed, accent, band=True).save(os.path.join(tex_dir, f"{name}_side.png"))
            metal_texture(32, base, dark, light, seed, accent, band=True, lit=True).save(os.path.join(tex_dir, f"{name}_side_lit.png"))
            metal_texture(32, base, dark, light, seed + 50, accent, top=True).save(os.path.join(tex_dir, f"{name}_top.png"))
            metal_texture(32, base, dark, light, seed + 50, accent, top=True, lit=True).save(os.path.join(tex_dir, f"{name}_top_lit.png"))
            metal_texture(32, base, dark, light, seed + 90, accent).save(os.path.join(tex_dir, f"{name}_plain.png"))
            block = f"alchemy_furnace_{name}"
            for lit in (False, True):
                suffix = "_lit" if lit else ""
                write_json(os.path.join(ASSETS, "models", "block", "furnace", f"{name}{suffix}.json"), {
                    "parent": "celestialarts:block/furnace/base",
                    "textures": {
                        "particle": f"celestialarts:block/furnace/{name}_plain",
                        "side": f"celestialarts:block/furnace/{name}_side{suffix}",
                        "top": f"celestialarts:block/furnace/{name}_top{suffix}",
                        "plain": f"celestialarts:block/furnace/{name}_plain",
                    }
                })
            variants = {}
            for facing, rot in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
                for lit in ("false", "true"):
                    v = {"model": f"celestialarts:block/furnace/{name}{'_lit' if lit == 'true' else ''}"}
                    if rot:
                        v["y"] = rot
                    variants[f"facing={facing},lit={lit}"] = v
            write_json(os.path.join(ASSETS, "blockstates", block + ".json"), {"variants": variants})
            write_json(os.path.join(ASSETS, "models", "item", block + ".json"), {"parent": f"celestialarts:block/furnace/{name}"})
            # Crafting: metal core + 4 metal + type item + 2 stone; the grade item sits in the middle.
            type_item = {"steady": "minecraft:moss_block", "fierce": "minecraft:blaze_powder", "spirit": "celestialarts:spirit_stone"}[tk]
            write_json(os.path.join(DATA, "recipes", block + ".json"), {
                "type": "minecraft:crafting_shaped",
                "pattern": ["MTM", "MCM", "SSS"],
                "key": {
                    "M": {"item": {"mortal": "minecraft:iron_ingot", "spirit": "minecraft:copper_ingot", "treasure": "minecraft:gold_ingot", "immortal": "minecraft:diamond"}[gk]},
                    "T": {"item": type_item},
                    "C": {"item": core},
                    "S": {"item": "minecraft:smooth_stone" if gk in ("mortal", "spirit") else "minecraft:polished_deepslate"},
                },
                "result": {"item": "celestialarts:" + block, "count": 1},
            })
    # Shared parametric model: tripod cauldron with a lid.
    def box(f, t, tex, uv=None):
        faces = {}
        for face in ("north", "south", "east", "west", "up", "down"):
            faces[face] = {"texture": "#" + tex}
        return {"from": f, "to": t, "faces": faces}
    elements = [
        box([3, 3, 3], [13, 12, 13], "side"),
        box([2, 11, 2], [14, 13, 14], "plain"),
        box([3, 0, 3], [5, 3, 5], "plain"), box([11, 0, 3], [13, 3, 5], "plain"), box([7, 0, 11], [9, 3, 13], "plain"),
        box([4, 13, 4], [12, 14, 12], "top"),
        box([7, 14, 7], [9, 16, 9], "plain"),
    ]
    elements[5]["faces"]["up"] = {"texture": "#top", "uv": [0, 0, 16, 16]}
    write_json(os.path.join(ASSETS, "models", "block", "furnace", "base.json"), {
        "parent": "minecraft:block/block",
        "textures": {"particle": "#plain"},
        "display": {
            "gui": {"rotation": [30, 225, 0], "translation": [0, 0, 0], "scale": [0.625, 0.625, 0.625]},
            "ground": {"translation": [0, 3, 0], "scale": [0.25, 0.25, 0.25]},
            "fixed": {"scale": [0.5, 0.5, 0.5]},
            "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.375, 0.375, 0.375]},
            "firstperson_righthand": {"rotation": [0, 45, 0], "scale": [0.4, 0.4, 0.4]},
            "firstperson_lefthand": {"rotation": [0, 225, 0], "scale": [0.4, 0.4, 0.4]},
        },
        "elements": elements,
    })
    print("furnaces", len(FURNACE_TYPES) * len(FURNACE_GRADES))


def emit_items():
    item_tex = os.path.join(ASSETS, "textures", "item", "pill")
    os.makedirs(item_tex, exist_ok=True)
    for pill in PILLS:
        key, vi, en, element = pill[:4]
        for g in range(1, 6):
            name = f"pill_{key}_{g}"
            pill_texture(os.path.join(item_tex, f"{key}_{g}.png"), element, g)
            write_json(os.path.join(ASSETS, "models", "item", name + ".json"), {
                "parent": "minecraft:item/generated", "textures": {"layer0": f"celestialarts:item/pill/{key}_{g}"}})
    for (key, vi, en, tier, power, vol, color, dv, de) in FLAMES:
        flame_texture(os.path.join(ASSETS, "textures", "item", key + ".png"), color, tier)
        write_json(os.path.join(ASSETS, "models", "item", key + ".json"), {
            "parent": "minecraft:item/generated", "textures": {"layer0": f"celestialarts:item/{key}"}})
    # Đan tra – slag of a ruined pill.
    img = T.new(32, 32)
    T.ellipse_glow(img, [8, 12, 24, 26], (0x3A, 0x32, 0x2E), glow=0.6)
    T.ellipse_glow(img, [11, 10, 21, 20], (0x5A, 0x4E, 0x46), glow=0.0)
    for i in range(6):
        x, y = 10 + i * 2.2, 14 + (i % 3) * 3
        T.ellipse_glow(img, [x, y, x + 1.5, y + 1.5], (0x8A, 0x3A, 0x22), glow=0.5)
    T.save(T.finish(img, 32, 32), os.path.join(ASSETS, "textures", "item", "pill_slag.png"))
    write_json(os.path.join(ASSETS, "models", "item", "pill_slag.json"), {"parent": "minecraft:item/generated", "textures": {"layer0": "celestialarts:item/pill_slag"}})
    # Phàm Hỏa is craftable: a jar of coal lit with flint and steel.
    write_json(os.path.join(DATA, "recipes", "flame_mortal.json"), {
        "type": "minecraft:crafting_shaped",
        "pattern": [" F ", "GCG", " G "],
        "key": {"F": {"item": "minecraft:flint_and_steel"}, "G": {"item": "minecraft:glass"}, "C": {"item": "minecraft:coal_block"}},
        "result": {"item": "celestialarts:flame_mortal", "count": 1},
    })
    print("pill/flame items", len(PILLS) * 5 + len(FLAMES) + 1)


def emit_gui():
    """Furnace screen background, 256x256: 200x222 panel."""
    W, Hh = 200, 222
    img = Image.new("RGBA", (256, 256), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    # Dark lacquer panel with a gold frame.
    d.rounded_rectangle([0, 0, W - 1, Hh - 1], radius=6, fill=(26, 20, 24, 255), outline=(150, 118, 58, 255), width=2)
    d.rounded_rectangle([3, 3, W - 4, Hh - 4], radius=4, outline=(70, 52, 36, 255), width=1)

    def slot(x, y):
        d.rectangle([x - 1, y - 1, x + 16, y + 16], fill=(20, 16, 18, 255), outline=(96, 78, 46, 255))
        d.line([(x - 1, y - 1), (x + 16, y - 1)], fill=(12, 10, 12, 255))
        d.line([(x - 1, y - 1), (x - 1, y + 16)], fill=(12, 10, 12, 255))
        d.line([(x - 1, y + 16), (x + 16, y + 16)], fill=(140, 112, 64, 255))
        d.line([(x + 16, y - 1), (x + 16, y + 16)], fill=(140, 112, 64, 255))
    # herb slots 2x3
    for r in range(3):
        for c in range(2):
            slot(9 + c * 18, 19 + r * 18)
    slot(18, 82)   # flame
    slot(18, 112)  # output
    # graph frame
    d.rectangle([59, 17, 188, 74], fill=(14, 12, 16, 255), outline=(96, 78, 46, 255))
    # bar frames
    for y in (79, 89, 99):
        d.rectangle([59, y, 188, y + 6], fill=(14, 12, 16, 255), outline=(70, 56, 36, 255))
    # inventory
    for r in range(3):
        for c in range(9):
            slot(20 + c * 18, 140 + r * 18)
    for c in range(9):
        slot(20 + c * 18, 198)
    # little flame glyph beside the flame slot, pill glyph beside output
    img.save(os.path.join(ASSETS, "textures", "gui", "alchemy_furnace.png"))
    print("gui")


def emit_effect_icons():
    import gen_gui_textures as G

    def pill_fortune(img):
        col, col2 = G.ELEMENT["dao"]
        img.alpha_composite(T.radial(32, 32, col + (110,), col + (0,), power=1.3, radius=13))
        # a pill with an upward arrow of fortune
        T.ellipse_glow(img, [8, 12, 24, 28], (0xC8, 0x8A, 0x2A), glow=0.0)
        T.ellipse_glow(img, [10, 14, 22, 26], (0xFF, 0xC8, 0x5A), glow=0.6)
        T.ellipse_glow(img, [12, 15, 16, 18], (255, 255, 255), glow=0.8, fill_alpha=200)
        T.polyline_glow(img, [(16, 12), (16, 3)], col2, 1.8, glow=1.0)
        T.polyline_glow(img, [(12, 7), (16, 3), (20, 7)], col2, 1.8, glow=1.0)

    def spirit_power(img):
        col, col2 = G.ELEMENT["lightning"]
        img.alpha_composite(T.radial(32, 32, col + (120,), col + (0,), power=1.3, radius=13))
        T.ellipse_glow(img, [9, 9, 23, 23], (0x6A, 0x3A, 0xB8), glow=0.0)
        T.ellipse_glow(img, [11, 11, 21, 21], col, glow=0.8)
        for k in range(6):
            a = k * math.pi / 3
            T.polyline_glow(img, [(16 + 7 * math.cos(a), 16 + 7 * math.sin(a)), (16 + 12 * math.cos(a), 16 + 12 * math.sin(a))], col2, 1.4, glow=1.0)
        T.polygon_glow(img, T.star_points(16, 16, 3.5, 1.5, 4), (255, 255, 255), glow=1.0)

    G.effect_icon("pill_fortune", pill_fortune)
    G.effect_icon("spirit_power", spirit_power)


def emit_lang():
    for lang, idx in (("vi_vn", 1), ("en_us", 2)):
        p = os.path.join(ASSETS, "lang", lang + ".json")
        with open(p, encoding="utf-8") as f:
            dct = json.load(f, object_pairs_hook=OrderedDict)
        gr = GRADE_VI if lang == "vi_vn" else GRADE_EN
        for pill in PILLS:
            for g in range(1, 6):
                dct[f"item.celestialarts.pill_{pill[0]}_{g}"] = f"{pill[idx]} · {gr[g]}"
            dct[f"pill.celestialarts.{pill[0]}.desc"] = pill[8] if lang == "vi_vn" else pill[9]
        for fl in FLAMES:
            dct["item.celestialarts." + fl[0]] = fl[idx]
            dct[f"flame.celestialarts.{fl[0]}.desc"] = fl[7] if lang == "vi_vn" else fl[8]
        for (tk, tvi, ten, _a, dv, de) in FURNACE_TYPES:
            for (gk, gvi, gen, _m, maxg, _c) in FURNACE_GRADES:
                dct[f"block.celestialarts.alchemy_furnace_{tk}_{gk}"] = f"{tvi} ({gvi})" if lang == "vi_vn" else f"{gen} {ten}"
            dct[f"furnace.celestialarts.type.{tk}.desc"] = dv if lang == "vi_vn" else de
        common_vi = {
            "item.celestialarts.pill_slag": "Đan Tra",
            "pill.celestialarts.quality.0": "Hạ phẩm", "pill.celestialarts.quality.1": "Trung phẩm",
            "pill.celestialarts.quality.2": "Thượng phẩm", "pill.celestialarts.quality.3": "Cực phẩm",
            "pill.celestialarts.tooltip.quality": "Đan chất: %s (hiệu lực ×%s)",
            "pill.celestialarts.tooltip.grade": "Đan dược %s · hệ %s",
            "pill.celestialarts.tooltip.slag": "Tàn dư của một lò đan hỏng. Chẳng còn dùng được vào việc gì.",
            "flame.celestialarts.tooltip.tier": "Hỏa chủng cấp %s · hỏa lực %s",
            "flame.celestialarts.tooltip.pills": "Luyện được đan tới %s",
            "furnace.celestialarts.tooltip.grade": "Lò %s · luyện được đan tới %s",
            "container.celestialarts.alchemy_furnace": "Luyện Đan",
            "furnace.celestialarts.btn.less": "− Hỏa", "furnace.celestialarts.btn.more": "+ Hỏa",
            "furnace.celestialarts.btn.qi": "Chú Linh", "furnace.celestialarts.btn.start": "Khởi Lô",
            "furnace.celestialarts.btn.collect": "Thu Đan", "furnace.celestialarts.btn.abort": "Dập Lò",
            "furnace.celestialarts.heat": "Hỏa hầu", "furnace.celestialarts.qi": "Linh khí", "furnace.celestialarts.damage": "Hư tổn",
            "furnace.celestialarts.state.idle": "Chưa khởi lô", "furnace.celestialarts.state.no_recipe": "Dược liệu không hợp đan phương",
            "furnace.celestialarts.state.no_fire": "Thiếu hỏa chủng", "furnace.celestialarts.state.weak_fire": "Hỏa chủng quá yếu (cần cấp %s)",
            "furnace.celestialarts.state.furnace_grade": "Lò không đủ phẩm cấp (đan %s)",
            "furnace.celestialarts.state.refining": "Đang luyện · %s%%", "furnace.celestialarts.state.qi_phase": "Ngưng đan – cần chú linh!",
            "furnace.celestialarts.state.done": "Đan thành! Chất lượng %s", "furnace.celestialarts.state.ruined": "Hư đan…",
            "furnace.celestialarts.state.output_full": "Ô đan dược còn đầy",
            "furnace.celestialarts.fire_level": "Mức lửa %s/4",
            "furnace.celestialarts.recipe": "Đan phương: %s",
            "effect.celestialarts.pill_fortune": "Đan Vận", "effect.celestialarts.spirit_power": "Linh Lực",
            "advancement.celestialarts.first_pill.title": "Đan thành", "advancement.celestialarts.first_pill.description": "Luyện thành viên đan đầu tiên",
            "message.celestialarts.root_upgraded": "Tẩy tủy phạt mao! Linh căn thăng lên %s phẩm.",
            "message.celestialarts.root_unchanged": "Dược lực tan đi, linh căn vẫn như cũ.",
            "pill.celestialarts.effect.heal": "Hồi %s máu", "pill.celestialarts.effect.status": "%s%s trong %s phút %s giây",
            "pill.celestialarts.effect.cleanse": "Giải trừ hiệu ứng xấu", "pill.celestialarts.effect.cure_deviation": "Chữa tẩu hỏa nhập ma",
            "pill.celestialarts.effect.root_upgrade": "%s%% cơ hội nâng phẩm linh căn",
        }
        common_en = {
            "item.celestialarts.pill_slag": "Pill Slag",
            "pill.celestialarts.quality.0": "Low quality", "pill.celestialarts.quality.1": "Medium quality",
            "pill.celestialarts.quality.2": "High quality", "pill.celestialarts.quality.3": "Supreme quality",
            "pill.celestialarts.tooltip.quality": "Quality: %s (potency ×%s)",
            "pill.celestialarts.tooltip.grade": "%s pill · %s element",
            "pill.celestialarts.tooltip.slag": "Residue of a ruined batch. Good for nothing.",
            "flame.celestialarts.tooltip.tier": "Flame tier %s · power %s",
            "flame.celestialarts.tooltip.pills": "Refines pills up to %s",
            "furnace.celestialarts.tooltip.grade": "%s furnace · refines pills up to %s",
            "container.celestialarts.alchemy_furnace": "Alchemy",
            "furnace.celestialarts.btn.less": "− Fire", "furnace.celestialarts.btn.more": "+ Fire",
            "furnace.celestialarts.btn.qi": "Inject Qi", "furnace.celestialarts.btn.start": "Ignite",
            "furnace.celestialarts.btn.collect": "Collect", "furnace.celestialarts.btn.abort": "Quench",
            "furnace.celestialarts.heat": "Heat", "furnace.celestialarts.qi": "Qi", "furnace.celestialarts.damage": "Damage",
            "furnace.celestialarts.state.idle": "Not lit", "furnace.celestialarts.state.no_recipe": "Herbs match no recipe",
            "furnace.celestialarts.state.no_fire": "No flame", "furnace.celestialarts.state.weak_fire": "Flame too weak (needs tier %s)",
            "furnace.celestialarts.state.furnace_grade": "Furnace grade too low (grade %s pill)",
            "furnace.celestialarts.state.refining": "Refining · %s%%", "furnace.celestialarts.state.qi_phase": "Condensing – inject qi!",
            "furnace.celestialarts.state.done": "Pill formed! Quality %s", "furnace.celestialarts.state.ruined": "Ruined…",
            "furnace.celestialarts.state.output_full": "Output slot is full",
            "furnace.celestialarts.fire_level": "Fire level %s/4",
            "furnace.celestialarts.recipe": "Recipe: %s",
            "effect.celestialarts.pill_fortune": "Pill Fortune", "effect.celestialarts.spirit_power": "Spirit Power",
            "advancement.celestialarts.first_pill.title": "A Pill Is Born", "advancement.celestialarts.first_pill.description": "Refine your first pill",
            "message.celestialarts.root_upgraded": "Marrow cleansed! Spirit root rises to grade %s.",
            "message.celestialarts.root_unchanged": "The medicine fades; the spirit root is unchanged.",
            "pill.celestialarts.effect.heal": "Heals %s HP", "pill.celestialarts.effect.status": "%s%s for %s min %s s",
            "pill.celestialarts.effect.cleanse": "Cleanses harmful effects", "pill.celestialarts.effect.cure_deviation": "Cures qi deviation",
            "pill.celestialarts.effect.root_upgrade": "%s%% chance to raise spirit-root grade",
        }
        for (gk, gvi, gen, _m, maxg, _c) in FURNACE_GRADES:
            (common_vi if True else None)[f"furnace.celestialarts.grade.{gk}"] = gvi
            common_en[f"furnace.celestialarts.grade.{gk}"] = gen
        dct.update(common_vi if lang == "vi_vn" else common_en)
        with open(p, "w", encoding="utf-8") as f:
            json.dump(dct, f, ensure_ascii=False, indent="\t")
            f.write("\n")
    print("lang merged")


def main():
    emit_java()
    emit_recipes()
    emit_items()
    emit_furnaces()
    emit_gui()
    emit_effect_icons()
    emit_lang()


if __name__ == "__main__":
    main()
