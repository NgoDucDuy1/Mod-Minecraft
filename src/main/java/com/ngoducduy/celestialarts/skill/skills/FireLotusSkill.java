package com.ngoducduy.celestialarts.skill.skills;

import com.ngoducduy.celestialarts.cultivation.Realm;
import com.ngoducduy.celestialarts.entity.FireLotusEntity;
import com.ngoducduy.celestialarts.network.FxData;
import com.ngoducduy.celestialarts.network.FxType;
import com.ngoducduy.celestialarts.network.ModPackets;
import com.ngoducduy.celestialarts.registry.ModEntities;
import com.ngoducduy.celestialarts.registry.ModSounds;
import com.ngoducduy.celestialarts.skill.Element;
import com.ngoducduy.celestialarts.skill.Skill;
import com.ngoducduy.celestialarts.skill.SkillContext;
import com.ngoducduy.celestialarts.skill.SkillType;
import com.ngoducduy.celestialarts.skill.cast.ActiveCast;
import com.ngoducduy.celestialarts.skill.cast.ZoneCast;
import com.ngoducduy.celestialarts.skill.SkillRegistry;
import com.ngoducduy.celestialarts.cultivation.QiHolder;
import com.ngoducduy.celestialarts.registry.ModDamageTypes;
import com.ngoducduy.celestialarts.registry.ModEffects;
import com.ngoducduy.celestialarts.registry.ModParticles;
import com.ngoducduy.celestialarts.util.Targeting;
import net.minecraft.entity.LivingEntity;
import java.util.List;
import com.ngoducduy.celestialarts.util.SkillFx;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.Vec3d;

/**
 * Phật Nộ Hỏa Liên – the caster gathers different flames into a lotus in the palm.
 * The lotus blooms over ~2 seconds and is then hurled forward, detonating on impact and
 * leaving a burning sea of fire behind.
 */
public class FireLotusSkill extends Skill {
	public FireLotusSkill() {
		super(Settings.of(Element.FIRE, SkillType.PROJECTILE, Realm.GOLDEN_CORE, 45f, 360));
	}

	@Override
	public boolean activate(SkillContext ctx) {
		ServerPlayerEntity player = ctx.player();
		Vec3d palm = FireLotusEntity.palmPos(player);

		FireLotusEntity lotus = new FireLotusEntity(ModEntities.FIRE_LOTUS, ctx.world());
		lotus.setOwner(player);
		lotus.setPower(1.0f + (ctx.qi().getRealm().getLevel() - Realm.GOLDEN_CORE.getLevel()) * 0.12f);
		lotus.refreshPositionAndAngles(palm.x, palm.y, palm.z, player.getYaw(), 0);
		ctx.world().spawnEntity(lotus);

		ModPackets.sendFx(player, FxData.follow(FxType.MAGIC_CIRCLE, player.getId(), player.getPos(), 0xFF7A1A, 2.2f, FireLotusEntity.CHARGE_TICKS).withExtra(1));
		ModPackets.sendFx(player, FxData.follow(FxType.QI_AURA, player.getId(), player.getPos(), 0xFF9A3C, 0.8f, FireLotusEntity.CHARGE_TICKS));
		ctx.world().playSound(null, player.getBlockPos(), ModSounds.QI_GATHER, SoundCategory.PLAYERS, 1.0f, 1.2f);
		ctx.qi().addActiveCast(new ChargeCast(player, this));
		return true;
	}

	/** Called by the lotus on detonation: the ground keeps burning for a few seconds (Hỏa Hải). */
	public static void igniteSea(ServerPlayerEntity owner, Vec3d center, double radius) {
		Vec3d ground = Targeting.snapToGround(owner.getServerWorld(), center.add(0, 1.0, 0), 6);
		QiHolder.get(owner).addActiveCast(new FireSeaZone(owner, ground, radius));
		// Scorched earth stays long after the flames die.
		ModPackets.sendFx(owner.getServerWorld(), FxData.at(FxType.GROUND_DECAL, ground.add(0, 0.03, 0), 0xFF7A1A, (float) (radius * 1.15), 600));
	}

	private static final class FireSeaZone extends ZoneCast {
		private static final int TICKS = 80;

		FireSeaZone(ServerPlayerEntity caster, Vec3d center, double radius) {
			super(caster, SkillRegistry.FIRE_LOTUS, center, radius, 3.0, TICKS, 10);
		}

		@Override
		protected void onStart() {
			ModPackets.sendFx(world, FxData.at(FxType.MAGIC_CIRCLE, center.add(0, 0.08, 0), 0xFF7A1A, (float) radius, TICKS).withExtra(1));
			for (int i = 0; i < 3; i++) {
				Vec3d p = randomPoint(-0.4);
				ModPackets.sendFx(world, FxData.at(FxType.FLAME_PILLAR, p, 0xFF7A1A, 0.5f, TICKS - i * 8));
			}
		}

		@Override
		protected void onAmbient() {
			if (age % 2 == 0) {
				Vec3d p = randomPoint(0.1);
				SkillFx.single(world, age % 4 == 0 ? ModParticles.FLAME_WISP : ModParticles.EMBER, p, new Vec3d(0, 0.04 + world.random.nextDouble() * 0.06, 0));
			}
			if (age % 20 == 0) {
				world.playSound(null, net.minecraft.util.math.BlockPos.ofFloored(center), ModSounds.FIRE_WHOOSH, SoundCategory.PLAYERS, 0.5f, 0.7f);
			}
		}

		@Override
		protected void onPulse(List<LivingEntity> enemies, List<LivingEntity> allies) {
			for (LivingEntity e : enemies) {
				if (e.damage(ModDamageTypes.source(world, ModDamageTypes.FLAME, caster), 2.5f)) {
					e.setOnFireFor(3);
					e.addStatusEffect(new StatusEffectInstance(ModEffects.QI_BURN, 40, 0, false, true, true), caster);
					SkillFx.flameBurst(world, e.getPos().add(0, 0.4, 0), 5, 0.12);
				}
			}
		}
	}

	/** Keeps the caster slowed and glowing while the lotus blooms. */
	private static final class ChargeCast extends ActiveCast {
		ChargeCast(ServerPlayerEntity caster, Skill skill) {
			super(caster, skill, FireLotusEntity.CHARGE_TICKS);
		}

		@Override
		protected void onTick() {
			if (age == 0) {
				caster.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, FireLotusEntity.CHARGE_TICKS, 2, false, false, false));
			}
			if (age % 6 == 0) {
				SkillFx.helix(world, com.ngoducduy.celestialarts.registry.GlowParticleEffect.glow(0xFF7A1A, 0.5f, 14),
						caster.getPos(), 1.1, 2.2, 8, 1, age * 0.3);
			}
			if (age % 3 == 0) {
				// Fire qi streams into the blooming lotus above the caster's palms.
				SkillFx.gatherQi(world, caster.getEyePos().add(caster.getRotationVec(1.0f).multiply(1.2)), 0xFFB35C, 2.4, 6);
			}
		}
	}
}
