package com.ngoducduy.celestialarts.client.particle;

import com.ngoducduy.celestialarts.registry.ModParticles;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;

/** Binds every particle type to its client factory. */
public final class ModParticleFactories {
	private ModParticleFactories() {
	}

	public static void register() {
		ParticleFactoryRegistry reg = ParticleFactoryRegistry.getInstance();
		reg.register(ModParticles.GLOW, GlowParticle.Factory::new);
		reg.register(ModParticles.SPARK, SparkParticle.Factory::new);
		reg.register(ModParticles.FLAME_WISP, FlameWispParticle.Factory::new);
		reg.register(ModParticles.EMBER, EmberParticle.Factory::new);
		reg.register(ModParticles.ICE_CRYSTAL, IceCrystalParticle.Factory::new);
		reg.register(ModParticles.SNOWFLAKE, SnowflakeParticle.Factory::new);
		reg.register(ModParticles.FROST_MIST, FrostMistParticle.Factory::new);
		reg.register(ModParticles.LIGHTNING_ARC, LightningArcParticle.Factory::new);
		reg.register(ModParticles.WIND_STREAK, WindStreakParticle.Factory::new);
		reg.register(ModParticles.VOID_SMOKE, VoidSmokeParticle.Factory::new);
		reg.register(ModParticles.LOTUS_PETAL, LotusPetalParticle.Factory::new);
		reg.register(ModParticles.RUNE, RuneParticle.Factory::new);
		reg.register(ModParticles.SWORD_GLINT, SwordGlintParticle.Factory::new);
		reg.register(ModParticles.ROCK_DEBRIS, RockDebrisParticle.Factory::new);
		reg.register(ModParticles.GOLDEN_LIGHT, GoldenLightParticle.Factory::new);
	}
}
