package com.ngoducduy.celestialarts.cultivation;

import com.ngoducduy.celestialarts.network.FxData;
import com.ngoducduy.celestialarts.network.FxType;
import com.ngoducduy.celestialarts.network.ModPackets;
import com.ngoducduy.celestialarts.registry.ModAdvancements;
import com.ngoducduy.celestialarts.registry.ModDamageTypes;
import com.ngoducduy.celestialarts.registry.ModEffects;
import com.ngoducduy.celestialarts.registry.ModSounds;
import com.ngoducduy.celestialarts.skill.Skill;
import com.ngoducduy.celestialarts.skill.SkillRegistry;
import com.ngoducduy.celestialarts.skill.cast.ActiveCast;
import com.ngoducduy.celestialarts.util.SkillFx;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;

/**
 * Đột phá – advancing the cultivation.
 *
 * <p>Two kinds, both attempted from meditation (the trance is started automatically):</p>
 * <ul>
 *   <li><b>Small breakthrough</b> (sơ → trung → hậu → viên mãn): 6 s of gathering qi, then a roll
 *       against {@link CultivationStats#breakthroughChance}. Success advances the stage; failure
 *       means qi deviation – half the stage's experience is lost, the body is hurt and no skill
 *       works for a minute.</li>
 *   <li><b>Great breakthrough</b> (viên mãn → next realm): a real heavenly tribulation. A cloud
 *       gathers and {@link CultivationStats#tribulationBolts N} bolts strike the cultivator with
 *       {@link CultivationStats#tribulationBoltDamage real damage}. Better talent = more and
 *       heavier bolts. Leaving the seat or the spot is "fleeing the tribulation": a failed
 *       tribulation drops the cultivator back to hậu kỳ with two minutes of severe qi deviation.
 *       Dying under the lightning has the same cost. Surviving every bolt opens the next realm.</li>
 * </ul>
 */
public final class Breakthrough {
	private Breakthrough() {
	}

	public static void tryBreakthrough(ServerPlayerEntity player) {
		PlayerQi qi = QiHolder.get(player);
		if (qi.isAtPeakOfCultivation()) {
			player.sendMessage(Text.translatable("message.celestialarts.max_realm").formatted(Formatting.GOLD), true);
			return;
		}
		if (!qi.canBreakthrough()) {
			player.sendMessage(Text.translatable("message.celestialarts.not_enough_exp",
					qi.getExp(), qi.getExpForBreakthrough()).formatted(Formatting.RED), true);
			return;
		}
		if (player.hasStatusEffect(ModEffects.QI_DEVIATION)) {
			player.sendMessage(Text.translatable("message.celestialarts.deviation_blocks").formatted(Formatting.DARK_RED), true);
			return;
		}
		if (qi.isChanneling()) {
			player.sendMessage(Text.translatable("message.celestialarts.channeling").formatted(Formatting.RED), true);
			return;
		}
		for (ActiveCast c : qi.getActiveCasts()) {
			if (c instanceof BreakthroughCast) return;
		}
		if (!Meditation.isMeditating(player) && !Meditation.start(player)) return;

		if (qi.nextBreakthroughIsTribulation()) {
			qi.addActiveCast(new TribulationCast(player));
		} else {
			qi.addActiveCast(new StageBreakthroughCast(player));
		}
	}

	public static boolean isInTribulation(PlayerQi qi) {
		for (ActiveCast c : qi.getActiveCasts()) {
			if (c instanceof TribulationCast && !c.isFinished()) return true;
		}
		return false;
	}

	public static boolean isBreakingThrough(PlayerQi qi) {
		for (ActiveCast c : qi.getActiveCasts()) {
			if (c instanceof BreakthroughCast && !c.isFinished()) return true;
		}
		return false;
	}

