package com.ngoducduy.celestialarts.skill.skills;

import com.ngoducduy.celestialarts.cultivation.Realm;
import com.ngoducduy.celestialarts.entity.ThunderDragonEntity;
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
import com.ngoducduy.celestialarts.util.SkillFx;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;

/**
 * Lôi Long Phá – the cultivator gathers heavenly thunder for a moment, then a serpent of
 * purple lightning erupts from the palms, snaking toward enemies and detonating at the end.
 */
public class ThunderDragonSkill extends Skill {
	private static final int CHARGE = 18;
	private static final int PURPLE = 0xB57BFF;

	public ThunderDragonSkill() {
		super(Settings.of(Element.LIGHTNING, SkillType.PROJECTILE, Realm.SPIRIT_TRANSFORMATION, 70f, 520));
	}

	@Override
	public boolean activate(SkillContext ctx) {
		ServerPlayerEntity player = ctx.player();
		ModPackets.sendFx(player, FxData.follow(FxType.MAGIC_CIRCLE, player.getId(), player.getPos(), PURPLE, 2.6f, CHARGE + 6).withExtra(2));
		ModPackets.sendFx(player, FxData.follow(FxType.QI_AURA, player.getId(), player.getPos(), PURPLE, 0.9f, CHARGE + 4));
		ctx.world().playSound(null, player.getBlockPos(), ModSounds.THUNDER_CHARGE, SoundCategory.PLAYERS, 1.4f, 0.8f);
		ctx.qi().addActiveCast(new ChargeCast(player, this));
		return true;
	}

	private static final class ChargeCast extends ActiveCast {
		ChargeCast(ServerPlayerEntity caster, Skill skill) {
			super(caster, skill, CHARGE + 1);
		}

		@Override
		protected void onTick() {
			if (age == 0) {
				caster.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, CHARGE, 3, false, false, false));
			}
			if (age % 4 == 0) {
				Vec3d hand = caster.getEyePos().add(caster.getRotationVec(1.0f).multiply(1.4)).add(0, -0.4, 0);
				SkillFx.thunderSparks(world, hand, 5, 0.15);
				Vec3d from = hand.add(SkillFx.randomUnit(world.random).multiply(1.2 + world.random.nextDouble() * 1.2));
				ModPackets.sendFx(world, FxData.line(FxType.LIGHTNING_BOLT, from, hand, PURPLE, 0.12f, 3));
			}
			if (age == CHARGE) release();
		}

		private void release() {
			Vec3d look = caster.getRotationVec(1.0f);
			Vec3d spawn = caster.getEyePos().add(look.multiply(1.3)).add(0, -0.2, 0);
			ThunderDragonEntity dragon = new ThunderDragonEntity(ModEntities.THUNDER_DRAGON, world);
			dragon.setOwner(caster);
			dragon.refreshPositionAndAngles(spawn.x, spawn.y, spawn.z, caster.getYaw(), caster.getPitch());
			dragon.launch(look, 0.85);
			world.spawnEntity(dragon);

			caster.swingHand(Hand.MAIN_HAND, true);
			ModPackets.sendFx(world, FxData.at(FxType.ENERGY_BURST, spawn, 0xE6D6FF, 1.6f, 8));
			ModPackets.sendFx(world, FxData.at(FxType.SHOCKWAVE_RING, spawn, PURPLE, 2.5f, 8).withTarget(look));
			SkillFx.thunderSparks(world, spawn, 30, 0.5);
			world.playSound(null, caster.getBlockPos(), ModSounds.THUNDER_STRIKE, SoundCategory.PLAYERS, 2.0f, 0.7f);
			world.playSound(null, caster.getBlockPos(), ModSounds.SWORD_LAUNCH, SoundCategory.PLAYERS, 1.0f, 0.6f);
			ModPackets.sendCameraShake(world, spawn, 16.0, 0.5f, 6);
			ModPackets.sendFx(world, FxData.at(FxType.SCREEN_FLASH, spawn, 0xD9C7FF, 0.5f, 6));
		}
	}
}
