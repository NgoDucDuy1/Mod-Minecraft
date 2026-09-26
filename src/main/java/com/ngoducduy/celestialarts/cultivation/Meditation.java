package com.ngoducduy.celestialarts.cultivation;

import com.ngoducduy.celestialarts.entity.MeditationSeatEntity;
import com.ngoducduy.celestialarts.network.FxData;
import com.ngoducduy.celestialarts.network.FxType;
import com.ngoducduy.celestialarts.network.ModPackets;
import com.ngoducduy.celestialarts.registry.GlowParticleEffect;
import com.ngoducduy.celestialarts.registry.ModEffects;
import com.ngoducduy.celestialarts.registry.ModParticles;
import com.ngoducduy.celestialarts.registry.ModSounds;
import com.ngoducduy.celestialarts.util.SkillFx;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/**
 * Thiền định – sitting in trance to draw in the spiritual qi of heaven and earth.
 *
 * <p>One key press seats the cultivator on a {@link MeditationSeatEntity}; another press, sneaking,
 * taking a hit or logging out ends it. While in trance:</p>
 * <ul>
 *   <li>qi regenerates at {@code (2 + spiritQi) ×} the normal rate,</li>
 *   <li>cultivation grows by {@code (0.6 + 0.4·realm) · spiritQi · aptitude} points per second,</li>
 *   <li>the spiritual qi of the place is sampled every second and shown on the HUD,</li>
 *   <li>breakthroughs can be attempted (they need the trance).</li>
 * </ul>
 * Qi deviation stops all gain – the cultivator can only sit and endure.
 */
public final class Meditation {
	public enum StopReason { TOGGLE, HURT, SEAT_LOST, DISCONNECT, BREAKTHROUGH }

	/** Follow effect kept on the cultivator for the whole trance; removed explicitly on stop. */
	private static final int CIRCLE_DURATION = 20 * 60 * 30;

	private Meditation() {
	}

	public static boolean isMeditating(ServerPlayerEntity player) {
		return player.getVehicle() instanceof MeditationSeatEntity;
	}

	public static void toggle(ServerPlayerEntity player) {
		if (isMeditating(player)) stop(player, StopReason.TOGGLE);
		else start(player);
	}

	/** Seats the player. Returns false (with a message) when the trance cannot begin here. */
	public static boolean start(ServerPlayerEntity player) {
		if (isMeditating(player)) return true;
		PlayerQi qi = QiHolder.get(player);
		if (player.hasVehicle()) {
			player.sendMessage(Text.translatable("message.celestialarts.meditate_riding").formatted(Formatting.RED), true);
			return false;
		}
		if (!player.isOnGround() || player.isTouchingWater() || player.isInLava() || player.isFallFlying()) {
			player.sendMessage(Text.translatable("message.celestialarts.meditate_need_ground").formatted(Formatting.RED), true);
			return false;
		}
		if (qi.isChanneling()) {
			player.sendMessage(Text.translatable("message.celestialarts.channeling").formatted(Formatting.RED), true);
			return false;
		}
		ServerWorld world = player.getServerWorld();
		int color = qi.getRoot() != null ? qi.getRoot().getRgb() : qi.getRealm().getRgb();
		MeditationSeatEntity seat = MeditationSeatEntity.seat(world, player, color);
		if (seat == null) return false;

		qi.setMeditating(true);
		qi.setMeditateTicks(0);
		qi.setSpiritQi(SpiritQi.density(world, player.getBlockPos()));
		qi.markDirty();
		Vec3d pos = player.getPos();
		ModPackets.sendFx(world, FxData.follow(FxType.MAGIC_CIRCLE, player.getId(), pos, color, 2.0F, CIRCLE_DURATION).withExtra(1));
		ModPackets.sendFx(world, FxData.at(FxType.SHOCKWAVE_RING, pos.add(0, 0.05, 0), color, 1.6F, 14));
		SkillFx.glowBurst(world, pos.add(0, 0.6, 0), color, 14, 0.6F, 0.10);
		world.playSound(null, player.getBlockPos(), ModSounds.QI_GATHER, SoundCategory.PLAYERS, 0.9F, 1.0F);
		player.sendMessage(Text.translatable("message.celestialarts.meditate_start",
				Text.translatable("spiritqi.celestialarts." + SpiritQi.label(qi.getSpiritQi())),
				String.format("%.1f", qi.getSpiritQi())).formatted(Formatting.AQUA), true);
		return true;
	}

