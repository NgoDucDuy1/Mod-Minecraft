package com.ngoducduy.celestialarts.client.particle;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleFactory;
import net.minecraft.client.particle.ParticleTextureSheet;
import net.minecraft.client.particle.SpriteProvider;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.DefaultParticleType;
import net.minecraft.util.math.MathHelper;

/** Mote of heavenly golden light that rises gently, swells and fades. */
public class GoldenLightParticle extends BaseParticle {
	private final SpriteProvider sprites;

	protected GoldenLightParticle(ClientWorld world, double x, double y, double z, double vx, double vy, double vz, SpriteProvider sprites) {
		super(world, x, y, z, vx, vy, vz);
		this.sprites = sprites;
		this.setSprite(sprites);
		this.setBaseScale(0.08F + this.random.nextFloat() * 0.08F);
		this.maxAge = 30 + this.random.nextInt(30);
		this.setRGB(0xFFE9A8);
		this.gravityStrength = -0.012F;
		this.velocityMultiplier = 0.95F;
		this.fadeIn = 0.2F;
		this.fadeOut = 0.45F;
	}

	@Override
	public void tick() {
		super.tick();
		if (this.dead) return;
		float t = life();
		this.scale = baseScale * (0.6F + 0.8F * MathHelper.sin(t * (float) Math.PI));
		this.velocityX += Math.sin((this.age + this.hashCode() % 23) * 0.1) * 0.002;
		this.velocityZ += Math.cos((this.age + this.hashCode() % 29) * 0.1) * 0.002;
	}

	public static class Factory implements ParticleFactory<DefaultParticleType> {
		private final SpriteProvider sprites;

		public Factory(SpriteProvider sprites) {
			this.sprites = sprites;
		}

		@Override
		public Particle createParticle(DefaultParticleType type, ClientWorld world, double x, double y, double z, double vx, double vy, double vz) {
			return new GoldenLightParticle(world, x, y, z, vx, vy, vz, sprites);
		}
	}
}
