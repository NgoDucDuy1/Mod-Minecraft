package com.ngoducduy.celestialarts.entity;

import com.ngoducduy.celestialarts.registry.GlowParticleEffect;
import com.ngoducduy.celestialarts.registry.ModDamageTypes;
import com.ngoducduy.celestialarts.registry.ModParticles;
import com.ngoducduy.celestialarts.registry.ModSounds;
import com.ngoducduy.celestialarts.util.EntityUtil;
import com.ngoducduy.celestialarts.util.SkillFx;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * Phi Kiếm – a summoned spirit sword.
 *
 * <p>Phase ORBIT: hovers in a ring around the owner, tip pointing outward.<br>
 * Phase LAUNCH: after its delay it locks onto the nearest enemy and homes in.<br>
 * Phase STUCK: after hitting it lingers a moment and dissolves into light.</p>
 */
public class SpiritSwordEntity extends Entity {
	private static final TrackedData<Integer> PHASE = DataTracker.registerData(SpiritSwordEntity.class, TrackedDataHandlerRegistry.INTEGER);
	private static final TrackedData<Integer> OWNER_ID = DataTracker.registerData(SpiritSwordEntity.class, TrackedDataHandlerRegistry.INTEGER);
	private static final TrackedData<Float> ORBIT_OFFSET = DataTracker.registerData(SpiritSwordEntity.class, TrackedDataHandlerRegistry.FLOAT);

	public static final int PHASE_ORBIT = 0;
	public static final int PHASE_LAUNCH = 1;
	public static final int PHASE_STUCK = 2;

	private LivingEntity owner;
	private int launchDelay = 20;
	private float damage = 7.0f;
	private double orbitRadius = 2.4;
	private double orbitHeight = 1.4;
	private int targetId = -1;
	private int lifeAfterLaunch = 0;
	private int stuckTicks = 0;

	public SpiritSwordEntity(EntityType<? extends SpiritSwordEntity> type, World world) {
		super(type, world);
		this.noClip = true;
		this.setNoGravity(true);
	}

	public void init(LivingEntity owner, float orbitOffsetDeg, int launchDelay, float damage) {
		this.owner = owner;
		this.launchDelay = launchDelay;
		this.damage = damage;
		this.dataTracker.set(OWNER_ID, owner.getId());
		this.dataTracker.set(ORBIT_OFFSET, orbitOffsetDeg);
	}

	@Override
	protected void initDataTracker() {
		this.dataTracker.startTracking(PHASE, PHASE_ORBIT);
		this.dataTracker.startTracking(OWNER_ID, -1);
		this.dataTracker.startTracking(ORBIT_OFFSET, 0.0f);
	}

	public int getPhase() {
		return this.dataTracker.get(PHASE);
	}

	private void setPhase(int phase) {
		this.dataTracker.set(PHASE, phase);
	}

	public LivingEntity getOwner() {
		if (owner == null) {
			Entity e = this.getWorld().getEntityById(this.dataTracker.get(OWNER_ID));
			if (e instanceof LivingEntity l) owner = l;
		}
		return owner;
	}

	public float getOrbitOffset() {
		return this.dataTracker.get(ORBIT_OFFSET);
	}

	@Override
	public void tick() {
		super.tick();
		int phase = getPhase();
		LivingEntity own = getOwner();

		if (!this.getWorld().isClient) {
			if (own == null || !own.isAlive()) {
				dissolve();
				return;
			}
			switch (phase) {
				case PHASE_ORBIT -> tickOrbit(own);
				case PHASE_LAUNCH -> tickLaunch(own);
				default -> tickStuck();
			}
		} else {
			clientParticles(phase);
		}
	}

	private void tickOrbit(LivingEntity own) {
		double angle = Math.toRadians(getOrbitOffset() + this.age * 4.0);
		double bob = Math.sin((this.age + getOrbitOffset()) * 0.15) * 0.15;
		Vec3d target = own.getPos().add(Math.cos(angle) * orbitRadius, orbitHeight + bob, Math.sin(angle) * orbitRadius);
		Vec3d cur = this.getPos();
		Vec3d next = this.age < 6 ? cur.lerp(target, 0.5) : target;
		this.setPosition(next.x, next.y, next.z);
		// Point outward from the owner, slightly tilted upward.
		this.setYaw((float) Math.toDegrees(-angle) + 90f);
		this.setPitch(20f);
		this.prevYaw = this.getYaw();
		this.prevPitch = this.getPitch();

		if (this.age >= launchDelay) {
			LivingEntity t = pickTarget(own);
			if (t != null) {
				targetId = t.getId();
				setPhase(PHASE_LAUNCH);
				Vec3d dir = t.getBoundingBox().getCenter().subtract(this.getPos()).normalize();
				this.setVelocity(dir.multiply(0.9));
				this.getWorld().playSound(null, this.getBlockPos(), ModSounds.SWORD_LAUNCH, SoundCategory.PLAYERS, 0.9f, 1.0f + random.nextFloat() * 0.3f);
			} else if (this.age > launchDelay + 60) {
				dissolve();
			}
		}
	}