	/** Small helper used by the meditation logic to hint when a breakthrough is ready. */
	public static void hintIfReady(ServerPlayerEntity player, PlayerQi qi) {
		if (qi.canBreakthrough() && player.age % 200 == 0) {
			player.sendMessage(Text.translatable("message.celestialarts.breakthrough_ready").formatted(Formatting.GOLD), true);
		}
	}

	// ------------------------------------------------------------ shared

	private abstract static class BreakthroughCast extends ActiveCast {
		protected final Vec3d origin;
		protected final int color;

		BreakthroughCast(ServerPlayerEntity caster, int duration) {
			super(caster, SkillRegistry.NINE_TRIBULATIONS, duration);
			this.origin = caster.getPos();
			PlayerQi qi = QiHolder.get(caster);
			this.color = qi.getRoot() != null ? qi.getRoot().getRgb() : qi.getRealm().getRgb();
		}

		@Override
		public boolean isChannel() {
			return true;
		}

		protected boolean stillSeated() {
			return Meditation.isMeditating(caster) && caster.getPos().squaredDistanceTo(origin) < 6.0 * 6.0;
		}

		protected void title(ServerPlayerEntity to, Text title, Text subtitle, int fadeIn, int stay, int fadeOut) {
			if (to.networkHandler == null) return;
			to.networkHandler.sendPacket(new TitleFadeS2CPacket(fadeIn, stay, fadeOut));
			to.networkHandler.sendPacket(new SubtitleS2CPacket(subtitle));
			to.networkHandler.sendPacket(new TitleS2CPacket(title));
		}

		/** Qi deviation plus the physical toll of a failed attempt. */
		protected void deviate(PlayerQi qi, boolean tribulation) {
			int ticks = CultivationStats.deviationTicks(qi, tribulation);
			caster.addStatusEffect(new StatusEffectInstance(ModEffects.QI_DEVIATION, ticks, tribulation ? 1 : 0, false, false, true));
			float hurt = caster.getMaxHealth() * (tribulation ? 0.40F : 0.30F);
			caster.setHealth(Math.max(1.0F, caster.getHealth() - hurt));
			caster.hurtTime = 10;
			caster.timeUntilRegen = 0;
			qi.setQi(qi.getMaxQi() * (tribulation ? 0.0F : 0.10F));
			Vec3d pos = caster.getPos();
			ModPackets.sendFx(world, FxData.at(FxType.ENERGY_BURST, pos.add(0, 1.0, 0), 0x8A0F0F, tribulation ? 3.2F : 2.2F, 16));
			ModPackets.sendFx(world, FxData.at(FxType.SHOCKWAVE_RING, pos.add(0, 0.1, 0), 0x3A0606, 3.0F, 14));
			ModPackets.sendFxTo(caster, FxData.at(FxType.SCREEN_FLASH, caster.getEyePos(), 0x6A0000, 0.9F, 22));
			SkillFx.voidSmoke(world, pos.add(0, 1.0, 0), 30, 0.25);
			SkillFx.glowBurst(world, pos.add(0, 1.0, 0), 0x8A0F0F, 40, 0.8F, 0.30);
			ModPackets.sendCameraShake(world, pos, 24.0, 1.2F, 18);
			world.playSound(null, caster.getBlockPos(), ModSounds.VOID_COLLAPSE, SoundCategory.PLAYERS, 1.4F, 0.7F);
			world.playSound(null, caster.getBlockPos(), SoundEvents.ENTITY_PLAYER_HURT, SoundCategory.PLAYERS, 1.0F, 0.6F);
		}

		protected void announce(Text msg) {
			for (ServerPlayerEntity p : caster.getServer().getPlayerManager().getPlayerList()) {
				p.sendMessage(msg, false);
			}
		}
	}

	// ----------------------------------------------------- small breakthrough

	private static final class StageBreakthroughCast extends BreakthroughCast {
		private static final int DURATION = 120;

		StageBreakthroughCast(ServerPlayerEntity caster) {
			super(caster, DURATION);
		}

