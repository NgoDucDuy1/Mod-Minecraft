package com.ngoducduy.celestialarts.item;

import com.ngoducduy.celestialarts.entity.FireSpiritEntity;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Bình Hỏa Phách – an empty vessel bound with a ghast's soul to seal a fire spirit's essence. Use
 * it on a weakened (≤ {@value FireSpiritEntity#WEAKEN_THRESHOLD} health) Hỏa Linh to try to bind
 * its hỏa chủng; see {@link FlameCapture}.
 */
public class FlameCaptureBottleItem extends Item {
	public FlameCaptureBottleItem(Settings settings) {
		super(settings);
	}

	@Override
	public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
		tooltip.add(Text.translatable("item.celestialarts.flame_capture_bottle.tooltip1").formatted(Formatting.GRAY));
		tooltip.add(Text.translatable("item.celestialarts.flame_capture_bottle.tooltip2").formatted(Formatting.DARK_GRAY, Formatting.ITALIC));
	}
}