	private LivingEntity pickTarget(LivingEntity own) {
		Vec3d look = own.getRotationVec(1.0f);
		// Prefer targets in front of the owner, then any nearby.
		var cone = EntityUtil.inCone(this.getWorld(), own, own.getEyePos(), look, 28, 35);
		if (!cone.isEmpty()) return EntityUtil.prioritised(cone, own.getPos()).get(0);
		var near = EntityUtil.inSphere(this.getWorld(), own, own.getPos(), 14);
		if (!near.isEmpty()) return EntityUtil.prioritised(near, own.getPos()).get(0);
		return null;
	}

	private void tickLaunch(LivingEntity own) {
		lifeAfterLaunch++;
		Entity t = this.getWorld().getEntityById(targetId);
		Vec3d vel = this.getVelocity();
		if (t instanceof LivingEntity target && target.isAlive()) {
			Vec3d want = target.getBoundingBox().getCenter().subtract(this.getPos()).normalize().multiply(1.25);
			vel = vel.lerp(want, 0.22);
		}
		this.setVelocity(vel);

		// Hit detection along the path.
		Vec3d start = this.getPos();
		Vec3d end = start.add(vel);
		EntityHitResult hit = ProjectileUtil.getEntityCollision(this.getWorld(), this, start, end,
				this.getBoundingBox().stretch(vel).expand(0.6), e -> EntityUtil.isValidTarget(own, e));
		if (hit != null) {
			Entity victim = hit.getEntity();
			if (victim.damage(ModDamageTypes.projectile(this.getWorld(), ModDamageTypes.SWORD_QI, this, own), damage)) {
				if (victim instanceof LivingEntity lv) {
					lv.addVelocity(vel.x * 0.2, 0.1, vel.z * 0.2);
					lv.velocityModified = true;
				}
			}
			if (this.getWorld() instanceof ServerWorld sw) {
				SkillFx.swordGlints(sw, hit.getPos(), 12, 0.3);
				sw.playSound(null, this.getBlockPos(), ModSounds.SWORD_QI, SoundCategory.PLAYERS, 1.0f, 1.2f + random.nextFloat() * 0.3f);
			}
			this.setPosition(hit.getPos().x, hit.getPos().y, hit.getPos().z);
			this.setVelocity(Vec3d.ZERO);
			setPhase(PHASE_STUCK);
			return;
		}

		this.setPosition(end.x, end.y, end.z);
		faceVelocity(vel);

		if (lifeAfterLaunch > 70 || !this.getWorld().getBlockState(this.getBlockPos()).isAir()
				&& this.getWorld().getBlockState(this.getBlockPos()).isOpaque()) {
			if (this.getWorld() instanceof ServerWorld sw) SkillFx.swordGlints(sw, this.getPos(), 8, 0.2);
			setPhase(PHASE_STUCK);
		}
	}

	private void tickStuck() {
		stuckTicks++;
		if (stuckTicks > 14) dissolve();
	}

	private void faceVelocity(Vec3d vel) {
		if (vel.lengthSquared() < 1.0E-6) return;
		double h = vel.horizontalLength();
		float yaw = (float) (MathHelper.atan2(vel.x, vel.z) * (180F / (float) Math.PI));
		float pitch = (float) (MathHelper.atan2(vel.y, h) * (180F / (float) Math.PI));
		this.prevYaw = this.getYaw();
		this.prevPitch = this.getPitch();
		this.setYaw(yaw);
		this.setPitch(pitch);
	}

	public void dissolve() {
		if (this.getWorld() instanceof ServerWorld sw) {
			SkillFx.burst(sw, GlowParticleEffect.glow(0x9FE8FF, 0.5f, 14), this.getPos(), 8, 0.3, 0.05);
		}
		this.discard();
	}

	private void clientParticles(int phase) {
		World world = this.getWorld();
		Vec3d p = this.getPos();
		if (phase == PHASE_LAUNCH) {
			Vec3d v = this.getVelocity();
			for (int i = 0; i < 2; i++) {
				Vec3d q = p.subtract(v.multiply(random.nextDouble()));
				world.addParticle(GlowParticleEffect.glow(0x9FE8FF, 0.45f, 10), q.x, q.y, q.z, 0, 0, 0);
			}
		} else if (random.nextInt(3) == 0) {
			world.addParticle(ModParticles.SWORD_GLINT, p.x, p.y + 0.2, p.z, 0, 0.01, 0);
		}
	}

	@Override
	public boolean shouldRender(double distance) {
		return distance < 128 * 128;
	}

	@Override
	public boolean isAttackable() {
		return false;
	}

	@Override
	protected void readCustomDataFromNbt(NbtCompound nbt) {
		// Spirit swords are transient: never persisted.
		this.discard();
	}

	@Override
	protected void writeCustomDataToNbt(NbtCompound nbt) {
	}
}
