package com.ngoducduy.celestialarts.cultivation;

import net.minecraft.text.Text;

/**
 * Minor stage (tiểu cảnh giới) inside a realm: sơ kỳ → trung kỳ → hậu kỳ → viên mãn.
 * Advancing a stage is a "small breakthrough" that can fail; leaving {@link #PEAK} for the next
 * realm's {@link #EARLY} is a "great breakthrough" that summons a heavenly tribulation.
 */
public enum Stage {
	EARLY("early"),
	MIDDLE("middle"),
	LATE("late"),
	PEAK("peak");

	private final String key;

	Stage(String key) {
		this.key = key;
	}

	public String getKey() {
		return key;
	}

	public int getIndex() {
		return ordinal();
	}

	public boolean isPeak() {
		return this == PEAK;
	}

	public Stage next() {
		return isPeak() ? this : values()[ordinal() + 1];
	}

	public Stage previous() {
		return this == EARLY ? this : values()[ordinal() - 1];
	}

	public Text getName() {
		return Text.translatable("stage.celestialarts." + key);
	}

	public static Stage byIndex(int index) {
		if (index <= 0) return EARLY;
		if (index >= values().length) return PEAK;
		return values()[index];
	}

	/**
	 * Base chance that a small breakthrough out of this stage succeeds, before spirit-root,
	 * talent and spiritual-qi modifiers. The peak stage has no small breakthrough (it leads to the
	 * tribulation instead).
	 */
	public float baseBreakthroughChance() {
		return switch (this) {
			case EARLY -> 0.85F;
			case MIDDLE -> 0.75F;
			case LATE -> 0.65F;
			case PEAK -> 1.0F;
		};
	}
}
