package com.ngoducduy.celestialarts.effect;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.effect.StatusEffectInstance;

/** Linh Lực – skill damage +{@link #PER_LEVEL} per amplifier level. */
public class SpiritPowerEffect extends StatusEffect {
	public static final float PER_LEVEL = 0.08F;

	public SpiritPowerEffect() {
		super(StatusEffectCategory.BENEFICIAL, 0xB57BFF);
	}

	public static float multiplier(LivingEntity entity) {
		StatusEffectInstance inst = entity.getStatusEffect(com.ngoducduy.celestialarts.registry.ModEffects.SPIRIT_POWER);
		return inst == null ? 1.0F : 1.0F + PER_LEVEL * (inst.getAmplifier() + 1);
	}
}
