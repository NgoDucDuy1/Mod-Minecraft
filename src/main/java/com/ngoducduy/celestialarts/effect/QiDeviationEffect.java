package com.ngoducduy.celestialarts.effect;

import com.ngoducduy.celestialarts.registry.GlowParticleEffect;
import com.ngoducduy.celestialarts.registry.ModParticles;
import com.ngoducduy.celestialarts.util.SkillFx;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;

/**
 * Tẩu hỏa nhập ma – qi running wild after a failed breakthrough. While it lasts:
 * <ul>
 *   <li>no qi regenerates and no skill can be cast ({@code SkillManager}),</li>
 *   <li>meditation yields nothing – one can only sit and endure,</li>
 *   <li>the body is weakened (−30% speed, −4 attack) and burns from the inside: 1 damage every
 *       3 s that never kills (stops at half a heart),</li>
 *   <li>black-red qi leaks from the meridians (particles) and the world darkens red at the edges
 *       for the victim ({@code ScreenOverlay}).</li>
 * </ul>
 * Amplifier 1 (a failed tribulation) doubles the internal burn.
 */
public class QiDeviationEffect extends StatusEffect {
	public QiDeviationEffect() {
		super(StatusEffectCategory.HARMFUL, 0x6A0B0B);
		addAttributeModifier(EntityAttributes.GENERIC_MOVEMENT_SPEED, "7d2a4c1e-3b5f-4a8e-9c0d-1e2f3a4b5c01", -0.30D, EntityAttributeModifier.Operation.MULTIPLY_TOTAL);
		addAttributeModifier(EntityAttributes.GENERIC_ATTACK_DAMAGE, "7d2a4c1e-3b5f-4a8e-9c0d-1e2f3a4b5c02", -4.0D, EntityAttributeModifier.Operation.ADDITION);
	}

	@Override
	public boolean canApplyUpdateEffect(int duration, int amplifier) {
		return true;
	}

	@Override
	public void applyUpdateEffect(LivingEntity entity, int amplifier) {
		if (!(entity.getWorld() instanceof ServerWorld world)) return;
		int age = entity.age;
		int burnEvery = amplifier >= 1 ? 30 : 60;
		if (age % burnEvery == 0 && entity.getHealth() > 1.0F) {
			float dmg = Math.min(1.0F, entity.getHealth() - 1.0F);
			if (dmg > 0) entity.damage(world.getDamageSources().create(DamageTypes.MAGIC), dmg);
		}
		if (age % 4 == 0) {
			Vec3d p = entity.getPos().add((world.random.nextDouble() - 0.5) * 0.8, world.random.nextDouble() * entity.getHeight(), (world.random.nextDouble() - 0.5) * 0.8);
			SkillFx.single(world, GlowParticleEffect.glow(world.random.nextBoolean() ? 0x8A0F0F : 0x1A0A0A, 0.45F, 18), p, new Vec3d(0, 0.04, 0));
		}
		if (age % 10 == 0) {
			SkillFx.single(world, ModParticles.VOID_SMOKE, entity.getPos().add(0, entity.getHeight() * 0.5, 0), new Vec3d((world.random.nextDouble() - 0.5) * 0.05, 0.06, (world.random.nextDouble() - 0.5) * 0.05));
		}
	}
}
