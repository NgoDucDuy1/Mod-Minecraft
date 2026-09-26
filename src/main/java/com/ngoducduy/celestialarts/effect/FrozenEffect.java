package com.ngoducduy.celestialarts.effect;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;

/**
 * Băng Phong – the entity is locked inside ice. Movement speed is removed entirely
 * through an attribute modifier and jumping is blocked by {@code LivingEntityMixin}.
 */
public class FrozenEffect extends StatusEffect {
	public FrozenEffect() {
		super(StatusEffectCategory.HARMFUL, 0x9BE4FF);
		addAttributeModifier(EntityAttributes.GENERIC_MOVEMENT_SPEED, "c8c3a4f0-2c8e-4a4e-9d0a-5b3f1e7d1a01", -1.0D, EntityAttributeModifier.Operation.MULTIPLY_TOTAL);
		addAttributeModifier(EntityAttributes.GENERIC_ATTACK_SPEED, "c8c3a4f0-2c8e-4a4e-9d0a-5b3f1e7d1a02", -0.8D, EntityAttributeModifier.Operation.MULTIPLY_TOTAL);
	}

	@Override
	public boolean canApplyUpdateEffect(int duration, int amplifier) {
		return true;
	}

	@Override
	public void applyUpdateEffect(LivingEntity entity, int amplifier) {
		// Keep the entity pinned in place and visually frosted.
		entity.setVelocity(entity.getVelocity().multiply(0.0, entity.getVelocity().y < 0 ? 1.0 : 0.0, 0.0));
		entity.setFrozenTicks(Math.max(entity.getFrozenTicks(), 140));
	}
}
