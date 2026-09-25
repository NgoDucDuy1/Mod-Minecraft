package com.ngoducduy.celestialarts.network;

/**
 * Every custom, client-rendered visual effect the server can request.
 * The client maps these to {@code ClientFx} implementations that draw procedural
 * geometry (rings, circles, bolts, beams, domes...) with custom render layers.
 */
public enum FxType {
	/** Expanding flat ring on the ground. scale = final radius. */
	SHOCKWAVE_RING,
	/** Glowing magic circle / formation array. extra = style (0 taiji, 1 runes, 2 thunder, 3 ice). */
	MAGIC_CIRCLE,
	/** Jagged lightning bolt from pos to target. */
	LIGHTNING_BOLT,
	/** Crescent slash sweeping in front of the entity. extra = 0 horizontal, 1 diagonal left, 2 diagonal right, 3 vertical. */
	SLASH_ARC,
	/** Continuous beam from entity eyes to target, follows the entity while alive. */
	BEAM,
	/** Column of fire rising from the ground. scale = radius. */
	FLAME_PILLAR,
	/** Hemispherical ice dome + frost floor. scale = radius. */
	FROST_DOME,
	/** Crescent wind blades orbiting an entity. */
	WIND_BLADES,
	/** Hexagonal energy shell around an entity. */
	TORTOISE_SHIELD,
	/** Dark tribulation cloud with rune ring, hovering above pos. */
	TRIBULATION_CLOUD,
	/** Glowing cracks on the ground radiating from pos in direction. */
	GROUND_CRACK,
	/** Swirling dark vortex in front of the entity. */
	VORTEX,
	/** Spherical burst of light (explosion core). */
	ENERGY_BURST,
	/** Aura column of light around an entity (meditation / breakthrough). */
	QI_AURA,
	/** Ring of ice spikes rising from the ground. */
	ICE_SPIKES,
	/** Pillar of heavenly light + descending circles (breakthrough / heaven sword). */
	HEAVEN_PILLAR,
	/** Blooming lotus petals burst. */
	LOTUS_BLOOM,
	/** Glowing rune glyphs orbiting a point/entity. */
	RUNE_ORBIT,
	/**
	 * Screen-space overlay for nearby players (not a world object). scale = strength 0..1,
	 * extra = 0 flash (bright, fades out), 1 darken (dark vignette held for the duration, fades in/out).
	 * Strength falls off with the viewer's distance to pos over 24 blocks.
	 */
	SCREEN_FLASH;

	private static final FxType[] VALUES = values();

	public static FxType byOrdinal(int ordinal) {
		if (ordinal < 0 || ordinal >= VALUES.length) return SHOCKWAVE_RING;
		return VALUES[ordinal];
	}
}
