package com.ngoducduy.celestialarts.util;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

/**
 * Target selection helpers shared by all skills.
 */
public final class EntityUtil {
	private EntityUtil() {
	}

	/** True when {@code target} may be harmed by {@code caster}'s skills. */
	public static boolean isValidTarget(Entity caster, Entity target) {
		if (target == caster || !target.isAlive() || target.isSpectator()) return false;
		if (!(target instanceof LivingEntity living)) return false;
		if (living instanceof ArmorStandEntity) return false;
		if (living.isInvulnerable()) return false;
		if (target instanceof PlayerEntity p && caster instanceof PlayerEntity c) {
			if (!c.shouldDamagePlayer(p)) return false;
		}
		if (living instanceof TameableEntity tameable && tameable.isTamed() && tameable.getOwner() == caster) return false;
		if (caster.isTeammate(target)) return false;
		// Skip passengers / vehicle of the caster (e.g. the flying sword).
		if (target.hasPassenger(caster) || caster.hasPassenger(target)) return false;
		return true;
	}

	public static Predicate<Entity> targetPredicate(Entity caster) {
		return e -> isValidTarget(caster, e);
	}

	/** Living entities within a sphere. */
	public static List<LivingEntity> inSphere(World world, Entity caster, Vec3d center, double radius) {
		Box box = new Box(center.subtract(radius, radius, radius), center.add(radius, radius, radius));
		List<LivingEntity> out = new ArrayList<>();
		for (LivingEntity e : world.getEntitiesByClass(LivingEntity.class, box, targetPredicate(caster))) {
			if (e.getBoundingBox().getCenter().squaredDistanceTo(center) <= radius * radius
					|| e.getPos().squaredDistanceTo(center) <= radius * radius) {
				out.add(e);
			}
		}
		return out;
	}

	/** Living entities in a horizontal cylinder (ground area of effect). */
	public static List<LivingEntity> inCylinder(World world, Entity caster, Vec3d center, double radius, double height) {
		Box box = new Box(center.x - radius, center.y - 1, center.z - radius, center.x + radius, center.y + height, center.z + radius);
		List<LivingEntity> out = new ArrayList<>();
		for (LivingEntity e : world.getEntitiesByClass(LivingEntity.class, box, targetPredicate(caster))) {
			double dx = e.getX() - center.x;
			double dz = e.getZ() - center.z;
			if (dx * dx + dz * dz <= radius * radius) out.add(e);
		}
		return out;
	}

	/**
	 * Living entities inside a cone in front of {@code origin}.
	 *
	 * @param halfAngleDeg half opening angle in degrees
	 */
	public static List<LivingEntity> inCone(World world, Entity caster, Vec3d origin, Vec3d direction, double range, double halfAngleDeg) {
		Vec3d dir = direction.normalize();
		Box box = new Box(origin.subtract(range, range, range), origin.add(range, range, range));
		double cosLimit = Math.cos(Math.toRadians(halfAngleDeg));
		List<LivingEntity> out = new ArrayList<>();
		for (LivingEntity e : world.getEntitiesByClass(LivingEntity.class, box, targetPredicate(caster))) {
			Vec3d to = e.getBoundingBox().getCenter().subtract(origin);
			double dist = to.length();
			if (dist > range + e.getWidth() * 0.5) continue;
			if (dist < 0.5) {
				out.add(e);
				continue;
			}
			double cos = to.normalize().dotProduct(dir);
			// Widen the cone slightly for big targets.
			double bonus = Math.min(0.25, e.getWidth() / Math.max(1.0, dist) * 0.5);
			if (cos + bonus >= cosLimit) out.add(e);
		}
		return out;
	}

	/** Living entities close to a line segment. */
	public static List<LivingEntity> alongLine(World world, Entity caster, Vec3d from, Vec3d to, double radius) {
		Box box = new Box(from, to).expand(radius);
		Vec3d seg = to.subtract(from);
		double len2 = seg.lengthSquared();
		List<LivingEntity> out = new ArrayList<>();
		for (LivingEntity e : world.getEntitiesByClass(LivingEntity.class, box, targetPredicate(caster))) {
			Vec3d c = e.getBoundingBox().getCenter();
			double t = len2 < 1.0E-6 ? 0 : Math.max(0, Math.min(1, c.subtract(from).dotProduct(seg) / len2));
			Vec3d closest = from.add(seg.multiply(t));
			double r = radius + e.getWidth() * 0.5;
			if (closest.squaredDistanceTo(c) <= r * r) out.add(e);
		}
		return out;
	}

	/** Nearest valid target in a sphere, or null. */
	public static LivingEntity nearest(World world, Entity caster, Vec3d center, double radius) {
		return inSphere(world, caster, center, radius).stream()
				.min(Comparator.comparingDouble(e -> e.squaredDistanceTo(center)))
				.orElse(null);
	}

	/** Priority: hostile monsters first, then nearest. */
	public static List<LivingEntity> prioritised(List<LivingEntity> list, Vec3d from) {
		List<LivingEntity> sorted = new ArrayList<>(list);
		sorted.sort(Comparator.<LivingEntity>comparingInt(e -> e instanceof Monster ? 0 : 1)
				.thenComparingDouble(e -> e.squaredDistanceTo(from)));
		return sorted;
	}

	/** Pushes the target away from a point. */
	public static void knockback(LivingEntity target, Vec3d from, double strength, double upward) {
		Vec3d dir = target.getPos().subtract(from);
		Vec3d flat = new Vec3d(dir.x, 0, dir.z);
		if (flat.lengthSquared() < 1.0E-4) flat = new Vec3d(0, 0, 1);
		flat = flat.normalize();
		target.addVelocity(flat.x * strength, upward, flat.z * strength);
		target.velocityModified = true;
	}

	/** Pulls the target towards a point. */
	public static void pull(LivingEntity target, Vec3d to, double strength) {
		Vec3d dir = to.subtract(target.getBoundingBox().getCenter());
		double dist = dir.length();
		if (dist < 0.3) return;
		Vec3d v = dir.normalize().multiply(Math.min(strength, dist * 0.5));
		target.addVelocity(v.x, v.y * 0.6 + 0.02, v.z);
		target.velocityModified = true;
	}
}