	public static void stop(ServerPlayerEntity player, StopReason reason) {
		PlayerQi qi = QiHolder.get(player);
		if (player.getVehicle() instanceof MeditationSeatEntity seat) {
			player.stopRiding();
			seat.discard();
		}
		if (!qi.isMeditating() && reason != StopReason.SEAT_LOST) return;
		qi.setMeditating(false);
		qi.setMeditateTicks(0);
		qi.markDirty();
		ServerWorld world = player.getServerWorld();
		// Remove the follow circle (extra −1 / duration 0 = "remove this type from this entity").
		ModPackets.sendFx(world, FxData.follow(FxType.MAGIC_CIRCLE, player.getId(), player.getPos(), 0, 0F, 0).withExtra(-1));
		String key = switch (reason) {
			case HURT -> "message.celestialarts.meditate_interrupted";
			case BREAKTHROUGH -> null;
			default -> "message.celestialarts.meditate_stop";
		};
		if (key != null) player.sendMessage(Text.translatable(key).formatted(reason == StopReason.HURT ? Formatting.RED : Formatting.GRAY), true);
	}

	/** The seat vanished under the player (chunk unload, /kill …): tidy the state. */
	public static void onSeatLost(ServerPlayerEntity player) {
		if (QiHolder.get(player).isMeditating()) stop(player, StopReason.SEAT_LOST);
	}

	/** Called every server tick for online players. */
	public static void tick(ServerPlayerEntity player, PlayerQi qi) {
		boolean seated = isMeditating(player);
		if (!seated) {
			if (qi.isMeditating()) stop(player, StopReason.SEAT_LOST);
			qi.setMeditateTicks(0);
			// Off-trance sampling for the HUD hint, cheap at 1 Hz.
			if (player.age % 20 == 0) qi.setSpiritQi(SpiritQi.density(player.getServerWorld(), player.getBlockPos()));
			return;
		}
		if (!qi.isMeditating()) qi.setMeditating(true);
		int med = qi.getMeditateTicks() + 1;
		qi.setMeditateTicks(med);
		ServerWorld world = player.getServerWorld();
		BlockPos at = player.getBlockPos();
		if (med % 20 == 0) qi.setSpiritQi(SpiritQi.density(world, at));
		float density = qi.getSpiritQi();
		boolean deviating = player.hasStatusEffect(ModEffects.QI_DEVIATION);

		// Cultivation gain once per second.
		if (!deviating && med % 20 == 0 && !qi.isAtPeakOfCultivation()) {
			float perSecond = (0.6F + 0.4F * qi.getRealm().getLevel()) * density * CultivationStats.expMultiplier(qi);
			qi.addExpFraction(perSecond);
			if (qi.canBreakthrough() && med % 400 == 0) {
				player.sendMessage(Text.translatable(qi.nextBreakthroughIsTribulation()
						? "message.celestialarts.tribulation_ready" : "message.celestialarts.breakthrough_ready").formatted(Formatting.GOLD), true);
			}
		}

		// Visuals: motes spiralling into the dantian, more of them where the qi is dense.
		int color = qi.getRoot() != null ? qi.getRoot().getRgb() : qi.getRealm().getRgb();
		if (deviating) color = 0x6A0B0B;
		int every = density >= 1.6F ? 4 : density >= 1.0F ? 6 : 9;
		if (med > 10 && med % every == 0) {
			double a = med * 0.35;
			double r = 1.2 + 0.6 * Math.min(1.0, density / 2.0);
			Vec3d p = player.getPos().add(Math.cos(a) * r, 0.1 + (med % 60) / 60.0 * 1.4, Math.sin(a) * r);
			Vec3d v = player.getPos().add(0, 0.9, 0).subtract(p).multiply(0.07);
			SkillFx.single(world, GlowParticleEffect.glow(color, 0.55F, 22), p, v);
			if (med % 48 == 0) SkillFx.single(world, ModParticles.RUNE, p.add(0, 0.4, 0), new Vec3d(0, 0.02, 0));
		}
		if (med % 200 == 100 && !deviating) {
			ModPackets.sendFx(world, FxData.follow(FxType.QI_AURA, player.getId(), player.getPos(), color, 0.9F, 200));
		}
		if (med % 100 == 50 && density >= 1.6F) {
			// Rich qi: visible wisps drift in from the surroundings.
			SkillFx.gatherQi(world, player.getPos().add(0, 1.0, 0), color, 4.0 + density, 6);
		}
	}
}