		@Override
		protected void onTick() {
			PlayerQi qi = QiHolder.get(caster);
			Vec3d pos = caster.getPos();
			if (age == 0) {
				caster.sendMessage(Text.translatable("message.celestialarts.stage_breakthrough_start",
						qi.getRealm().getName(), qi.getStage().next().getName(),
						Math.round(CultivationStats.breakthroughChance(qi, qi.getSpiritQi()) * 100)).formatted(Formatting.LIGHT_PURPLE), true);
				ModPackets.sendFx(world, FxData.at(FxType.MAGIC_CIRCLE, pos.add(0, 0.05, 0), color, 3.2F, DURATION).withExtra(1));
				ModPackets.sendFx(world, FxData.follow(FxType.QI_AURA, caster.getId(), pos, color, 1.2F, DURATION));
				ModPackets.sendFxTo(caster, FxData.at(FxType.SCREEN_FLASH, caster.getEyePos(), 0x0A0814, 0.35F, DURATION - 10).withExtra(1));
				world.playSound(null, caster.getBlockPos(), ModSounds.QI_GATHER, SoundCategory.PLAYERS, 1.2F, 1.0F);
				world.playSound(null, caster.getBlockPos(), ModSounds.RISER, SoundCategory.PLAYERS, 1.4F, 1.0F);
			}
			if (!stillSeated()) {
				cancel();
				return;
			}
			float t = age / (float) DURATION;
			if (age % 2 == 0) {
				SkillFx.gatherQi(world, pos.add(0, 1.0, 0), color, 6.0 - 4.5 * t, 6 + (int) (10 * t));
			}
			if (age == 60) {
				ModPackets.sendFx(world, FxData.at(FxType.SHOCKWAVE_RING, pos.add(0, 0.1, 0), color, 2.5F, 12));
				ModPackets.sendCameraShake(world, pos, 24.0, 0.25F, 30);
			}
			if (age >= 90 && age % 6 == 0) {
				SkillFx.glowBurst(world, pos.add(0, 1.0, 0), color, 6, 0.5F, 0.12);
			}
			if (age == DURATION - 1) resolve(qi);
		}

		private void resolve(PlayerQi qi) {
			if (!qi.canBreakthrough()) return;
			float chance = CultivationStats.breakthroughChance(qi, qi.getSpiritQi());
			// Đan Vận from a Trúc Cơ / Độ Kiếp pill.
			chance = Math.min(0.98F, chance + com.ngoducduy.celestialarts.effect.PillFortuneEffect.bonus(caster));
			int cost = qi.getExpForBreakthrough();
			Vec3d pos = caster.getPos();
			if (world.random.nextFloat() < chance) {
				qi.setExp(qi.getExp() - cost);
				qi.setStage(qi.getStage().next());
				qi.setQi(qi.getMaxQi());
				qi.markDirty();
				ModPackets.sendSync(caster, qi);
				RealmPassives.apply(caster);
				ModPackets.sendFx(world, FxData.at(FxType.HEAVEN_PILLAR, pos, color, 1.4F, 40));
				ModPackets.sendFx(world, FxData.at(FxType.ENERGY_BURST, pos.add(0, 1, 0), color, 2.2F, 16));
				ModPackets.sendFx(world, FxData.at(FxType.SHOCKWAVE_RING, pos.add(0, 0.1, 0), color, 4.0F, 16));
				ModPackets.sendFxTo(caster, FxData.at(FxType.SCREEN_FLASH, caster.getEyePos(), color, 0.6F, 14));
				SkillFx.glowBurst(world, pos.add(0, 1, 0), color, 60, 1.0F, 0.30);
				world.playSound(null, caster.getBlockPos(), ModSounds.BREAKTHROUGH, SoundCategory.PLAYERS, 1.3F, 1.1F);
				title(caster, Text.translatable("title.celestialarts.stage_success").formatted(Formatting.GOLD),
						Text.empty().append(qi.getRealm().getName()).append(" · ").append(qi.getStage().getName()), 5, 50, 15);
				caster.sendMessage(Text.translatable("message.celestialarts.stage_breakthrough_success",
						qi.getRealm().getName(), qi.getStage().getName()).formatted(Formatting.GOLD), false);
				if (qi.getStage().isPeak() && !qi.getRealm().isMax()) {
					caster.sendMessage(Text.translatable("message.celestialarts.peak_reached", qi.getRealm().next().getName()).formatted(Formatting.LIGHT_PURPLE), false);
				}
			} else {
				qi.setExp(qi.getExp() - cost / 2);
				qi.markDirty();
				deviate(qi, false);
				ModPackets.sendSync(caster, qi);
				title(caster, Text.translatable("title.celestialarts.stage_failed").formatted(Formatting.DARK_RED),
						Text.translatable("title.celestialarts.qi_deviation").formatted(Formatting.RED), 5, 50, 20);
				caster.sendMessage(Text.translatable("message.celestialarts.stage_breakthrough_failed").formatted(Formatting.RED), false);
			}
		}

