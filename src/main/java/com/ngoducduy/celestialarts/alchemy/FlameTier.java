package com.ngoducduy.celestialarts.alchemy;

import net.minecraft.text.Text;

/**
 * Hỏa chủng – the fires an alchemist can feed a furnace, from mundane Phàm Hỏa to Thiên Hỏa.
 * {@code tier} gates recipes ({@link AlchemyRecipe#fireTier()}), {@code power} is the heat one
 * fire level adds per tick, {@code volatility} scales the random wobble of the heat.
 */
public enum FlameTier {
	MORTAL("flame_mortal", 1, 0.30F, 1.0F, 0xFF9A3A),
	EARTH("flame_earth", 2, 0.40F, 0.9F, 0xFF5A1A),
	SPIRIT("flame_spirit", 3, 0.48F, 0.8F, 0x7AE8FF),
	GHOST("flame_ghost", 3, 0.42F, 0.7F, 0x8AFFB8),
	KARMIC("flame_karmic", 4, 0.56F, 1.1F, 0xFF2A2A),
	STRANGE("flame_strange", 4, 0.60F, 0.75F, 0xD26BFF),
	SAMADHI("flame_samadhi", 5, 0.70F, 0.6F, 0xFFD85A),
	HEAVEN("flame_heaven", 6, 0.85F, 0.5F, 0xFFFFE0);

	private final String key;
	private final int tier;
	private final float power;
	private final float volatility;
	private final int rgb;

	FlameTier(String key, int tier, float power, float volatility, int rgb) {
		this.key = key;
		this.tier = tier;
		this.power = power;
		this.volatility = volatility;
		this.rgb = rgb;
	}

	public String getKey() {
		return key;
	}

	public int getTier() {
		return tier;
	}

	public float getPower() {
		return power;
	}

	public float getVolatility() {
		return volatility;
	}

	public int getRgb() {
		return rgb;
	}

	/** Success bonus for using a fire above the recipe's requirement; Dị Hỏa is famous for it. */
	public float successBonusPerTier() {
		return this == STRANGE ? 0.12F : 0.07F;
	}

	/** Highest pill grade this fire can refine: tier 1 → grade 2, then one grade per tier up to 5. */
	public int maxPillGrade() {
		return Math.min(5, tier + 1);
	}

	public Text getName() {
		return Text.translatable("item.celestialarts." + key);
	}

	public Text getDescription() {
		return Text.translatable("flame.celestialarts." + key + ".desc");
	}
}
