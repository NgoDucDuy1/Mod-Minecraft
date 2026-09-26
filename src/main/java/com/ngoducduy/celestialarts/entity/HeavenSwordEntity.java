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
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * Thiên Ngoại Phi Kiếm – a colossal sword that materialises in the heavens above the
 * target, hangs for a heartbeat and plunges down, detonating on impact.
 *
 * <p>Phases: 0 materialise (fade in high above), 1 fall, 2 embedded (glow fades).</p>
 */
public class HeavenSwordEntity extends Entity {
	private static final TrackedData<Integer> PHASE = DataTracker.registerData(HeavenSwordEntity.class, TrackedDataHandlerRegistry.INTEGER);
	private static final TrackedData<Integer> OWNER_ID = DataTracker.registerData(HeavenSwordEntity.class, TrackedDataHandlerRegistry.INTEGER);

	public static final int PHASE_MATERIALISE = 0;
	public static final int PHASE_FALL = 1;
	public static final int PHASE_EMBEDDED = 2;

	public static final int MATERIALISE_TICKS = 40;
	public static final int EMBED_TICKS = 70;
	public static final double SPAWN_HEIGHT = 34.0;
	public static final double FALL_SPEED = 2.6;
	public static final double BLAST_RADIUS = 9.0;

	private LivingEntity owner;
	private float damage = 42.0f;
	private double groundY;
	private int phaseAge;
	private int clientLastPhase = -1;

	public HeavenSwordEntity(EntityType<? extends HeavenSwordEntity> type, World world) {
		super(type, world);
		this.noClip = true;
		this.setNoGravity(true);
	}

	public void init(LivingEntity owner, double groundY, float damage) {
		this.owner = owner;
		this.groundY = groundY;
		this.damage = damage;
		this.dataTracker.set(OWNER_ID, owner.getId());
	}

	@Override
	protected void initDataTracker() {
		this.dataTracker.startTracking(PHASE, PHASE_MATERIALISE);
		this.dataTracker.startTracking(OWNER_ID, -1);
	}

	public int getPhase() {
		return this.dataTracker.get(PHASE);
	}

	private void setPhase(int p) {
		this.dataTracker.set(PHASE, p);
		this.phaseAge = 0;
	}

	public int getPhaseAge() {
		return phaseAge;
	}

	@Override
	public void tick() {
		super.tick();
		phaseAge++;
		int phase = getPhase();
		// The client never calls setPhase(), so restart its phase timer when the tracked phase changes.
		if (this.getWorld().isClient && phase != clientLastPhase) {
			if (clientLastPhase != -1) phaseAge = 0;
			clientLastPhase = phase;
		}

		if (this.getWorld().isClient) {
			clientParticles(phase);
			return;
		}

		if (owner == null) {
			Entity e = this.getWorld().getEntityById(this.dataTracker.get(OWNER_ID));
			if (e instanceof LivingEntity l) owner = l;
		}
		ServerWorld sw = (ServerWorld) this.getWorld();

		switch (phase) {
			case PHASE_MATERIALISE -> {
				if (phaseAge == 1) {
					ModPackets.sendFx(sw, FxData.at(FxType.MAGIC_CIRCLE, this.getPos().add(0, 2.0, 0), 0xFFE9A8, 9.0f, MATERIALISE_TICKS + 10).withExtra(1));
					ModPackets.sendFx(sw, FxData.at(FxType.MAGIC_CIRCLE, new Vec3d(this.getX(), groundY + 0.1, this.getZ()), 0xFFD36B, 8.0f, MATERIALISE_TICKS + 30).withExtra(1));
					sw.playSound(null, this.getBlockPos(), ModSounds.RISER, SoundCategory.PLAYERS, 2.5f, 1.0f);
					sw.playSound(null, this.getBlockPos(), ModSounds.QI_GATHER, SoundCategory.PLAYERS, 1.2f, 0.9f);
				}
				if (phaseAge % 5 == 0) {
					SkillFx.goldenLight(sw, this.getPos().add(0, -4, 0), 12, 3.0);
				}
				if (phaseAge >= MATERIALISE_TICKS) {
					setPhase(PHASE_FALL);
					sw.playSound(null, this.getBlockPos(), ModSounds.HEAVEN_SWORD_FALL, SoundCategory.PLAYERS, 3.0f, 1.0f);
				}
			}
			case PHASE_FALL -> {
				double ny = this.getY() - FALL_SPEED;
				// The tip is ~6 blocks below the entity origin; stop when it reaches the ground.
				if (ny - 6.0 <= groundY) {
					this.setPosition(this.getX(), groundY + 6.0, this.getZ());
					impact(sw);
					setPhase(PHASE_EMBEDDED);
				} else {
					this.setPosition(this.getX(), ny, this.getZ());
				}
			}
			default -> {
				if (phaseAge % 10 == 0) {
					SkillFx.burst(sw, ModParticles.GOLDEN_LIGHT, new Vec3d(this.getX(), groundY + 0.5, this.getZ()), 6, 2.5, 0.02);
				}
				if (phaseAge >= EMBED_TICKS) {
					SkillFx.burst(sw, GlowParticleEffect.glow(0xFFE9A8, 1.2f, 20), this.getPos(), 40, 2.5, 0.05);
					this.discard();
				}
			}
		}
	}