		@Override
		protected void onEnd(boolean cancelled) {
			if (cancelled && caster.isAlive()) {
				caster.sendMessage(Text.translatable("message.celestialarts.breakthrough_interrupted").formatted(Formatting.RED), true);
			}
		}
	}

	// ------------------------------------------------------------ tribulation

	private static final class TribulationCast extends BreakthroughCast {
		private static final int FIRST_BOLT = 80;
		private static final int BOLT_GAP = 20;
		private static final int AFTERMATH = 50;

		private final int bolts;
		private final float boltDamage;
		private int struck;
		private boolean resolved;

		TribulationCast(ServerPlayerEntity caster) {
			super(caster, FIRST_BOLT + CultivationStats.tribulationBolts(QiHolder.get(caster)) * BOLT_GAP + AFTERMATH);
			PlayerQi qi = QiHolder.get(caster);
			this.bolts = CultivationStats.tribulationBolts(qi);
			this.boltDamage = CultivationStats.tribulationBoltDamage(qi) * qi.getTalent().tribulationDamageMultiplier();
		}

		private Vec3d cloud() {
			return origin.add(0, 16, 0);
		}

		@Override
		protected void onTick() {
			PlayerQi qi = QiHolder.get(caster);
			Vec3d pos = caster.getPos();
			if (age == 0) {
				Realm next = qi.getRealm().next();
				float cloudScale = 6.0F + bolts * 0.5F;
				ModPackets.sendFx(world, FxData.at(FxType.TRIBULATION_CLOUD, cloud(), 0x7A5CFF, cloudScale, duration - 30));
				ModPackets.sendFx(world, FxData.at(FxType.SCREEN_FLASH, pos, 0x140A2A, 0.6F, duration - 40).withExtra(1), 96.0);
				ModPackets.sendFx(world, FxData.follow(FxType.QI_AURA, caster.getId(), pos, color, 1.2F, duration));
				ModPackets.sendFx(world, FxData.at(FxType.MAGIC_CIRCLE, pos.add(0, 0.05, 0), next.getRgb(), 4.0F, duration).withExtra(2));
				world.playSound(null, caster.getBlockPos(), ModSounds.QI_GATHER, SoundCategory.PLAYERS, 1.4F, 0.8F);
				world.playSound(null, caster.getBlockPos(), ModSounds.RISER, SoundCategory.PLAYERS, 2.0F, 0.8F);
				world.playSound(null, caster.getBlockPos(), SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.WEATHER, 3.0F, 0.6F);
				title(caster, Text.translatable("title.celestialarts.tribulation").formatted(Formatting.LIGHT_PURPLE),
						Text.translatable("title.celestialarts.tribulation_sub", bolts).formatted(Formatting.GRAY), 10, 60, 20);
				caster.sendMessage(Text.translatable("message.celestialarts.tribulation_start", bolts, String.format("%.1f", boltDamage / 2.0F)).formatted(Formatting.LIGHT_PURPLE), false);
				announce(Text.translatable("message.celestialarts.tribulation_announce", caster.getDisplayName(), next.getName()).formatted(Formatting.DARK_PURPLE));
			}
			if (resolved) return;
			if (!stillSeated()) {
				fail(qi, "fled");
				return;
			}
			if (age < FIRST_BOLT && age % 3 == 0) {
				SkillFx.gatherQi(world, pos.add(0, 1.0, 0), color, 5.0 + 3.0 * age / FIRST_BOLT, 8);
			}
			if (age >= FIRST_BOLT - 30 && age < FIRST_BOLT && age % 6 == 0) {
				// The cloud grumbles before the first bolt.
				ModPackets.sendCameraShake(world, pos, 40.0, 0.2F, 6);
			}
			int sinceFirst = age - FIRST_BOLT;
			if (sinceFirst >= 0 && sinceFirst % BOLT_GAP == 0 && struck < bolts) {
				strike(qi);
			}
			if (struck >= bolts && sinceFirst == (bolts - 1) * BOLT_GAP + 30) {
				succeed(qi);
			}
		}

