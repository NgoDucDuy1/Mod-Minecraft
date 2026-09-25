package com.ngoducduy.celestialarts.item;

import com.ngoducduy.celestialarts.cultivation.PlayerQi;
import com.ngoducduy.celestialarts.cultivation.QiHolder;
import com.ngoducduy.celestialarts.network.FxData;
import com.ngoducduy.celestialarts.network.FxType;
import com.ngoducduy.celestialarts.network.ModPackets;
import com.ngoducduy.celestialarts.registry.ModSounds;
import com.ngoducduy.celestialarts.skill.Skill;
import com.ngoducduy.celestialarts.registry.ModAdvancements;
import com.ngoducduy.celestialarts.skill.SkillRegistry;

import com.ngoducduy.celestialarts.util.SkillFx;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Rarity;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Supplier;

/**
 * Bí Tịch – a manual that teaches one skill when used. The skill is looked up lazily
 * because items are registered after skills but before the registry is frozen.
 */
public class SkillScrollItem extends Item {
	private final Supplier<Skill> skill;

	public SkillScrollItem(Supplier<Skill> skill, Settings settings) {
		super(settings);
		this.skill = skill;
	}

	public Skill getSkill() {
		return skill.get();
	}

	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
		ItemStack stack = user.getStackInHand(hand);
		Skill s = getSkill();
		if (world.isClient) {
			return TypedActionResult.success(stack, true);
		}
		PlayerQi qi = QiHolder.get(user);
		if (qi.hasLearned(s.getId())) {
			user.sendMessage(Text.translatable("message.celestialarts.already_learned", s.getName()).formatted(Formatting.YELLOW), true);
			return TypedActionResult.fail(stack);
		}
		qi.learn(s.getId());
		if (!user.getAbilities().creativeMode) stack.decrement(1);

		user.sendMessage(Text.translatable("message.celestialarts.learned", s.getName()).formatted(Formatting.GOLD), false);
		if (qi.getRealm().getLevel() < s.getRealm().getLevel()) {
			user.sendMessage(Text.translatable("message.celestialarts.learned_locked", s.getRealm().getName()).formatted(Formatting.GRAY), false);
		}
		if (user instanceof ServerPlayerEntity sp) {
			ModPackets.sendSync(sp, qi);
			ModAdvancements.onSkillLearned(sp, s.getId());
			if (qi.getLearned().size() >= SkillRegistry.all().size()) ModAdvancements.onAllSkillsLearned(sp);
			ModPackets.sendFx(sp, FxData.follow(FxType.RUNE_ORBIT, sp.getId(), sp.getPos(), s.getElement().getPrimary(), 1.4f, 50));
			ModPackets.sendFx(sp, FxData.follow(FxType.QI_AURA, sp.getId(), sp.getPos(), s.getElement().getPrimary(), 0.8f, 40));
		}
		if (world instanceof ServerWorld sw) {
			SkillFx.runes(sw, user.getPos().add(0, 1, 0), 16, 0.8);
			sw.playSound(null, user.getBlockPos(), ModSounds.LEARN_SKILL, SoundCategory.PLAYERS, 1.0f, 1.0f);
		}
		return TypedActionResult.consume(stack);
	}

	@Override
	public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
		Skill s = getSkill();
		tooltip.add(Text.translatable("tooltip.celestialarts.teaches", s.getName()).formatted(Formatting.GRAY));
		tooltip.add(Text.translatable("tooltip.celestialarts.realm", s.getRealm().getName()).formatted(Formatting.GRAY));
		tooltip.add(Text.translatable("tooltip.celestialarts.type", s.getType().getName(), s.getElement().getName()).formatted(Formatting.DARK_GRAY));
		tooltip.add(Text.translatable("tooltip.celestialarts.cost", (int) s.getQiCost(), String.format("%.1f", s.getCooldownTicks() / 20.0)).formatted(Formatting.DARK_AQUA));
		tooltip.add(s.getDescription().copy().formatted(Formatting.ITALIC, Formatting.DARK_GRAY));
	}

	@Override
	public Rarity getRarity(ItemStack stack) {
		int lvl = getSkill().getRealm().getLevel();
		if (lvl >= 6) return Rarity.EPIC;
		if (lvl >= 4) return Rarity.RARE;
		if (lvl >= 3) return Rarity.UNCOMMON;
		return Rarity.COMMON;
	}

	@Override
	public boolean hasGlint(ItemStack stack) {
		return getSkill().getRealm().getLevel() >= 5;
	}
}
