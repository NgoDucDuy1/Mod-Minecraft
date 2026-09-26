package com.ngoducduy.celestialarts.alchemy;

import com.ngoducduy.celestialarts.skill.Element;
import net.minecraft.text.Text;

import java.util.List;

/** One pill: archetype key, grade 1–5, its element (decides which herbs form it) and what it does when eaten. */
public record Pill(String key, int grade, Element element, int rgb, List<PillEffect> effects) {
	public String itemKey() {
		return "pill_" + key + "_" + grade;
	}

	public Text getDescription() {
		return Text.translatable("pill.celestialarts." + key + ".desc");
	}

	public Text getGradeName() {
		return Text.translatable("herb.celestialarts.grade." + grade).formatted(Herb.gradeFormatting(grade));
	}
}
