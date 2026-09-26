package com.ngoducduy.celestialarts.item;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Đan tra – what a ruined batch leaves behind. */
public class PillSlagItem extends Item {
	public PillSlagItem(Settings settings) {
		super(settings);
	}

	@Override
	public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
		tooltip.add(Text.translatable("pill.celestialarts.tooltip.slag").formatted(Formatting.DARK_GRAY, Formatting.ITALIC));
	}
}
