#!/usr/bin/env python3
"""Textures for B3 (hỏa chủng trong tự nhiên): the Bình Hỏa Phách capture bottle item and the
Hỏa Linh entity skin. Kept separate from gen_alchemy.py / gen_entity_textures.py so re-running it
never touches the 78-herb / 8-flame / 75-pill asset set those own.

    python3 tools/gen_fire_spirit_assets.py
"""
import math
import os
import sys

sys.path.insert(0, os.path.dirname(__file__))
import numpy as np
from PIL import Image
import texlib as T

ASSETS = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "celestialarts")
ITEM_TEX = os.path.join(ASSETS, "textures", "item")
ENTITY_TEX = os.path.join(ASSETS, "textures", "entity")


def capture_bottle():
	"""An empty glass bottle bound with soul-touched iron bands, a wisp of unclaimed ember
	swirling inside – ready to seal a weakened Hỏa Linh's flame."""
	img = T.new(32, 32)
	glass = (0x9F, 0xD8, 0xE6)
	glass_dark = (0x4A, 0x7A, 0x86)
	band = (0x3A, 0x2E, 0x24)
	band_light = (0x6A, 0x56, 0x40)
	ember = (0xFF, 0xA0, 0x40)
	ember_light = (0xFF, 0xE0, 0xA0)

	# Bottle body (glass), a classic potion-bottle silhouette.
	T.polygon_glow(img, [(11, 30), (9, 20), (9, 16), (12, 13), (20, 13), (23, 16), (23, 20), (21, 30)], glass_dark, glow=1.4, fill_alpha=150)
	T.polygon_glow(img, [(12, 29), (10.5, 20), (10.5, 16.5), (13, 14), (14.5, 14), (13, 29)], glass, glow=0.4, fill_alpha=90)
	# Neck and cork.
	T.polygon_glow(img, [(13, 13), (13, 8), (19, 8), (19, 13)], glass_dark, glow=0.4, fill_alpha=170)
	T.polygon_glow(img, [(12.5, 5), (19.5, 5), (19.5, 8.5), (12.5, 8.5)], band, glow=0.0)
	T.polygon_glow(img, [(13, 5.5), (19, 5.5), (19, 6.3), (13, 6.3)], band_light, glow=0.0)
	# Soul-iron binding bands around the belly.
	for y in (18, 24):
		T.polygon_glow(img, [(9.3, y), (22.7, y), (22.7, y + 1.6), (9.3, y + 1.6)], band, glow=0.2, fill_alpha=235)
		T.polygon_glow(img, [(9.3, y), (22.7, y), (22.7, y + 0.5), (9.3, y + 0.5)], band_light, glow=0.0)
	# A trapped ember wisp, swirling but not yet a flame of its own.
	T.arc_glow(img, [13.5, 18, 18.5, 25], 20, 300, ember, 1.3, glow=1.6)
	T.ellipse_glow(img, [14.5, 20, 17.5, 23], ember_light, glow=1.2, fill_alpha=220)
	T.ellipse_glow(img, [15.3, 20.7, 16.7, 22.1], (255, 255, 255), glow=0.8, fill_alpha=200)
	# Glass highlight.
	T.polygon_glow(img, [(11, 17), (10.3, 22), (11.2, 22), (11.9, 17)], (255, 255, 255), glow=0.4, fill_alpha=90)
	T.save(T.finish(img, 32, 32), os.path.join(ITEM_TEX, "flame_capture_bottle.png"))
	print("item/flame_capture_bottle")


# --------------------------------------------------------------------- entity

def _mix(c1, c2, t):
	t = max(0.0, min(1.0, t))
	return tuple(c1[i] + (c2[i] - c1[i]) * t for i in range(3))


class _Tex:
	"""Tiny stand-in for gen_entity_textures.Tex so this file has no cross-script dependency."""

	def __init__(self, w, h):
		self.w, self.h = w, h
		self.arr = np.zeros((h, w, 4), dtype=np.float32)

	def _regions(self, u, v, w, h, d):
		return {
			"top": (u + d, v, w, d),
			"bottom": (u + d + w, v, w, d),
			"right": (u, v + d, d, h),
			"front": (u + d, v + d, w, h),
			"left": (u + d + w, v + d, d, h),
			"back": (u + 2 * d + w, v + d, w, h),
		}

	def cuboid(self, u, v, w, h, d, painter):
		shade = {"top": 1.0, "bottom": 0.6, "front": 0.9, "back": 0.8, "left": 0.72, "right": 0.72}
		for face, (x0, y0, fw, fh) in self._regions(u, v, w, h, d).items():
			x0, y0, fw, fh = round(x0), round(y0), round(fw), round(fh)
			if fw <= 0 or fh <= 0 or y0 + fh > self.h or x0 + fw > self.w:
				continue
			s = shade[face]
			for yy in range(fh):
				fy = (yy + 0.5) / fh
				for xx in range(fw):
					fx = (xx + 0.5) / fw
					r, g, b, a = painter(face, fx, fy)
					self.arr[y0 + yy, x0 + xx] = (r * s, g * s, b * s, a)

	def save(self, name):
		img = Image.fromarray(np.clip(self.arr + 0.5, 0, 255).astype(np.uint8), "RGBA")
		os.makedirs(ENTITY_TEX, exist_ok=True)
		img.save(os.path.join(ENTITY_TEX, name + ".png"))
		print("entity/" + name)


def fire_spirit():
	"""Hỏa Linh skin, 32x32: a white-hot core (uv 0,0, 7x7x7) and a flame tongue (uv 0,14,
	3x6x3, reused by all six radial copies via yaw in the model)."""
	t = _Tex(32, 32)
	white = (255, 250, 230)
	yellow = (255, 200, 90)
	orange = (255, 120, 30)
	red = (200, 30, 10)

	def core(face, fx, fy):
		d = math.hypot(fx - 0.5, fy - 0.5) * 2.0
		return (*_mix(white, yellow, d), 255)
	t.cuboid(0, 0, 7, 7, 7, core)

	def tongue(face, fx, fy):
		if face in ("top", "bottom"):
			c = _mix(yellow, orange, fy)
			c = _mix(c, red, max(0.0, fy - 0.6) * 2.2)
			c = _mix(c, white, max(0.0, 0.22 - abs(fx - 0.5)) * 3.0 * (1.0 - fy))
			return (*c, 255)
		c = _mix(orange, red, 0.5)
		return (*c, 255)
	t.cuboid(0, 14, 3, 6, 3, tongue)
	t.save("fire_spirit")


def main():
	os.makedirs(ITEM_TEX, exist_ok=True)
	capture_bottle()
	fire_spirit()


if __name__ == "__main__":
	main()
