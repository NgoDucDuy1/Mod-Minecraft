package com.ngoducduy.celestialarts.item;

import com.ngoducduy.celestialarts.cultivation.PlayerQi;
import com.ngoducduy.celestialarts.cultivation.QiHolder;
import com.ngoducduy.celestialarts.network.ModPackets;
import com.ngoducduy.celestialarts.registry.GlowParticleEffect;
import com.ngoducduy.celestialarts.registry.ModSounds;
import com.ngoducduy.celestialarts.util.SkillFx;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Linh Thạch – absorbed over one second to restore Qi and add a little cultivation.
 */
public class SpiritStoneItem extends Item {
	private final float qi;
	private final int exp;
	private final int color;

	public SpiritStoneItem(float qi, int exp, int color, Settings settings) {
		super(settings);
		this.qi = qi;
		this.exp = exp;
		this.color = color;
	}

	@Override
	public UseAction getUseAction(ItemStack stack) {
		return UseAction.BOW;
	}

	@Override
	public int getMaxUseTime(ItemStack stack) {
		return 24;
	}

	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
		user.setCurrentHand(hand);
		return TypedActionResult.consume(user.getStackInHand(hand));
	}

	@Override
	public void usageTick(World world, LivingEntity user, ItemStack stack, int remainingUseTicks) {
		if (world instanceof ServerWorld sw && remainingUseTicks % 3 == 0) {
			SkillFx.helix(sw, GlowParticleEffect.glow(color, 0.35f, 10), user.getPos(), 0.7, 1.8, 6, 1, remainingUseTicks * 0.5);
		}
	}

	@Override
	public ItemStack finishUsing(ItemStack stack, World world, LivingEntity user) {
		if (!(user instanceof PlayerEntity player)) return stack;
		if (!world.isClient) {
			PlayerQi data = QiHolder.get(player);
			data.addQi(qi);
			data.addExp(exp);
			if (!player.getAbilities().creativeMode) stack.decrement(1);
			if (player instanceof ServerPlayerEntity sp) ModPackets.sendSync(sp, data);
			if (world instanceof ServerWorld sw) {
				SkillFx.glowBurst(sw, player.getPos().add(0, 1.2, 0), color, 24, 0.7f, 0.15);
				sw.playSound(null, player.getBlockPos(), ModSounds.SPIRIT_STONE, SoundCategory.PLAYERS, 1.0f, 1.0f);
			}
		}
		return stack;
	}

	@Override
	public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
		tooltip.add(Text.translatable("tooltip.celestialarts.restores_qi", (int) qi).formatted(Formatting.AQUA));
		tooltip.add(Text.translatable("tooltip.celestialarts.grants_exp", exp).formatted(Formatting.LIGHT_PURPLE));
	}
}
