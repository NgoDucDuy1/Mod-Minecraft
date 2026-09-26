package com.ngoducduy.celestialarts.item;

import com.ngoducduy.celestialarts.alchemy.FlameTier;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** A vessel holding one hỏa chủng. It goes in the furnace's fire slot and is not used up. */
public class FlameItem extends Item {
	private final FlameTier tier;

	public FlameItem(FlameTier tier, Settings settings) {
		super(settings);
		this.tier = tier;
	}

	public FlameTier getTier() {
		return tier;
	}

	@Override
	public boolean hasGlint(ItemStack stack) {
		return tier.getTier() >= 4;
	}

	@Override
	public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
		tooltip.add(Text.translatable("flame.celestialarts.tooltip.tier", tier.getTier(), String.format("%.2f", tier.getPower())).formatted(Formatting.GOLD));
		tooltip.add(Text.translatable("flame.celestialarts.tooltip.pills", Text.translatable("herb.celestialarts.grade." + tier.maxPillGrade())).formatted(Formatting.GRAY));
		tooltip.add(tier.getDescription().copy().formatted(Formatting.DARK_GRAY, Formatting.ITALIC));
	}
}
