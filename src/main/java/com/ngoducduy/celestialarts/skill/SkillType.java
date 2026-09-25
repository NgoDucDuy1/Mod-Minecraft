package com.ngoducduy.celestialarts.skill;

import net.minecraft.text.Text;

/**
 * Gameplay category of a skill; shown in tooltips and used for balancing rules
 * (e.g. only one channelled skill at a time).
 */
public enum SkillType {
	PROJECTILE("projectile"),
	MELEE("melee"),
	AREA("area"),
	BEAM("beam"),
	CHANNEL("channel"),
	BUFF("buff"),
	MOVEMENT("movement"),
	SUMMON("summon"),
	FORMATION("formation"),
	ULTIMATE("ultimate");

	private final String key;

	SkillType(String key) {
		this.key = key;
	}

	public String getKey() {
		return key;
	}

	public Text getName() {
		return Text.translatable("skilltype.celestialarts." + key);
	}
}
