package com.ngoducduy.celestialarts.client.particle;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleFactory;
import net.minecraft.client.particle.SpriteProvider;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.DefaultParticleType;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/**
 * Streak of wind stretched along its own velocity (velocity-aligned ribbon instead of a billboard).
 */
public class WindStreakParticle extends BaseParticle {
	protected WindStreakParticle(ClientWorld world, double x, double y, double z, double vx, double vy, double vz, SpriteProvider sprites) {
		super(world, x, y, z, vx, vy, vz);
		this.setSprite(sprites);
		this.setBaseScale(0.06F + this.random.nextFloat() * 0.04F);
		this.maxAge = 10 + this.random.nextInt(8);
		this.setRGB(0xD6FFEC);
		this.baseAlpha = 0.9F;
		this.velocityMultiplier = 0.9F;
		this.fadeIn = 0.1F;
		this.fadeOut = 0.5F;
	}

	@Override
	public void buildGeometry(VertexConsumer vc, Camera camera, float tickDelta) {
		Vec3d cam = camera.getPos();
		double px = MathHelper.lerp(tickDelta, this.prevPosX, this.x) - cam.x;
		double py = MathHelper.lerp(tickDelta, this.prevPosY, this.y) - cam.y;
		double pz = MathHelper.lerp(tickDelta, this.prevPosZ, this.z) - cam.z;
		Vec3d vel = new Vec3d(this.velocityX, this.velocityY, this.velocityZ);
		double speed = vel.length();
		if (speed < 1.0E-4) return;
		Vec3d dir = vel.multiply(1.0 / speed);
		float len = (float) Math.min(1.2, 0.25 + speed * 6.0);
		Vec3d center = new Vec3d(px, py, pz);
		Vec3d toCam = center.negate();
		Vec3d side = dir.crossProduct(toCam);
		if (side.lengthSquared() < 1.0E-6) side = dir.crossProduct(new Vec3d(0, 1, 0));
		side = side.normalize().multiply(this.scale);
		Vec3d a = center.subtract(dir.multiply(len * 0.5));
		Vec3d b = center.add(dir.multiply(len * 0.5));
		float u0 = getMinU(), u1 = getMaxU(), v0 = getMinV(), v1 = getMaxV();
		int light = getBrightness(tickDelta);
		vertex(vc, a.subtract(side), u0, v1, light);
		vertex(vc, a.add(side), u0, v0, light);
		vertex(vc, b.add(side), u1, v0, light);
		vertex(vc, b.subtract(side), u1, v1, light);
	}

	private void vertex(VertexConsumer vc, Vec3d p, float u, float v, int light) {
		vc.vertex(p.x, p.y, p.z).texture(u, v).color(this.red, this.green, this.blue, this.alpha).light(light).next();
	}

	public static class Factory implements ParticleFactory<DefaultParticleType> {
		private final SpriteProvider sprites;

		public Factory(SpriteProvider sprites) {
			this.sprites = sprites;
		}

		@Override
		public Particle createParticle(DefaultParticleType type, ClientWorld world, double x, double y, double z, double vx, double vy, double vz) {
			return new WindStreakParticle(world, x, y, z, vx, vy, vz, sprites);
		}
	}
}
