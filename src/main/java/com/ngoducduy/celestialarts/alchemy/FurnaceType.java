package com.ngoducduy.celestialarts.alchemy;

import net.minecraft.text.Text;

/** The three furnace designs; each bends the refining rules a different way. */
public enum FurnaceType {
	/** Ổn Hỏa Lô – wider tolerance, slower damage. */
	STEADY("steady", 1.30F, 0.75F, 1.0F, 1.0F, 0.0F),
	/** Liệt Diễm Lô – hotter, faster, and a quality bonus. */
	FIERCE("fierce", 1.0F, 1.0F, 1.35F, 1.0F, 0.06F),
	/** Tụ Linh Lô – injected qi counts for more and lingers. */
	SPIRIT("spirit", 1.0F, 1.0F, 1.0F, 1.6F, 0.0F);

	private final String key;
	private final float toleranceMul;
	private final float damageMul;
	private final float heatMul;
	private final float qiMul;
	private final float qualityBonus;

	FurnaceType(String key, float toleranceMul, float damageMul, float heatMul, float qiMul, float qualityBonus) {
		this.key = key;
		this.toleranceMul = toleranceMul;
		this.damageMul = damageMul;
		this.heatMul = heatMul;
		this.qiMul = qiMul;
		this.qualityBonus = qualityBonus;
	}

	public String getKey() {
		return key;
	}

	public float getToleranceMul() {
		return toleranceMul;
	}

	public float getDamageMul() {
		return damageMul;
	}

	public float getHeatMul() {
		return heatMul;
	}

	public float getQiMul() {
		return qiMul;
	}

	public float getQualityBonus() {
		return qualityBonus;
	}

	public Text getDescription() {
		return Text.translatable("furnace.celestialarts.type." + key + ".desc");
	}
}
