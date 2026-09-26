package com.ngoducduy.celestialarts.alchemy;

import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/** Đan chất – how well a batch was refined. Stored on the pill stack as {@code Quality}. */
public enum PillQuality {
	LOW(0.8F, Formatting.GRAY),
	MEDIUM(1.0F, Formatting.WHITE),
	HIGH(1.25F, Formatting.AQUA),
	SUPREME(1.6F, Formatting.GOLD);

	public static final String NBT_KEY = "Quality";
	private final float strength;
	private final Formatting formatting;

	PillQuality(float strength, Formatting formatting) {
		this.strength = strength;
		this.formatting = formatting;
	}

	public float getStrength() {
		return strength;
	}

	public Formatting getFormatting() {
		return formatting;
	}

	public Text getName() {
		return Text.translatable("pill.celestialarts.quality." + ordinal()).formatted(formatting);
	}

	public static PillQuality byIndex(int i) {
		PillQuality[] v = values();
		return v[Math.max(0, Math.min(v.length - 1, i))];
	}

	/** From the refining score 0..1. */
	public static PillQuality fromScore(float score) {
		if (score >= 0.85F) return SUPREME;
		if (score >= 0.65F) return HIGH;
		if (score >= 0.40F) return MEDIUM;
		return LOW;
	}
}
