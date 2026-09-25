package com.ngoducduy.celestialarts.cultivation;

import com.ngoducduy.celestialarts.network.FxData;
import com.ngoducduy.celestialarts.network.FxType;
import com.ngoducduy.celestialarts.network.ModPackets;
import com.ngoducduy.celestialarts.registry.ModAdvancements;
import com.ngoducduy.celestialarts.registry.ModSounds;
import com.ngoducduy.celestialarts.skill.Skill;
import com.ngoducduy.celestialarts.skill.SkillRegistry;
import com.ngoducduy.celestialarts.skill.cast.ActiveCast;
import com.ngoducduy.celestialarts.util.SkillFx;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;

/**
 * Realm breakthrough (đột phá). The player sits in meditation while a tribulation
 * cloud gathers, lightning strikes the caster and finally a pillar of light
 * announces the new realm.
 */
public final class Breakthrough {
	private Breakthrough() {
	}

	public static void tryBreakthrough(ServerPlayerEntity player) {
		PlayerQi qi = QiHolder.get(player);
		if (qi.getRealm().isMax()) {
			player.sendMessage(Text.translatable("message.celestialarts.max_realm").formatted(Formatting.GOLD), true);
			return;
		}
		if (!qi.canBreakthrough()) {
			player.sendMessage(Text.translatable("message.celestialarts.not_enough_exp",
					qi.getExp(), qi.getExpForBreakthrough()).formatted(Formatting.RED), true);
			return;
		}
		if (qi.isChanneling()) {
			player.sendMessage(Text.translatable("message.celestialarts.channeling").formatted(Formatting.RED), true);
			return;
		}
		for (ActiveCast c : qi.getActiveCasts()) {
			if (c instanceof BreakthroughCast) return;
		}
		qi.addActiveCast(new BreakthroughCast(player));
		player.sendMessage(Text.translatable("message.celestialarts.breakthrough_start").formatted(Formatting.LIGHT_PURPLE), true);
	}

	/** Multi-tick tribulation sequence. Uses a dummy skill reference for ids. */
	private static final class BreakthroughCast extends ActiveCast {
		private static final int DURATION = 140;

		BreakthroughCast(ServerPlayerEntity caster) {
			super(caster, SkillRegistry.NINE_TRIBULATIONS, DURATION);
		}

		@Override
		public boolean isChannel() {
			return true;
		}

		@Override
		protected void onTick() {
			Vec3d pos = caster.getPos();
			PlayerQi qi = QiHolder.get(caster);
			Realm next = qi.getRealm().next();
			int color = next.getRgb();

			if (age == 0) {
				ModPackets.sendFx(world, FxData.at(FxType.TRIBULATION_CLOUD, pos.add(0, 14, 0), 0x7A5CFF, 7.0f, DURATION));
				ModPackets.sendFx(world, FxData.at(FxType.SCREEN_FLASH, pos, 0x140A2A, 0.8f, DURATION - 10).withExtra(1));
				ModPackets.sendFx(world, FxData.follow(FxType.QI_AURA, caster.getId(), pos, color, 1.2f, DURATION));
				ModPackets.sendFx(world, FxData.at(FxType.MAGIC_CIRCLE, pos.add(0, 0.05, 0), color, 3.5f, DURATION).withExtra(1));
				world.playSound(null, caster.getBlockPos(), ModSounds.QI_GATHER, SoundCategory.PLAYERS, 1.2f, 0.9f);
			}

			// Slow the player down: they are sitting in meditation.
			caster.setVelocity(Vec3d.ZERO);
			caster.velocityModified = true;

			// Lightning strikes every 20 ticks after a short build-up.
			if (age >= 40 && age < DURATION - 20 && age % 20 == 0) {
				Vec3d from = pos.add(world.random.nextGaussian() * 2.0, 14.0, world.random.nextGaussian() * 2.0);
				Vec3d to = pos.add(0, 1.0, 0);
				ModPackets.sendFx(world, FxData.line(FxType.LIGHTNING_BOLT, from, to, 0xB57BFF, 1.4f, 10));
				ModPackets.sendFx(world, FxData.at(FxType.SHOCKWAVE_RING, pos.add(0, 0.1, 0), 0xD9C7FF, 4.0f, 12));
				SkillFx.thunderSparks(world, to, 30, 0.8);
				world.playSound(null, caster.getBlockPos(), ModSounds.THUNDER_STRIKE, SoundCategory.PLAYERS, 1.5f, 0.9f + world.random.nextFloat() * 0.2f);
				ModPackets.sendCameraShake(world, pos, 32.0, 0.6f, 8);
				ModPackets.sendFx(world, FxData.at(FxType.SCREEN_FLASH, pos, 0xD9C7FF, 0.5f, 6));
			}

			if (age == DURATION - 15) {
				ModPackets.sendFx(world, FxData.at(FxType.HEAVEN_PILLAR, pos, color, 2.0f, 60));
				ModPackets.sendFx(world, FxData.at(FxType.ENERGY_BURST, pos.add(0, 1, 0), color, 3.0f, 20));
				ModPackets.sendFx(world, FxData.at(FxType.SCREEN_FLASH, pos, color, 1.0f, 18));
				SkillFx.glowBurst(world, pos.add(0, 1, 0), color, 80, 1.2f, 0.35);
				world.playSound(null, caster.getBlockPos(), SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.PLAYERS, 1.0f, 1.4f);
				world.playSound(null, caster.getBlockPos(), ModSounds.BREAKTHROUGH, SoundCategory.PLAYERS, 1.5f, 1.0f);
			}
		}

