package com.ngoducduy.celestialarts.entity;

import com.ngoducduy.celestialarts.network.FxData;
import com.ngoducduy.celestialarts.network.FxType;
import com.ngoducduy.celestialarts.network.ModPackets;
import com.ngoducduy.celestialarts.registry.GlowParticleEffect;
import com.ngoducduy.celestialarts.registry.ModDamageTypes;
import com.ngoducduy.celestialarts.registry.ModEffects;
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
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * Phật Nộ Hỏa Liên – the Buddha's Fury Fire Lotus.
 *
 * <p>Spawned in the caster's palm where it blooms (growth 0 → 1) while following the
 * caster, then launched slowly toward the target. On impact it detonates into a
 * fire pillar and burning shockwave.</p>
 */
public class FireLotusEntity extends SkillProjectileEntity {
	private static final TrackedData<Float> GROWTH = DataTracker.registerData(FireLotusEntity.class, TrackedDataHandlerRegistry.FLOAT);
	private static final TrackedData<Boolean> CHARGING = DataTracker.registerData(FireLotusEntity.class, TrackedDataHandlerRegistry.BOOLEAN);

	public static final int CHARGE_TICKS = 36;
	public static final double BLAST_RADIUS = 5.5;

	private float power = 1.0f;

	public FireLotusEntity(EntityType<? extends FireLotusEntity> type, World world) {
		super(type, world);
		this.damage = 18.0f;
		this.maxAge = 120;
		this.pierce = 0;
	}

	@Override
	protected void initDataTracker() {
		this.dataTracker.startTracking(GROWTH, 0.0f);
		this.dataTracker.startTracking(CHARGING, true);
	}

	public float getGrowth() {
		return this.dataTracker.get(GROWTH);
	}

	public boolean isCharging() {
		return this.dataTracker.get(CHARGING);
	}

	public void setPower(float power) {
		this.power = power;
	}

	/** Position in front of the caster's right palm. */
	public static Vec3d palmPos(LivingEntity caster) {
		Vec3d look = caster.getRotationVec(1.0f);
		Vec3d flat = new Vec3d(look.x, 0, look.z).normalize();
		Vec3d right = flat.crossProduct(new Vec3d(0, 1, 0)).normalize();
		return caster.getEyePos().add(look.multiply(0.9)).add(right.multiply(0.35)).add(0, -0.45, 0);
	}

	@Override
	public void tick() {
		if (isCharging()) {
			tickCharging();
			return;
		}
		super.tick();
	}

	private void tickCharging() {
		this.age++;
		Entity owner = this.getOwner();
		if (owner instanceof LivingEntity caster && caster.isAlive()) {
			Vec3d p = palmPos(caster);
			this.setPosition(p.x, p.y, p.z);
			this.setYaw(caster.getYaw());
		} else if (!this.getWorld().isClient) {
			this.discard();
			return;
		}

		if (!this.getWorld().isClient) {
			float g = MathHelper.clamp(this.age / (float) CHARGE_TICKS, 0f, 1f);
			this.dataTracker.set(GROWTH, g);
			if (this.getWorld() instanceof ServerWorld sw) {
				if (this.age % 4 == 0) {
					SkillFx.helix(sw, GlowParticleEffect.glow(0xFF9A3C, 0.4f, 12), this.getPos().add(0, -0.4, 0), 0.5 * g + 0.15, 0.9, 6, 1, this.age * 0.4);
				}
				if (this.age % 6 == 0) {
					SkillFx.burst(sw, ModParticles.EMBER, this.getPos(), 3, 0.3, 0.02);
				}
				if (this.age == 1) {
					sw.playSound(null, this.getBlockPos(), ModSounds.FIRE_WHOOSH, SoundCategory.PLAYERS, 1.0f, 0.7f);
				}
			}
			if (this.age >= CHARGE_TICKS && owner instanceof LivingEntity caster) {
				launchFromCaster(caster);
			}
		} else {
			// client sparkle
			float g = getGrowth();
			for (int i = 0; i < 2; i++) {
				double a = random.nextDouble() * Math.PI * 2;
				double r = 0.2 + g * 0.6;
				Vec3d p = this.getPos().add(Math.cos(a) * r, random.nextDouble() * 0.3 - 0.1, Math.sin(a) * r);
				this.getWorld().addParticle(ModParticles.EMBER, p.x, p.y, p.z, 0, 0.03, 0);
			}
		}
	}

