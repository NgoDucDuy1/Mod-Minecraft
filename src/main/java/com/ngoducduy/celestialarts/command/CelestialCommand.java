package com.ngoducduy.celestialarts.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.ngoducduy.celestialarts.cultivation.Awakening;
import com.ngoducduy.celestialarts.cultivation.Breakthrough;
import com.ngoducduy.celestialarts.cultivation.CultivationStats;
import com.ngoducduy.celestialarts.cultivation.Meditation;
import com.ngoducduy.celestialarts.cultivation.RealmPassives;
import com.ngoducduy.celestialarts.cultivation.SpiritQi;
import com.ngoducduy.celestialarts.cultivation.SpiritRoot;
import com.ngoducduy.celestialarts.cultivation.Stage;
import com.ngoducduy.celestialarts.cultivation.Talent;
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
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
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
 * /celestial stage &lt;0-3&gt; [player]          – sơ / trung / hậu / viên mãn
 * /celestial root &lt;kinds&gt; &lt;grade&gt; [player] – kinds joined with "+", e.g. metal+fire 4
 * /celestial talent &lt;talent&gt; [player]
 * /celestial reroll [player]                – roll root + talent again (with the ceremony)
 * /celestial meditate                       – toggle meditation
 * /celestial spiritqi                       – spiritual qi density here and the nearest vein
 * /celestial chance                         – odds of the next breakthrough / tribulation preview
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
						}))
				.then(CommandManager.literal("stage")
						.requires(src -> src.hasPermissionLevel(2))
						.then(CommandManager.argument("stage", IntegerArgumentType.integer(0, 3))
								.executes(ctx -> stage(ctx, List.of(ctx.getSource().getPlayerOrThrow())))
								.then(CommandManager.argument("player", EntityArgumentType.players())
										.executes(ctx -> stage(ctx, EntityArgumentType.getPlayers(ctx, "player"))))))
				.then(CommandManager.literal("root")
						.requires(src -> src.hasPermissionLevel(2))
						.then(CommandManager.argument("kinds", StringArgumentType.string()).suggests(ROOT_SUGGESTIONS)
								.then(CommandManager.argument("grade", IntegerArgumentType.integer(SpiritRoot.MIN_GRADE, SpiritRoot.MAX_GRADE))
										.executes(ctx -> root(ctx, List.of(ctx.getSource().getPlayerOrThrow())))
										.then(CommandManager.argument("player", EntityArgumentType.players())
												.executes(ctx -> root(ctx, EntityArgumentType.getPlayers(ctx, "player")))))))
				.then(CommandManager.literal("talent")
						.requires(src -> src.hasPermissionLevel(2))
						.then(CommandManager.argument("talent", StringArgumentType.word()).suggests(TALENT_SUGGESTIONS)
								.executes(ctx -> talent(ctx, List.of(ctx.getSource().getPlayerOrThrow())))
								.then(CommandManager.argument("player", EntityArgumentType.players())
										.executes(ctx -> talent(ctx, EntityArgumentType.getPlayers(ctx, "player"))))))
				.then(CommandManager.literal("reroll")
						.requires(src -> src.hasPermissionLevel(2))
						.executes(ctx -> reroll(ctx, List.of(ctx.getSource().getPlayerOrThrow())))
						.then(CommandManager.argument("player", EntityArgumentType.players())
								.executes(ctx -> reroll(ctx, EntityArgumentType.getPlayers(ctx, "player")))))
				.then(CommandManager.literal("meditate")
						.executes(ctx -> {
							Meditation.toggle(ctx.getSource().getPlayerOrThrow());
							return 1;
						}))
				.then(CommandManager.literal("spiritqi")
						.executes(ctx -> spiritQi(ctx)))
				.then(CommandManager.literal("chance")
						.executes(ctx -> chance(ctx))));
	}

	private static final SuggestionProvider<ServerCommandSource> ROOT_SUGGESTIONS = (ctx, builder) -> {
		for (SpiritRoot.Kind k : SpiritRoot.Kind.values()) builder.suggest(k.getKey());
		builder.suggest("metal,fire");
		builder.suggest("wood,water,earth");
		return builder.buildFuture();
	};

	private static final SuggestionProvider<ServerCommandSource> TALENT_SUGGESTIONS = (ctx, builder) -> {
		for (Talent t : Talent.values()) builder.suggest(t.getKey());
		return builder.buildFuture();
	};

	private static int stage(CommandContext<ServerCommandSource> ctx, Collection<ServerPlayerEntity> players) {
		Stage stage = Stage.byIndex(IntegerArgumentType.getInteger(ctx, "stage"));
		for (ServerPlayerEntity p : players) {
			PlayerQi qi = QiHolder.get(p);
			qi.setStage(stage);
			qi.setQi(qi.getMaxQi());
			RealmPassives.apply(p);
			ModPackets.sendSync(p, qi);
		}
		ctx.getSource().sendFeedback(() -> Text.translatable("command.celestialarts.stage_set", stage.getName(), players.size()), true);
		return players.size();
	}

	private static int root(CommandContext<ServerCommandSource> ctx, Collection<ServerPlayerEntity> players) {
		String spec = StringArgumentType.getString(ctx, "kinds");
		int grade = IntegerArgumentType.getInteger(ctx, "grade");
		List<SpiritRoot.Kind> kinds = new ArrayList<>();
		for (String part : spec.split("[+,/]")) {
			if (part.isBlank()) continue;
			SpiritRoot.Kind k = SpiritRoot.Kind.byKey(part.trim().toLowerCase());
			if (k == null) {
				ctx.getSource().sendError(Text.translatable("command.celestialarts.unknown_root", part));
				return 0;
			}
			if (!kinds.contains(k)) kinds.add(k);
		}
		if (kinds.isEmpty() || kinds.size() > 3) {
			ctx.getSource().sendError(Text.translatable("command.celestialarts.root_count"));
			return 0;
		}
		SpiritRoot root = new SpiritRoot(kinds, grade);
		for (ServerPlayerEntity p : players) {
			PlayerQi qi = QiHolder.get(p);
			qi.setRoot(root);
			RealmPassives.apply(p);
			ModPackets.sendSync(p, qi);
			p.sendMessage(Awakening.summary(qi), false);
		}
		ctx.getSource().sendFeedback(() -> Text.translatable("command.celestialarts.root_set", root.getKindsText(), root.getGradeName(), players.size()), true);
		return players.size();
	}

	private static int talent(CommandContext<ServerCommandSource> ctx, Collection<ServerPlayerEntity> players) {
		String key = StringArgumentType.getString(ctx, "talent").toLowerCase();
		Talent talent = null;
		for (Talent t : Talent.values()) {
			if (t.getKey().equals(key)) talent = t;
		}
		if (talent == null) {
			ctx.getSource().sendError(Text.translatable("command.celestialarts.unknown_talent", key));
			return 0;
		}
		for (ServerPlayerEntity p : players) {
			PlayerQi qi = QiHolder.get(p);
			qi.setTalent(talent);
			RealmPassives.apply(p);
			ModPackets.sendSync(p, qi);
			p.sendMessage(Awakening.summary(qi), false);
		}
		Talent finalTalent = talent;
		ctx.getSource().sendFeedback(() -> Text.translatable("command.celestialarts.talent_set", finalTalent.getName(), players.size()), true);
		return players.size();
	}

	private static int reroll(CommandContext<ServerCommandSource> ctx, Collection<ServerPlayerEntity> players) {
		for (ServerPlayerEntity p : players) {
			Awakening.roll(p, p.getServerWorld().getRandom(), true);
		}
		ctx.getSource().sendFeedback(() -> Text.translatable("command.celestialarts.rerolled", players.size()), true);
		return players.size();
	}

	private static int spiritQi(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
		ServerPlayerEntity p = ctx.getSource().getPlayerOrThrow();
		ServerWorld world = p.getServerWorld();
		BlockPos at = p.getBlockPos();
		float density = SpiritQi.density(world, at);
		float biome = SpiritQi.biomeFactor(world.getBiome(at));
		float vein = SpiritQi.veinFactor(world.getSeed(), at.getX(), at.getZ());
		double veinDist = SpiritQi.nearestVeinDistance(world.getSeed(), at.getX(), at.getZ());
		ctx.getSource().sendFeedback(() -> Text.translatable("command.celestialarts.spiritqi",
				String.format("%.2f", density), Text.translatable("spiritqi.celestialarts." + SpiritQi.label(density)),
				String.format("%.2f", biome), String.format("%.2f", vein), (int) veinDist).formatted(Formatting.AQUA), false);
		return 1;
	}

	private static int chance(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
		ServerPlayerEntity p = ctx.getSource().getPlayerOrThrow();
		PlayerQi qi = QiHolder.get(p);
		float density = SpiritQi.density(p.getServerWorld(), p.getBlockPos());
		if (qi.nextBreakthroughIsTribulation()) {
			ctx.getSource().sendFeedback(() -> Text.translatable("command.celestialarts.chance_tribulation",
					CultivationStats.tribulationBolts(qi),
					String.format("%.1f", CultivationStats.tribulationBoltDamage(qi) * qi.getTalent().tribulationDamageMultiplier() / 2.0F)).formatted(Formatting.LIGHT_PURPLE), false);
		} else {
			ctx.getSource().sendFeedback(() -> Text.translatable("command.celestialarts.chance_stage",
					qi.getStage().next().getName(), Math.round(CultivationStats.breakthroughChance(qi, density) * 100)).formatted(Formatting.AQUA), false);
		}
		return 1;
	}

	private static int info(CommandContext<ServerCommandSource> ctx, Collection<ServerPlayerEntity> players) {
		for (ServerPlayerEntity p : players) {
			PlayerQi qi = QiHolder.get(p);
			ctx.getSource().sendFeedback(() -> Text.literal("")
					.append(p.getDisplayName()).append(Text.literal(" — ").formatted(Formatting.GRAY))
					.append(qi.getRealm().getName()).append(" ").append(qi.getStage().getName())
					.append(Text.literal(String.format("  Qi %.0f/%.0f  Exp %d/%s  Apt %d  Skills %d",
							qi.getQi(), qi.getMaxQi(), qi.getExp(),
							qi.getExpForBreakthrough() < 0 ? "MAX" : String.valueOf(qi.getExpForBreakthrough()),
							CultivationStats.aptitude(qi),
							qi.getLearned().size())).formatted(Formatting.AQUA)), false);
			ctx.getSource().sendFeedback(() -> Awakening.summary(qi), false);
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
					else com.ngoducduy.celestialarts.skill.SkillManager.forget(p, s.getId(), false, false);
				}
			} else if (learn) {
				qi.learn(id);
			} else {
				com.ngoducduy.celestialarts.skill.SkillManager.forget(p, id, false, false);
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
