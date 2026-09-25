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

/**
 * Liệt Diễm Trảo – three rapid claw strikes of spirit fire in a cone in front of
 * the caster. Each strike is a burning crescent; hit targets keep burning.
 */
public class FlameClawSkill extends Skill {
	private static final double RANGE = 4.2;
	private static final double HALF_ANGLE = 45;
	private static final float DAMAGE = 6.5f;

	public FlameClawSkill() {
		super(Settings.of(Element.FIRE, SkillType.MELEE, Realm.QI_REFINING, 15f, 70));
	}

	@Override
	public boolean activate(SkillContext ctx) {
		ctx.qi().addActiveCast(new ClawCast(ctx.player(), this));
		return true;
	}

	private static final class ClawCast extends ActiveCast {
		ClawCast(ServerPlayerEntity caster, Skill skill) {
			super(caster, skill, 13);
		}

		@Override
		protected void onTick() {
			if (age == 0 || age == 5 || age == 10) {
				int strike = age / 5;
				strike(strike);
			}
		}

		private void strike(int index) {
			Vec3d origin = caster.getEyePos().add(0, -0.3, 0);
			Vec3d dir = caster.getRotationVec(1.0f);
			int style = index == 0 ? 1 : (index == 1 ? 2 : 0);

			caster.swingHand(index % 2 == 0 ? Hand.MAIN_HAND : Hand.OFF_HAND, true);
			ModPackets.sendFx(caster, FxData.follow(FxType.SLASH_ARC, caster.getId(), caster.getPos(), 0xFF7A1A, 1.5f, 7).withExtra(style));
			SkillFx.cone(world, ModParticles.FLAME_WISP, origin.add(dir.multiply(0.8)), dir, 30, 18, 0.45);
			SkillFx.cone(world, ModParticles.EMBER, origin.add(dir.multiply(0.8)), dir, 35, 10, 0.6);
			world.playSound(null, caster.getBlockPos(), ModSounds.FIRE_WHOOSH, SoundCategory.PLAYERS, 1.0f, 1.1f + index * 0.1f);

			for (LivingEntity target : EntityUtil.inCone(world, caster, origin, dir, RANGE, HALF_ANGLE)) {
				float dmg = DAMAGE + (index == 2 ? 3f : 0f);
				if (target.damage(ModDamageTypes.source(world, ModDamageTypes.FLAME, caster), dmg)) {
					target.setOnFireFor(4);
					target.addStatusEffect(new StatusEffectInstance(ModEffects.QI_BURN, 60, 0, false, true, true), caster);
					if (index == 2) EntityUtil.knockback(target, caster.getPos(), 0.8, 0.35);
					SkillFx.flameBurst(world, target.getBoundingBox().getCenter(), 8, 0.15);
				}
			}
		}
	}
}
