package com.ngoducduy.celestialarts.alchemy;

import com.ngoducduy.celestialarts.cultivation.PlayerQi;
import com.ngoducduy.celestialarts.cultivation.QiHolder;
import com.ngoducduy.celestialarts.cultivation.SpiritRoot;
import com.ngoducduy.celestialarts.network.ModPackets;
import com.ngoducduy.celestialarts.registry.ModEffects;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;
import java.util.function.Supplier;

/**
 * What a pill does when swallowed. {@code strength} is the quality multiplier of the individual
 * pill (Hạ 0.8 … Cực 1.6): it scales amounts and durations, never amplifiers.
 */
public interface PillEffect {
	void apply(ServerPlayerEntity player, PlayerQi qi, float strength);

	Text describe();

	static PillEffect qi(float amount) {
		return new PillEffect() {
			public void apply(ServerPlayerEntity player, PlayerQi qi, float strength) {
				qi.addQi(amount * strength);
			}

			public Text describe() {
				return Text.translatable("tooltip.celestialarts.restores_qi", (int) amount).formatted(Formatting.AQUA);
			}
		};
	}

	static PillEffect exp(int amount) {
		return new PillEffect() {
			public void apply(ServerPlayerEntity player, PlayerQi qi, float strength) {
				qi.addExp(Math.round(amount * strength));
			}

			public Text describe() {
				return Text.translatable("tooltip.celestialarts.grants_exp", amount).formatted(Formatting.LIGHT_PURPLE);
			}
		};
	}

	static PillEffect heal(float amount) {
		return new PillEffect() {
			public void apply(ServerPlayerEntity player, PlayerQi qi, float strength) {
				player.heal(amount * strength);
			}

			public Text describe() {
				return Text.translatable("pill.celestialarts.effect.heal", (int) amount).formatted(Formatting.RED);
			}
		};
	}

	static PillEffect status(Supplier<StatusEffect> effect, int duration, int amplifier) {
		return new PillEffect() {
			public void apply(ServerPlayerEntity player, PlayerQi qi, float strength) {
				player.addStatusEffect(new StatusEffectInstance(effect.get(), Math.round(duration * strength), amplifier, false, true, true));
			}

			public Text describe() {
				Text name = effect.get().getName();
				String amp = amplifier > 0 ? " " + toRoman(amplifier + 1) : "";
				return Text.translatable("pill.celestialarts.effect.status", name, amp, duration / 1200, (duration / 20) % 60).formatted(Formatting.GREEN);
			}
		};
	}

	/** Removes every harmful status effect. */
	static PillEffect cleanse() {
		return new PillEffect() {
			public void apply(ServerPlayerEntity player, PlayerQi qi, float strength) {
				List<StatusEffectInstance> bad = player.getStatusEffects().stream().filter(i -> !i.getEffectType().isBeneficial()).toList();
				for (StatusEffectInstance i : bad) player.removeStatusEffect(i.getEffectType());
			}

			public Text describe() {
				return Text.translatable("pill.celestialarts.effect.cleanse").formatted(Formatting.GREEN);
			}
		};
	}

	/** Ends tẩu hỏa nhập ma. */
	static PillEffect cureDeviation() {
		return new PillEffect() {
			public void apply(ServerPlayerEntity player, PlayerQi qi, float strength) {
				player.removeStatusEffect(ModEffects.QI_DEVIATION);
			}

			public Text describe() {
				return Text.translatable("pill.celestialarts.effect.cure_deviation").formatted(Formatting.GREEN);
			}
		};
	}

	/** Tẩy tủy: a chance to raise the spirit root one grade (up to 5). */
	static PillEffect rootUpgrade(float chance) {
		return new PillEffect() {
			public void apply(ServerPlayerEntity player, PlayerQi qi, float strength) {
				SpiritRoot root = qi.getRoot();
				float c = Math.min(0.95F, chance * strength);
				if (root != null && root.getGrade() < SpiritRoot.MAX_GRADE && player.getRandom().nextFloat() < c) {
					qi.setRoot(new SpiritRoot(root.getKinds(), root.getGrade() + 1));
					qi.markDirty();
					ModPackets.sendSync(player, QiHolder.get(player));
					player.sendMessage(Text.translatable("message.celestialarts.root_upgraded", root.getGrade() + 1).formatted(Formatting.GOLD), false);
				} else {
					player.sendMessage(Text.translatable("message.celestialarts.root_unchanged").formatted(Formatting.GRAY), false);
				}
			}

			public Text describe() {
				return Text.translatable("pill.celestialarts.effect.root_upgrade", Math.round(chance * 100)).formatted(Formatting.GOLD);
			}
		};
	}

	static String toRoman(int n) {
		return switch (n) {
			case 1 -> "I";
			case 2 -> "II";
			case 3 -> "III";
			case 4 -> "IV";
			case 5 -> "V";
			default -> String.valueOf(n);
		};
	}
}
