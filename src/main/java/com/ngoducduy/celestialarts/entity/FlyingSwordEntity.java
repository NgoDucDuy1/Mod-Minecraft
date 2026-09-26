package com.ngoducduy.celestialarts.entity;

import com.ngoducduy.celestialarts.registry.GlowParticleEffect;
import com.ngoducduy.celestialarts.registry.ModParticles;
import com.ngoducduy.celestialarts.util.SkillFx;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * Ngự Kiếm – a large spirit sword the cultivator stands on and rides through the sky.
 *
 * <p>Movement is client-authoritative for the riding player (like boats): the rider's
 * look direction steers, W/S move along the view vector, A/D strafe, sprint boosts.
 * Other clients interpolate the tracked position. The entity has no gravity, so the
 * vanilla "flying vehicle" kick never triggers.</p>
 */
public class FlyingSwordEntity extends Entity {
	private static final TrackedData<Integer> OWNER_ID = DataTracker.registerData(FlyingSwordEntity.class, TrackedDataHandlerRegistry.INTEGER);
	private static final TrackedData<Float> SPEED_FACTOR = DataTracker.registerData(FlyingSwordEntity.class, TrackedDataHandlerRegistry.FLOAT);

	private static final double BASE_SPEED = 0.85;
	private static final double SPRINT_SPEED = 1.35;

	private int lerpTicks;
	private double lerpX;
	private double lerpY;
	private double lerpZ;
	private double lerpYaw;
	private double lerpPitch;

	private int emptyTicks;
	private int maxLife = 20 * 90;

	public FlyingSwordEntity(EntityType<? extends FlyingSwordEntity> type, World world) {
		super(type, world);
		this.setNoGravity(true);
		this.intersectionChecked = true;
	}

	public void setOwner(LivingEntity owner) {
		this.dataTracker.set(OWNER_ID, owner.getId());
	}

	public void setMaxLife(int ticks) {
		this.maxLife = ticks;
	}

	@Override
	protected void initDataTracker() {
		this.dataTracker.startTracking(OWNER_ID, -1);
		this.dataTracker.startTracking(SPEED_FACTOR, 0.0f);
	}

	/** 0..1 how fast the sword is currently moving, for visual tilt and trails. */
	public float getSpeedFactor() {
		return this.dataTracker.get(SPEED_FACTOR);
	}

	@Nullable
	@Override
	public LivingEntity getControllingPassenger() {
		Entity first = this.getFirstPassenger();
		return first instanceof LivingEntity living ? living : null;
	}

	@Override
	protected boolean canAddPassenger(Entity passenger) {
		return this.getPassengerList().isEmpty() && passenger instanceof LivingEntity;
	}

	@Override
	public double getMountedHeightOffset() {
		return 0.18;
	}

