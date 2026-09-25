package com.ngoducduy.celestialarts.effect;

import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;

/**
 * Kiếm Ý – after using sword arts the cultivator's strikes become sharper.
 */
public class SwordIntentEffect extends StatusEffect {
	public SwordIntentEffect() {
		super(StatusEffectCategory.BENEFICIAL, 0x9FE8FF);
		addAttributeModifier(EntityAttributes.GENERIC_ATTACK_DAMAGE, "c8c3a4f0-2c8e-4a4e-9d0a-5b3f1e7d1a03", 2.0D, EntityAttributeModifier.Operation.ADDITION);
		addAttributeModifier(EntityAttributes.GENERIC_ATTACK_SPEED, "c8c3a4f0-2c8e-4a4e-9d0a-5b3f1e7d1a04", 0.25D, EntityAttributeModifier.Operation.MULTIPLY_TOTAL);
	}
}
