package com.ngoducduy.celestialarts.skill.skills;

import com.ngoducduy.celestialarts.cultivation.Realm;
import com.ngoducduy.celestialarts.entity.HeavenSwordEntity;
import com.ngoducduy.celestialarts.network.FxData;
import com.ngoducduy.celestialarts.network.FxType;
import com.ngoducduy.celestialarts.network.ModPackets;
import com.ngoducduy.celestialarts.registry.ModEntities;
import com.ngoducduy.celestialarts.registry.ModSounds;
import com.ngoducduy.celestialarts.skill.Element;
import com.ngoducduy.celestialarts.skill.Skill;
import com.ngoducduy.celestialarts.skill.SkillContext;
import com.ngoducduy.celestialarts.skill.SkillType;
import com.ngoducduy.celestialarts.util.SkillFx;
import com.ngoducduy.celestialarts.skill.cast.ZoneCast;
import com.ngoducduy.celestialarts.skill.SkillRegistry;
import com.ngoducduy.celestialarts.cultivation.QiHolder;
import com.ngoducduy.celestialarts.registry.ModDamageTypes;
import com.ngoducduy.celestialarts.registry.ModParticles;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import java.util.List;
import com.ngoducduy.celestialarts.util.Targeting;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;

/**
 * Thiên Ngoại Phi Kiếm – the ultimate art. The cultivator points at the sky and a
 * colossal sword descends from beyond the heavens onto the chosen ground, leaving the crater
 * sealed under the Heavenly Dao for a while.
 */
public class HeavenSwordSkill extends Skill {
	private static final double TARGET_RANGE = 48.0;

	public HeavenSwordSkill() {
		super(Settings.of(Element.DAO, SkillType.ULTIMATE, Realm.TRIBULATION, 120f, 1800));
	}

	@Override
	public boolean activate(SkillContext ctx) {
		ServerPlayerEntity player = ctx.player();
		Vec3d ground = Targeting.groundPoint(player, TARGET_RANGE);

		HeavenSwordEntity sword = new HeavenSwordEntity(ModEntities.HEAVEN_SWORD, ctx.world());
		sword.init(player, ground.y, 42f + ctx.qi().getRealm().getLevel() * 2f);
		sword.refreshPositionAndAngles(ground.x, ground.y + HeavenSwordEntity.SPAWN_HEIGHT, ground.z, player.getYaw(), 0);
		ctx.world().spawnEntity(sword);

		player.swingHand(Hand.MAIN_HAND, true);
		ModPackets.sendFx(player, FxData.follow(FxType.QI_AURA, player.getId(), player.getPos(), 0xFFE9A8, 1.3f, HeavenSwordEntity.MATERIALISE_TICKS));
		ModPackets.sendFx(player, FxData.follow(FxType.MAGIC_CIRCLE, player.getId(), player.getPos(), 0xFFE9A8, 3.0f, HeavenSwordEntity.MATERIALISE_TICKS).withExtra(1));
		SkillFx.goldenLight(ctx.world(), player.getPos().add(0, 1, 0), 40, 1.5);
		ctx.world().playSound(null, player.getBlockPos(), ModSounds.QI_GATHER, SoundCategory.PLAYERS, 1.2f, 0.9f);
		ctx.world().playSound(null, player.getBlockPos(), ModSounds.RISER, SoundCategory.PLAYERS, 1.4f, 1.0f);
		ctx.world().playSound(null, player.getBlockPos(), ModSounds.SWORD_HUM, SoundCategory.PLAYERS, 1.5f, 0.8f);
		return true;
	}

	/** Called by the sword on impact: the crater stays sealed under the Heavenly Dao for six seconds. */
	public static void seal(ServerPlayerEntity owner, Vec3d center) {
		QiHolder.get(owner).addActiveCast(new HeavenSealZone(owner, center));
	}

	private static final class HeavenSealZone extends ZoneCast {
		private static final int TICKS = 120;
		private static final double R = 7.0;

		HeavenSealZone(ServerPlayerEntity caster, Vec3d center) {
			super(caster, SkillRegistry.HEAVEN_SWORD, center, R, 4.0, TICKS, 20);
		}

		@Override
		protected void onStart() {
			ModPackets.sendFx(world, FxData.at(FxType.MAGIC_CIRCLE, center.add(0, 0.12, 0), 0xFFE9A8, (float) R, TICKS).withExtra(0));
			ModPackets.sendFx(world, FxData.at(FxType.RUNE_ORBIT, center.add(0, 1.5, 0), 0xFFE9A8, (float) R * 0.7f, TICKS));
		}

		@Override
		protected void onAmbient() {
			if (age % 3 == 0) {
				SkillFx.single(world, ModParticles.GOLDEN_LIGHT, randomPoint(0.2), new Vec3d(0, 0.05, 0));
			}
			if (age % 40 == 20) {
				world.playSound(null, net.minecraft.util.math.BlockPos.ofFloored(center), ModSounds.FORMATION, SoundCategory.PLAYERS, 0.8f, 0.8f);
			}
		}

		@Override
		protected void onPulse(List<LivingEntity> enemies, List<LivingEntity> allies) {
			ModPackets.sendFx(world, FxData.at(FxType.SHOCKWAVE_RING, center.add(0, 0.15, 0), 0xFFFFFF, (float) R, 12));
			for (LivingEntity e : enemies) {
				// Suppressed by the Heavenly Dao: slow, weak, pressed to the ground.
				e.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 30, 3, false, false, true), caster);
				e.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 30, 1, false, false, true), caster);
				if (e.damage(ModDamageTypes.source(world, ModDamageTypes.HEAVEN, caster), 4.0f)) {
					e.setVelocity(e.getVelocity().x, Math.min(e.getVelocity().y, -0.2), e.getVelocity().z);
					e.velocityModified = true;
					SkillFx.goldenLight(world, e.getBoundingBox().getCenter(), 6, 0.5);
				}
			}
			for (LivingEntity a : allies) {
				a.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 40, 1, false, false, true));
			}
		}
	}
}
