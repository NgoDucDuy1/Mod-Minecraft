package com.ngoducduy.celestialarts.skill.skills;

import com.ngoducduy.celestialarts.cultivation.Realm;
import com.ngoducduy.celestialarts.network.FxData;
import com.ngoducduy.celestialarts.network.FxType;
import com.ngoducduy.celestialarts.network.ModPackets;
import com.ngoducduy.celestialarts.registry.ModDamageTypes;
import com.ngoducduy.celestialarts.registry.ModEffects;
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
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

/**
 * Băng Phong Vạn Lý – the caster slams a palm down; frost races outward, ice spikes
 * burst from the ground and a crystal dome seals the area. Everything caught inside
 * is frozen solid, then shattered when the dome collapses.
 */
public class FrozenDomainSkill extends Skill {
	private static final double RADIUS = 7.0;
	private static final int DURATION = 90;

	public FrozenDomainSkill() {
		super(Settings.of(Element.ICE, SkillType.AREA, Realm.GOLDEN_CORE, 50f, 500));
	}

	@Override
	public boolean canUseWhileRiding() {
		return false;
	}

	@Override
	public boolean activate(SkillContext ctx) {
		ctx.player().swingHand(Hand.OFF_HAND, true);
		ctx.qi().addActiveCast(new DomainCast(ctx.player(), this, ctx.player().getPos()));
		return true;
	}

	private static final class DomainCast extends ActiveCast {
		private final Vec3d center;
		private final List<Integer> frozen = new ArrayList<>();

		DomainCast(ServerPlayerEntity caster, Skill skill, Vec3d center) {
			super(caster, skill, DURATION);
			this.center = center;
		}

		@Override
		protected void onTick() {
			int ice = 0x9BE4FF;
			if (age == 0) {
				ModPackets.sendFx(world, FxData.at(FxType.MAGIC_CIRCLE, center.add(0, 0.06, 0), ice, (float) RADIUS, DURATION).withExtra(3));
				ModPackets.sendFx(world, FxData.at(FxType.SHOCKWAVE_RING, center.add(0, 0.1, 0), 0xE8FBFF, (float) RADIUS * 1.1f, 14));
				world.playSound(null, caster.getBlockPos(), ModSounds.ICE_CAST, SoundCategory.PLAYERS, 1.5f, 0.7f);
				SkillFx.frostBurst(world, center.add(0, 0.5, 0), 40, 0.3);
			}
			if (age == 6) {
				ModPackets.sendFx(world, FxData.at(FxType.ICE_SPIKES, center, ice, (float) RADIUS, DURATION - 6));
				ModPackets.sendFx(world, FxData.at(FxType.FROST_DOME, center, ice, (float) RADIUS, DURATION - 6));
				world.playSound(null, caster.getBlockPos(), ModSounds.ICE_SHATTER, SoundCategory.PLAYERS, 1.2f, 0.6f);
				ModPackets.sendCameraShake(world, center, 24.0, 0.5f, 8);
				SkillFx.ring(world, ModParticles.ICE_CRYSTAL, center.add(0, 0.3, 0), RADIUS * 0.9, 40, 0.05, 0.25);

				for (LivingEntity target : EntityUtil.inCylinder(world, caster, center, RADIUS, 4.0)) {
					if (target.damage(ModDamageTypes.source(world, ModDamageTypes.FROST, caster), 8f)) {
						target.addStatusEffect(new StatusEffectInstance(ModEffects.FROZEN, DURATION, 0, false, false, true), caster);
						frozen.add(target.getId());
						SkillFx.frostBurst(world, target.getBoundingBox().getCenter(), 20, 0.15);
					}
				}
			}
			if (age == 6) {
				// Băng Tâm: inside her own domain the caster is untouched by the cold.
				caster.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, DURATION, 0, false, false, true));
				caster.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, DURATION, 0, false, false, true));
			}
			if (age > 6 && age % 20 == 10) {
				// Anything that walks into the domain afterwards is caught by the cold as well.
				for (LivingEntity target : EntityUtil.inCylinder(world, caster, center, RADIUS, 4.0)) {
					if (target.hasStatusEffect(ModEffects.FROZEN)) continue;
					if (target.damage(ModDamageTypes.source(world, ModDamageTypes.FROST, caster), 3f)) {
						target.addStatusEffect(new StatusEffectInstance(ModEffects.FROZEN, 40, 0, false, false, true), caster);
						if (!frozen.contains(target.getId())) frozen.add(target.getId());
						SkillFx.frostBurst(world, target.getBoundingBox().getCenter(), 12, 0.12);
						world.playSound(null, target.getBlockPos(), ModSounds.ICE_CAST, SoundCategory.PLAYERS, 0.7f, 1.4f);
					}
				}
			}
			if (age > 6 && age % 4 == 0) {
				double a = world.random.nextDouble() * Math.PI * 2;
				double r = Math.sqrt(world.random.nextDouble()) * RADIUS;
				SkillFx.single(world, ModParticles.SNOWFLAKE, center.add(Math.cos(a) * r, 3.5, Math.sin(a) * r), new Vec3d(0, -0.03, 0));
				SkillFx.single(world, ModParticles.FROST_MIST, center.add(Math.cos(a) * r, 0.3, Math.sin(a) * r), new Vec3d(0, 0.01, 0));
			}
		}

		@Override
		protected void onEnd(boolean cancelled) {
			// Shatter: still frozen targets take burst damage.
			for (int id : frozen) {
				if (world.getEntityById(id) instanceof LivingEntity target && target.isAlive() && target.hasStatusEffect(ModEffects.FROZEN)) {
					target.removeStatusEffect(ModEffects.FROZEN);
					target.damage(ModDamageTypes.source(world, ModDamageTypes.FROST, caster), 10f);
					SkillFx.frostBurst(world, target.getBoundingBox().getCenter(), 30, 0.3);
				}
			}
			ModPackets.sendFx(world, FxData.at(FxType.SHOCKWAVE_RING, center.add(0, 0.1, 0), 0xE8FBFF, (float) RADIUS, 12));
			SkillFx.shell(world, ModParticles.ICE_CRYSTAL, center.add(0, 2, 0), RADIUS * 0.6, 60, 0.3);
			world.playSound(null, caster.getBlockPos(), ModSounds.ICE_SHATTER, SoundCategory.PLAYERS, 1.6f, 0.8f);
		}
	}
}
