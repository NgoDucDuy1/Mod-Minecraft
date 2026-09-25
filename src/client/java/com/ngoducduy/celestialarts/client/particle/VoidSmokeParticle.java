package com.ngoducduy.celestialarts.client.particle;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleFactory;
import net.minecraft.client.particle.ParticleTextureSheet;
import net.minecraft.client.particle.SpriteProvider;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.DefaultParticleType;
import net.minecraft.util.math.MathHelper;

/** Dark violet smoke that swells and slowly spins, used by void techniques. */
public class VoidSmokeParticle extends BaseParticle {
	private final SpriteProvider sprites;

	protected VoidSmokeParticle(ClientWorld world, double x, double y, double z, double vx, double vy, double vz, SpriteProvider sprites) {
		super(world, x, y, z, vx, vy, vz);
		this.sprites = sprites;
		this.setBaseScale(0.22F + this.random.nextFloat() * 0.15F);
		this.maxAge = 20 + this.random.nextInt(16);
		this.setSpriteForAge(sprites);
		this.setRGB(0x4E2A7A);
		this.baseAlpha = 0.85F;
		this.velocityMultiplier = 0.93F;
		this.angle = this.random.nextFloat() * (float) Math.PI * 2.0F;
		this.prevAngle = this.angle;
		this.fadeIn = 0.1F;
		this.fadeOut = 0.5F;
	}

	@Override
	public void tick() {
		super.tick();
		if (this.dead) return;
		this.setSpriteForAge(sprites);
		this.prevAngle = this.angle;
		this.angle += 0.05F;
		this.scale = baseScale * (1.0F + 0.8F * life());
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
			return new VoidSmokeParticle(world, x, y, z, vx, vy, vz, sprites);
		}
	}
}
