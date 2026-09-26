package com.ngoducduy.celestialarts.skill.cast;

import com.ngoducduy.celestialarts.skill.Skill;
import com.ngoducduy.celestialarts.util.EntityUtil;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

/**
 * A lingering area on the ground owned by a player: a sea of fire left by a lotus, the
 * heavenly seal under a fallen sword, a frost field... Subclasses draw the ambience each
 * tick and apply their effect on every pulse.
 */
public abstract class ZoneCast extends ActiveCast {
	protected final Vec3d center;
	protected final double radius;
	protected final int interval;
	protected final double height;

	protected ZoneCast(ServerPlayerEntity caster, Skill skill, Vec3d center, double radius, double height, int duration, int interval) {
		super(caster, skill, duration);
		this.center = center;
		this.radius = radius;
		this.height = height;
		this.interval = Math.max(1, interval);
	}

	@Override
	protected final void onTick() {
		if (age == 0) onStart();
		onAmbient();
		if (age % interval == interval / 2) {
			List<LivingEntity> enemies = new ArrayList<>();
			List<LivingEntity> allies = new ArrayList<>();
			Box box = new Box(center.x - radius, center.y - 1.0, center.z - radius, center.x + radius, center.y + height, center.z + radius);
			for (LivingEntity e : world.getEntitiesByClass(LivingEntity.class, box, LivingEntity::isAlive)) {
				double dx = e.getX() - center.x;
				double dz = e.getZ() - center.z;
				if (dx * dx + dz * dz > radius * radius) continue;
				if (isAlly(e)) allies.add(e);
				else if (EntityUtil.isValidTarget(caster, e)) enemies.add(e);
			}
			onPulse(enemies, allies);
		}
	}

	protected boolean isAlly(LivingEntity e) {
		if (e == caster) return true;
		if (e instanceof PlayerEntity p) return !caster.shouldDamagePlayer(p) || caster.isTeammate(p);
		if (e instanceof TameableEntity t) return t.isTamed();
		return caster.isTeammate(e);
	}

	/** Random point inside the zone at ground height + {@code y}. */
	protected Vec3d randomPoint(double y) {
		double a = world.random.nextDouble() * Math.PI * 2;
		double r = Math.sqrt(world.random.nextDouble()) * radius;
		return center.add(Math.cos(a) * r, y, Math.sin(a) * r);
	}

	protected void onStart() {
	}

	protected abstract void onAmbient();

	protected abstract void onPulse(List<LivingEntity> enemies, List<LivingEntity> allies);

	public Vec3d getCenter() {
		return center;
	}

	public double getRadius() {
		return radius;
	}
}
