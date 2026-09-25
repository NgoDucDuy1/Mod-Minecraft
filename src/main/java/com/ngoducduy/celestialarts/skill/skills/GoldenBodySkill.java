package com.ngoducduy.celestialarts.skill.skills;

import com.ngoducduy.celestialarts.cultivation.Realm;
import com.ngoducduy.celestialarts.network.FxData;
import com.ngoducduy.celestialarts.network.FxType;
import com.ngoducduy.celestialarts.network.ModPackets;
import com.ngoducduy.celestialarts.registry.ModParticles;
import com.ngoducduy.celestialarts.registry.ModSounds;
import com.ngoducduy.celestialarts.skill.Element;
import com.ngoducduy.celestialarts.skill.Skill;
import com.ngoducduy.celestialarts.skill.SkillContext;
import com.ngoducduy.celestialarts.skill.SkillType;
import com.ngoducduy.celestialarts.skill.cast.ShieldCast;
import com.ngoducduy.celestialarts.util.SkillFx;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.Vec3d;

/**
 * Đại Nhật Kim Thân – the Great Sun Golden Body. For ten seconds the cultivator's skin turns
 * to burnished gold: half of every blow is shrugged off and thrown back at the attacker,
 * fire cannot touch them, their strikes hit harder, and a sun-like aura burns around them.
 */
public class GoldenBodySkill extends Skill {
	private static final int DURATION = 200;
	private static final int GOLD = 0xFFD36B;

	public GoldenBodySkill() {
		super(Settings.of(Element.DAO, SkillType.BUFF, Realm.NASCENT_SOUL, 50f, 700));
	}

	@Override
	public boolean activate(SkillContext ctx) {
		float pool = 60f + ctx.qi().getRealm().getLevel() * 10f;
		ctx.qi().addActiveCast(new GoldenBodyCast(ctx.player(), this, pool));
		return true;
	}

	private static final class GoldenBodyCast extends ShieldCast {
		GoldenBodyCast(ServerPlayerEntity caster, Skill skill, float pool) {
			// Takes half of each hit (up to the pool), reflects 60% of what it takes; no hex shell.
			super(caster, skill, DURATION, pool, 0.6f, GOLD, 0.5f, false);
		}

		@Override
		protected void onTick() {
			super.onTick();
			if (age == 0) {
				ModPackets.sendFx(caster, FxData.follow(FxType.QI_AURA, caster.getId(), caster.getPos(), GOLD, 1.1f, DURATION));
				ModPackets.sendFx(caster, FxData.follow(FxType.RUNE_ORBIT, caster.getId(), caster.getPos(), 0xFFE9A8, 1.6f, DURATION));
				ModPackets.sendFx(caster, FxData.follow(FxType.MAGIC_CIRCLE, caster.getId(), caster.getPos(), GOLD, 2.4f, 30).withExtra(1));
				ModPackets.sendFx(caster, FxData.at(FxType.ENERGY_BURST, caster.getPos().add(0, 1, 0), 0xFFFFFF, 1.8f, 10));
				caster.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, DURATION, 0, false, false, true));
				caster.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, DURATION, 0, false, false, true));
				caster.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, DURATION, 0, false, false, true));
				caster.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, DURATION, 0, false, false, false));
				SkillFx.goldenLight(world, caster.getPos().add(0, 1, 0), 40, 1.4);
				world.playSound(null, caster.getBlockPos(), ModSounds.FORMATION, SoundCategory.PLAYERS, 1.2f, 0.9f);
				world.playSound(null, caster.getBlockPos(), ModSounds.BREAKTHROUGH, SoundCategory.PLAYERS, 0.5f, 1.5f);
			}
			if (age % 4 == 0) {
				Vec3d p = caster.getPos().add(world.random.nextGaussian() * 0.5, world.random.nextDouble() * 1.8, world.random.nextGaussian() * 0.5);
				SkillFx.single(world, ModParticles.GOLDEN_LIGHT, p, new Vec3d(0, 0.05, 0));
			}
			if (age % 40 == 20) {
				ModPackets.sendFx(caster, FxData.at(FxType.SHOCKWAVE_RING, caster.getPos().add(0, 0.1, 0), GOLD, 2.0f, 8));
			}
		}

		/** The golden body cannot be detonated early like the tortoise shell. */
		@Override
		public void onRelease() {
		}

		@Override
		protected void onEnd(boolean cancelled) {
			caster.removeStatusEffect(StatusEffects.GLOWING);
			SkillFx.goldenLight(world, caster.getPos().add(0, 1, 0), 20, 1.0);
			world.playSound(null, caster.getBlockPos(), ModSounds.FORMATION, SoundCategory.PLAYERS, 0.7f, 1.4f);
		}
	}
}
