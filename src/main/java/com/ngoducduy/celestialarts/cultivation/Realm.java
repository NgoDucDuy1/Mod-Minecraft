package com.ngoducduy.celestialarts.cultivation;

import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Cultivation realms (đại cảnh giới). Each realm raises the Qi pool, Qi regeneration and unlocks
 * higher tier skills, and is walked through four {@link Stage minor stages}. Every stage costs
 * {@link #getStageExp()} cultivation experience; the last step out of a realm's peak stage is a
 * real heavenly tribulation.
 */
public enum Realm {
	QI_REFINING(1, "qi_refining", 100f, 0.35f, 100, Formatting.WHITE, 0xB0E0FF),
	FOUNDATION(2, "foundation", 160f, 0.50f, 375, Formatting.GREEN, 0x7CFFB0),
	GOLDEN_CORE(3, "golden_core", 240f, 0.70f, 1000, Formatting.GOLD, 0xFFD36B),
	NASCENT_SOUL(4, "nascent_soul", 340f, 0.95f, 2250, Formatting.AQUA, 0x8AF3FF),
	SPIRIT_TRANSFORMATION(5, "spirit_transformation", 460f, 1.25f, 4500, Formatting.LIGHT_PURPLE, 0xD98BFF),
	TRIBULATION(6, "tribulation", 620f, 1.70f, 8000, Formatting.RED, 0xFF6B6B);

	private final int level;
	private final String key;
	private final float maxQi;
	private final float regenPerTick;
	private final int stageExp;
	private final Formatting color;
	private final int rgb;

	Realm(int level, String key, float maxQi, float regenPerTick, int stageExp, Formatting color, int rgb) {
		this.level = level;
		this.key = key;
		this.maxQi = maxQi;
		this.regenPerTick = regenPerTick;
		this.stageExp = stageExp;
		this.color = color;
		this.rgb = rgb;
	}

	public int getLevel() {
		return level;
	}

	public String getKey() {
		return key;
	}

	public float getMaxQi() {
		return maxQi;
	}

	public float getRegenPerTick() {
		return regenPerTick;
	}

	/** Cultivation experience needed to leave each minor stage of this realm. */
	public int getStageExp() {
		return stageExp;
	}

	/** Total experience from the first step of this realm to its tribulation (four stages). */
	public int getTotalExp() {
		return stageExp * Stage.values().length;
	}

	public Formatting getColor() {
		return color;
	}

	public int getRgb() {
		return rgb;
	}

	public Text getName() {
		return Text.translatable("realm.celestialarts." + key).formatted(color);
	}

	public boolean isMax() {
		return this == TRIBULATION;
	}

	public Realm next() {
		return isMax() ? this : values()[ordinal() + 1];
	}

	public Realm previous() {
		return this == QI_REFINING ? this : values()[ordinal() - 1];
	}

	public static Realm byLevel(int level) {
		for (Realm r : values()) {
			if (r.level == level) return r;
		}
		return level < 1 ? QI_REFINING : TRIBULATION;
	}
}
