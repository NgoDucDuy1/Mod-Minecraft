package com.ngoducduy.celestialarts.item;

import com.ngoducduy.celestialarts.alchemy.FlameTier;
import com.ngoducduy.celestialarts.cultivation.PlayerQi;
import com.ngoducduy.celestialarts.cultivation.QiHolder;
import com.ngoducduy.celestialarts.cultivation.SpiritRoot;
import com.ngoducduy.celestialarts.entity.FireSpiritEntity;
import com.ngoducduy.celestialarts.network.FxData;
import com.ngoducduy.celestialarts.network.FxType;
import com.ngoducduy.celestialarts.network.ModPackets;
import com.ngoducduy.celestialarts.registry.ModAdvancements;
import com.ngoducduy.celestialarts.registry.ModItems;
import com.ngoducduy.celestialarts.registry.ModSounds;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;

/**
 * Khống chế lửa – binding a weakened Hỏa Linh into its flame with a Bình Hỏa Phách. Odds improve
 * with cultivation realm and a Hỏa (fire) spirit root, and fall off the higher (rarer) the tier
 * is; a failure still burns the bottle and leaves the spirit briefly enraged.
 */
public final class FlameCapture {
	private FlameCapture() {
	}

	public static ActionResult attempt(ServerWorld world, PlayerEntity player, FireSpiritEntity spirit, ItemStack bottle, Hand hand) {
		if (!spirit.isWeakened()) {
			player.sendMessage(Text.translatable("message.celestialarts.flame_not_weak", spirit.getFlameTier().getName()).formatted(Formatting.GRAY), true);
			return ActionResult.FAIL;
		}
		FlameTier tier = spirit.getFlameTier();
		float chance = chanceFor(player, tier);
		bottle.decrement(1);

		if (world.getRandom().nextFloat() < chance) {
			ItemStack reward = new ItemStack(ModItems.flameFor(tier));
			if (!player.getInventory().insertStack(reward)) player.dropItem(reward, false);
			world.playSound(null, spirit.getBlockPos(), ModSounds.BREAKTHROUGH, SoundCategory.PLAYERS, 1.0F, 1.1F);
			ModPackets.sendFx(world, FxData.at(FxType.SHOCKWAVE_RING, spirit.getPos(), tier.getRgb(), 1.2F, 14));
			player.sendMessage(Text.translatable("message.celestialarts.flame_captured", tier.getName()).formatted(Formatting.GOLD), true);
			if (player instanceof ServerPlayerEntity sp) ModAdvancements.onFlameCaptured(sp, tier);
			spirit.discard();
			return ActionResult.SUCCESS;
		}

		world.playSound(null, spirit.getBlockPos(), ModSounds.SHIELD_HIT, SoundCategory.PLAYERS, 1.0F, 0.8F);
		spirit.enrage(200);
		player.sendMessage(Text.translatable("message.celestialarts.flame_capture_failed").formatted(Formatting.RED), true);
		return ActionResult.SUCCESS;
	}

	/** Odds of a successful bind: base 30%, up with realm and a fire root, down with the tier's rarity. */
	public static float chanceFor(PlayerEntity player, FlameTier tier) {
		PlayerQi qi = QiHolder.get(player);
		float chance = 0.30F;
		chance += (qi.getRealm().getLevel() - 1) * 0.05F;
		SpiritRoot root = qi.getRoot();
		if (root != null && root.has(SpiritRoot.Kind.FIRE)) {
			chance += 0.10F + 0.03F * root.getGrade();
		}
		chance -= Math.max(0, tier.getTier() - 2) * 0.06F;
		return MathHelper.clamp(chance, 0.05F, 0.9F);
	}
}
