package com.ngoducduy.celestialarts.item;

import com.ngoducduy.celestialarts.alchemy.Herb;
import com.ngoducduy.celestialarts.alchemy.Pill;
import com.ngoducduy.celestialarts.alchemy.PillEffect;
import com.ngoducduy.celestialarts.alchemy.PillQuality;
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
import net.minecraft.nbt.NbtCompound;
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

/** A refined pill. Its {@link PillQuality} (NBT) scales every effect. */
public class PillItem extends Item {
	private final Pill pill;

	public PillItem(Pill pill, Settings settings) {
		super(settings);
		this.pill = pill;
	}

	public Pill getPill() {
		return pill;
	}

	public static PillQuality qualityOf(ItemStack stack) {
		NbtCompound nbt = stack.getNbt();
		return nbt != null && nbt.contains(PillQuality.NBT_KEY) ? PillQuality.byIndex(nbt.getInt(PillQuality.NBT_KEY)) : PillQuality.MEDIUM;
	}

	public static ItemStack withQuality(ItemStack stack, PillQuality quality) {
		stack.getOrCreateNbt().putInt(PillQuality.NBT_KEY, quality.ordinal());
		return stack;
	}

	@Override
	public Text getName(ItemStack stack) {
		return Text.translatable(getTranslationKey(stack)).formatted(Herb.gradeFormatting(pill.grade()));
	}

	@Override
	public boolean hasGlint(ItemStack stack) {
		return qualityOf(stack) == PillQuality.SUPREME;
	}

	@Override
	public UseAction getUseAction(ItemStack stack) {
		return UseAction.EAT;
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
		if (world instanceof ServerWorld sw && remainingUseTicks % 4 == 0) {
			SkillFx.helix(sw, GlowParticleEffect.glow(pill.rgb(), 0.3f, 10), user.getPos(), 0.6, 1.6, 5, 1, remainingUseTicks * 0.5);
		}
	}

	@Override
	public ItemStack finishUsing(ItemStack stack, World world, LivingEntity user) {
		if (!(user instanceof ServerPlayerEntity player)) return stack;
		PlayerQi qi = QiHolder.get(player);
		float strength = qualityOf(stack).getStrength();
		for (PillEffect effect : pill.effects()) effect.apply(player, qi, strength);
		qi.markDirty();
		ModPackets.sendSync(player, qi);
		if (!player.getAbilities().creativeMode) stack.decrement(1);
		if (world instanceof ServerWorld sw) {
			SkillFx.glowBurst(sw, player.getPos().add(0, 1.2, 0), pill.rgb(), 20, 0.6f, 0.14);
			sw.playSound(null, player.getBlockPos(), ModSounds.SPIRIT_STONE, SoundCategory.PLAYERS, 0.9f, 1.15f);
		}
		return stack;
	}

	@Override
	public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
		PillQuality q = qualityOf(stack);
		tooltip.add(Text.translatable("pill.celestialarts.tooltip.grade", pill.getGradeName(), pill.element().getName()).formatted(Formatting.GRAY));
		tooltip.add(Text.translatable("pill.celestialarts.tooltip.quality", q.getName(), String.format("%.2f", q.getStrength())).formatted(Formatting.GRAY));
		tooltip.add(pill.getDescription().copy().formatted(Formatting.DARK_AQUA, Formatting.ITALIC));
		for (PillEffect effect : pill.effects()) tooltip.add(Text.literal(" ").append(effect.describe()));
	}
}
