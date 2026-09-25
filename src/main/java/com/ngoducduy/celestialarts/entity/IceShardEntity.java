package com.ngoducduy.celestialarts.entity;

import com.ngoducduy.celestialarts.registry.GlowParticleEffect;
import com.ngoducduy.celestialarts.registry.ModDamageTypes;
import com.ngoducduy.celestialarts.registry.ModEffects;
import com.ngoducduy.celestialarts.registry.ModParticles;
import com.ngoducduy.celestialarts.registry.ModSounds;
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
 * of chill freeze the target solid.
 */
public class IceShardEntity extends SkillProjectileEntity {
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
