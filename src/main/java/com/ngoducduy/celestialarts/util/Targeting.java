package com.ngoducduy.celestialarts.util;

import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

/**
 * Ray-casting and ground-finding helpers for targeted skills.
 */
public final class Targeting {
	private Targeting() {
	}

	/** Block raycast along the caster's look. */
	public static BlockHitResult raycastBlocks(LivingEntity caster, double range) {
		Vec3d from = caster.getEyePos();
		Vec3d to = from.add(caster.getRotationVec(1.0f).multiply(range));
		return caster.getWorld().raycast(new RaycastContext(from, to, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, caster));
	}

	/** Block raycast between two arbitrary points. */
	public static BlockHitResult raycastBlocks(World world, Entity context, Vec3d from, Vec3d to) {
		return world.raycast(new RaycastContext(from, to, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, context));
	}

	/**
	 * Point the caster is looking at: the first block hit, the first valid entity hit,
	 * or a point {@code range} blocks ahead.
	 */
	public static Vec3d lookPoint(LivingEntity caster, double range) {
		Vec3d from = caster.getEyePos();
		Vec3d dir = caster.getRotationVec(1.0f);
		Vec3d to = from.add(dir.multiply(range));
		BlockHitResult blockHit = raycastBlocks(caster, range);
		if (blockHit.getType() != HitResult.Type.MISS) {
			to = blockHit.getPos();
		}
		// Entities along the line.
		double best = from.squaredDistanceTo(to);
		Vec3d result = to;
		Box box = new Box(from, to).expand(1.0);
		for (LivingEntity e : caster.getWorld().getEntitiesByClass(LivingEntity.class, box, EntityUtil.targetPredicate(caster))) {
			Box eb = e.getBoundingBox().expand(0.3);
			var opt = eb.raycast(from, to);
			if (opt.isPresent()) {
				double d = from.squaredDistanceTo(opt.get());
				if (d < best) {
					best = d;
					result = e.getBoundingBox().getCenter();
				}
			}
		}
		return result;
	}

	/** Point on the ground the caster is aiming at (block hit or the ground under the far point). */
	public static Vec3d groundPoint(LivingEntity caster, double range) {
		Vec3d p = lookPoint(caster, range);
		return snapToGround(caster.getWorld(), p, 12);
	}

	/**
	 * Finds the top surface of the ground at (x, z) near y. Searches down then up a
	 * limited number of blocks; returns the input if nothing solid is found.
	 */
	public static Vec3d snapToGround(World world, Vec3d p, int search) {
		BlockPos.Mutable pos = new BlockPos.Mutable(Math.floor(p.x), Math.floor(p.y), Math.floor(p.z));
		// If we are inside a solid block, go up until free.
		int up = 0;
		while (isSolid(world, pos) && up < search) {
			pos.move(0, 1, 0);
			up++;
		}
		int down = 0;
		while (!isSolid(world, pos.down()) && down < search) {
			pos.move(0, -1, 0);
			down++;
		}
		if (!isSolid(world, pos.down())) return p;
		return new Vec3d(p.x, pos.getY(), p.z);
	}

	public static double groundY(World world, double x, double y, double z, int search) {
		return snapToGround(world, new Vec3d(x, y, z), search).y;
	}

	public static boolean isSolid(World world, BlockPos pos) {
		BlockState state = world.getBlockState(pos);
		return !state.getCollisionShape(world, pos).isEmpty();
	}

	/** Position {@code distance} blocks ahead of the caster's feet, clipped by blocks. */
	public static Vec3d dashDestination(LivingEntity caster, Vec3d dir, double distance) {
		World world = caster.getWorld();
		Vec3d start = caster.getPos().add(0, 0.5, 0);
		Vec3d end = start.add(dir.normalize().multiply(distance));
		BlockHitResult hit = raycastBlocks(world, caster, start, end);
		Vec3d dest = end;
		if (hit.getType() != HitResult.Type.MISS) {
			dest = hit.getPos().subtract(dir.normalize().multiply(0.6));
		}
		// Make sure the destination has head room.
		Vec3d feet = dest.subtract(0, 0.5, 0);
		for (int i = 0; i < 3; i++) {
			BlockPos bp = BlockPos.ofFloored(feet.x, feet.y + 0.1, feet.z);
			BlockPos head = bp.up();
			if (!isSolid(world, bp) && !isSolid(world, head)) break;
			feet = feet.add(0, 1, 0);
		}
		return feet;
	}
}
