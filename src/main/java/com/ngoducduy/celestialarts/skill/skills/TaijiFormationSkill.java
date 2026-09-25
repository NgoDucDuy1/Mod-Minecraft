package com.ngoducduy.celestialarts.skill.skills;

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
import com.ngoducduy.celestialarts.util.Targeting;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

/**
 * Thái Cực Trận – inscribes a rotating Taiji formation on the ground. Inside it,
 * allies are mended and cleansed every pulse while enemies are suppressed and hurt.
 */
public class TaijiFormationSkill extends Skill {
	private static final int DURATION = 260;
	private static final double RADIUS = 5.5;

	public TaijiFormationSkill() {
		super(Settings.of(Element.DAO, SkillType.FORMATION, Realm.GOLDEN_CORE, 40f, 600));
	}

	@Override
	public boolean activate(SkillContext ctx) {
		Vec3d center = Targeting.snapToGround(ctx.world(), ctx.player().getPos().add(0, 0.5, 0), 4);
		ctx.qi().addActiveCast(new FormationCast(ctx.player(), this, center));
		return true;
	}

	private static final class FormationCast extends ActiveCast {
		private final Vec3d center;

		FormationCast(ServerPlayerEntity caster, Skill skill, Vec3d center) {
			super(caster, skill, DURATION);
			this.center = center;
		}

		@Override
		protected void onTick() {
			if (age == 0) {
				ModPackets.sendFx(world, FxData.at(FxType.MAGIC_CIRCLE, center.add(0, 0.06, 0), 0xFFE9A8, (float) RADIUS, DURATION).withExtra(0));
				ModPackets.sendFx(world, FxData.at(FxType.RUNE_ORBIT, center.add(0, 1.2, 0), 0xFFE9A8, (float) RADIUS * 0.8f, DURATION));
				world.playSound(null, caster.getBlockPos(), ModSounds.FORMATION, SoundCategory.PLAYERS, 1.2f, 1.0f);
				SkillFx.glowRing(world, center.add(0, 0.2, 0), RADIUS, 0xFFE9A8, 48, 0.0);
			}
			if (age % 5 == 0) {
				// Slow rising motes across the formation.
				double a = world.random.nextDouble() * Math.PI * 2;
				double r = Math.sqrt(world.random.nextDouble()) * RADIUS;
				SkillFx.single(world, ModParticles.GOLDEN_LIGHT, center.add(Math.cos(a) * r, 0.2, Math.sin(a) * r), new Vec3d(0, 0.04, 0));
			}
			if (age % 30 == 15) {
				pulse();
			}
		}

		private void pulse() {
			ModPackets.sendFx(world, FxData.at(FxType.SHOCKWAVE_RING, center.add(0, 0.12, 0), 0xFFFFFF, (float) RADIUS, 14));
			world.playSound(null, caster.getBlockPos(), ModSounds.FORMATION, SoundCategory.PLAYERS, 0.6f, 1.6f);

			Box box = new Box(center.x - RADIUS, center.y - 1, center.z - RADIUS, center.x + RADIUS, center.y + 4, center.z + RADIUS);
			for (LivingEntity e : world.getEntitiesByClass(LivingEntity.class, box, le -> le.isAlive())) {
				double dx = e.getX() - center.x;
				double dz = e.getZ() - center.z;
				if (dx * dx + dz * dz > RADIUS * RADIUS) continue;
				if (isAlly(e)) {
					e.heal(2.5f);
					e.removeStatusEffect(StatusEffects.POISON);
					e.removeStatusEffect(StatusEffects.WITHER);
					e.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 40, 0, false, false, true));
					SkillFx.burst(world, GlowParticleEffect.glow(0xFFF3C4, 0.5f, 16), e.getBoundingBox().getCenter(), 6, 0.4, 0.03);
				} else if (EntityUtil.isValidTarget(caster, e)) {
					if (e.damage(ModDamageTypes.source(world, ModDamageTypes.HEAVEN, caster), 4.0f)) {
						e.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 40, 1, false, false, true), caster);
						e.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 40, 0, false, false, true), caster);
						SkillFx.burst(world, GlowParticleEffect.glow(0xFFFFFF, 0.6f, 12), e.getBoundingBox().getCenter(), 8, 0.4, 0.06);
					}
				}
			}
		}

		private boolean isAlly(LivingEntity e) {
			if (e == caster) return true;
			if (e instanceof PlayerEntity p) return !caster.shouldDamagePlayer(p) || caster.isTeammate(p);
			if (e instanceof TameableEntity t) return t.isTamed();
			return caster.isTeammate(e);
		}

		@Override
		protected void onEnd(boolean cancelled) {
			SkillFx.glowRing(world, center.add(0, 0.2, 0), RADIUS, 0xFFE9A8, 40, -0.15);
		}
	}
}
