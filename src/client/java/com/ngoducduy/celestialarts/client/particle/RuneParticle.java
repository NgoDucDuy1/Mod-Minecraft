package com.ngoducduy.celestialarts.client.particle;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleFactory;
import net.minecraft.client.particle.ParticleTextureSheet;
import net.minecraft.client.particle.SpriteProvider;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.DefaultParticleType;
import net.minecraft.util.math.MathHelper;

/** A single random dao glyph that materialises, floats upward and dissolves. */
public class RuneParticle extends BaseParticle {
	private final SpriteProvider sprites;

	protected RuneParticle(ClientWorld world, double x, double y, double z, double vx, double vy, double vz, SpriteProvider sprites) {
		super(world, x, y, z, vx, vy, vz);
		this.sprites = sprites;
		this.setSprite(sprites.getSprite(this.random));
		this.setBaseScale(0.14F + this.random.nextFloat() * 0.08F);
		this.maxAge = 24 + this.random.nextInt(16);
		this.setRGB(0xFFE9A8);
		this.velocityMultiplier = 0.95F;
		this.fadeIn = 0.25F;
		this.fadeOut = 0.4F;
	}

	@Override
	public void tick() {
		super.tick();
		if (this.dead) return;
		this.velocityY += 0.003;
		float t = life();
		this.scale = baseScale * (0.7F + 0.3F * MathHelper.sin(t * (float) Math.PI));
	}

	public static class Factory implements ParticleFactory<DefaultParticleType> {
		private final SpriteProvider sprites;

		public Factory(SpriteProvider sprites) {
			this.sprites = sprites;
		}

		@Override
		public Particle createParticle(DefaultParticleType type, ClientWorld world, double x, double y, double z, double vx, double vy, double vz) {
			return new RuneParticle(world, x, y, z, vx, vy, vz, sprites);
		}
	}
}
