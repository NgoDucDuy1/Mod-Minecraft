package com.ngoducduy.celestialarts.effect;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

/**
 * Trấn Áp – crushed beneath the will of the heavens (Thiên Đạo Chi Thủ). The entity is pressed
 * towards the ground: it cannot jump ({@code LivingEntityMixin}), moves slower and attacks slower per
 * amplifier, is dragged down hard while airborne (the pull overpowers elytra gliding, levitation and
 * creative-style flight) and
 * takes no fall damage from being forced down. Amplifier 0–3 scales with how close the victim is to
 * the centre of the palm.
 */
public class SuppressedEffect extends StatusEffect {
	public SuppressedEffect() {
		super(StatusEffectCategory.HARMFUL, 0xFFD86B);
		addAttributeModifier(EntityAttributes.GENERIC_MOVEMENT_SPEED, "3f6c1b8e-9a1d-4d7c-8f0e-6b2a9c4d1e10", -0.25D, EntityAttributeModifier.Operation.MULTIPLY_TOTAL);
		addAttributeModifier(EntityAttributes.GENERIC_ATTACK_SPEED, "3f6c1b8e-9a1d-4d7c-8f0e-6b2a9c4d1e11", -0.20D, EntityAttributeModifier.Operation.MULTIPLY_TOTAL);
	}

	@Override
	public boolean canApplyUpdateEffect(int duration, int amplifier) {
		return true;
	}

	@Override
	public void applyUpdateEffect(LivingEntity entity, int amplifier) {
		Vec3d v = entity.getVelocity();
		if (!entity.isOnGround()) {
			// Drag airborne victims down – the pressure grows with the amplifier and beats elytra/flight.
			double pull = 0.12D + 0.06D * amplifier;
			entity.setVelocity(v.x * 0.9D, Math.min(v.y, 0.0D) - pull, v.z * 0.9D);
			entity.velocityModified = true;
		} else if (v.y > 0.0D) {
			entity.setVelocity(v.x, 0.0D, v.z);
			entity.velocityModified = true;
		}
		// Being forced down must not kill through fall damage – the slam itself deals the damage.
		entity.fallDistance = 0.0F;
		if (entity instanceof PlayerEntity player && !player.isCreative() && !player.isSpectator() && player.getAbilities().flying) {
			player.getAbilities().flying = false;
			player.sendAbilitiesUpdate();
		}
	}
}
