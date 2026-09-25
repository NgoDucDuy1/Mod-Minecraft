package com.ngoducduy.celestialarts.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.ngoducduy.celestialarts.cultivation.Breakthrough;
import com.ngoducduy.celestialarts.cultivation.PlayerQi;
import com.ngoducduy.celestialarts.cultivation.QiHolder;
import com.ngoducduy.celestialarts.cultivation.Realm;
import com.ngoducduy.celestialarts.network.ModPackets;
import com.ngoducduy.celestialarts.skill.Skill;
import com.ngoducduy.celestialarts.skill.SkillRegistry;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.Collection;
import java.util.List;

/**
 * {@code /celestial} admin & debug command.
 *
 * <pre>
 * /celestial info [player]
 * /celestial learn (all|&lt;skill&gt;) [player]
 * /celestial forget (all|&lt;skill&gt;) [player]
 * /celestial realm &lt;1-6&gt; [player]
 * /celestial qi &lt;amount&gt; [player]
 * /celestial exp &lt;amount&gt; [player]
 * /celestial breakthrough [player]
 * </pre>
 */
public final class CelestialCommand {
	private static final SuggestionProvider<ServerCommandSource> SKILL_SUGGESTIONS = (ctx, builder) -> {
		builder.suggest("all");
		return CommandSource.suggestIdentifiers(SkillRegistry.all().stream().map(Skill::getId), builder);
	};

	private CelestialCommand() {
	}

