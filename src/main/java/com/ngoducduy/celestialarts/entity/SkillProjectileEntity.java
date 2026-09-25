package com.ngoducduy.celestialarts.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.HashSet;
import java.util.Set;

/**
 * Shared behaviour for skill projectiles: straight (optionally gravity affected)
 * flight, entity/block collision, piercing with per-target hit memory, lifetime,
 * and client-side trail hooks.
 */
public abstract class SkillProjectileEntity extends ProjectileEntity {
	protected float damage = 6.0f;
	protected int maxAge = 60;
	protected int pierce = 0;
	protected double gravity = 0.0;
	protected double drag = 1.0;
	protected final Set<Integer> hitEntities = new HashSet<>();

	protected SkillProjectileEntity(EntityType<? extends SkillProjectileEntity> type, World world) {
		super(type, world);
		this.noClip = false;
		this.setNoGravity(true);
	}

	@Override
	protected void initDataTracker() {
	}

	public float getDamage() {
		return damage;
	}

	public void setDamage(float damage) {
		this.damage = damage;
	}

	public void setMaxAge(int maxAge) {
		this.maxAge = maxAge;
	}

	public void setPierce(int pierce) {
		this.pierce = pierce;
	}

	/** Launches along {@code dir} with the given speed and updates the rotation. */
	public void launch(Vec3d dir, double speed) {
		Vec3d v = dir.normalize().multiply(speed);
		this.setVelocity(v);
		double h = v.horizontalLength();
		this.setYaw((float) (MathHelper.atan2(v.x, v.z) * (180F / (float) Math.PI)));
		this.setPitch((float) (MathHelper.atan2(v.y, h) * (180F / (float) Math.PI)));
		this.prevYaw = this.getYaw();
		this.prevPitch = this.getPitch();
	}

	@Override
	public void tick() {
		super.tick();
		Vec3d vel = this.getVelocity();

		// Collision check against the path travelled this tick.
		HitResult hit = ProjectileUtil.getCollision(this, this::canHit);
		if (hit.getType() != HitResult.Type.MISS) {
			this.onCollision(hit);
		}
		if (this.isRemoved()) return;

		this.checkBlockCollision();

		Vec3d pos = this.getPos().add(vel);
		this.setPosition(pos.x, pos.y, pos.z);

		// Face the direction of travel.
		double h = vel.horizontalLength();
		this.setYaw(updateRotation(this.prevYaw, (float) (MathHelper.atan2(vel.x, vel.z) * (180F / (float) Math.PI))));
		this.setPitch(updateRotation(this.prevPitch, (float) (MathHelper.atan2(vel.y, h) * (180F / (float) Math.PI))));

		if (gravity != 0) vel = vel.add(0, -gravity, 0);
		if (drag != 1.0) vel = vel.multiply(drag);
		this.setVelocity(vel);

		if (this.getWorld().isClient) {
			this.clientTick();
		} else if (this.age >= maxAge) {
			this.onExpire();
			this.discard();
		}
	}

	/** Client-only visual update (trails). */
	protected void clientTick() {
	}

	/** Called on the server when the projectile times out. */
	protected void onExpire() {
	}

	@Override
	protected boolean canHit(Entity entity) {
		if (!super.canHit(entity)) return false;
		if (hitEntities.contains(entity.getId())) return false;
		if (!(entity instanceof LivingEntity)) return false;
		Entity owner = this.getOwner();
		if (owner != null && (owner.hasPassenger(entity) || entity.hasPassenger(owner))) return false;
		return true;
	}

	@Override
	protected void onEntityHit(EntityHitResult hit) {
		super.onEntityHit(hit);
		if (this.getWorld().isClient) return;
		Entity target = hit.getEntity();
		hitEntities.add(target.getId());
		boolean consumed = hitTarget(target);
		if (consumed && hitEntities.size() > pierce) {
			this.onDissipate(hit.getPos());
			this.discard();
		}
	}

	@Override
	protected void onBlockHit(BlockHitResult hit) {
		super.onBlockHit(hit);
		if (this.getWorld().isClient) return;
		this.onDissipate(hit.getPos());
		this.discard();
	}

	/** Applies the projectile's effect to a target. Return true when it counts towards pierce. */
	protected abstract boolean hitTarget(Entity target);

	/** Visual/sound when the projectile is destroyed. */
	protected void onDissipate(Vec3d pos) {
	}

	@Override
	public boolean shouldRender(double distance) {
		return distance < 128 * 128;
	}

	@Override
	protected void writeCustomDataToNbt(NbtCompound nbt) {
		super.writeCustomDataToNbt(nbt);
		nbt.putFloat("Damage", damage);
		nbt.putInt("MaxAge", maxAge);
		nbt.putInt("Pierce", pierce);
	}

	@Override
	protected void readCustomDataFromNbt(NbtCompound nbt) {
		super.readCustomDataFromNbt(nbt);
		if (nbt.contains("Damage")) damage = nbt.getFloat("Damage");
		if (nbt.contains("MaxAge")) maxAge = nbt.getInt("MaxAge");
		if (nbt.contains("Pierce")) pierce = nbt.getInt("Pierce");
	}
}
