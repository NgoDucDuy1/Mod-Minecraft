package com.ngoducduy.celestialarts.entity;

import com.ngoducduy.celestialarts.network.FxData;
import com.ngoducduy.celestialarts.network.FxType;
import com.ngoducduy.celestialarts.network.ModPackets;
import com.ngoducduy.celestialarts.registry.GlowParticleEffect;
import com.ngoducduy.celestialarts.registry.ModDamageTypes;
import com.ngoducduy.celestialarts.registry.ModParticles;
import com.ngoducduy.celestialarts.registry.ModSounds;
import com.ngoducduy.celestialarts.util.EntityUtil;
import com.ngoducduy.celestialarts.util.SkillFx;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Lôi Long – a serpent of purple lightning. Weaves forward, bends toward the nearest enemy,
 * pierces several bodies (forking arcs into their neighbours) and detonates at the end.
 * The client keeps a short position history so the body can be drawn as a crackling ribbon.
 */
public class ThunderDragonEntity extends SkillProjectileEntity {
	public static final int TRAIL_LENGTH = 14;
	private static final double HOMING_RANGE = 14.0;
	private static final float FORK_DAMAGE = 5.0F;
	private static final float BURST_DAMAGE = 10.0F;

	/** Client-side: recent positions, newest first. */
	private final Deque<Vec3d> trail = new ArrayDeque<>();
	private final double phase = Math.random() * Math.PI * 2;

	public ThunderDragonEntity(EntityType<? extends ThunderDragonEntity> type, World world) {
		super(type, world);
		this.damage = 13.0F;
		this.maxAge = 60;
		this.pierce = 3;
	}

	@Override
	public void tick() {
		if (!this.getWorld().isClient && !this.isRemoved()) {
			steer();
		}
		super.tick();
	}

	/** Homing with a sinuous side-to-side weave so the dragon really snakes through the air. */
	private void steer() {
		Vec3d vel = this.getVelocity();
		double speed = vel.length();
		if (speed < 1.0E-3) return;
		Vec3d dir = vel.normalize();
		Entity owner = this.getOwner();
		LivingEntity target = null;
		double best = HOMING_RANGE * HOMING_RANGE;
		for (LivingEntity e : EntityUtil.inSphere(this.getWorld(), owner == null ? this : owner, this.getPos(), HOMING_RANGE)) {
			if (hitEntities.contains(e.getId())) continue;
			Vec3d to = e.getBoundingBox().getCenter().subtract(this.getPos());
			if (to.normalize().dotProduct(dir) < 0.2) continue; // only things roughly ahead
			double d = to.lengthSquared();
			if (d < best) {
				best = d;
				target = e;
			}
		}
		if (target != null) {
			Vec3d want = target.getBoundingBox().getCenter().subtract(this.getPos()).normalize();
			dir = dir.lerp(want, 0.18).normalize();
		}
		Vec3d[] basis = SkillFx.basis(dir);
		double weave = Math.sin(this.age * 0.7 + phase) * 0.22;
		Vec3d next = dir.add(basis[0].multiply(weave)).normalize().multiply(speed);
		this.setVelocity(next);
	}