	public static void register() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> register(dispatcher));
	}

	private static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
		dispatcher.register(CommandManager.literal("celestial")
				.then(CommandManager.literal("info")
						.executes(ctx -> info(ctx, List.of(ctx.getSource().getPlayerOrThrow())))
						.then(CommandManager.argument("player", EntityArgumentType.players())
								.requires(src -> src.hasPermissionLevel(2))
								.executes(ctx -> info(ctx, EntityArgumentType.getPlayers(ctx, "player")))))
				.then(CommandManager.literal("learn")
						.requires(src -> src.hasPermissionLevel(2))
						.then(CommandManager.argument("skill", IdentifierArgumentType.identifier()).suggests(SKILL_SUGGESTIONS)
								.executes(ctx -> learn(ctx, List.of(ctx.getSource().getPlayerOrThrow()), true))
								.then(CommandManager.argument("player", EntityArgumentType.players())
										.executes(ctx -> learn(ctx, EntityArgumentType.getPlayers(ctx, "player"), true)))))
				.then(CommandManager.literal("forget")
						.requires(src -> src.hasPermissionLevel(2))
						.then(CommandManager.argument("skill", IdentifierArgumentType.identifier()).suggests(SKILL_SUGGESTIONS)
								.executes(ctx -> learn(ctx, List.of(ctx.getSource().getPlayerOrThrow()), false))
								.then(CommandManager.argument("player", EntityArgumentType.players())
										.executes(ctx -> learn(ctx, EntityArgumentType.getPlayers(ctx, "player"), false)))))
				.then(CommandManager.literal("realm")
						.requires(src -> src.hasPermissionLevel(2))
						.then(CommandManager.argument("level", IntegerArgumentType.integer(1, 6))
								.executes(ctx -> realm(ctx, List.of(ctx.getSource().getPlayerOrThrow())))
								.then(CommandManager.argument("player", EntityArgumentType.players())
										.executes(ctx -> realm(ctx, EntityArgumentType.getPlayers(ctx, "player"))))))
				.then(CommandManager.literal("qi")
						.requires(src -> src.hasPermissionLevel(2))
						.then(CommandManager.argument("amount", FloatArgumentType.floatArg(0))
								.executes(ctx -> qi(ctx, List.of(ctx.getSource().getPlayerOrThrow())))
								.then(CommandManager.argument("player", EntityArgumentType.players())
										.executes(ctx -> qi(ctx, EntityArgumentType.getPlayers(ctx, "player"))))))
				.then(CommandManager.literal("exp")
						.requires(src -> src.hasPermissionLevel(2))
						.then(CommandManager.argument("amount", IntegerArgumentType.integer(0))
								.executes(ctx -> exp(ctx, List.of(ctx.getSource().getPlayerOrThrow())))
								.then(CommandManager.argument("player", EntityArgumentType.players())
										.executes(ctx -> exp(ctx, EntityArgumentType.getPlayers(ctx, "player"))))))
				.then(CommandManager.literal("breakthrough")
						.executes(ctx -> {
							Breakthrough.tryBreakthrough(ctx.getSource().getPlayerOrThrow());
							return 1;
						})));
	}

	private static int info(CommandContext<ServerCommandSource> ctx, Collection<ServerPlayerEntity> players) {
		for (ServerPlayerEntity p : players) {
			PlayerQi qi = QiHolder.get(p);
			ctx.getSource().sendFeedback(() -> Text.literal("")
					.append(p.getDisplayName()).append(Text.literal(" — ").formatted(Formatting.GRAY))
					.append(qi.getRealm().getName())
					.append(Text.literal(String.format("  Qi %.0f/%.0f  Exp %d/%s  Skills %d",
							qi.getQi(), qi.getMaxQi(), qi.getExp(),
							qi.getExpForBreakthrough() < 0 ? "MAX" : String.valueOf(qi.getExpForBreakthrough()),
							qi.getLearned().size())).formatted(Formatting.AQUA)), false);
		}
		return players.size();
	}

	private static int learn(CommandContext<ServerCommandSource> ctx, Collection<ServerPlayerEntity> players, boolean learn) throws CommandSyntaxException {
		Identifier id = IdentifierArgumentType.getIdentifier(ctx, "skill");
		boolean all = id.getPath().equals("all");
		if (!all && !SkillRegistry.exists(id)) {
			ctx.getSource().sendError(Text.translatable("command.celestialarts.unknown_skill", id.toString()));
			return 0;
		}
		for (ServerPlayerEntity p : players) {
			PlayerQi qi = QiHolder.get(p);
			if (all) {
				for (Skill s : SkillRegistry.all()) {
					if (learn) qi.learn(s.getId());
					else qi.forget(s.getId());
				}
			} else if (learn) {
				qi.learn(id);
			} else {
				qi.forget(id);
			}
			ModPackets.sendSync(p, qi);
		}
		ctx.getSource().sendFeedback(() -> Text.translatable(learn ? "command.celestialarts.learned" : "command.celestialarts.forgotten",
				all ? "all" : id.toString(), players.size()), true);
		return players.size();
	}

	private static int realm(CommandContext<ServerCommandSource> ctx, Collection<ServerPlayerEntity> players) {
		int level = IntegerArgumentType.getInteger(ctx, "level");
		Realm realm = Realm.byLevel(level);
		for (ServerPlayerEntity p : players) {
			PlayerQi qi = QiHolder.get(p);
			qi.setRealm(realm);
			qi.setQi(qi.getMaxQi());
			ModPackets.sendSync(p, qi);
		}
		ctx.getSource().sendFeedback(() -> Text.translatable("command.celestialarts.realm_set", realm.getName(), players.size()), true);
		return players.size();
	}

	private static int qi(CommandContext<ServerCommandSource> ctx, Collection<ServerPlayerEntity> players) {
		float amount = FloatArgumentType.getFloat(ctx, "amount");
		for (ServerPlayerEntity p : players) {
			PlayerQi qi = QiHolder.get(p);
			qi.setQi(amount);
			ModPackets.sendSync(p, qi);
		}
		ctx.getSource().sendFeedback(() -> Text.translatable("command.celestialarts.qi_set", (int) amount, players.size()), true);
		return players.size();
	}

	private static int exp(CommandContext<ServerCommandSource> ctx, Collection<ServerPlayerEntity> players) {
		int amount = IntegerArgumentType.getInteger(ctx, "amount");
		for (ServerPlayerEntity p : players) {
			PlayerQi qi = QiHolder.get(p);
			qi.addExp(amount);
			ModPackets.sendSync(p, qi);
		}
		ctx.getSource().sendFeedback(() -> Text.translatable("command.celestialarts.exp_added", amount, players.size()), true);
		return players.size();
	}
}