		@Override
		protected void onEnd(boolean cancelled) {
			if (cancelled) return;
			PlayerQi qi = QiHolder.get(caster);
			if (!qi.canBreakthrough()) return;
			Realm next = qi.getRealm().next();
			qi.setExp(qi.getExp() - next.getRequiredExp());
			qi.setRealm(next);
			qi.setQi(qi.getMaxQi());
			qi.markDirty();
			ModPackets.sendSync(caster, qi);
			ModAdvancements.onRealmReached(caster, next);
			RealmPassives.apply(caster);

			// Announce to the whole server – a breakthrough is a big deal.
			Text msg = Text.translatable("message.celestialarts.breakthrough_success", caster.getDisplayName(), next.getName())
					.formatted(Formatting.GOLD);
			for (ServerPlayerEntity p : caster.getServer().getPlayerManager().getPlayerList()) {
				p.sendMessage(msg, false);
			}

			// Skills that just became usable.
			for (Skill s : SkillRegistry.all()) {
				if (s.getRealm() == next && qi.hasLearned(s.getId())) {
					caster.sendMessage(Text.translatable("message.celestialarts.skill_unlocked", s.getName()).formatted(Formatting.AQUA), false);
				}
			}
			// Body tempering gained with this realm.
			caster.sendMessage(Text.translatable("message.celestialarts.body_tempered",
					RealmPassives.bonusHealth(next) / 2, Math.round(RealmPassives.bonusSpeed(next) * 100), (int) RealmPassives.bonusAttack(next)).formatted(Formatting.GREEN), false);
			String passive = switch (next.getLevel()) {
				case 2 -> "message.celestialarts.passive_air_jump";
				case 3 -> "message.celestialarts.passive_no_fall";
				case 4 -> "message.celestialarts.passive_hover";
				case 5 -> "message.celestialarts.passive_fire";
				case 6 -> "message.celestialarts.passive_double_air_jump";
				default -> null;
			};
			if (passive != null) caster.sendMessage(Text.translatable(passive).formatted(Formatting.GREEN), false);
		}
	}

	/** Small helper used by the meditation logic to hint when a breakthrough is ready. */
	public static void hintIfReady(ServerPlayerEntity player, PlayerQi qi) {
		if (qi.canBreakthrough() && player.age % 200 == 0) {
			player.sendMessage(Text.translatable("message.celestialarts.breakthrough_ready").formatted(Formatting.GOLD), true);
		}
	}
}