	@Override
	protected boolean hitTarget(Entity target) {
		Entity owner = this.getOwner();
		World world = this.getWorld();
		boolean ok = target.damage(ModDamageTypes.projectile(world, ModDamageTypes.THUNDER, this, owner), damage);
		if (ok && target instanceof LivingEntity living) {
			living.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 40, 2, false, false, true), owner instanceof LivingEntity l ? l : null);
		}
		if (world instanceof ServerWorld sw) {
			Vec3d c = target.getBoundingBox().getCenter();
			SkillFx.thunderSparks(sw, c, 16, 0.4);
			ModPackets.sendFx(sw, FxData.at(FxType.ENERGY_BURST, c, 0xE6D6FF, 1.0F, 6));
			sw.playSound(null, target.getBlockPos(), ModSounds.THUNDER_STRIKE, SoundCategory.PLAYERS, 1.0F, 1.3F + random.nextFloat() * 0.2F);
			// Fork into up to two neighbours.
			int forks = 0;
			for (LivingEntity other : EntityUtil.inSphere(sw, owner == null ? this : owner, c, 5.0)) {
				if (other == target || forks >= 2) continue;
				forks++;
				Vec3d oc = other.getBoundingBox().getCenter();
				ModPackets.sendFx(sw, FxData.line(FxType.LIGHTNING_BOLT, c, oc, 0xD9C7FF, 0.5F, 7).withExtra(1));
				other.damage(ModDamageTypes.projectile(sw, ModDamageTypes.THUNDER, this, owner), FORK_DAMAGE);
				SkillFx.thunderSparks(sw, oc, 8, 0.3);
			}
		}
		return true;
	}

	@Override
	protected void onDissipate(Vec3d pos) {
		burst(pos);
	}

	@Override
	protected void onExpire() {
		burst(this.getPos());
	}

	private boolean burst;

	/** Lôi Bạo: the serpent discharges everything it has left. */
	private void burst(Vec3d c) {
		if (burst || !(this.getWorld() instanceof ServerWorld sw)) return;
		burst = true;
		Entity owner = this.getOwner();
		for (LivingEntity target : EntityUtil.inSphere(sw, owner == null ? this : owner, c, 3.5)) {
			if (target.damage(ModDamageTypes.projectile(sw, ModDamageTypes.THUNDER, this, owner), BURST_DAMAGE)) {
				EntityUtil.knockback(target, c, 0.7, 0.35);
				ModPackets.sendFx(sw, FxData.line(FxType.LIGHTNING_BOLT, c, target.getBoundingBox().getCenter(), 0xD9C7FF, 0.45F, 6).withExtra(1));
			}
		}
		ModPackets.sendFx(sw, FxData.at(FxType.ENERGY_BURST, c, 0xE6D6FF, 2.0F, 9));
		ModPackets.sendFx(sw, FxData.at(FxType.SHOCKWAVE_RING, c, 0xB57BFF, 4.0F, 10));
		for (int i = 0; i < 5; i++) {
			Vec3d off = SkillFx.randomUnit(random).multiply(2.0 + random.nextDouble() * 2.0);
			ModPackets.sendFx(sw, FxData.line(FxType.LIGHTNING_BOLT, c, c.add(off), 0xB57BFF, 0.35F, 5));
		}
		SkillFx.thunderSparks(sw, c, 40, 0.7);
		sw.playSound(null, this.getBlockPos(), ModSounds.THUNDER_STRIKE, SoundCategory.PLAYERS, 1.8F, 0.8F);
		ModPackets.sendCameraShake(sw, c, 24.0, 0.6F, 8);
	}

	/** Recent positions (newest first) for the client renderer. */
	public Deque<Vec3d> getTrail() {
		return trail;
	}

	@Override
	protected void clientTick() {
		trail.addFirst(this.getPos());
		while (trail.size() > TRAIL_LENGTH) trail.removeLast();
		World world = this.getWorld();
		Vec3d p = this.getPos();
		for (int i = 0; i < 2; i++) {
			Vec3d q = p.add(random.nextGaussian() * 0.3, random.nextGaussian() * 0.3, random.nextGaussian() * 0.3);
			world.addParticle(ModParticles.LIGHTNING_ARC, q.x, q.y, q.z, random.nextGaussian() * 0.05, random.nextGaussian() * 0.05, random.nextGaussian() * 0.05);
		}
		world.addParticle(GlowParticleEffect.glow(0xB57BFF, 0.9F, 6), p.x, p.y, p.z, 0, 0, 0);
	}

	/** Approximate body length in blocks, for the renderer. */
	public float bodyLength() {
		return (float) MathHelper.clamp(this.getVelocity().length() * TRAIL_LENGTH, 3.0, 12.0);
	}
}
