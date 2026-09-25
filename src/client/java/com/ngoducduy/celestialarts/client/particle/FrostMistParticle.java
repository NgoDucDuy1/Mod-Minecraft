package com.ngoducduy.celestialarts.client.particle;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleFactory;
import net.minecraft.client.particle.ParticleTextureSheet;
import net.minecraft.client.particle.SpriteProvider;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.DefaultParticleType;
import net.minecraft.util.math.MathHelper;

/** Large, faint mist cloud that expands and lingers near the ground. */
public class FrostMistParticle extends BaseParticle {
	private final SpriteProvider sprites;

	protected FrostMistParticle(ClientWorld world, double x, double y, double z, double vx, double vy, double vz, SpriteProvider sprites) {
		super(world, x, y, z, vx, vy, vz);
		this.sprites = sprites;
		this.setSprite(sprites);
		this.setBaseScale(0.35F + this.random.nextFloat() * 0.25F);
		this.maxAge = 40 + this.random.nextInt(30);
		this.setRGB(0xCFEFFF);
		this.baseAlpha = 0.35F;
		this.velocityMultiplier = 0.92F;
		this.angle = this.random.nextFloat() * (float) Math.PI * 2.0F;
		this.prevAngle = this.angle;
		this.fadeIn = 0.2F;
		this.fadeOut = 0.5F;
	}

	@Override
	public void tick() {
		super.tick();
		if (this.dead) return;
		this.prevAngle = this.angle;
		this.angle += 0.01F;
		this.scale = baseScale * (1.0F + 1.2F * life());
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
			return new FrostMistParticle(world, x, y, z, vx, vy, vz, sprites);
		}
	}
}
