package com.ngoducduy.celestialarts.item;

import com.ngoducduy.celestialarts.alchemy.Herb;
import com.ngoducduy.celestialarts.block.HerbBlock;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;

import java.util.List;

/** The harvested herb. Placing it plants a sprout (age 0) on suitable ground. */
public class HerbItem extends BlockItem {
	private final Herb herb;

	public HerbItem(HerbBlock block, Settings settings) {
		super(block, settings);
		this.herb = block.getHerb();
	}

	public Herb getHerb() {
		return herb;
	}

	@Override
	public Text getName(ItemStack stack) {
		return Text.translatable(getTranslationKey(stack)).formatted(Herb.gradeFormatting(herb.grade()));
	}

	@Override
	public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext context) {
		tooltip.add(Text.translatable("herb.celestialarts.tooltip.kind", herb.getGradeName(), herb.element().getName()).formatted(Formatting.GRAY));
		tooltip.add(Text.translatable("herb.celestialarts.tooltip.nature", herb.getNatureName(),
				Text.literal(String.valueOf(herb.potency())).formatted(Formatting.GREEN),
				Text.literal(String.valueOf(herb.toxicity())).formatted(herb.toxicity() >= 2 ? Formatting.DARK_PURPLE : Formatting.GRAY)).formatted(Formatting.GRAY));
		tooltip.add(Text.translatable("herb.celestialarts.tooltip.habitat", herb.habitat().getName()).formatted(Formatting.DARK_GRAY));
		if (context.isAdvanced()) {
			tooltip.add(Text.translatable("herb.celestialarts.tooltip.grow").formatted(Formatting.DARK_GRAY, Formatting.ITALIC));
		}
	}
}
