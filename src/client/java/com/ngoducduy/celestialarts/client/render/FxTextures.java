package com.ngoducduy.celestialarts.client.render;

import com.ngoducduy.celestialarts.CelestialArts;
import net.minecraft.util.Identifier;

/** Identifiers of the procedural effect textures (assets/celestialarts/textures/fx). */
public final class FxTextures {
	public static final Identifier RING = fx("ring");
	public static final Identifier GLOW = fx("glow_soft");
	public static final Identifier CIRCLE_TAIJI = fx("circle_taiji");
	public static final Identifier CIRCLE_RUNES = fx("circle_runes");
	public static final Identifier CIRCLE_THUNDER = fx("circle_thunder");
	public static final Identifier CIRCLE_ICE = fx("circle_ice");
	public static final Identifier BEAM = fx("beam");
	public static final Identifier BEAM_CORE = fx("beam_core");
	public static final Identifier SLASH = fx("slash");
	public static final Identifier HEX_SHIELD = fx("hex_shield");
	public static final Identifier FLAME_COLUMN = fx("flame_column");
	public static final Identifier CRACK = fx("crack");
	public static final Identifier VORTEX = fx("vortex");
	public static final Identifier CLOUD = fx("cloud");
	public static final Identifier GLYPHS = fx("glyph_strip");
	public static final Identifier PETAL = fx("petal");
	public static final Identifier FROST = fx("frost");
	public static final Identifier WIND_BLADE = fx("wind_blade");
	public static final Identifier PILLAR = fx("pillar");
	public static final Identifier ICE_SPIKE = fx("ice_spike");
	public static final Identifier SPARKLE = fx("sparkle");
	public static final Identifier SCORCH = fx("scorch");
	public static final Identifier FROST_PATCH = fx("frost_patch");
	/** 512x512 sky formation of Thiên Đạo Chi Thủ (eight trigrams, three rings, 64 glyphs). */
	public static final Identifier CIRCLE_HEAVEN = fx("circle_heaven");
	/** 256x256 golden palm print seared into the ground after the slam. */
	public static final Identifier PALM_PRINT = fx("palm_print");
	/** 256x128 skin of the giant hand model (entity texture folder, used by the FX renderer). */
	public static final Identifier HEAVEN_HAND = new Identifier(CelestialArts.MOD_ID, "textures/entity/heaven_hand.png");

	private FxTextures() {
	}

	private static Identifier fx(String name) {
		return CelestialArts.id("textures/fx/" + name + ".png");
	}
}
