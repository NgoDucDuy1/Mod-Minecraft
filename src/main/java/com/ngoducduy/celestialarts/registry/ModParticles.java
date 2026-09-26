package com.ngoducduy.celestialarts.registry;

import com.mojang.serialization.Codec;
import com.ngoducduy.celestialarts.CelestialArts;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.particle.DefaultParticleType;
import net.minecraft.particle.ParticleType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

/**
 * All custom particle types. Textures live in assets/celestialarts/textures/particle
 * and are generated procedurally at 32x32 or larger (see tools/gen_textures.py).
 */
public final class ModParticles {
	/** Soft coloured glow orb (colour, size and lifetime carried by the effect). */
	public static final ParticleType<GlowParticleEffect> GLOW = register("glow", new ParticleType<>(false, GlowParticleEffect.FACTORY) {
		@Override
		public Codec<GlowParticleEffect> getCodec() {
			return GlowParticleEffect.CODEC_GLOW;
		}
	});
	/** Small four-pointed coloured spark with gravity. */
	public static final ParticleType<GlowParticleEffect> SPARK = register("spark", new ParticleType<>(false, GlowParticleEffect.FACTORY) {
		@Override
		public Codec<GlowParticleEffect> getCodec() {
			return GlowParticleEffect.CODEC_SPARK;
		}
	});

	public static final DefaultParticleType FLAME_WISP = simple("flame_wisp");
	public static final DefaultParticleType EMBER = simple("ember");
	public static final DefaultParticleType ICE_CRYSTAL = simple("ice_crystal");
	public static final DefaultParticleType SNOWFLAKE = simple("snowflake");
	public static final DefaultParticleType FROST_MIST = simple("frost_mist");
	public static final DefaultParticleType LIGHTNING_ARC = simple("lightning_arc");
	public static final DefaultParticleType WIND_STREAK = simple("wind_streak");
	public static final DefaultParticleType VOID_SMOKE = simple("void_smoke");
	public static final DefaultParticleType LOTUS_PETAL = simple("lotus_petal");
	public static final DefaultParticleType RUNE = simple("rune");
	public static final DefaultParticleType SWORD_GLINT = simple("sword_glint");
	public static final DefaultParticleType ROCK_DEBRIS = simple("rock_debris");
	public static final DefaultParticleType GOLDEN_LIGHT = simple("golden_light");

	private ModParticles() {
	}

	private static DefaultParticleType simple(String path) {
		return register(path, FabricParticleTypes.simple());
	}

	private static <T extends ParticleType<?>> T register(String path, T type) {
		return Registry.register(Registries.PARTICLE_TYPE, CelestialArts.id(path), type);
	}

	public static void register() {
	}
}