		private void strike(PlayerQi qi) {
			struck++;
			Vec3d to = caster.getPos().add(0, 1.0, 0);
			Vec3d from = cloud().add(world.random.nextGaussian() * 2.0, 0, world.random.nextGaussian() * 2.0);
			// Later bolts are thicker, brighter and hit harder: the tribulation crescendos.
			float grow = 0.75F + 0.5F * struck / (float) bolts;
			ModPackets.sendFx(world, FxData.line(FxType.LIGHTNING_BOLT, from, to, 0xB57BFF, 1.2F * grow, 14), 96.0);
			ModPackets.sendFx(world, FxData.at(FxType.SHOCKWAVE_RING, caster.getPos().add(0, 0.1, 0), 0xD9C7FF, 3.0F + 2.0F * grow, 12));
			ModPackets.sendFx(world, FxData.at(FxType.SCREEN_FLASH, caster.getPos(), 0xD9C7FF, 0.45F * grow, 6), 96.0);
			SkillFx.thunderSparks(world, to, 24 + 6 * struck, 0.9);
			world.playSound(null, caster.getBlockPos(), ModSounds.THUNDER_STRIKE, SoundCategory.WEATHER, 2.0F, 0.85F + world.random.nextFloat() * 0.2F);
			world.playSound(null, caster.getBlockPos(), SoundEvents.ENTITY_LIGHTNING_BOLT_IMPACT, SoundCategory.WEATHER, 1.5F, 0.9F);
			ModPackets.sendCameraShake(world, caster.getPos(), 48.0, 0.9F * grow, 10);

			float dmg = boltDamage * grow;
			caster.timeUntilRegen = 0;
			caster.damage(ModDamageTypes.source(world, ModDamageTypes.TRIBULATION, null), dmg);
			if (caster.isAlive()) {
				caster.sendMessage(Text.translatable("message.celestialarts.tribulation_bolt", struck, bolts).formatted(Formatting.LIGHT_PURPLE), true);
			}
		}

