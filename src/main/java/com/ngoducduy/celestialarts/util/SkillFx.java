package com.ngoducduy.celestialarts.util;

import com.ngoducduy.celestialarts.registry.GlowParticleEffect;
import com.ngoducduy.celestialarts.registry.ModParticles;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;

/**
 * Server-side particle "brushes". Each helper spawns a small, shaped burst of
 * particles so skills read as coherent shapes (rings, cones, spirals, trails)
 * instead of random noise.
 */
public final class SkillFx {
	private SkillFx() {
	}

	// ---------------------------------------------------------------- basic

	/** Spawns {@code count} particles spread in a sphere of {@code radius} with a random speed. */
	public static void burst(ServerWorld world, ParticleEffect effect, Vec3d pos, int count, double radius, double speed) {
		world.spawnParticles(effect, pos.x, pos.y, pos.z, count, radius, radius, radius, speed);
	}

	/** Single particle with an explicit velocity. */
	public static void single(ServerWorld world, ParticleEffect effect, Vec3d pos, Vec3d velocity) {
		world.spawnParticles(effect, pos.x, pos.y, pos.z, 0, velocity.x, velocity.y, velocity.z, 1.0);
	}

	public static void glowBurst(ServerWorld world, Vec3d pos, int rgb, int count, float scale, double speed) {
		burst(world, GlowParticleEffect.glow(rgb, scale, 18), pos, count, 0.2, speed);
	}

	public static void sparkBurst(ServerWorld world, Vec3d pos, int rgb, int count, double speed) {
		Random r = world.random;
		for (int i = 0; i < count; i++) {
			Vec3d v = randomUnit(r).multiply(speed * (0.4 + r.nextDouble() * 0.8));
			single(world, GlowParticleEffect.spark(rgb, 0.25f + r.nextFloat() * 0.25f, 14 + r.nextInt(12)), pos, v);
		}
	}

	// --------------------------------------------------------------- shapes

	/** Horizontal ring of particles. */
	public static void ring(ServerWorld world, ParticleEffect effect, Vec3d center, double radius, int count, double outwardSpeed, double upSpeed) {
		for (int i = 0; i < count; i++) {
			double a = (Math.PI * 2 * i) / count;
			double x = Math.cos(a);
			double z = Math.sin(a);
			single(world, effect, center.add(x * radius, 0, z * radius), new Vec3d(x * outwardSpeed, upSpeed, z * outwardSpeed));
		}
	}

	/** Expanding ring of coloured glow. */
	public static void glowRing(ServerWorld world, Vec3d center, double radius, int rgb, int count, double outwardSpeed) {
		ring(world, GlowParticleEffect.glow(rgb, 0.5f, 16), center, radius, count, outwardSpeed, 0.02);
	}

	/** Particles along a straight line from a to b. */
	public static void line(ServerWorld world, ParticleEffect effect, Vec3d a, Vec3d b, double spacing, Vec3d velocity) {
		Vec3d d = b.subtract(a);
		double len = d.length();
		if (len < 1.0E-4) return;
		Vec3d step = d.multiply(spacing / len);
		int n = (int) Math.floor(len / spacing);
		Vec3d p = a;
		for (int i = 0; i <= n; i++) {
			single(world, effect, p, velocity);
			p = p.add(step);
		}
	}

	/** Cone spray from origin along dir. */
	public static void cone(ServerWorld world, ParticleEffect effect, Vec3d origin, Vec3d dir, double halfAngleDeg, int count, double speed) {
		Random r = world.random;
		Vec3d d = dir.normalize();
		for (int i = 0; i < count; i++) {
			Vec3d v = jitter(d, halfAngleDeg, r).multiply(speed * (0.6 + r.nextDouble() * 0.6));
			single(world, effect, origin, v);
		}
	}

	/** Vertical helix rising from base. */
	public static void helix(ServerWorld world, ParticleEffect effect, Vec3d base, double radius, double height, int count, int turns, double phase) {
		for (int i = 0; i < count; i++) {
			double t = i / (double) count;
			double a = phase + t * turns * Math.PI * 2;
			single(world, effect, base.add(Math.cos(a) * radius, t * height, Math.sin(a) * radius), Vec3d.ZERO);
		}
	}

	/**
	 * Particles born on a sphere of {@code radius} that drift <em>inwards</em> and meet at the centre
	 * roughly when a glow particle's velocity has decayed (velocityMultiplier 0.9 ⇒ total travel ≈ 10×v).
	 * This is the "gathering qi" anticipation before a big technique.
	 */
	public static void converge(ServerWorld world, ParticleEffect effect, Vec3d center, double radius, int count) {
		Random r = world.random;
		for (int i = 0; i < count; i++) {
			Vec3d u = randomUnit(r);
			double d = radius * (0.7 + 0.3 * r.nextDouble());
			single(world, effect, center.add(u.multiply(d)), u.multiply(-d * 0.105));
		}
	}

	/** Glow particles converging on a point in the given colour. */
	public static void gatherQi(ServerWorld world, Vec3d center, int rgb, double radius, int count) {
		converge(world, GlowParticleEffect.glow(rgb, 0.35f, 14), center, radius, count);
	}

