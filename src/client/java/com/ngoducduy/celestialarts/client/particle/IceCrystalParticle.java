package com.ngoducduy.celestialarts.client.particle;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleFactory;
import net.minecraft.client.particle.ParticleTextureSheet;
import net.minecraft.client.particle.SpriteProvider;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.DefaultParticleType;
import net.minecraft.util.math.MathHelper;

/** Shard of spirit ice that tumbles, glints and slowly sinks. */
public class IceCrystalParticle extends BaseParticle {
	private final SpriteProvider sprites;

	protected IceCrystalParticle(ClientWorld world, double x, double y, double z, double vx, double vy, double vz, SpriteProvider sprites) {
		super(world, x, y, z, vx, vy, vz);
		this.sprites = sprites;
		this.setSprite(sprites);
		this.setBaseScale(0.09F + this.random.nextFloat() * 0.07F);
		this.maxAge = 24 + this.random.nextInt(20);
		this.setRGB(0xDFF7FF);
		this.gravityStrength = 0.08F;
		this.velocityMultiplier = 0.95F;
		this.collidesWithWorld = true;
		this.angle = this.random.nextFloat() * (float) Math.PI * 2.0F;
		this.prevAngle = this.angle;
		this.fadeOut = 0.3F;
	}

	@Override
	public void tick() {
		super.tick();
		if (this.dead) return;
		this.prevAngle = this.angle;
		this.angle += 0.18F;
		float glint = 0.75F + 0.25F * MathHelper.sin(this.age * 0.6F + this.hashCode() % 7);
		this.alpha *= glint;
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
			return new IceCrystalParticle(world, x, y, z, vx, vy, vz, sprites);
		}
	}
}
