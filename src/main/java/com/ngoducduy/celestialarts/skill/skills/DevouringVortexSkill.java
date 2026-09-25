package com.ngoducduy.celestialarts.skill.skills;

import com.ngoducduy.celestialarts.cultivation.PlayerQi;
import com.ngoducduy.celestialarts.cultivation.QiHolder;
import com.ngoducduy.celestialarts.cultivation.Realm;
import com.ngoducduy.celestialarts.network.FxData;
import com.ngoducduy.celestialarts.network.FxType;
import com.ngoducduy.celestialarts.network.ModPackets;
import com.ngoducduy.celestialarts.registry.GlowParticleEffect;
import com.ngoducduy.celestialarts.registry.ModDamageTypes;
import com.ngoducduy.celestialarts.registry.ModParticles;
import com.ngoducduy.celestialarts.registry.ModSounds;
import com.ngoducduy.celestialarts.skill.Element;
import com.ngoducduy.celestialarts.skill.Skill;
import com.ngoducduy.celestialarts.skill.SkillContext;
import com.ngoducduy.celestialarts.skill.SkillType;
import com.ngoducduy.celestialarts.skill.cast.ActiveCast;
import com.ngoducduy.celestialarts.util.EntityUtil;
import com.ngoducduy.celestialarts.util.SkillFx;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.Vec3d;

/**
 * Thôn Thiên Ma Công – a devouring vortex opens in front of the caster, dragging
 * enemies in and draining their vitality to restore the cultivator's health and Qi.
 */
public class DevouringVortexSkill extends Skill {
	private static final int MAX_DURATION = 100;
	private static final double PULL_RADIUS = 10.0;
	private static final double CORE_RADIUS = 2.2;
	private static final float DRAIN_PER_TICK = 0.9f;

	public DevouringVortexSkill() {
		super(Settings.of(Element.VOID, SkillType.CHANNEL, Realm.NASCENT_SOUL, 20f, 300));
	}

	@Override
	public boolean isChannel() {
		return true;
	}

	@Override
	public boolean activate(SkillContext ctx) {
		ctx.qi().addActiveCast(new VortexCast(ctx.player(), this));
		return true;
	}

	private static final class VortexCast extends ActiveCast {
		private float devoured;

		VortexCast(ServerPlayerEntity caster, Skill skill) {
			super(caster, skill, MAX_DURATION);
		}

		@Override
		public boolean isChannel() {
			return true;
		}

		@Override
		public void onRelease() {
			finish();
		}

		private Vec3d core() {
			Vec3d flat = caster.getRotationVec(1.0f);
			flat = new Vec3d(flat.x, 0, flat.z);
			if (flat.lengthSquared() < 1.0E-4) flat = new Vec3d(0, 0, 1);
			return caster.getPos().add(flat.normalize().multiply(3.0)).add(0, 1.4, 0);
		}

		@Override
		protected void onTick() {
			int voidColor = 0x6A1FB0;
			if (age == 0) {
				ModPackets.sendFx(caster, FxData.follow(FxType.VORTEX, caster.getId(), caster.getPos(), voidColor, (float) CORE_RADIUS, MAX_DURATION));
				world.playSound(null, caster.getBlockPos(), ModSounds.VOID_DRAIN, SoundCategory.PLAYERS, 1.3f, 0.8f);
			}
			PlayerQi qi = QiHolder.get(caster);
			if (!caster.isCreative() && !qi.consumeQi(DRAIN_PER_TICK)) {
				finish();
				return;
			}
			caster.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 5, 2, false, false, false));
			Vec3d core = core();

			if (age % 20 == 0 && age > 0) {
				world.playSound(null, caster.getBlockPos(), ModSounds.VOID_DRAIN, SoundCategory.PLAYERS, 0.8f, 0.9f + world.random.nextFloat() * 0.2f);
			}
			// Inward spiralling smoke.
			if (age % 2 == 0) {
				double a = world.random.nextDouble() * Math.PI * 2;
				double r = 3 + world.random.nextDouble() * 5;
				Vec3d p = core.add(Math.cos(a) * r, world.random.nextGaussian() * 1.2, Math.sin(a) * r);
				Vec3d v = core.subtract(p).normalize().multiply(0.35).add(-Math.sin(a) * 0.15, 0, Math.cos(a) * 0.15);
				SkillFx.single(world, ModParticles.VOID_SMOKE, p, v);
				SkillFx.single(world, GlowParticleEffect.glow(0xD24BFF, 0.35f, 16), p, v);
			}

			for (LivingEntity target : EntityUtil.inSphere(world, caster, core, PULL_RADIUS)) {
				EntityUtil.pull(target, core, 0.16);
				if (age % 5 == 0 && target.getBoundingBox().getCenter().squaredDistanceTo(core) <= CORE_RADIUS * CORE_RADIUS) {
					float dmg = 2.5f;
					if (target.damage(ModDamageTypes.source(world, ModDamageTypes.VOID_DRAIN, caster), dmg)) {
						devoured += dmg;
						caster.heal(dmg * 0.5f);
						qi.addQi(dmg * 1.5f);
						target.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 40, 0, false, false, true), caster);
						SkillFx.line(world, GlowParticleEffect.glow(0xD24BFF, 0.4f, 8), target.getBoundingBox().getCenter(), caster.getEyePos(), 0.5, Vec3d.ZERO);
					}
				}
			}
		}

		@Override
		protected void onEnd(boolean cancelled) {
			Vec3d core = core();
			SkillFx.voidSmoke(world, core, 40, 0.3);
			// Hư Không Băng Hoại: the vortex collapses and spits the devoured essence back out.
			float burst = 4.0f + Math.min(16.0f, devoured * 0.6f);
			float size = 2.0f + Math.min(2.0f, devoured * 0.08f);
			ModPackets.sendFx(world, FxData.at(FxType.ENERGY_BURST, core, 0xD24BFF, size, 12));
			ModPackets.sendFx(world, FxData.at(FxType.SHOCKWAVE_RING, core.add(0, -1.2, 0), 0x6A1FB0, 2.0f + size, 14));
			SkillFx.shell(world, GlowParticleEffect.glow(0xD24BFF, 0.6f, 14), core, 1.0, 30, 0.5);
			for (LivingEntity target : EntityUtil.inSphere(world, caster, core, 5.0)) {
				if (target.damage(ModDamageTypes.source(world, ModDamageTypes.VOID_DRAIN, caster), burst)) {
					EntityUtil.knockback(target, core, 1.2, 0.5);
				}
			}
			ModPackets.sendFx(caster, FxData.follow(FxType.VORTEX, caster.getId(), caster.getPos(), 0x6A1FB0, 0f, 0).withExtra(-1));
			world.playSound(null, caster.getBlockPos(), ModSounds.VOID_DRAIN, SoundCategory.PLAYERS, 1.0f, 1.4f);
		}
	}
}
