package com.ngoducduy.celestialarts.client.particle;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleFactory;
import net.minecraft.client.particle.ParticleTextureSheet;
import net.minecraft.client.particle.SpriteProvider;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.DefaultParticleType;
import net.minecraft.util.math.MathHelper;

/** Slow drifting snowflake that sways sideways as it falls. */
public class SnowflakeParticle extends BaseParticle {
	private final SpriteProvider sprites;

	protected SnowflakeParticle(ClientWorld world, double x, double y, double z, double vx, double vy, double vz, SpriteProvider sprites) {
		super(world, x, y, z, vx, vy, vz);
		this.sprites = sprites;
		this.setSprite(sprites);
		this.setBaseScale(0.07F + this.random.nextFloat() * 0.05F);
		this.maxAge = 50 + this.random.nextInt(40);
		this.setRGB(0xF4FDFF);
		this.gravityStrength = 0.015F;
		this.velocityMultiplier = 0.97F;
		this.angle = this.random.nextFloat() * (float) Math.PI * 2.0F;
		this.prevAngle = this.angle;
		this.fadeIn = 0.1F;
		this.fadeOut = 0.35F;
	}

	@Override
	public void tick() {
		super.tick();
		if (this.dead) return;
		this.prevAngle = this.angle;
		this.angle += 0.04F;
		double sway = Math.sin((this.age + this.hashCode() % 20) * 0.15) * 0.004;
		this.velocityX += sway;
		this.velocityZ += Math.cos((this.age + this.hashCode() % 13) * 0.12) * 0.004;
	}

	@Override
	public ParticleTextureSheet getType() {
		return ParticleTextureSheet.PARTICLE_SHEET_TRANSLUCENT;
	}

	public static class Factory implements ParticleFactory<DefaultParticleType> {
		private final SpriteProvider sprites;

		public Factory(SpriteProvider sprites) {
			this.sprites = sprites;
		}

		@Override
		public Particle createParticle(DefaultParticleType type, ClientWorld world, double x, double y, double z, double vx, double vy, double vz) {
			return new SnowflakeParticle(world, x, y, z, vx, vy, vz, sprites);
		}
	}
}
