package com.ngoducduy.celestialarts.client.particle;

import com.ngoducduy.celestialarts.registry.GlowParticleEffect;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleFactory;
import net.minecraft.client.particle.SpriteProvider;
import net.minecraft.client.world.ClientWorld;

/** Fast, bright, gravity-affected spark that collides with the world and fades out. */
public class SparkParticle extends BaseParticle {
	protected SparkParticle(ClientWorld world, double x, double y, double z, double vx, double vy, double vz, GlowParticleEffect fx, SpriteProvider sprites) {
		super(world, x, y, z, vx, vy, vz);
		this.setSprite(sprites);
		this.red = fx.getRed();
		this.green = fx.getGreen();
		this.blue = fx.getBlue();
		this.setBaseScale(0.06F * fx.getScale());
		this.maxAge = Math.max(2, fx.getLifetime() + this.random.nextInt(6));
		this.gravityStrength = 0.35F;
		this.collidesWithWorld = true;
		this.velocityMultiplier = 0.94F;
		this.fadeOut = 0.35F;
	}

	@Override
	public void tick() {
		super.tick();
		if (this.onGround) {
			this.velocityX *= 0.6;
			this.velocityZ *= 0.6;
		}
		this.scale = baseScale * (0.5F + 0.5F * (1.0F - life()));
	}

	public static class Factory implements ParticleFactory<GlowParticleEffect> {
		private final SpriteProvider sprites;

		public Factory(SpriteProvider sprites) {
			this.sprites = sprites;
		}

		@Override
		public Particle createParticle(GlowParticleEffect fx, ClientWorld world, double x, double y, double z, double vx, double vy, double vz) {
			return new SparkParticle(world, x, y, z, vx, vy, vz, fx, sprites);
		}
	}
}
