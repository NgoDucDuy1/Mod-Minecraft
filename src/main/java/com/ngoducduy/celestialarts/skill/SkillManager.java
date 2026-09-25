package com.ngoducduy.celestialarts.skill;

import com.ngoducduy.celestialarts.cultivation.PlayerQi;
import com.ngoducduy.celestialarts.cultivation.QiHolder;
import com.ngoducduy.celestialarts.network.ModPackets;
import com.ngoducduy.celestialarts.skill.cast.ActiveCast;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.Iterator;
import java.util.List;

/**
 * Server-side entry point for casting skills and ticking players' cultivation state.
 */
public final class SkillManager {
	private SkillManager() {
	}

	/** Attempts to cast the skill bound to the given slot. */
	public static void castSlot(ServerPlayerEntity player, int slot) {
		PlayerQi qi = QiHolder.get(player);
		Skill skill = qi.getSlotSkill(slot);
		if (skill == null) return;
		cast(player, qi, skill);
	}

	public static boolean cast(ServerPlayerEntity player, PlayerQi qi, Skill skill) {
		Identifier id = skill.getId();
		if (!qi.hasLearned(id)) {
			player.sendMessage(Text.translatable("message.celestialarts.not_learned", skill.getName()).formatted(Formatting.RED), true);
			return false;
		}
		if (qi.getRealm().getLevel() < skill.getRealm().getLevel()) {
			player.sendMessage(Text.translatable("message.celestialarts.realm_too_low", skill.getRealm().getName()).formatted(Formatting.RED), true);
			return false;
		}
		if (qi.isOnCooldown(id)) {
			player.sendMessage(Text.translatable("message.celestialarts.on_cooldown", skill.getName(),
					String.format("%.1f", qi.getCooldown(id) / 20.0)).formatted(Formatting.GRAY), true);
			return false;
		}
		if (player.hasVehicle() && !skill.canUseWhileRiding()) {
			player.sendMessage(Text.translatable("message.celestialarts.cannot_while_riding").formatted(Formatting.RED), true);
			return false;
		}
		// A running channel blocks other skills; pressing the same key again releases it.
		if (qi.isChanneling()) {
			ActiveCast running = qi.getActiveCast(id);
			if (running != null && running.isChannel()) {
				running.onRelease();
				return true;
			}
			player.sendMessage(Text.translatable("message.celestialarts.channeling").formatted(Formatting.RED), true);
			return false;
		}
		// Toggle skills (e.g. sword flight) end when the key is pressed again.
		ActiveCast existing = qi.getActiveCast(id);
		if (existing != null) {
			existing.onRelease();
			return true;
		}
		if (!qi.hasQi(skill.getQiCost()) && !player.isCreative()) {
			player.sendMessage(Text.translatable("message.celestialarts.not_enough_qi").formatted(Formatting.RED), true);
			return false;
		}

		SkillContext ctx = new SkillContext(player, player.getServerWorld(), qi);
		boolean ok = skill.activate(ctx);
		if (ok) {
			if (!player.isCreative()) qi.consumeQi(skill.getQiCost());
			qi.setCooldown(id, skill.getCooldownTicks());
			qi.markDirty();
		}
		return ok;
	}

	/** Called when the client releases the key of a slot. */
	public static void releaseSlot(ServerPlayerEntity player, int slot) {
		PlayerQi qi = QiHolder.get(player);
		Skill skill = qi.getSlotSkill(slot);
		if (skill == null) return;
		ActiveCast cast = qi.getActiveCast(skill.getId());
		if (cast != null && cast.isChannel()) {
			cast.onRelease();
		}
		skill.release(new SkillContext(player, player.getServerWorld(), qi));
	}

	/** Per-tick server update for a player: casts, cooldowns, regeneration, syncing. */
	public static void tickPlayer(ServerPlayerEntity player) {
		PlayerQi qi = QiHolder.get(player);

		List<ActiveCast> casts = qi.getActiveCasts();
		if (!casts.isEmpty()) {
			// Iterate over a snapshot: casts may add new casts while ticking.
			for (ActiveCast cast : casts.toArray(new ActiveCast[0])) {
				cast.tick();
			}
			Iterator<ActiveCast> it = casts.iterator();
			while (it.hasNext()) {
				if (it.next().isFinished()) {
					it.remove();
					qi.markDirty();
				}
			}
		}

		qi.tickCooldowns();

		// Qi regeneration: base regen from realm, doubled while meditating (sneaking still).
		float regen = qi.getRealm().getRegenPerTick();
		boolean meditating = player.isSneaking() && !player.hasVehicle()
				&& player.getVelocity().horizontalLengthSquared() < 1.0E-4 && player.isOnGround();
		if (meditating) {
			qi.setMeditateTicks(qi.getMeditateTicks() + 1);
			regen *= 3.0f;
		} else {
			qi.setMeditateTicks(0);
		}
		if (qi.getQi() < qi.getMaxQi()) {
			qi.addQi(regen);
		}

		if (qi.isDirty() || player.age % 40 == 0) {
			ModPackets.sendSync(player, qi);
			qi.clearDirty();
		}
	}
}
