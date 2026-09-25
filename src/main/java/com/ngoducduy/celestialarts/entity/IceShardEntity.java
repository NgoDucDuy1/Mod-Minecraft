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
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * Hàn Băng Tiễn – an icicle arrow. Slight gravity, chills on hit; three stacks
 * of chill freeze the target solid; an arrow striking an already frozen target shatters
 * the ice, hurting everything nearby.
 */
public class IceShardEntity extends SkillProjectileEntity {
	private static final double SHATTER_RADIUS = 3.0;
	private static final float SHATTER_DAMAGE = 9.0f;

	public IceShardEntity(EntityType<? extends IceShardEntity> type, World world) {
		super(type, world);
		this.damage = 5.0f;
		this.maxAge = 60;
		this.pierce = 0;
		this.gravity = 0.015;
	}

	@Override
	protected boolean hitTarget(Entity target) {
		Entity owner = this.getOwner();
		boolean ok = target.damage(ModDamageTypes.projectile(this.getWorld(), ModDamageTypes.FROST, this, owner), damage);
		if (ok && target instanceof LivingEntity living) {
			if (living.hasStatusEffect(ModEffects.FROZEN) && this.getWorld() instanceof ServerWorld sw) {
				// Băng Toái: an arrow striking a frozen body shatters the ice into a burst of shards.
				living.removeStatusEffect(ModEffects.FROZEN);
				Vec3d c = living.getBoundingBox().getCenter();
				for (LivingEntity other : EntityUtil.inSphere(sw, owner == null ? this : owner, c, SHATTER_RADIUS)) {
					other.damage(ModDamageTypes.projectile(sw, ModDamageTypes.FROST, this, owner), other == living ? SHATTER_DAMAGE : SHATTER_DAMAGE * 0.6f);
					if (other != living) other.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 60, 1, false, false, true), owner);
				}
				ModPackets.sendFx(sw, FxData.at(FxType.ICE_SPIKES, living.getPos(), 0x9BE4FF, (float) SHATTER_RADIUS * 0.6f, 14));
				ModPackets.sendFx(sw, FxData.at(FxType.GROUND_DECAL, living.getPos().add(0, 0.03, 0), 0x9BE4FF, (float) SHATTER_RADIUS * 0.8f, 300).withExtra(1));
				ModPackets.sendFx(sw, FxData.at(FxType.ENERGY_BURST, c, 0xE8FBFF, 1.2f, 6));
				SkillFx.frostBurst(sw, c, 50, 0.4);
				SkillFx.shell(sw, ModParticles.ICE_CRYSTAL, c, 0.8, 30, 0.35);
				sw.playSound(null, living.getBlockPos(), ModSounds.ICE_SHATTER, SoundCategory.PLAYERS, 1.4f, 0.7f);
				return true;
			}
			StatusEffectInstance slow = living.getStatusEffect(StatusEffects.SLOWNESS);
			int stacks = slow == null ? 0 : slow.getAmplifier() + 1;
			if (stacks >= 2) {
				living.removeStatusEffect(StatusEffects.SLOWNESS);
				living.addStatusEffect(new StatusEffectInstance(ModEffects.FROZEN, 60, 0, false, false, true), owner);
				if (this.getWorld() instanceof ServerWorld sw) {
					SkillFx.frostBurst(sw, living.getBoundingBox().getCenter(), 30, 0.2);
					sw.playSound(null, living.getBlockPos(), ModSounds.ICE_SHATTER, SoundCategory.PLAYERS, 1.0f, 0.8f);
				}
			} else {
				living.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 80, stacks, false, false, true), owner);
			}
			living.setFrozenTicks(Math.max(living.getFrozenTicks(), 100));
		}
		return true;
	}

	@Override
	protected void onDissipate(Vec3d pos) {
		if (this.getWorld() instanceof ServerWorld sw) {
			SkillFx.frostBurst(sw, pos, 12, 0.18);
			sw.playSound(null, this.getBlockPos(), ModSounds.ICE_SHATTER, SoundCategory.PLAYERS, 0.6f, 1.3f + random.nextFloat() * 0.3f);
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
		Vec3d p = this.getPos().subtract(v.multiply(0.4));
		world.addParticle(ModParticles.FROST_MIST, p.x, p.y, p.z, 0, 0.005, 0);
		if (random.nextInt(3) == 0) {
			world.addParticle(GlowParticleEffect.glow(0xBFF0FF, 0.3f, 8), p.x, p.y, p.z, 0, 0, 0);
		}
		if (random.nextInt(4) == 0) {
			world.addParticle(ModParticles.SNOWFLAKE, p.x, p.y, p.z, random.nextGaussian() * 0.01, -0.01, random.nextGaussian() * 0.01);
		}
	}
}
