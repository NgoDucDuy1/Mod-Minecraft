package com.ngoducduy.celestialarts.effect;

import com.ngoducduy.celestialarts.registry.ModDamageTypes;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;

/**
 * Linh Hỏa Thiêu Đốt – spirit fire that keeps burning for a while.
 */
public class QiBurnEffect extends StatusEffect {
	public QiBurnEffect() {
		super(StatusEffectCategory.HARMFUL, 0xFF7A1A);
	}

	@Override
	public boolean canApplyUpdateEffect(int duration, int amplifier) {
		int interval = Math.max(5, 20 >> amplifier);
		return duration % interval == 0;
	}

	@Override
	public void applyUpdateEffect(LivingEntity entity, int amplifier) {
		entity.damage(ModDamageTypes.source(entity.getWorld(), ModDamageTypes.FLAME, null), 1.0f + amplifier);
	}
}
