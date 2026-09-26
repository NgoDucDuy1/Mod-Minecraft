package com.ngoducduy.celestialarts.client.particle;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleFactory;
import net.minecraft.client.particle.ParticleTextureSheet;
import net.minecraft.client.particle.SpriteProvider;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.DefaultParticleType;
import net.minecraft.util.math.MathHelper;

/** Tiny glowing ember drifting upward with a flickering brightness. */
public class EmberParticle extends BaseParticle {
	private final SpriteProvider sprites;

	protected EmberParticle(ClientWorld world, double x, double y, double z, double vx, double vy, double vz, SpriteProvider sprites) {
		super(world, x, y, z, vx, vy, vz);
		this.sprites = sprites;
		this.setSprite(sprites);
		this.setBaseScale(0.035F + this.random.nextFloat() * 0.03F);
		this.maxAge = 30 + this.random.nextInt(30);
		this.setRGB(0xFF9A3C);
		this.gravityStrength = -0.02F;
		this.velocityMultiplier = 0.96F;
		this.fadeIn = 0.1F;
		this.fadeOut = 0.5F;
	}

	@Override
	public void tick() {
		super.tick();
		if (this.dead) return;
		float flick = 0.7F + 0.3F * MathHelper.sin(this.age * 0.9F + this.hashCode());
		this.alpha *= flick;
		this.velocityX += (this.random.nextFloat() - 0.5F) * 0.004;
		this.velocityZ += (this.random.nextFloat() - 0.5F) * 0.004;
	}

	public static class Factory implements ParticleFactory<DefaultParticleType> {
		private final SpriteProvider sprites;

		public Factory(SpriteProvider sprites) {
			this.sprites = sprites;
		}

		@Override
		public Particle createParticle(DefaultParticleType type, ClientWorld world, double x, double y, double z, double vx, double vy, double vz) {
			return new EmberParticle(world, x, y, z, vx, vy, vz, sprites);
		}
	}
}