	@Override
	public boolean isCollidable() {
		return false;
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	public boolean canHit() {
		return false;
	}

	@Override
	protected MoveEffect getMoveEffect() {
		return MoveEffect.NONE;
	}

	@Override
	public boolean shouldDismountUnderwater() {
		return false;
	}

	@Override
	public boolean isAttackable() {
		return false;
	}

	@Override
	public void updateTrackedPositionAndAngles(double x, double y, double z, float yaw, float pitch, int interpolationSteps, boolean interpolate) {
		this.lerpX = x;
		this.lerpY = y;
		this.lerpZ = z;
		this.lerpYaw = yaw;
		this.lerpPitch = pitch;
		this.lerpTicks = 10;
	}

	@Override
	public void tick() {
		super.tick();
		LivingEntity rider = this.getControllingPassenger();

		if (this.isLogicalSideForUpdatingMovement()) {
			this.lerpTicks = 0;
			if (rider != null) {
				steer(rider);
			} else {
				this.setVelocity(this.getVelocity().multiply(0.8));
			}
			this.move(MovementType.SELF, this.getVelocity());
		} else {
			if (this.lerpTicks > 0) {
				double nx = this.getX() + (this.lerpX - this.getX()) / this.lerpTicks;
				double ny = this.getY() + (this.lerpY - this.getY()) / this.lerpTicks;
				double nz = this.getZ() + (this.lerpZ - this.getZ()) / this.lerpTicks;
				double dYaw = MathHelper.wrapDegrees(this.lerpYaw - this.getYaw());
				this.setYaw(this.getYaw() + (float) dYaw / this.lerpTicks);
				this.setPitch(this.getPitch() + (float) (this.lerpPitch - this.getPitch()) / this.lerpTicks);
				this.lerpTicks--;
				this.setPosition(nx, ny, nz);
				this.setRotation(this.getYaw(), this.getPitch());
			} else {
				this.setVelocity(Vec3d.ZERO);
			}
		}

		if (!this.getWorld().isClient) {
			if (rider == null) {
				emptyTicks++;
				if (emptyTicks > 15) this.discard();
			} else {
				emptyTicks = 0;
				float f = (float) MathHelper.clamp(this.getVelocity().length() / SPRINT_SPEED, 0.0, 1.0);
				this.dataTracker.set(SPEED_FACTOR, f);
			}
			if (this.age > maxLife) {
				this.removeAllPassengers();
				this.discard();
			}
		} else {
			clientTrail();
		}
	}

	private void steer(LivingEntity rider) {
		float forward = rider.forwardSpeed;
		float sideways = rider.sidewaysSpeed;
		float yaw = rider.getYaw();
		float pitch = rider.getPitch();

		this.setYaw(yaw);
		this.setPitch(pitch * 0.55f);
		this.setRotation(this.getYaw(), this.getPitch());

		Vec3d look = Vec3d.fromPolar(pitch, yaw);
		double rad = Math.toRadians(yaw);
		// +x in local space is the rider's left; positive sidewaysSpeed means "strafe left".
		Vec3d strafe = new Vec3d(Math.cos(rad), 0, Math.sin(rad)).multiply(sideways);

		double speed = rider.isSprinting() ? SPRINT_SPEED : BASE_SPEED;
		Vec3d wish = look.multiply(forward).add(strafe.multiply(0.6));
		if (wish.lengthSquared() > 1.0) wish = wish.normalize();
		wish = wish.multiply(speed);

		Vec3d vel = this.getVelocity();
		if (wish.lengthSquared() < 1.0E-4) {
			// Hovering: bleed off speed and bob gently.
			vel = vel.multiply(0.82);
			vel = vel.add(0, Math.sin(this.age * 0.15) * 0.004, 0);
		} else {
			vel = vel.lerp(wish, 0.28);
		}
		this.setVelocity(vel);
	}

	private void clientTrail() {
		World world = this.getWorld();
		float sf = getSpeedFactor();
		Vec3d back = Vec3d.fromPolar(0, this.getYaw()).multiply(-1.2);
		Vec3d base = this.getPos().add(back).add(0, 0.1, 0);
		world.addParticle(GlowParticleEffect.glow(0x9FE8FF, 0.6f + sf * 0.4f, 12), base.x, base.y, base.z, 0, 0, 0);
		if (sf > 0.15f) {
			int n = 1 + Math.round(sf * 3);
			for (int i = 0; i < n; i++) {
				Vec3d p = base.add(random.nextGaussian() * 0.25, random.nextGaussian() * 0.1, random.nextGaussian() * 0.25);
				world.addParticle(ModParticles.WIND_STREAK, p.x, p.y, p.z, back.x * 0.15, 0, back.z * 0.15);
			}
			if (random.nextInt(3) == 0) {
				world.addParticle(ModParticles.SWORD_GLINT, base.x, base.y, base.z, 0, 0, 0);
			}
		}
		if (this.age % 3 == 0) {
			Vec3d p = this.getPos().add(random.nextGaussian() * 0.4, 0.05, random.nextGaussian() * 0.4);
			world.addParticle(GlowParticleEffect.spark(0xBFF6FF, 0.2f, 12), p.x, p.y, p.z, 0, -0.02, 0);
		}
	}

	@Override
	protected void removePassenger(Entity passenger) {
		super.removePassenger(passenger);
		if (!this.getWorld().isClient && passenger instanceof LivingEntity living) {
			// Gentle landing after dismounting mid-air.
			living.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOW_FALLING, 120, 0, false, false, true));
			living.fallDistance = 0;
		}
	}

	@Override
	protected void updatePassengerPosition(Entity passenger, Entity.PositionUpdater positionUpdater) {
		if (!this.hasPassenger(passenger)) return;
		// Stand slightly back from the centre of the blade.
		Vec3d back = Vec3d.fromPolar(0, this.getYaw()).multiply(-0.15);
		positionUpdater.accept(passenger, this.getX() + back.x, this.getY() + this.getMountedHeightOffset() + passenger.getHeightOffset(), this.getZ() + back.z);
		if (passenger instanceof PlayerEntity) {
			passenger.fallDistance = 0;
		}
	}

	@Override
	public boolean shouldRender(double distance) {
		return distance < 160 * 160;
	}

	@Override
	protected void readCustomDataFromNbt(NbtCompound nbt) {
		this.discard();
	}

	@Override
	protected void writeCustomDataToNbt(NbtCompound nbt) {
	}

	@Override
	public Vec3d updatePassengerForDismount(LivingEntity passenger) {
		Vec3d pos = super.updatePassengerForDismount(passenger);
		return pos == null ? this.getPos() : pos;
	}

	/** Helper for the skill: spawns and mounts the player. */
	public static FlyingSwordEntity summonFor(LivingEntity rider, EntityType<FlyingSwordEntity> type) {
		World world = rider.getWorld();
		FlyingSwordEntity sword = new FlyingSwordEntity(type, world);
		sword.setOwner(rider);
		sword.refreshPositionAndAngles(rider.getX(), rider.getY() + 0.05, rider.getZ(), rider.getYaw(), 0);
		world.spawnEntity(sword);
		rider.startRiding(sword, true);
		if (world instanceof net.minecraft.server.world.ServerWorld sw) {
			SkillFx.swordGlints(sw, rider.getPos().add(0, 0.3, 0), 20, 0.3);
			SkillFx.glowRing(sw, rider.getPos().add(0, 0.1, 0), 1.2, 0x9FE8FF, 24, 0.12);
		}
		return sword;
	}
}