	public void launchFromCaster(LivingEntity caster) {
		this.dataTracker.set(CHARGING, false);
		this.dataTracker.set(GROWTH, 1.0f);
		this.age = 0;
		Vec3d dir = caster.getRotationVec(1.0f);
		this.launch(dir, 0.55);
		if (this.getWorld() instanceof ServerWorld sw) {
			sw.playSound(null, this.getBlockPos(), ModSounds.FIRE_WHOOSH, SoundCategory.PLAYERS, 1.4f, 1.0f);
			SkillFx.flameBurst(sw, this.getPos(), 20, 0.2);
		}
	}

	@Override
	protected boolean hitTarget(Entity target) {
		explode(target.getBoundingBox().getCenter());
		return true;
	}

	@Override
	protected void onDissipate(Vec3d pos) {
		explode(pos);
	}

	@Override
	protected void onExpire() {
		explode(this.getPos());
	}

	private boolean exploded;

	private void explode(Vec3d center) {
		if (exploded || !(this.getWorld() instanceof ServerWorld sw)) return;
		exploded = true;
		Entity owner = this.getOwner();
		double radius = BLAST_RADIUS * power;

		for (LivingEntity target : EntityUtil.inSphere(sw, owner == null ? this : owner, center, radius)) {
			double d = target.getBoundingBox().getCenter().distanceTo(center);
			float falloff = (float) MathHelper.clamp(1.0 - d / radius, 0.25, 1.0);
			float dmg = (damage * power) * falloff;
			if (target.damage(ModDamageTypes.projectile(sw, ModDamageTypes.FLAME, this, owner), dmg)) {
				target.addStatusEffect(new StatusEffectInstance(ModEffects.QI_BURN, 100, 1, false, true, true), owner);
				target.setOnFireFor(6);
				EntityUtil.knockback(target, center, 0.9 * falloff + 0.3, 0.45);
			}
		}

		// Visuals.
		int orange = 0xFF7A1A;
		ModPackets.sendFx(sw, FxData.at(FxType.ENERGY_BURST, center, 0xFFD36B, (float) (radius * 0.55), 14));
		ModPackets.sendFx(sw, FxData.at(FxType.FLAME_PILLAR, center.add(0, -0.5, 0), orange, (float) (radius * 0.35), 34));
		ModPackets.sendFx(sw, FxData.at(FxType.SHOCKWAVE_RING, center.add(0, 0.1, 0), 0xFFB347, (float) (radius * 1.3), 18));
		ModPackets.sendFx(sw, FxData.at(FxType.LOTUS_BLOOM, center, 0xFF5A2A, (float) radius, 26));
		SkillFx.flameBurst(sw, center, 60, 0.45);
		SkillFx.shell(sw, ModParticles.FLAME_WISP, center, 1.0, 40, 0.35);
		SkillFx.ring(sw, ModParticles.EMBER, center.add(0, 0.2, 0), 1.0, 32, 0.5, 0.15);
		SkillFx.burst(sw, ModParticles.LOTUS_PETAL, center, 40, 0.5, 0.4);
		sw.playSound(null, this.getBlockPos(), ModSounds.FIRE_EXPLOSION, SoundCategory.PLAYERS, 2.0f, 0.9f);
		sw.playSound(null, this.getBlockPos(), SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 1.2f, 0.7f);
		ModPackets.sendCameraShake(sw, center, 30.0, 0.9f, 12);
		this.discard();
	}

	@Override
	protected void clientTick() {
		World world = this.getWorld();
		Vec3d v = this.getVelocity();
		Vec3d p = this.getPos();
		for (int i = 0; i < 3; i++) {
			Vec3d q = p.subtract(v.multiply(random.nextDouble() * 1.5)).add(random.nextGaussian() * 0.25, random.nextGaussian() * 0.25, random.nextGaussian() * 0.25);
			world.addParticle(ModParticles.FLAME_WISP, q.x, q.y, q.z, 0, 0.02, 0);
		}
		world.addParticle(GlowParticleEffect.glow(0xFF9A3C, 1.4f, 8), p.x, p.y, p.z, 0, 0, 0);
		if (random.nextInt(2) == 0) {
			world.addParticle(ModParticles.LOTUS_PETAL, p.x, p.y, p.z, random.nextGaussian() * 0.05, -0.02, random.nextGaussian() * 0.05);
		}
	}
}
