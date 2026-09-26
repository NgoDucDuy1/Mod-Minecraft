package com.ngoducduy.celestialarts.skill.cast;

import com.ngoducduy.celestialarts.cultivation.PlayerQi;
import com.ngoducduy.celestialarts.network.FxData;
import com.ngoducduy.celestialarts.network.FxType;
import com.ngoducduy.celestialarts.network.ModPackets;
import com.ngoducduy.celestialarts.registry.GlowParticleEffect;
import com.ngoducduy.celestialarts.registry.ModDamageTypes;
import com.ngoducduy.celestialarts.registry.ModSounds;
import com.ngoducduy.celestialarts.skill.Skill;
import com.ngoducduy.celestialarts.util.EntityUtil;
import com.ngoducduy.celestialarts.util.SkillFx;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

/**
 * Huyền Vũ Hộ Thể – an energy shell that absorbs a pool of damage and reflects part
 * of melee hits back to the attacker. Queried from {@code LivingEntityMixin}.
 */
public class ShieldCast extends ActiveCast {
	private float pool;
	private final float maxPool;
	private final float reflect;
	private final int color;
	/** Share of each hit the shield takes; 1 = full absorption, lower values only soften blows. */
	private final float fraction;
	/** Whether the hexagonal shell is drawn (Huyền Vũ) or the caster supplies their own aura. */
	private final boolean shell;
	private float totalAbsorbed;

	public ShieldCast(ServerPlayerEntity caster, Skill skill, int duration, float pool, float reflect, int color) {
		this(caster, skill, duration, pool, reflect, color, 1.0f, true);
	}

	public ShieldCast(ServerPlayerEntity caster, Skill skill, int duration, float pool, float reflect, int color, float fraction, boolean shell) {
		super(caster, skill, duration);
		this.pool = pool;
		this.maxPool = pool;
		this.reflect = reflect;
		this.color = color;
		this.fraction = fraction;
		this.shell = shell;
	}

	@Nullable
	public static ShieldCast find(PlayerQi qi) {
		for (ActiveCast c : qi.getActiveCasts()) {
			if (c instanceof ShieldCast s && !s.isFinished()) return s;
		}
		return null;
	}

	public float getPool() {
		return pool;
	}

	public float getPoolFraction() {
		return maxPool <= 0 ? 0 : pool / maxPool;
	}

	/** Absorbs damage, returns the amount that still goes through. */
	public float absorb(DamageSource source, float amount) {
		if (isFinished() || amount <= 0) return amount;
		if (source.isOf(DamageTypes.OUT_OF_WORLD) || source.isOf(DamageTypes.STARVE) || source.isOf(DamageTypes.GENERIC_KILL)) {
			return amount;
		}
		float absorbed = Math.min(pool, amount * fraction);
		pool -= absorbed;
		totalAbsorbed += absorbed;
		float remaining = amount - absorbed;

		Vec3d center = caster.getBoundingBox().getCenter();
		Vec3d hitFrom = source.getPosition() != null ? source.getPosition() : center.add(caster.getRotationVec(1f));
		Vec3d hitDir = hitFrom.subtract(center);
		Vec3d hitPos = hitDir.lengthSquared() < 1.0E-4 ? center : center.add(hitDir.normalize().multiply(1.2));
		SkillFx.burst(world, GlowParticleEffect.glow(color, 0.7f, 10), hitPos, 8, 0.25, 0.05);
		SkillFx.sparkBurst(world, hitPos, 0xE0FFF4, 8, 0.25);
		world.playSound(null, caster.getBlockPos(), ModSounds.SHIELD_HIT, SoundCategory.PLAYERS, 0.9f, 0.9f + world.random.nextFloat() * 0.3f);

		Entity attacker = source.getAttacker();
		if (reflect > 0 && attacker instanceof LivingEntity living && attacker != caster && !source.isIndirect()
				&& EntityUtil.isValidTarget(caster, living)) {
			living.damage(ModDamageTypes.source(world, ModDamageTypes.EARTH, caster), absorbed * reflect);
			EntityUtil.knockback(living, center, 0.6, 0.2);
		}

		if (pool <= 0.01f) {
			shatter();
		}
		return remaining;
	}

	public float getTotalAbsorbed() {
		return totalAbsorbed;
	}

	/** The shell breaks: the stored force is released as a repulsion wave (Huyền Vũ Phản Chấn). */
	private void shatter() {
		Vec3d center = caster.getPos();
		float wave = 4.0f + Math.min(12.0f, totalAbsorbed * 0.25f);
		ModPackets.sendFx(world, FxData.at(FxType.SHOCKWAVE_RING, center.add(0, 0.1, 0), color, 4.0f, 12));
		ModPackets.sendFx(world, FxData.at(FxType.ENERGY_BURST, center.add(0, 1.0, 0), color, 1.6f, 8));
		SkillFx.shell(world, GlowParticleEffect.glow(color, 0.6f, 16), caster.getBoundingBox().getCenter(), 1.3, 40, 0.25);
		world.playSound(null, caster.getBlockPos(), ModSounds.ICE_SHATTER, SoundCategory.PLAYERS, 1.0f, 0.9f);
		world.playSound(null, caster.getBlockPos(), ModSounds.EARTH_QUAKE, SoundCategory.PLAYERS, 0.8f, 1.2f);
		for (LivingEntity e : EntityUtil.inSphere(world, caster, center.add(0, 1, 0), 4.0)) {
			if (e.damage(ModDamageTypes.source(world, ModDamageTypes.EARTH, caster), wave)) {
				EntityUtil.knockback(e, center, 1.1, 0.45);
			}
		}
		cancel();
	}

	/** Pressing the key again detonates the shell early. */
	@Override
	public void onRelease() {
		if (!isFinished() && age > 5) shatter();
	}

	@Override
	protected void onTick() {
		if (age == 0) {
			if (shell) ModPackets.sendFx(caster, FxData.follow(FxType.TORTOISE_SHIELD, caster.getId(), caster.getPos(), color, 1.0f, duration));
			world.playSound(null, caster.getBlockPos(), ModSounds.SHIELD_UP, SoundCategory.PLAYERS, 1.0f, 1.0f);
			SkillFx.glowRing(world, caster.getPos().add(0, 0.1, 0), 1.4, color, 24, 0.08);
		}
		if (age % 10 == 0) {
			double a = world.random.nextDouble() * Math.PI * 2;
			Vec3d p = caster.getPos().add(Math.cos(a) * 1.1, 0.1, Math.sin(a) * 1.1);
			SkillFx.single(world, GlowParticleEffect.glow(color, 0.4f, 20), p, new Vec3d(0, 0.06, 0));
		}
	}

	@Override
	public boolean isChannel() {
		return false;
	}
}
