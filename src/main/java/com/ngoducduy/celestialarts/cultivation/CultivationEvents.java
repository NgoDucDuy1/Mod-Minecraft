package com.ngoducduy.celestialarts.cultivation;

import com.ngoducduy.celestialarts.network.ModPackets;
import com.ngoducduy.celestialarts.skill.SkillManager;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Wires the cultivation system into the server lifecycle.
 */
public final class CultivationEvents {
	private CultivationEvents() {
	}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
				SkillManager.tickPlayer(player);
				PlayerQi qi = QiHolder.get(player);
				RealmPassives.tick(player, qi);
			}
		});

		ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) -> {
			PlayerQi from = QiHolder.get(oldPlayer);
			PlayerQi to = QiHolder.get(newPlayer);
			// The old player entity is never ticked again, so running casts (shields, zones, the
			// flying sword...) must be closed here or their cleanup would never happen.
			from.interruptAllCasts();
			to.copyFrom(from);
			if (!alive) {
				// Death drains the Qi pool but keeps everything else.
				to.setQi(to.getMaxQi() * 0.25f);
			}
		});

		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
			RealmPassives.apply(newPlayer);
			ModPackets.sendSync(newPlayer, QiHolder.get(newPlayer));
		});

		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			QiHolder.get(handler.player).interruptAllCasts();
			Meditation.stop(handler.player, Meditation.StopReason.DISCONNECT);
		});

		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ServerPlayerEntity player = handler.player;
			PlayerQi qi = QiHolder.get(player);
			// A seat never survives a relog; make sure the flag does not either.
			qi.setMeditating(false);
			RealmPassives.apply(player);
			ModPackets.sendSync(player, qi);
			// First join in this world: the spirit root awakens (a few ticks later so the client is ready).
			if (!qi.hasAwakened()) {
				server.execute(() -> server.execute(() -> {
					if (player.isAlive() && !player.isDisconnected()) Awakening.ensure(player);
				}));
			}
		});

		// Killing creatures grants cultivation experience (tu vi).
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (source.getAttacker() instanceof ServerPlayerEntity player) {
				int exp = expFor(entity);
				if (exp > 0) {
					PlayerQi qi = QiHolder.get(player);
					if (!qi.isAtPeakOfCultivation()) qi.addExpFraction(exp * CultivationStats.expMultiplier(qi));
					qi.addQi(exp * 0.5f);
				}
			}
		});
	}

	private static int expFor(LivingEntity entity) {
		if (entity instanceof EnderDragonEntity) return 3000;
		if (entity instanceof WitherEntity) return 1500;
		if (entity instanceof PlayerEntity) return 120;
		if (entity instanceof HostileEntity) return 8 + Math.round(entity.getMaxHealth() * 0.4f);
		return 2 + Math.round(entity.getMaxHealth() * 0.15f);
	}
}
