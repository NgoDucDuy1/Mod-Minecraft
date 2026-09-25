package com.ngoducduy.celestialarts.client.particle;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleFactory;
import net.minecraft.client.particle.ParticleTextureSheet;
import net.minecraft.client.particle.SpriteProvider;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.DefaultParticleType;
import net.minecraft.util.math.MathHelper;

/** Four-point sword glint that flares up, spins 45 degrees and vanishes. */
public class SwordGlintParticle extends BaseParticle {
	private final SpriteProvider sprites;

	protected SwordGlintParticle(ClientWorld world, double x, double y, double z, double vx, double vy, double vz, SpriteProvider sprites) {
		super(world, x, y, z, vx, vy, vz);
		this.sprites = sprites;
		this.setSprite(sprites);
		this.setBaseScale(0.12F + this.random.nextFloat() * 0.08F);
		this.maxAge = 6 + this.random.nextInt(6);
		this.setRGB(0xDFFBFF);
		this.velocityMultiplier = 0.85F;
		this.angle = this.random.nextFloat() * 0.5F;
		this.prevAngle = this.angle;
		this.fadeIn = 0.15F;
		this.fadeOut = 0.5F;
	}

	@Override
	public void tick() {
		super.tick();
		if (this.dead) return;
		this.prevAngle = this.angle;
		this.angle += 0.12F;
		float t = life();
		this.scale = baseScale * MathHelper.sin(t * (float) Math.PI) * 1.4F;
	}

	public static class Factory implements ParticleFactory<DefaultParticleType> {
		private final SpriteProvider sprites;

		public Factory(SpriteProvider sprites) {
			this.sprites = sprites;
		}

		@Override
		public Particle createParticle(DefaultParticleType type, ClientWorld world, double x, double y, double z, double vx, double vy, double vz) {
			return new SwordGlintParticle(world, x, y, z, vx, vy, vz, sprites);
		}
	}
}
