package com.ngoducduy.celestialarts.entity;

import com.ngoducduy.celestialarts.registry.GlowParticleEffect;
import com.ngoducduy.celestialarts.registry.ModDamageTypes;
import com.ngoducduy.celestialarts.registry.ModParticles;
import com.ngoducduy.celestialarts.registry.ModSounds;
import com.ngoducduy.celestialarts.util.SkillFx;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * Kiếm Khí – a crescent of condensed sword energy. Flies straight, pierces several
 * targets and leaves a trail of glints. {@code roll} tilts the crescent so the
 * three-slash combo alternates horizontal / diagonal cuts.
 */
public class SwordQiEntity extends SkillProjectileEntity {
	private static final TrackedData<Float> ROLL = DataTracker.registerData(SwordQiEntity.class, TrackedDataHandlerRegistry.FLOAT);
	private static final TrackedData<Float> SIZE = DataTracker.registerData(SwordQiEntity.class, TrackedDataHandlerRegistry.FLOAT);

	public SwordQiEntity(EntityType<? extends SwordQiEntity> type, World world) {
		super(type, world);
		this.damage = 9.0f;
		this.maxAge = 45;
		this.pierce = 3;
	}

	@Override
	protected void initDataTracker() {
		this.dataTracker.startTracking(ROLL, 0.0f);
		this.dataTracker.startTracking(SIZE, 1.0f);
	}

	public float getRoll() {
		return this.dataTracker.get(ROLL);
	}

	public void setRoll(float roll) {
		this.dataTracker.set(ROLL, roll);
	}

	public float getSize() {
		return this.dataTracker.get(SIZE);
	}

	public void setSize(float size) {
		this.dataTracker.set(SIZE, size);
	}

	@Override
	protected boolean hitTarget(Entity target) {
		Entity owner = this.getOwner();
		boolean ok = target.damage(ModDamageTypes.projectile(this.getWorld(), ModDamageTypes.SWORD_QI, this, owner), damage);
		if (ok && target instanceof LivingEntity living) {
			Vec3d push = this.getVelocity().normalize().multiply(0.35);
			living.addVelocity(push.x, 0.15, push.z);
			living.velocityModified = true;
			if (this.getWorld() instanceof ServerWorld sw) {
				SkillFx.swordGlints(sw, target.getBoundingBox().getCenter(), 10, 0.25);
				sw.playSound(null, target.getBlockPos(), ModSounds.SWORD_QI, SoundCategory.PLAYERS, 0.8f, 1.4f + random.nextFloat() * 0.3f);
			}
		}
		return true;
	}

	@Override
	protected void onDissipate(Vec3d pos) {
		if (this.getWorld() instanceof ServerWorld sw) {
			SkillFx.swordGlints(sw, pos, 14, 0.3);
			SkillFx.glowBurst(sw, pos, 0x9FE8FF, 8, 0.8f, 0.15);
		}
	}

	@Override
	protected void onExpire() {
		onDissipate(this.getPos());
	}

	@Override
	protected void clientTick() {
		World world = this.getWorld();
		Vec3d v = this.getVelocity();
		float size = getSize();
		// Glints along the edge of the crescent.
		Vec3d[] basis = SkillFx.basis(v);
		double roll = Math.toRadians(getRoll());
		Vec3d edge = basis[0].multiply(Math.cos(roll)).add(basis[1].multiply(Math.sin(roll)));
		for (int i = 0; i < 3; i++) {
			double t = (random.nextDouble() * 2 - 1) * 0.9 * size;
			Vec3d p = this.getPos().add(edge.multiply(t)).subtract(v.multiply(random.nextDouble() * 0.6));
			world.addParticle(GlowParticleEffect.glow(0x9FE8FF, 0.35f + random.nextFloat() * 0.25f, 10), p.x, p.y, p.z, 0, 0, 0);
		}
		if (random.nextInt(2) == 0) {
			Vec3d p = this.getPos().subtract(v.multiply(0.5));
			world.addParticle(ModParticles.SWORD_GLINT, p.x, p.y, p.z, -v.x * 0.05, 0.01, -v.z * 0.05);
		}
	}
}
