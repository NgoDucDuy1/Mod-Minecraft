package com.ngoducduy.celestialarts.skill.skills;

import com.ngoducduy.celestialarts.cultivation.Realm;
import com.ngoducduy.celestialarts.network.FxData;
import com.ngoducduy.celestialarts.network.FxType;
import com.ngoducduy.celestialarts.network.ModPackets;
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
 * Phong Nhận Loạn Vũ – three crescent wind blades whirl around the cultivator for
 * eight seconds, slicing anything that comes close and quickening the caster's steps.
 * When the dance ends (or the key is pressed again) the blades scatter outward.
 */
public class WindBladeDanceSkill extends Skill {
	private static final int DURATION = 160;
	private static final double RADIUS = 3.3;
	private static final double FINALE_RANGE = 9.0;

	public WindBladeDanceSkill() {
		super(Settings.of(Element.WIND, SkillType.BUFF, Realm.FOUNDATION, 26f, 300));
	}

	@Override
	public boolean activate(SkillContext ctx) {
		ctx.qi().addActiveCast(new DanceCast(ctx.player(), this));
		return true;
	}

	private static final class DanceCast extends ActiveCast {
		DanceCast(ServerPlayerEntity caster, Skill skill) {
			super(caster, skill, DURATION);
		}

		@Override
		protected void onTick() {
			if (age == 0) {
				ModPackets.sendFx(caster, FxData.follow(FxType.WIND_BLADES, caster.getId(), caster.getPos(), 0xB8FFD9, (float) RADIUS, DURATION));
				caster.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, DURATION, 1, false, false, true));
				world.playSound(null, caster.getBlockPos(), ModSounds.WIND_SLASH, SoundCategory.PLAYERS, 1.0f, 1.0f);
				SkillFx.glowRing(world, caster.getPos().add(0, 0.2, 0), 1.5, 0xB8FFD9, 24, 0.15);
			}
			if (age % 3 == 0) {
				// Wind streaks following the blades.
				for (int b = 0; b < 3; b++) {
					double a = Math.toRadians(age * 14.0 + b * 120.0);
					Vec3d p = caster.getPos().add(Math.cos(a) * RADIUS * 0.85, 0.9 + Math.sin(age * 0.2 + b) * 0.3, Math.sin(a) * RADIUS * 0.85);
					Vec3d v = new Vec3d(-Math.sin(a), 0, Math.cos(a)).multiply(0.25);
					SkillFx.single(world, ModParticles.WIND_STREAK, p, v);
				}
			}
			if (age % 8 == 4) {
				for (LivingEntity target : EntityUtil.inCylinder(world, caster, caster.getPos(), RADIUS, 2.5)) {
					if (target.damage(ModDamageTypes.source(world, ModDamageTypes.WIND, caster), 3.0f)) {
						EntityUtil.knockback(target, caster.getPos(), 0.25, 0.05);
						SkillFx.windGust(world, target.getBoundingBox().getCenter(), target.getPos().subtract(caster.getPos()), 6, 0.3);
						world.playSound(null, target.getBlockPos(), ModSounds.WIND_SLASH, SoundCategory.PLAYERS, 0.6f, 1.3f + world.random.nextFloat() * 0.3f);
					}
				}
			}
			if (age % 40 == 0 && age > 0) {
				world.playSound(null, caster.getBlockPos(), ModSounds.WIND_SLASH, SoundCategory.PLAYERS, 0.5f, 0.8f);
			}
		}

		/** Pressing the key again hurls the blades outward at once. */
		@Override
		public void onRelease() {
			if (age > 10) finish();
		}

		@Override
		protected void onEnd(boolean cancelled) {
			SkillFx.windGust(world, caster.getPos().add(0, 1, 0), new Vec3d(0, 1, 0), 20, 0.4);
			if (cancelled) return;
			// Phong Nhận Tán: the three blades fly outward in a widening spiral, cutting everything on their lines.
			Vec3d origin = caster.getPos().add(0, 1.0, 0);
			double base = Math.toRadians(age * 14.0);
			for (int b = 0; b < 3; b++) {
				double a = base + b * Math.PI * 2 / 3;
				Vec3d dir = new Vec3d(Math.cos(a), 0, Math.sin(a));
				Vec3d end = origin.add(dir.multiply(FINALE_RANGE));
				ModPackets.sendFx(world, FxData.line(FxType.LIGHTNING_BOLT, origin.add(dir.multiply(RADIUS)), end, 0xE8FFF3, 0.3f, 6).withExtra(0));
				ModPackets.sendFx(world, FxData.at(FxType.SLASH_ARC, origin.add(dir.multiply(RADIUS + 1.5)), 0xB8FFD9, 1.4f, 8).withExtra(0).withTarget(dir));
				SkillFx.cone(world, ModParticles.WIND_STREAK, origin.add(dir.multiply(RADIUS)), dir, 12, 14, 0.7);
				for (LivingEntity target : EntityUtil.alongLine(world, caster, origin, end, 1.1)) {
					if (target.damage(ModDamageTypes.source(world, ModDamageTypes.WIND, caster), 7.0f)) {
						EntityUtil.knockback(target, caster.getPos(), 0.6, 0.25);
						SkillFx.windGust(world, target.getBoundingBox().getCenter(), dir, 8, 0.35);
					}
				}
			}
			ModPackets.sendFx(world, FxData.at(FxType.SHOCKWAVE_RING, caster.getPos().add(0, 0.1, 0), 0xB8FFD9, (float) FINALE_RANGE, 12));
			world.playSound(null, caster.getBlockPos(), ModSounds.WIND_SLASH, SoundCategory.PLAYERS, 1.4f, 0.7f);
		}
	}
}