		private void succeed(PlayerQi qi) {
			resolved = true;
			Realm next = qi.getRealm().next();
			qi.setRealm(next);
			qi.setStage(Stage.EARLY);
			qi.setExp(0);
			qi.setQi(qi.getMaxQi());
			qi.markDirty();
			caster.setHealth(caster.getMaxHealth());
			ModPackets.sendSync(caster, qi);
			ModAdvancements.onRealmReached(caster, next);
			RealmPassives.apply(caster);

			Vec3d pos = caster.getPos();
			int c = next.getRgb();
			ModPackets.sendFx(world, FxData.at(FxType.HEAVEN_PILLAR, pos, c, 2.2F, 70), 96.0);
			ModPackets.sendFx(world, FxData.at(FxType.ENERGY_BURST, pos.add(0, 1, 0), c, 3.2F, 20));
			ModPackets.sendFx(world, FxData.at(FxType.SHOCKWAVE_RING, pos.add(0, 0.1, 0), 0xFFFFFF, 8.0F, 24));
			ModPackets.sendFx(world, FxData.at(FxType.SCREEN_FLASH, pos, c, 1.0F, 20), 96.0);
			SkillFx.glowBurst(world, pos.add(0, 1, 0), c, 100, 1.2F, 0.40);
			SkillFx.runes(world, pos.add(0, 0.3, 0), 24, 3.0);
			ModPackets.sendCameraShake(world, pos, 64.0, 1.2F, 24);
			world.playSound(null, caster.getBlockPos(), SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.PLAYERS, 1.0F, 1.4F);
			world.playSound(null, caster.getBlockPos(), ModSounds.BREAKTHROUGH, SoundCategory.PLAYERS, 1.6F, 1.0F);
			world.playSound(null, caster.getBlockPos(), ModSounds.SUB_DROP, SoundCategory.PLAYERS, 2.0F, 1.0F);

			title(caster, Text.translatable("title.celestialarts.tribulation_success").formatted(Formatting.GOLD), next.getName(), 5, 70, 20);
			announce(Text.translatable("message.celestialarts.breakthrough_success", caster.getDisplayName(), next.getName()).formatted(Formatting.GOLD));
			for (Skill s : SkillRegistry.all()) {
				if (s.getRealm() == next && qi.hasLearned(s.getId())) {
					caster.sendMessage(Text.translatable("message.celestialarts.skill_unlocked", s.getName()).formatted(Formatting.AQUA), false);
				}
			}
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
			Meditation.stop(caster, Meditation.StopReason.BREAKTHROUGH);
			finish();
		}

		/** Fled or died under the lightning: back to hậu kỳ and severe qi deviation. */
		private void fail(PlayerQi qi, String why) {
			resolved = true;
			qi.setStage(Stage.LATE);
			qi.setExp(0);
			qi.markDirty();
			if (caster.isAlive()) {
				deviate(qi, true);
				// Heaven's last word: one more bolt that cannot be dodged.
				Vec3d to = caster.getPos().add(0, 1.0, 0);
				ModPackets.sendFx(world, FxData.line(FxType.LIGHTNING_BOLT, cloud(), to, 0xFF6B6B, 1.6F, 12), 96.0);
				world.playSound(null, caster.getBlockPos(), ModSounds.THUNDER_STRIKE, SoundCategory.WEATHER, 2.0F, 0.7F);
				title(caster, Text.translatable("title.celestialarts.tribulation_failed").formatted(Formatting.DARK_RED),
						Text.translatable("title.celestialarts.qi_deviation").formatted(Formatting.RED), 5, 60, 20);
				Meditation.stop(caster, Meditation.StopReason.BREAKTHROUGH);
			}
			ModPackets.sendSync(caster, qi);
			announce(Text.translatable("message.celestialarts.tribulation_" + why, caster.getDisplayName()).formatted(Formatting.DARK_RED));
			finish();
		}

		@Override
		protected void onEnd(boolean cancelled) {
			if (resolved) return;
			PlayerQi qi = QiHolder.get(caster);
			if (!caster.isAlive()) {
				// Struck down: the cultivation base cracks with the body.
				resolved = true;
				qi.setStage(Stage.LATE);
				qi.setExp(0);
				qi.markDirty();
				announce(Text.translatable("message.celestialarts.tribulation_died", caster.getDisplayName()).formatted(Formatting.DARK_RED));
			} else if (cancelled) {
				fail(qi, "fled");
			}
		}
	}
}
