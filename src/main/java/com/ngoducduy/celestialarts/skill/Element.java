package com.ngoducduy.celestialarts.skill;

import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Elemental affinity of a skill. Mostly drives colours of effects and UI.
 */
public enum Element {
	SWORD("sword", 0x9FE8FF, 0xFFFFFF, Formatting.AQUA),
	FIRE("fire", 0xFF7A1A, 0xFFE07A, Formatting.GOLD),
	ICE("ice", 0x9BE4FF, 0xE8FBFF, Formatting.AQUA),
	LIGHTNING("lightning", 0xB57BFF, 0xF2E6FF, Formatting.LIGHT_PURPLE),
	WIND("wind", 0xB8FFD9, 0xF0FFF6, Formatting.GREEN),
	EARTH("earth", 0xC69C5B, 0xF2D9A6, Formatting.YELLOW),
	VOID("void", 0x6A1FB0, 0xD24BFF, Formatting.DARK_PURPLE),
	DAO("dao", 0xFFE9A8, 0xFFFFFF, Formatting.WHITE);

	private final String key;
	private final int primary;
	private final int secondary;
	private final Formatting formatting;

	Element(String key, int primary, int secondary, Formatting formatting) {
		this.key = key;
		this.primary = primary;
		this.secondary = secondary;
		this.formatting = formatting;
	}

	public String getKey() {
		return key;
	}

	/** Main effect colour as 0xRRGGBB. */
	public int getPrimary() {
		return primary;
	}

	/** Bright core / highlight colour as 0xRRGGBB. */
	public int getSecondary() {
		return secondary;
	}

	public Formatting getFormatting() {
		return formatting;
	}

	public Text getName() {
		return Text.translatable("element.celestialarts." + key).formatted(formatting);
	}
}
