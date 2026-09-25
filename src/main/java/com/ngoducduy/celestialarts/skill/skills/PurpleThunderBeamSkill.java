package com.ngoducduy.celestialarts.skill.skills;

import com.ngoducduy.celestialarts.cultivation.PlayerQi;
import com.ngoducduy.celestialarts.cultivation.QiHolder;
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
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;

/**
 * Tử Tiêu Thần Lôi – a channelled beam of purple divine thunder pours from the
 * caster's palms for up to four seconds. Drains Qi every tick while held; the beam grows
 * thicker and hotter the longer it is held and forks into nearby enemies at full power.
 */
public class PurpleThunderBeamSkill extends Skill {
	public static final double RANGE = 26.0;
	private static final int MAX_DURATION = 80;
	private static final float DRAIN_PER_TICK = 1.1f;
	private static final int TIER_TICKS = 25;

	public PurpleThunderBeamSkill() {
		super(Settings.of(Element.LIGHTNING, SkillType.BEAM, Realm.NASCENT_SOUL, 20f, 240));
	}

	@Override
	public boolean isChannel() {
		return true;
	}

	@Override
	public boolean activate(SkillContext ctx) {
		ctx.qi().addActiveCast(new BeamCast(ctx.player(), this));
		return true;
	}

	private static final class BeamCast extends ActiveCast {
		BeamCast(ServerPlayerEntity caster, Skill skill) {
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

		@Override
		protected void onTick() {
			int purple = 0xB57BFF;
			if (age == 0) {
				ModPackets.sendFx(caster, FxData.follow(FxType.BEAM, caster.getId(), caster.getPos(), purple, 0.55f, MAX_DURATION).withExtra((int) RANGE));
				ModPackets.sendFx(caster, FxData.follow(FxType.MAGIC_CIRCLE, caster.getId(), caster.getPos(), purple, 2.4f, MAX_DURATION).withExtra(2));
				world.playSound(null, caster.getBlockPos(), ModSounds.THUNDER_CHARGE, SoundCategory.PLAYERS, 1.2f, 1.0f);
			}
			PlayerQi qi = QiHolder.get(caster);
			if (!caster.isCreative() && !qi.consumeQi(DRAIN_PER_TICK)) {
				finish();
				return;
			}
			caster.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 5, 3, false, false, false));

			// The longer the beam is held, the thicker and hotter it burns (three tiers).
			int tier = Math.min(2, age / TIER_TICKS);
			if (age > 0 && age % TIER_TICKS == 0 && tier > 0) {
				ModPackets.sendFx(caster, FxData.follow(FxType.BEAM, caster.getId(), caster.getPos(), purple, 0f, 0).withExtra(-1));
				ModPackets.sendFx(caster, FxData.follow(FxType.BEAM, caster.getId(), caster.getPos(), tier == 2 ? 0xD9C7FF : purple, 0.55f + tier * 0.25f, MAX_DURATION - age).withExtra((int) RANGE));
				ModPackets.sendFx(caster, FxData.at(FxType.SHOCKWAVE_RING, caster.getPos().add(0, 0.1, 0), 0xE6D6FF, 2.0f + tier, 8));
				world.playSound(null, caster.getBlockPos(), ModSounds.THUNDER_CHARGE, SoundCategory.PLAYERS, 1.0f, 1.2f + tier * 0.2f);
			}
			if (age % 8 == 0) {
				world.playSound(null, caster.getBlockPos(), ModSounds.BEAM_LOOP, SoundCategory.PLAYERS, 0.8f + tier * 0.15f, 1.0f + tier * 0.1f);
			}

			Vec3d from = caster.getEyePos().add(0, -0.2, 0);
			Vec3d dir = caster.getRotationVec(1.0f);
			Vec3d to = from.add(dir.multiply(RANGE));
			BlockHitResult hit = Targeting.raycastBlocks(caster, RANGE);
			if (hit.getType() != HitResult.Type.MISS) to = hit.getPos();

			if (age % 3 == 0) {
				SkillFx.thunderSparks(world, to, 6, 0.25);
				SkillFx.sparkBurst(world, from.add(dir.multiply(0.8)), 0xE6D6FF, 3, 0.15);
			}
			if (age % 5 == 0) {
				float dmg = 3.5f + tier * 1.5f;
				for (LivingEntity target : EntityUtil.alongLine(world, caster, from, to, 0.9 + tier * 0.25)) {
					if (target.damage(ModDamageTypes.source(world, ModDamageTypes.THUNDER, caster), dmg)) {
						target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 20, 1 + tier, false, false, true), caster);
						Vec3d c = target.getBoundingBox().getCenter();
						SkillFx.thunderSparks(world, c, 10, 0.3);
						// Small arcs jumping off the target.
						Vec3d off = SkillFx.randomUnit(world.random).multiply(1.5 + world.random.nextDouble());
						ModPackets.sendFx(world, FxData.line(FxType.LIGHTNING_BOLT, c, c.add(off), purple, 0.35f, 5));
						// At full power the lightning forks into nearby enemies.
						if (tier == 2) {
							int forks = 0;
							for (LivingEntity other : EntityUtil.inSphere(world, caster, c, 5.0)) {
								if (other == target || forks >= 2) continue;
								forks++;
								Vec3d oc = other.getBoundingBox().getCenter();
								ModPackets.sendFx(world, FxData.line(FxType.LIGHTNING_BOLT, c, oc, 0xE6D6FF, 0.45f, 6).withExtra(1));
								other.damage(ModDamageTypes.source(world, ModDamageTypes.THUNDER, caster), dmg * 0.6f);
								SkillFx.thunderSparks(world, oc, 6, 0.25);
							}
						}
					}
				}
			}
			if (age % 10 == 0 && hit.getType() != HitResult.Type.MISS) {
				ModPackets.sendFx(world, FxData.at(FxType.SHOCKWAVE_RING, to.add(0, 0.05, 0), 0xE6D6FF, 1.6f, 6).withTarget(Vec3d.of(hit.getSide().getVector())));
			}
		}

		@Override
		protected void onEnd(boolean cancelled) {
			world.playSound(null, caster.getBlockPos(), ModSounds.THUNDER_STRIKE, SoundCategory.PLAYERS, 0.6f, 1.5f);
			// Tell clients to stop the beam early (the fx checks entity + duration; a short
			// burst marks the end for the player).
			ModPackets.sendFx(caster, FxData.follow(FxType.BEAM, caster.getId(), caster.getPos(), 0xB57BFF, 0f, 0).withExtra(-1));
		}
	}
}
