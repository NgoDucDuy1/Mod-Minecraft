package com.ngoducduy.celestialarts.skill.skills;

import com.ngoducduy.celestialarts.cultivation.Realm;
import com.ngoducduy.celestialarts.network.FxData;
import com.ngoducduy.celestialarts.network.FxType;
import com.ngoducduy.celestialarts.network.ModPackets;
import com.ngoducduy.celestialarts.registry.ModDamageTypes;
import com.ngoducduy.celestialarts.registry.ModSounds;
import com.ngoducduy.celestialarts.skill.Element;
import com.ngoducduy.celestialarts.skill.Skill;
import com.ngoducduy.celestialarts.skill.SkillContext;
import com.ngoducduy.celestialarts.skill.SkillType;
import com.ngoducduy.celestialarts.skill.cast.ActiveCast;
import com.ngoducduy.celestialarts.util.EntityUtil;
import com.ngoducduy.celestialarts.util.SkillFx;
import com.ngoducduy.celestialarts.util.Targeting;
import java.util.Map;
import java.util.List;
import java.util.HashMap;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;

/**
 * Cửu Thiên Lôi Kiếp – calls a tribulation cloud over the target area. Nine bolts of
 * heavenly lightning fall in sequence; the ninth is the largest. Bolts seek enemies inside
 * the array and brand them; every third brand detonates.
 */
public class NineTribulationsSkill extends Skill {
	private static final double TARGET_RANGE = 32.0;
	private static final double AREA_RADIUS = 5.5;
	private static final int DELAY = 25;
	private static final int INTERVAL = 9;
	private static final int STRIKES = 9;
	private static final double CLOUD_HEIGHT = 13.0;

	public NineTribulationsSkill() {
		super(Settings.of(Element.LIGHTNING, SkillType.AREA, Realm.SPIRIT_TRANSFORMATION, 80f, 800));
	}

	@Override
	public boolean activate(SkillContext ctx) {
		ServerPlayerEntity player = ctx.player();
		Vec3d target = Targeting.groundPoint(player, TARGET_RANGE);
		player.swingHand(Hand.MAIN_HAND, true);
		ctx.qi().addActiveCast(new TribulationCast(player, this, target));
		return true;
	}

	private static final class TribulationCast extends ActiveCast {
		private final Vec3d center;
		private final Map<Integer, Integer> brands = new HashMap<>();
		private int strikes;

		TribulationCast(ServerPlayerEntity caster, Skill skill, Vec3d center) {
			super(caster, skill, DELAY + INTERVAL * STRIKES + 20);
			this.center = center;
		}

		@Override
		protected void onTick() {
			int purple = 0xB57BFF;
			if (age == 0) {
				ModPackets.sendFx(world, FxData.at(FxType.TRIBULATION_CLOUD, center.add(0, CLOUD_HEIGHT, 0), 0x5A3FA8, (float) AREA_RADIUS * 1.4f, duration));
				ModPackets.sendFx(world, FxData.at(FxType.MAGIC_CIRCLE, center.add(0, 0.06, 0), purple, (float) AREA_RADIUS, duration).withExtra(2));
				world.playSound(null, caster.getBlockPos(), ModSounds.THUNDER_CHARGE, SoundCategory.PLAYERS, 2.0f, 0.7f);
				world.playSound(null, net.minecraft.util.math.BlockPos.ofFloored(center), SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.WEATHER, 1.5f, 0.6f);
				SkillFx.runes(world, center.add(0, 0.3, 0), 20, AREA_RADIUS * 0.8);
			}
			if (age >= DELAY && strikes < STRIKES && (age - DELAY) % INTERVAL == 0) {
				strikes++;
				boolean last = strikes == STRIKES;
				Vec3d hit;
				if (strikes == 1 || last) {
					hit = center;
				} else {
					// Heaven's lightning seeks the guilty: aim at an enemy inside the array when there is one.
					List<LivingEntity> inside = EntityUtil.inCylinder(world, caster, center, AREA_RADIUS, 4.0);
					if (!inside.isEmpty() && world.random.nextInt(4) != 0) {
						LivingEntity chosen = inside.get(world.random.nextInt(inside.size()));
						hit = Targeting.snapToGround(world, chosen.getPos().add(0, 1.0, 0), 6);
					} else {
						double a = world.random.nextDouble() * Math.PI * 2;
						double r = Math.sqrt(world.random.nextDouble()) * (AREA_RADIUS - 0.8);
						hit = Targeting.snapToGround(world, center.add(Math.cos(a) * r, 1.0, Math.sin(a) * r), 6);
					}
				}
				strike(hit, last, purple);
			}
		}

		private void strike(Vec3d hit, boolean last, int color) {
			Vec3d from = hit.add(world.random.nextGaussian() * 1.5, CLOUD_HEIGHT - 1.0, world.random.nextGaussian() * 1.5);
			float width = last ? 2.2f : 1.1f;
			double radius = last ? 4.5 : 2.6;
			float damage = last ? 26f : 11f;

			ModPackets.sendFx(world, FxData.line(FxType.LIGHTNING_BOLT, from, hit, color, width, last ? 14 : 9).withExtra(last ? 3 : 2));
			ModPackets.sendFx(world, FxData.at(FxType.ENERGY_BURST, hit.add(0, 0.6, 0), 0xE6D6FF, last ? 3.0f : 1.4f, 8));
			ModPackets.sendFx(world, FxData.at(FxType.SHOCKWAVE_RING, hit.add(0, 0.1, 0), color, (float) radius * 1.6f, 12));
			SkillFx.thunderSparks(world, hit.add(0, 0.5, 0), last ? 60 : 24, last ? 0.8 : 0.45);
			SkillFx.rockDebris(world, hit, last ? 20 : 6, 0.5);
			world.playSound(null, net.minecraft.util.math.BlockPos.ofFloored(hit), ModSounds.THUNDER_STRIKE, SoundCategory.PLAYERS, last ? 3.0f : 1.8f, last ? 0.7f : 0.9f + world.random.nextFloat() * 0.3f);
			ModPackets.sendCameraShake(world, hit, 40.0, last ? 1.4f : 0.6f, last ? 16 : 7);

			for (LivingEntity target : EntityUtil.inCylinder(world, caster, hit, radius, 4.0)) {
				// Lôi Ấn: every strike brands the target; the third brand detonates.
				int brand = brands.merge(target.getId(), 1, Integer::sum);
				float dmg = damage;
				if (brand % 3 == 0) {
					dmg += 8f;
					ModPackets.sendFx(world, FxData.at(FxType.ENERGY_BURST, target.getBoundingBox().getCenter(), 0xFFFFFF, 1.6f, 8));
					SkillFx.sparkBurst(world, target.getBoundingBox().getCenter(), 0xE6D6FF, 20, 0.5);
				}
				if (target.damage(ModDamageTypes.source(world, ModDamageTypes.THUNDER, caster), dmg)) {
					target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 30, brand % 3 == 0 ? 4 : 2, false, false, true), caster);
					if (last) EntityUtil.knockback(target, hit, 1.0, 0.6);
					SkillFx.thunderSparks(world, target.getBoundingBox().getCenter(), 12, 0.3);
					if (brand % 3 != 0) ModPackets.sendFx(world, FxData.follow(FxType.RUNE_ORBIT, target.getId(), target.getPos(), color, 0.8f, INTERVAL * 3));
				}
			}
		}
	}
}
