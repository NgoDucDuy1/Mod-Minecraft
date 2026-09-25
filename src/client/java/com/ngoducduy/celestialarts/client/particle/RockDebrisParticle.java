package com.ngoducduy.celestialarts.client.particle;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleFactory;
import net.minecraft.client.particle.ParticleTextureSheet;
import net.minecraft.client.particle.SpriteProvider;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.DefaultParticleType;
import net.minecraft.util.math.MathHelper;

/** Chunk of stone thrown up by earth techniques: lit, tumbling, collides and settles. */
public class RockDebrisParticle extends BaseParticle {
	private final SpriteProvider sprites;

	protected RockDebrisParticle(ClientWorld world, double x, double y, double z, double vx, double vy, double vz, SpriteProvider sprites) {
		super(world, x, y, z, vx, vy, vz);
		this.sprites = sprites;
		this.setSprite(sprites);
		this.setBaseScale(0.08F + this.random.nextFloat() * 0.08F);
		this.maxAge = 30 + this.random.nextInt(30);
		this.setRGB(0xA89070);
		this.fullBright = false;
		this.gravityStrength = 0.9F;
		this.collidesWithWorld = true;
		this.velocityMultiplier = 0.98F;
		this.angle = this.random.nextFloat() * (float) Math.PI * 2.0F;
		this.prevAngle = this.angle;
		this.fadeOut = 0.2F;
	}

	@Override
	public void tick() {
		super.tick();
		if (this.dead) return;
		this.prevAngle = this.angle;
		if (!this.onGround) this.angle += 0.25F;
		else {
			this.velocityX *= 0.5;
			this.velocityZ *= 0.5;
		}
	}

	@Override
	public ParticleTextureSheet getType() {
		return ParticleTextureSheet.PARTICLE_SHEET_OPAQUE;
	}

	public static class Factory implements ParticleFactory<DefaultParticleType> {
		private final SpriteProvider sprites;

		public Factory(SpriteProvider sprites) {
			this.sprites = sprites;
		}

		@Override
		public Particle createParticle(DefaultParticleType type, ClientWorld world, double x, double y, double z, double vx, double vy, double vz) {
			return new RockDebrisParticle(world, x, y, z, vx, vy, vz, sprites);
		}
	}
}
