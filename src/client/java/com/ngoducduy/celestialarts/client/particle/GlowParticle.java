package com.ngoducduy.celestialarts.client.particle;

import com.ngoducduy.celestialarts.registry.GlowParticleEffect;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleFactory;
import net.minecraft.client.particle.SpriteProvider;
import net.minecraft.client.world.ClientWorld;

/** Soft additive glow orb with configurable colour, size and lifetime. Shrinks as it dies. */
public class GlowParticle extends BaseParticle {
	protected GlowParticle(ClientWorld world, double x, double y, double z, double vx, double vy, double vz, GlowParticleEffect fx, SpriteProvider sprites) {
		super(world, x, y, z, vx, vy, vz);
		this.setSprite(sprites);
		this.red = fx.getRed();
		this.green = fx.getGreen();
		this.blue = fx.getBlue();
		this.setBaseScale(0.12F * fx.getScale());
		this.maxAge = Math.max(2, fx.getLifetime() + this.random.nextInt(4));
		this.fadeIn = 0.1F;
		this.fadeOut = 0.5F;
		this.velocityMultiplier = 0.9F;
	}

	@Override
	public void tick() {
		super.tick();
		float t = life();
		this.scale = baseScale * (1.0F - 0.6F * t * t);
	}

	public static class Factory implements ParticleFactory<GlowParticleEffect> {
		private final SpriteProvider sprites;

		public Factory(SpriteProvider sprites) {
			this.sprites = sprites;
		}

		@Override
		public Particle createParticle(GlowParticleEffect fx, ClientWorld world, double x, double y, double z, double vx, double vy, double vz) {
			return new GlowParticle(world, x, y, z, vx, vy, vz, fx, sprites);
		}
	}
}
