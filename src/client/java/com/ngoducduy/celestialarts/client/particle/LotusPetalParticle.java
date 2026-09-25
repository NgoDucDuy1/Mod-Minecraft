package com.ngoducduy.celestialarts.client.particle;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleFactory;
import net.minecraft.client.particle.ParticleTextureSheet;
import net.minecraft.client.particle.SpriteProvider;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.DefaultParticleType;
import net.minecraft.util.math.MathHelper;

/** Fire lotus petal that flutters down, rocking side to side. */
public class LotusPetalParticle extends BaseParticle {
	private final SpriteProvider sprites;

	protected LotusPetalParticle(ClientWorld world, double x, double y, double z, double vx, double vy, double vz, SpriteProvider sprites) {
		super(world, x, y, z, vx, vy, vz);
		this.sprites = sprites;
		this.setSprite(sprites);
		this.setBaseScale(0.11F + this.random.nextFloat() * 0.06F);
		this.maxAge = 40 + this.random.nextInt(30);
		this.setRGB(0xFF6A3A);
		this.gravityStrength = 0.03F;
		this.velocityMultiplier = 0.96F;
		this.angle = this.random.nextFloat() * (float) Math.PI * 2.0F;
		this.prevAngle = this.angle;
		this.fadeOut = 0.3F;
	}

	@Override
	public void tick() {
		super.tick();
		if (this.dead) return;
		this.prevAngle = this.angle;
		this.angle += 0.07F;
		double rock = Math.sin((this.age + this.hashCode() % 17) * 0.2) * 0.006;
		this.velocityX += rock;
		this.velocityZ += Math.cos((this.age + this.hashCode() % 11) * 0.17) * 0.006;
		float t = life();
		this.red = 1.0F;
		this.green = MathHelper.lerp(t, 0.42F, 0.2F);
		this.blue = MathHelper.lerp(t, 0.23F, 0.1F);
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
			return new LotusPetalParticle(world, x, y, z, vx, vy, vz, sprites);
		}
	}
}
