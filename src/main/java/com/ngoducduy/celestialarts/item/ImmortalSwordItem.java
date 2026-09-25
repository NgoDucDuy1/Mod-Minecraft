package com.ngoducduy.celestialarts.item;

import com.ngoducduy.celestialarts.registry.GlowParticleEffect;
import com.ngoducduy.celestialarts.registry.ModEffects;
import com.ngoducduy.celestialarts.util.SkillFx;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterial;
import net.minecraft.recipe.Ingredient;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Tiên Kiếm – a cultivator's flying sword. Strikes leave sword-qi glints and grant
 * Sword Intent; while held it slowly hums with light.
 */
public class ImmortalSwordItem extends SwordItem {
	public static final ToolMaterial MATERIAL = new ToolMaterial() {
		@Override
		public int getDurability() {
			return 2031;
		}

		@Override
		public float getMiningSpeedMultiplier() {
			return 9.0f;
		}

		@Override
		public float getAttackDamage() {
			return 4.0f;
		}

		@Override
		public int getMiningLevel() {
			return 4;
		}

		@Override
		public int getEnchantability() {
			return 18;
		}

		@Override
		public Ingredient getRepairIngredient() {
			return Ingredient.ofItems(Items.DIAMOND);
		}
	};

	public ImmortalSwordItem(Settings settings) {
		super(MATERIAL, 4, -2.2f, settings);
	}

	@Override
	public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
		if (attacker.getWorld() instanceof ServerWorld sw) {
			SkillFx.swordGlints(sw, target.getBoundingBox().getCenter(), 6, 0.2);
			if (attacker instanceof PlayerEntity && sw.random.nextFloat() < 0.25f) {
				attacker.addStatusEffect(new StatusEffectInstance(ModEffects.SWORD_INTENT, 60, 0, false, false, true));
			}
		}
		return super.postHit(stack, target, attacker);
	}

	@Override
	public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
		if (selected && world instanceof ServerWorld sw && entity instanceof LivingEntity living && sw.random.nextInt(12) == 0) {
			SkillFx.single(sw, GlowParticleEffect.spark(0x9FE8FF, 0.2f, 12),
					living.getPos().add(sw.random.nextGaussian() * 0.4, 1.0 + sw.random.nextDouble() * 0.6, sw.random.nextGaussian() * 0.4),
					new net.minecraft.util.math.Vec3d(0, 0.02, 0));
		}
	}

	@Override
	public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
		tooltip.add(Text.translatable("tooltip.celestialarts.immortal_sword").formatted(Formatting.AQUA, Formatting.ITALIC));
	}
}
