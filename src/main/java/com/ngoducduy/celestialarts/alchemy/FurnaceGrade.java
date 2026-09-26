package com.ngoducduy.celestialarts.alchemy;

import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/** Phàm / Linh / Bảo / Tiên – the furnace's grade caps the pill grade and adds a little success. */
public enum FurnaceGrade {
	MORTAL("mortal", 2, 0.00F, Formatting.WHITE),
	SPIRIT("spirit", 3, 0.04F, Formatting.GREEN),
	TREASURE("treasure", 4, 0.08F, Formatting.LIGHT_PURPLE),
	IMMORTAL("immortal", 5, 0.12F, Formatting.GOLD);

	private final String key;
	private final int maxPillGrade;
	private final float successBonus;
	private final Formatting formatting;

	FurnaceGrade(String key, int maxPillGrade, float successBonus, Formatting formatting) {
		this.key = key;
		this.maxPillGrade = maxPillGrade;
		this.successBonus = successBonus;
		this.formatting = formatting;
	}

	public String getKey() {
		return key;
	}

	public int getMaxPillGrade() {
		return maxPillGrade;
	}

	public float getSuccessBonus() {
		return successBonus;
	}

	public Formatting getFormatting() {
		return formatting;
	}

	public Text getName() {
		return Text.translatable("furnace.celestialarts.grade." + key).formatted(formatting);
	}
}
