package com.ngoducduy.celestialarts.item;

import com.ngoducduy.celestialarts.cultivation.PlayerQi;
import com.ngoducduy.celestialarts.cultivation.QiHolder;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Đạo Kinh – opens the skill book screen (client) and shows the current realm.
 * The screen itself is opened from the client entrypoint through {@link #OPENER}.
 */
public class DaoManualItem extends Item {
	/** Set by the client initializer to open the skill book screen. */
	public static Runnable OPENER = () -> {
	};

	public DaoManualItem(Settings settings) {
		super(settings);
	}

	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
		if (world.isClient) {
			OPENER.run();
		} else {
			PlayerQi qi = QiHolder.get(user);
			user.sendMessage(Text.translatable("message.celestialarts.status", qi.getRealm().getName(),
					(int) qi.getQi(), (int) qi.getMaxQi(), qi.getExp(),
					qi.getExpForBreakthrough() < 0 ? "∞" : String.valueOf(qi.getExpForBreakthrough())).formatted(Formatting.GRAY), true);
		}
		return TypedActionResult.success(user.getStackInHand(hand), world.isClient);
	}

	@Override
	public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
		tooltip.add(Text.translatable("tooltip.celestialarts.dao_manual").formatted(Formatting.GRAY));
	}

	@Override
	public boolean hasGlint(ItemStack stack) {
		return true;
	}
}
