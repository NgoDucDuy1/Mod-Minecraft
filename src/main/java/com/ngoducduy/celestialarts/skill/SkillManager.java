package com.ngoducduy.celestialarts.skill;

import com.ngoducduy.celestialarts.cultivation.PlayerQi;
import com.ngoducduy.celestialarts.cultivation.QiHolder;
import com.ngoducduy.celestialarts.network.ModPackets;
import com.ngoducduy.celestialarts.registry.ModSounds;
import com.ngoducduy.celestialarts.skill.cast.ActiveCast;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
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
	/**
	 * "Phế bỏ công pháp": the player gives up a learned art. Any running cast of it is cancelled, it is
	 * removed from every slot and, when {@code refund} is set, the matching Bí Tịch is handed back so
	 * the art can be re-learned or passed on. Returns false if the art was not known.
	 */
	public static boolean forget(ServerPlayerEntity player, Identifier skillId, boolean refund) {
		return forget(player, skillId, refund, true);
	}

	/** As {@link #forget(ServerPlayerEntity, Identifier, boolean)}; {@code announce} = false keeps it silent (commands). */
	public static boolean forget(ServerPlayerEntity player, Identifier skillId, boolean refund, boolean announce) {
		Skill skill = SkillRegistry.get(skillId);
		PlayerQi qi = QiHolder.get(player);
		if (skill == null || !qi.hasLearned(skillId)) return false;
		ActiveCast running = qi.getActiveCast(skillId);
		if (running != null) running.cancel();
		qi.forget(skillId);
		if (refund) {
			for (com.ngoducduy.celestialarts.item.SkillScrollItem scroll : com.ngoducduy.celestialarts.registry.ModItems.scrolls()) {
				if (scroll.getSkill() == skill) {
					// Into the inventory, or dropped at the feet when it is full (never lost).
					player.getInventory().offerOrDrop(new net.minecraft.item.ItemStack(scroll));
					break;
				}
			}
		}
		if (announce) {
			player.sendMessage(Text.translatable(refund ? "message.celestialarts.forgot" : "message.celestialarts.forgot_plain", skill.getName()).formatted(Formatting.GRAY), false);
			player.getServerWorld().playSound(null, player.getBlockPos(), com.ngoducduy.celestialarts.registry.ModSounds.LEARN_SKILL, net.minecraft.sound.SoundCategory.PLAYERS, 0.8f, 0.6f);
			ModPackets.sendFx(player, com.ngoducduy.celestialarts.network.FxData.follow(com.ngoducduy.celestialarts.network.FxType.QI_AURA, player.getId(), player.getPos(), 0x777788, 0.7f, 30));
		}
		ModPackets.sendSync(player, qi);
		return true;
	}

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
		// Pressing the key of a running skill again releases it (channels stop, toggles such as
		// sword flight land). This must be checked before cooldown / riding gates, otherwise a
		// toggled skill could never be switched off.
		ActiveCast existing = qi.getActiveCast(id);
		if (existing != null) {
			existing.onRelease();
			return true;
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
		// A running channel blocks every other skill.
		if (qi.isChanneling()) {
			player.sendMessage(Text.translatable("message.celestialarts.channeling").formatted(Formatting.RED), true);
			return false;
		}
		if (!qi.hasQi(skill.getQiCost()) && !player.isCreative()) {
			player.sendMessage(Text.translatable("message.celestialarts.not_enough_qi").formatted(Formatting.RED), true);
			return false;
		}

		SkillContext ctx = new SkillContext(player, player.getServerWorld(), qi);
		boolean ok = skill.activate(ctx);
		if (ok) {
			// Every successful cast opens with a short qi release so casts read even when the skill's own sound is delayed.
			player.getServerWorld().playSound(null, player.getBlockPos(), ModSounds.CAST_QI, SoundCategory.PLAYERS, 0.6f, 0.95f + player.getRandom().nextFloat() * 0.1f);
		}
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
