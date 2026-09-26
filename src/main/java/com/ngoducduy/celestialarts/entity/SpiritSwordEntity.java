package com.ngoducduy.celestialarts.entity;

import com.ngoducduy.celestialarts.registry.GlowParticleEffect;
import com.ngoducduy.celestialarts.registry.ModDamageTypes;
import com.ngoducduy.celestialarts.registry.ModParticles;
import com.ngoducduy.celestialarts.registry.ModSounds;
import com.ngoducduy.celestialarts.util.EntityUtil;
import com.ngoducduy.celestialarts.util.SkillFx;
import com.ngoducduy.celestialarts.util.Targeting;
import com.ngoducduy.celestialarts.network.ModPackets;
import com.ngoducduy.celestialarts.network.FxType;
import com.ngoducduy.celestialarts.network.FxData;
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
 * Phase LAUNCH: after its delay it locks onto the nearest enemy and homes in (or rains on
 * the point the owner is looking at when there is no enemy).<br>
 * Phase STUCK: after hitting it lingers a moment and dissolves into light.</p>
 */
public class SpiritSwordEntity extends Entity {
	private static final TrackedData<Integer> PHASE = DataTracker.registerData(SpiritSwordEntity.class, TrackedDataHandlerRegistry.INTEGER);
	private static final TrackedData<Integer> OWNER_ID = DataTracker.registerData(SpiritSwordEntity.class, TrackedDataHandlerRegistry.INTEGER);
	private static final TrackedData<Float> ORBIT_OFFSET = DataTracker.registerData(SpiritSwordEntity.class, TrackedDataHandlerRegistry.FLOAT);
	/** 0 = ring around the owner (Kiếm Trận), 1 = wall behind the owner's back, tips forward (Vạn Kiếm Quy Tông). */
	private static final TrackedData<Integer> MODE = DataTracker.registerData(SpiritSwordEntity.class, TrackedDataHandlerRegistry.INTEGER);

	public static final int MODE_ORBIT = 0;
	public static final int MODE_FORMATION = 1;

	public static final int PHASE_ORBIT = 0;
	public static final int PHASE_LAUNCH = 1;
	public static final int PHASE_STUCK = 2;

	private LivingEntity owner;
	private int launchDelay = 20;
	private float damage = 7.0f;
	private double orbitRadius = 2.4;
	private double orbitHeight = 1.4;
	private int targetId = -1;
	private Vec3d targetPos;
	/** Formation offset in the owner's frame: x = right, y = up, z = behind. */
	private Vec3d formOffset = Vec3d.ZERO;
	private float formPitch;
	/** Index in the formation, used to spread the swords over several targets. */
	private int formIndex;
	private boolean launched;
	private int lifeAfterLaunch = 0;
	private int stuckTicks = 0;
	private static final double SHATTER_RADIUS = 2.0;

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

	/**
	 * Formation variant: the sword hangs at {@code offset} (right, up, behind) relative to the owner,
	 * always pointing where the owner faces, and launches forward after {@code launchDelay} ticks.
	 */
	public void initFormation(LivingEntity owner, int index, Vec3d offset, float pitchDeg, int launchDelay, float damage) {
		init(owner, index * 37.0f, launchDelay, damage);
		this.formIndex = index;
		this.formOffset = offset;
		this.formPitch = pitchDeg;
		this.dataTracker.set(MODE, MODE_FORMATION);
	}

	public int getMode() {
		return this.dataTracker.get(MODE);
	}

