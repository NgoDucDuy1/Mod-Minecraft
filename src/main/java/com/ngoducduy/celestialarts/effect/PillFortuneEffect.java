package com.ngoducduy.celestialarts.effect;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.effect.StatusEffectInstance;

/** Đan Vận – each amplifier level adds {@link #CHANCE_PER_LEVEL} to the breakthrough chance. */
public class PillFortuneEffect extends StatusEffect {
	public static final float CHANCE_PER_LEVEL = 0.06F;

	public PillFortuneEffect() {
		super(StatusEffectCategory.BENEFICIAL, 0xFFE08A);
	}

	public static float bonus(LivingEntity entity) {
		StatusEffectInstance inst = entity.getStatusEffect(com.ngoducduy.celestialarts.registry.ModEffects.PILL_FORTUNE);
		return inst == null ? 0.0F : CHANCE_PER_LEVEL * (inst.getAmplifier() + 1);
	}
}
