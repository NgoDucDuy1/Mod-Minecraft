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
import com.ngoducduy.celestialarts.util.Targeting;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;

/**
 * Liệt Diễm Trảo – three rapid claw strikes of spirit fire in a cone in front of
 * the caster. Each strike is a burning crescent; hit targets keep burning, and the
 * final claw rakes the ground, leaving three furrows of spirit fire for three seconds.
 */
public class FlameClawSkill extends Skill {
	private static final double RANGE = 4.2;
	private static final double HALF_ANGLE = 45;
	private static final float DAMAGE = 6.5f;
	private static final int EMBER_TICKS = 60;
	private static final double EMBER_REACH = 4.0;
	private static final float EMBER_DAMAGE = 1.5f;

	public FlameClawSkill() {
		super(Settings.of(Element.FIRE, SkillType.MELEE, Realm.QI_REFINING, 15f, 70));
	}

	@Override
	public boolean activate(SkillContext ctx) {
		ctx.qi().addActiveCast(new ClawCast(ctx.player(), this));
		return true;
	}

	private static final class ClawCast extends ActiveCast {
		/** Where the third claw scorched the ground; the embers keep burning there. */
		private Vec3d emberCenter;
		private Vec3d emberDir;

		ClawCast(ServerPlayerEntity caster, Skill skill) {
			super(caster, skill, 13 + EMBER_TICKS);
		}

		@Override
		protected void onTick() {
			if (age == 0 || age == 5 || age == 10) {
				int strike = age / 5;
				strike(strike);
			}
			if (age == 12) igniteGround();
			if (emberCenter != null && age > 12) tickEmbers();
		}

		private void igniteGround() {
			Vec3d dir = caster.getRotationVec(1.0f);
			Vec3d flat = new Vec3d(dir.x, 0, dir.z);
			if (flat.lengthSquared() < 1.0E-4) return;
			emberDir = flat.normalize();
			Vec3d ahead = caster.getPos().add(emberDir.multiply(EMBER_REACH * 0.5));
			emberCenter = Targeting.snapToGround(world, ahead.add(0, 1.0, 0), 4);
			Vec3d[] basis = SkillFx.basis(emberDir);
			// Three claw furrows of fire; the middle one longest.
			for (int i = -1; i <= 1; i++) {
				Vec3d off = basis[0].multiply(i * 0.8);
				Vec3d a = caster.getPos().add(off).add(emberDir.multiply(0.8));
				Vec3d b = a.add(emberDir.multiply(EMBER_REACH - Math.abs(i) * 0.7));
				ModPackets.sendFx(world, FxData.line(FxType.GROUND_CRACK, a.add(0, 0.05, 0), b, 0xFF7A1A, 0.45f, EMBER_TICKS + 10));
			}
			ModPackets.sendFx(world, FxData.at(FxType.FLAME_PILLAR, emberCenter.add(0, -0.4, 0), 0xFF7A1A, 0.55f, EMBER_TICKS));
			ModPackets.sendFx(world, FxData.at(FxType.GROUND_DECAL, emberCenter.add(0, 0.03, 0), 0xFF7A1A, 1.6f, 360));
			world.playSound(null, caster.getBlockPos(), ModSounds.FIRE_EXPLOSION, SoundCategory.PLAYERS, 0.6f, 1.4f);
		}

		private void tickEmbers() {
			int t = age - 12;
			if (t % 3 == 0) {
				double along = world.random.nextDouble() * EMBER_REACH;
				double side = (world.random.nextDouble() - 0.5) * 1.8;
				Vec3d[] basis = SkillFx.basis(emberDir);
				Vec3d p = caster.getPos().add(emberDir.multiply(0.8 + along)).add(basis[0].multiply(side));
				p = new Vec3d(p.x, emberCenter.y + 0.1, p.z);
				SkillFx.single(world, t % 2 == 0 ? ModParticles.FLAME_WISP : ModParticles.EMBER, p, new Vec3d(0, 0.05 + world.random.nextDouble() * 0.05, 0));
			}
			if (t % 10 == 5) {
				for (LivingEntity target : EntityUtil.inCone(world, caster, emberCenter.subtract(emberDir.multiply(EMBER_REACH * 0.5)).add(0, 0.5, 0), emberDir, EMBER_REACH, 28)) {
					if (target.getY() > emberCenter.y + 2.2 || target.getY() < emberCenter.y - 1.5) continue;
					if (target.damage(ModDamageTypes.source(world, ModDamageTypes.FLAME, caster), EMBER_DAMAGE)) {
						target.setOnFireFor(3);
						SkillFx.flameBurst(world, target.getPos().add(0, 0.3, 0), 4, 0.1);
					}
				}
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