	@Override
	protected void initDataTracker() {
		this.dataTracker.startTracking(MODE, MODE_ORBIT);
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
		if (getMode() == MODE_FORMATION) {
			tickFormation(own);
			return;
		}
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
			} else if (this.age >= launchDelay + 8) {
				// Kiếm Vũ: with nobody to hunt, the swords rain down on the spot the owner is looking at.
				Vec3d aim = Targeting.lookPoint(own, 30);
				targetPos = aim.add((random.nextDouble() - 0.5) * 3.0, 0, (random.nextDouble() - 0.5) * 3.0);
				setPhase(PHASE_LAUNCH);
				Vec3d dir = targetPos.subtract(this.getPos()).normalize();
				this.setVelocity(dir.multiply(0.9));
				this.getWorld().playSound(null, this.getBlockPos(), ModSounds.SWORD_LAUNCH, SoundCategory.PLAYERS, 0.8f, 1.1f + random.nextFloat() * 0.3f);
			}
		}
	}

	/** Vạn Kiếm Quy Tông: hold position in the wall behind the owner, then shoot forward. */
	private void tickFormation(LivingEntity own) {
		float yaw = own.getYaw();
		Vec3d forward = Vec3d.fromPolar(0.0f, yaw);
		Vec3d right = new Vec3d(-forward.z, 0, forward.x);
		double bob = Math.sin((this.age + formIndex * 1.7) * 0.2) * 0.06;
		Vec3d target = own.getPos()
				.add(right.multiply(formOffset.x))
				.add(0, formOffset.y + bob, 0)
				.subtract(forward.multiply(formOffset.z));
		Vec3d cur = this.getPos();
		// Unfold from the owner's back over the first ticks, then track rigidly.
		Vec3d next = this.age < 8 ? cur.lerp(target, 0.35) : target;
		this.setPosition(next.x, next.y, next.z);
		this.prevYaw = this.getYaw();
		this.prevPitch = this.getPitch();
		// Tip forward: projectile yaw convention is atan2(x, z) of the direction.
		this.setYaw((float) Math.toDegrees(MathHelper.atan2(forward.x, forward.z)));
		this.setPitch(formPitch);

		if (this.age >= launchDelay) {
			LivingEntity t = pickTargetSpread(own, formIndex);
			Vec3d launchDir = own.getRotationVec(1.0f);
			if (t != null) {
				targetId = t.getId();
			} else {
				// Nobody ahead: the wall of swords flies to the spot the owner is looking at.
				Vec3d aim = Targeting.lookPoint(own, 36);
				targetPos = aim.add((random.nextDouble() - 0.5) * 4.0, random.nextDouble() * 1.5, (random.nextDouble() - 0.5) * 4.0);
			}
			setPhase(PHASE_LAUNCH);
			// Every sword first shoots straight ahead past the owner, then homes in on its target.
			this.setVelocity(launchDir.multiply(1.3));
			faceVelocity(this.getVelocity());
			if (formIndex % 3 == 0) {
				this.getWorld().playSound(null, this.getBlockPos(), ModSounds.SWORD_LAUNCH, SoundCategory.PLAYERS, 0.8f, 1.0f + random.nextFloat() * 0.4f);
			}
		}
	}

	/** Like {@link #pickTarget} but rotates through the candidates so a volley is shared out. */
	private LivingEntity pickTargetSpread(LivingEntity own, int index) {
		Vec3d look = own.getRotationVec(1.0f);
		var cone = EntityUtil.inCone(this.getWorld(), own, own.getEyePos(), look, 36, 40);
		if (!cone.isEmpty()) {
			var list = EntityUtil.prioritised(cone, own.getPos());
			// Nearer targets take a bigger share: index modulo min(size, 4).
			return list.get(index % Math.min(list.size(), 4));
		}
		return null;
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
			Vec3d want = target.getBoundingBox().getCenter().subtract(this.getPos()).normalize().multiply(getMode() == MODE_FORMATION ? 1.5 : 1.25);
			// Formation swords fly straight for the first few ticks (past the caster), then curve in.
			double steer = getMode() == MODE_FORMATION ? (lifeAfterLaunch < 4 ? 0.05 : 0.25) : 0.22;
			vel = vel.lerp(want, steer);
		} else if (targetPos != null) {
			Vec3d toPoint = targetPos.subtract(this.getPos());
			if (toPoint.lengthSquared() < 1.0) {
				this.setPosition(targetPos.x, targetPos.y, targetPos.z);
				this.setVelocity(Vec3d.ZERO);
				launched = true;
				setPhase(PHASE_STUCK);
				return;
			}
			vel = vel.lerp(toPoint.normalize().multiply(getMode() == MODE_FORMATION ? 1.5 : 1.25), lifeAfterLaunch < 4 && getMode() == MODE_FORMATION ? 0.08 : 0.3);
		}
		this.setVelocity(vel);
		launched = true;

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
		if (stuckTicks > 14) {
			if (launched) shatter();
			dissolve();
		}
	}

	/** Kiếm Khí Bạo: a sword that has struck bursts into a small sword-qi explosion. */
	private void shatter() {
		if (!(this.getWorld() instanceof ServerWorld sw)) return;
		LivingEntity own = getOwner();
		Vec3d c = this.getPos();
		ModPackets.sendFx(sw, FxData.at(FxType.ENERGY_BURST, c, 0xBFF0FF, 0.9f, 6));
		SkillFx.swordGlints(sw, c, 10, 0.35);
		sw.playSound(null, this.getBlockPos(), ModSounds.SWORD_QI, SoundCategory.PLAYERS, 0.7f, 1.5f + random.nextFloat() * 0.2f);
		for (LivingEntity e : EntityUtil.inSphere(sw, own == null ? this : own, c, SHATTER_RADIUS)) {
			e.damage(ModDamageTypes.projectile(sw, ModDamageTypes.SWORD_QI, this, own), damage * 0.4f);
		}
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
