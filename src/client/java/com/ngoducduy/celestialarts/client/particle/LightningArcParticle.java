package com.ngoducduy.celestialarts.client.particle;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleFactory;
import net.minecraft.client.particle.ParticleTextureSheet;
import net.minecraft.client.particle.SpriteProvider;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.DefaultParticleType;
import net.minecraft.util.math.MathHelper;

/** Short-lived animated electric arc that flickers in place. */
public class LightningArcParticle extends BaseParticle {
	private final SpriteProvider sprites;

	protected LightningArcParticle(ClientWorld world, double x, double y, double z, double vx, double vy, double vz, SpriteProvider sprites) {
		super(world, x, y, z, vx, vy, vz);
		this.sprites = sprites;
		this.setBaseScale(0.25F + this.random.nextFloat() * 0.2F);
		this.maxAge = 4 + this.random.nextInt(4);
		this.setSpriteForAge(sprites);
		this.setRGB(0xE6D6FF);
		this.velocityMultiplier = 0.6F;
		this.angle = this.random.nextFloat() * (float) Math.PI * 2.0F;
		this.prevAngle = this.angle;
		this.fadeOut = 0.5F;
	}

	@Override
	public void tick() {
		super.tick();
		if (this.dead) return;
		this.setSpriteForAge(sprites);
		this.prevAngle = this.angle;
		if (this.random.nextInt(3) == 0) this.angle += (this.random.nextFloat() - 0.5F) * 1.5F;
		this.scale = baseScale * (0.8F + 0.4F * this.random.nextFloat());
	}

	public static class Factory implements ParticleFactory<DefaultParticleType> {
		private final SpriteProvider sprites;

		public Factory(SpriteProvider sprites) {
			this.sprites = sprites;
		}

		@Override
		public Particle createParticle(DefaultParticleType type, ClientWorld world, double x, double y, double z, double vx, double vy, double vz) {
			return new LightningArcParticle(world, x, y, z, vx, vy, vz, sprites);
		}
	}
}