	private void impact(ServerWorld sw) {
		Vec3d center = new Vec3d(this.getX(), groundY, this.getZ());
		Entity src = owner == null ? this : owner;
		for (LivingEntity target : EntityUtil.inSphere(sw, src, center.add(0, 1, 0), BLAST_RADIUS)) {
			double d = target.getPos().distanceTo(center);
			float falloff = (float) MathHelper.clamp(1.0 - d / BLAST_RADIUS, 0.3, 1.0);
			if (target.damage(ModDamageTypes.projectile(sw, ModDamageTypes.HEAVEN, this, owner), damage * falloff)) {
				EntityUtil.knockback(target, center, 1.6 * falloff, 0.8 * falloff);
			}
		}
		ModPackets.sendFx(sw, FxData.at(FxType.HEAVEN_PILLAR, center, 0xFFE9A8, 3.0f, 50));
		ModPackets.sendFx(sw, FxData.at(FxType.GROUND_DECAL, center.add(0, 0.03, 0), 0xFFE9A8, 7.0f, 800));
		ModPackets.sendFx(sw, FxData.at(FxType.ENERGY_BURST, center.add(0, 1.5, 0), 0xFFFFFF, 6.0f, 16));
		ModPackets.sendFx(sw, FxData.at(FxType.SHOCKWAVE_RING, center.add(0, 0.15, 0), 0xFFE9A8, 14.0f, 24));
		ModPackets.sendFx(sw, FxData.at(FxType.SHOCKWAVE_RING, center.add(0, 0.15, 0), 0xFFFFFF, 8.0f, 14));
		for (int i = 0; i < 8; i++) {
			double a = i * Math.PI / 4;
			Vec3d dir = new Vec3d(Math.cos(a), 0, Math.sin(a));
			ModPackets.sendFx(sw, FxData.line(FxType.GROUND_CRACK, center.add(0, 0.05, 0), center.add(dir.multiply(7)), 0xFFD36B, 1.0f, 80));
		}
		SkillFx.shell(sw, GlowParticleEffect.glow(0xFFE9A8, 1.0f, 24), center.add(0, 1, 0), 1.5, 80, 0.7);
		SkillFx.rockDebris(sw, center, 60, 1.3);
		SkillFx.goldenLight(sw, center.add(0, 2, 0), 60, 4.0);
		sw.playSound(null, this.getBlockPos(), ModSounds.HEAVEN_SWORD_IMPACT, SoundCategory.PLAYERS, 4.0f, 0.95f);
		sw.playSound(null, this.getBlockPos(), ModSounds.SUB_DROP, SoundCategory.PLAYERS, 3.0f, 1.0f);
		sw.playSound(null, this.getBlockPos(), SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 2.0f, 0.5f);
		ModPackets.sendCameraShake(sw, center, 48.0, 1.6f, 22);
		ModPackets.sendFx(sw, FxData.at(FxType.SCREEN_FLASH, center, 0xFFF1C8, 1.0f, 16));
		if (owner instanceof net.minecraft.server.network.ServerPlayerEntity sp) {
			com.ngoducduy.celestialarts.skill.skills.HeavenSwordSkill.seal(sp, center);
		}
	}

	private void clientParticles(int phase) {
		World world = this.getWorld();
		if (phase == PHASE_FALL) {
			for (int i = 0; i < 6; i++) {
				Vec3d p = this.getPos().add(random.nextGaussian() * 0.8, random.nextDouble() * 10 - 5, random.nextGaussian() * 0.8);
				world.addParticle(GlowParticleEffect.glow(0xFFE9A8, 1.2f, 10), p.x, p.y, p.z, 0, 0.1, 0);
			}
			for (int i = 0; i < 3; i++) {
				Vec3d p = this.getPos().add(random.nextGaussian() * 1.5, random.nextDouble() * 12 - 4, random.nextGaussian() * 1.5);
				world.addParticle(ModParticles.WIND_STREAK, p.x, p.y, p.z, 0, 0.4, 0);
			}
		} else if (phase == PHASE_MATERIALISE) {
			if (random.nextInt(2) == 0) {
				double a = random.nextDouble() * Math.PI * 2;
				double r = 3 + random.nextDouble() * 4;
				Vec3d p = this.getPos().add(Math.cos(a) * r, random.nextDouble() * 6 - 5, Math.sin(a) * r);
				world.addParticle(ModParticles.GOLDEN_LIGHT, p.x, p.y, p.z, -Math.cos(a) * 0.08, 0.02, -Math.sin(a) * 0.08);
			}
		}
	}

	@Override
	public boolean shouldRender(double distance) {
		return distance < 256 * 256;
	}

	@Override
	public boolean isAttackable() {
		return false;
	}

	@Override
	public boolean canHit() {
		return false;
	}

	@Override
	protected void readCustomDataFromNbt(NbtCompound nbt) {
		this.discard();
	}

	@Override
	protected void writeCustomDataToNbt(NbtCompound nbt) {
	}
}
