package com.ngoducduy.celestialarts.client.particle;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleFactory;
import net.minecraft.client.particle.ParticleTextureSheet;
import net.minecraft.client.particle.SpriteProvider;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.DefaultParticleType;
import net.minecraft.util.math.MathHelper;

/** Animated tongue of spirit fire that rises, flickers and cools from orange to deep red. */
public class FlameWispParticle extends BaseParticle {
	private final SpriteProvider sprites;

	protected FlameWispParticle(ClientWorld world, double x, double y, double z, double vx, double vy, double vz, SpriteProvider sprites) {
		super(world, x, y, z, vx, vy, vz);
		this.sprites = sprites;
		this.setBaseScale(0.16F + this.random.nextFloat() * 0.1F);
		this.maxAge = 14 + this.random.nextInt(8);
		this.setSpriteForAge(sprites);
		this.setRGB(0xFFB34A);
		this.velocityMultiplier = 0.9F;
		this.fadeIn = 0.05F;
		this.fadeOut = 0.45F;
	}

	@Override
	public void tick() {
		super.tick();
		if (this.dead) return;
		this.setSpriteForAge(sprites);
		this.velocityY += 0.012;
		float t = life();
		// Orange -> red -> dark.
		this.red = 1.0F;
		this.green = MathHelper.lerp(t, 0.7F, 0.15F);
		this.blue = MathHelper.lerp(t, 0.29F, 0.02F);
		this.scale = baseScale * (1.0F + 0.5F * t) * (1.0F - 0.3F * t);
	}

	public static class Factory implements ParticleFactory<DefaultParticleType> {
		private final SpriteProvider sprites;

		public Factory(SpriteProvider sprites) {
			this.sprites = sprites;
		}

		@Override
		public Particle createParticle(DefaultParticleType type, ClientWorld world, double x, double y, double z, double vx, double vy, double vz) {
			return new FlameWispParticle(world, x, y, z, vx, vy, vz, sprites);
		}
	}
}