	/** Sphere surface of particles (explosion shell). */
	public static void shell(ServerWorld world, ParticleEffect effect, Vec3d center, double radius, int count, double speed) {
		Random r = world.random;
		for (int i = 0; i < count; i++) {
			Vec3d u = randomUnit(r);
			single(world, effect, center.add(u.multiply(radius)), u.multiply(speed));
		}
	}

	// ------------------------------------------------------------- elements

	public static void flameBurst(ServerWorld world, Vec3d pos, int count, double speed) {
		burst(world, ModParticles.FLAME_WISP, pos, count, 0.3, speed);
		burst(world, ModParticles.EMBER, pos, count / 2, 0.3, speed * 1.5);
		glowBurst(world, pos, 0xFF7A1A, count / 3, 0.9f, speed * 0.5);
	}

	public static void frostBurst(ServerWorld world, Vec3d pos, int count, double speed) {
		burst(world, ModParticles.ICE_CRYSTAL, pos, count, 0.3, speed);
		burst(world, ModParticles.FROST_MIST, pos, count / 2, 0.5, speed * 0.4);
		glowBurst(world, pos, 0x9BE4FF, count / 3, 0.8f, speed * 0.4);
	}

	public static void thunderSparks(ServerWorld world, Vec3d pos, int count, double speed) {
		burst(world, ModParticles.LIGHTNING_ARC, pos, count, 0.4, speed);
		sparkBurst(world, pos, 0xD9C7FF, count / 2, speed);
	}

	public static void windGust(ServerWorld world, Vec3d pos, Vec3d dir, int count, double speed) {
		cone(world, ModParticles.WIND_STREAK, pos, dir, 25, count, speed);
	}

	public static void voidSmoke(ServerWorld world, Vec3d pos, int count, double speed) {
		burst(world, ModParticles.VOID_SMOKE, pos, count, 0.4, speed);
		glowBurst(world, pos, 0x6A1FB0, count / 3, 1.0f, speed * 0.3);
	}

	public static void rockDebris(ServerWorld world, Vec3d pos, int count, double speed) {
		Random r = world.random;
		for (int i = 0; i < count; i++) {
			Vec3d v = new Vec3d(r.nextGaussian() * 0.3, 0.6 + r.nextDouble() * 0.6, r.nextGaussian() * 0.3).multiply(speed);
			single(world, ModParticles.ROCK_DEBRIS, pos.add(r.nextGaussian() * 0.3, 0, r.nextGaussian() * 0.3), v);
		}
	}

	public static void swordGlints(ServerWorld world, Vec3d pos, int count, double speed) {
		burst(world, ModParticles.SWORD_GLINT, pos, count, 0.3, speed);
		sparkBurst(world, pos, 0x9FE8FF, count, speed);
	}

	public static void runes(ServerWorld world, Vec3d pos, int count, double radius) {
		Random r = world.random;
		for (int i = 0; i < count; i++) {
			double a = r.nextDouble() * Math.PI * 2;
			single(world, ModParticles.RUNE, pos.add(Math.cos(a) * radius, r.nextDouble() * 0.3, Math.sin(a) * radius), new Vec3d(0, 0.03 + r.nextDouble() * 0.03, 0));
		}
	}

	public static void goldenLight(ServerWorld world, Vec3d pos, int count, double radius) {
		burst(world, ModParticles.GOLDEN_LIGHT, pos, count, radius, 0.01);
	}

	// ----------------------------------------------------------------- math

	public static Vec3d randomUnit(Random r) {
		double z = r.nextDouble() * 2 - 1;
		double a = r.nextDouble() * Math.PI * 2;
		double s = Math.sqrt(1 - z * z);
		return new Vec3d(s * Math.cos(a), z, s * Math.sin(a));
	}

	/** Randomly rotates {@code dir} by up to {@code halfAngleDeg}. */
	public static Vec3d jitter(Vec3d dir, double halfAngleDeg, Random r) {
		Vec3d d = dir.normalize();
		Vec3d any = Math.abs(d.y) < 0.9 ? new Vec3d(0, 1, 0) : new Vec3d(1, 0, 0);
		Vec3d u = d.crossProduct(any).normalize();
		Vec3d v = d.crossProduct(u).normalize();
		double ang = Math.toRadians(halfAngleDeg) * Math.sqrt(r.nextDouble());
		double rot = r.nextDouble() * Math.PI * 2;
		Vec3d off = u.multiply(Math.cos(rot)).add(v.multiply(Math.sin(rot))).multiply(Math.sin(ang));
		return d.multiply(Math.cos(ang)).add(off).normalize();
	}

	/** Orthonormal basis (right, up) perpendicular to dir. */
	public static Vec3d[] basis(Vec3d dir) {
		Vec3d d = dir.normalize();
		Vec3d any = Math.abs(d.y) < 0.9 ? new Vec3d(0, 1, 0) : new Vec3d(1, 0, 0);
		Vec3d right = d.crossProduct(any).normalize();
		Vec3d up = right.crossProduct(d).normalize();
		return new Vec3d[]{right, up};
	}
}
