package com.ngoducduy.celestialarts.gametest;

import com.ngoducduy.celestialarts.CelestialArts;
import com.ngoducduy.celestialarts.cultivation.Breakthrough;
import com.ngoducduy.celestialarts.cultivation.CultivationStats;
import com.ngoducduy.celestialarts.cultivation.Meditation;
import com.ngoducduy.celestialarts.cultivation.PlayerQi;
import com.ngoducduy.celestialarts.cultivation.QiHolder;
import com.ngoducduy.celestialarts.cultivation.Realm;
import com.ngoducduy.celestialarts.cultivation.RealmPassives;
import com.ngoducduy.celestialarts.cultivation.SpiritRoot;
import com.ngoducduy.celestialarts.cultivation.Stage;
import com.ngoducduy.celestialarts.cultivation.Talent;
import com.ngoducduy.celestialarts.network.FxData;
import com.ngoducduy.celestialarts.network.FxType;
import com.ngoducduy.celestialarts.registry.ModDamageTypes;
import com.ngoducduy.celestialarts.registry.ModEntities;
import com.ngoducduy.celestialarts.registry.ModItems;
import com.ngoducduy.celestialarts.skill.Element;
import com.ngoducduy.celestialarts.skill.Skill;
import com.ngoducduy.celestialarts.skill.SkillManager;
import com.ngoducduy.celestialarts.skill.SkillRegistry;
import com.ngoducduy.celestialarts.skill.cast.ActiveCast;
import com.ngoducduy.celestialarts.skill.skills.HeavenHandSkill;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import io.netty.buffer.Unpooled;

import java.util.ArrayList;
import java.util.List;

/**
 * Server-side game tests, executed with {@code ./gradlew runGametest}. They exercise the whole
 * skill pipeline (learning, casting, active casts, entities, breakthrough) on a mock player so that
 * regressions in the server logic are caught in CI even without a rendering client.
 */
public final class CelestialGameTests implements FabricGameTest {
	private static final int CAST_SPACING = 14;

	private static ServerPlayerEntity spawnPlayer(TestContext ctx, Realm realm) {
		ServerPlayerEntity player = ctx.createMockCreativeServerPlayerInWorld();
		Vec3d pos = ctx.getAbsolute(new Vec3d(0.5, 2.0, 0.5));
		player.refreshPositionAndAngles(pos.x, pos.y, pos.z, 0.0F, 0.0F);
		PlayerQi qi = QiHolder.get(player);
		// Fixed root/talent so multipliers are deterministic across runs (aptitude 50).
		qi.setRoot(new SpiritRoot(List.of(SpiritRoot.Kind.METAL), 3));
		qi.setTalent(Talent.MORTAL_BODY);
		qi.setRealm(realm);
		qi.setStage(Stage.EARLY);
		qi.setQi(qi.getMaxQi());
		for (Skill s : SkillRegistry.all()) {
			qi.learn(s.getId());
		}
		return player;
	}

	private static void removePlayer(TestContext ctx, ServerPlayerEntity player) {
		MinecraftServer server = ctx.getWorld().getServer();
		if (server.getPlayerManager().getPlayerList().contains(player)) {
			server.getPlayerManager().remove(player);
		} else {
			player.discard();
		}
	}

	/** Ticks the mod's per-player logic manually if the mock player is not part of the player list. */
	private static void tickIfDetached(TestContext ctx, ServerPlayerEntity player) {
		if (!ctx.getWorld().getServer().getPlayerManager().getPlayerList().contains(player)) {
			SkillManager.tickPlayer(player);
		}
	}

	private static <T extends Entity> int count(ServerWorld world, EntityType<T> type, Vec3d center, double radius) {
		Box box = new Box(center, center).expand(radius);
		return world.getEntitiesByType(type, box, e -> true).size();
	}

	@GameTest(templateName = EMPTY_STRUCTURE, tickLimit = 900)
	public void castEverySkill(TestContext ctx) {
		ServerPlayerEntity player = spawnPlayer(ctx, Realm.TRIBULATION);
		PlayerQi qi = QiHolder.get(player);
		ServerWorld world = ctx.getWorld();
		Vec3d origin = player.getPos();
		// A few targets so melee / area skills have something to hit.
		for (int i = 0; i < 3; i++) {
			ZombieEntity zombie = ctx.spawnMob(EntityType.ZOMBIE, 0.5F + i, 2.0F, 3.5F);
			zombie.setAiDisabled(true);
		}

		ctx.runAtEveryTick(() -> tickIfDetached(ctx, player));

		List<Skill> skills = new ArrayList<>(SkillRegistry.all());
		List<String> failures = new ArrayList<>();
		for (int i = 0; i < skills.size(); i++) {
			Skill skill = skills.get(i);
			long castTick = 5L + (long) i * CAST_SPACING;
			ctx.runAtTick(castTick, () -> {
				qi.setQi(qi.getMaxQi());
				boolean ok = SkillManager.cast(player, qi, skill);
				if (!ok) failures.add(skill.getId().getPath() + " refused to cast");
			});
			// Release channels / toggles so the next skill is not blocked.
			ctx.runAtTick(castTick + CAST_SPACING - 4, () -> {
				ActiveCast cast = qi.getActiveCast(skill.getId());
				if (cast != null) cast.onRelease();
				// Thiên Đạo Chi Thủ cannot be released and would crush every mob of the other tests in
				// this batch within 220 blocks – it has its own test in a separate batch.
				if (cast != null && skill == SkillRegistry.HEAVEN_HAND) cast.cancel();
				if (player.hasVehicle()) player.stopRiding();
			});
		}

		long lastCast = 5L + (long) (skills.size() - 1) * CAST_SPACING;
		ctx.runAtTick(lastCast + 200, () -> {
			for (ActiveCast cast : qi.getActiveCasts()) {
				if (!cast.isFinished()) cast.onRelease();
			}
		});
		ctx.runAtTick(lastCast + 260, () -> {
			removePlayer(ctx, player);
			if (!failures.isEmpty()) {
				ctx.throwGameTestException(String.join("; ", failures));
			}
			ctx.complete();
		});
	}

	@GameTest(templateName = EMPTY_STRUCTURE, tickLimit = 200)
	public void projectileSkillsSpawnEntities(TestContext ctx) {
		ServerPlayerEntity player = spawnPlayer(ctx, Realm.TRIBULATION);
		PlayerQi qi = QiHolder.get(player);
		ServerWorld world = ctx.getWorld();
		ctx.runAtEveryTick(() -> tickIfDetached(ctx, player));

		ctx.runAtTick(2, () -> ctx.assertTrue(SkillManager.cast(player, qi, SkillRegistry.SWORD_QI_SLASH), "sword qi slash cast"));
		ctx.runAtTick(4, () -> ctx.assertTrue(count(world, ModEntities.SWORD_QI, player.getPos(), 16) >= 1, "sword qi entity spawned"));

		ctx.runAtTick(6, () -> ctx.assertTrue(SkillManager.cast(player, qi, SkillRegistry.ICE_ARROWS), "ice arrows cast"));
		ctx.runAtTick(8, () -> ctx.assertTrue(count(world, ModEntities.ICE_SHARD, player.getPos(), 16) >= 1, "ice shard entities spawned"));

		ctx.runAtTick(10, () -> ctx.assertTrue(SkillManager.cast(player, qi, SkillRegistry.FIRE_LOTUS), "fire lotus cast"));
		ctx.runAtTick(12, () -> ctx.assertTrue(count(world, ModEntities.FIRE_LOTUS, player.getPos(), 16) >= 1, "fire lotus entity spawned"));

		ctx.runAtTick(14, () -> ctx.assertTrue(SkillManager.cast(player, qi, SkillRegistry.THOUSAND_SWORDS), "thousand swords cast"));
		// The wall unfolds over 12 ticks and starts launching at tick 34: at 14+30 it must be complete.
		ctx.runAtTick(44, () -> ctx.assertTrue(count(world, ModEntities.SPIRIT_SWORD, player.getPos(), 12) >= com.ngoducduy.celestialarts.skill.skills.ThousandSwordsSkill.SWORDS - 2, "sword wall complete (" + count(world, ModEntities.SPIRIT_SWORD, player.getPos(), 12) + ")"));
		ctx.runAtTick(60, () -> ctx.assertTrue(count(world, ModEntities.SPIRIT_SWORD, player.getPos(), 48) >= 1, "spirit swords summoned"));

		ctx.runAtTick(62, () -> ctx.assertTrue(SkillManager.cast(player, qi, SkillRegistry.EARTH_SHATTER), "earth shatter cast"));
		ctx.runAtTick(80, () -> ctx.assertTrue(count(world, ModEntities.ROCK_SPIKE, player.getPos(), 24) >= 1, "rock spikes raised"));

		ctx.runAtTick(82, () -> ctx.assertTrue(SkillManager.cast(player, qi, SkillRegistry.HEAVEN_SWORD), "heaven sword cast"));
		ctx.runAtTick(100, () -> ctx.assertTrue(count(world, ModEntities.HEAVEN_SWORD, player.getPos(), 64) >= 1, "heaven sword descending"));

		ctx.runAtTick(102, () -> ctx.assertTrue(SkillManager.cast(player, qi, SkillRegistry.WIND_DRAGON), "wind dragon cast"));
		ctx.runAtTick(104, () -> ctx.assertTrue(count(world, ModEntities.WIND_DRAGON, player.getPos(), 16) >= 1, "wind dragon entity spawned"));

		ctx.runAtTick(106, () -> ctx.assertTrue(SkillManager.cast(player, qi, SkillRegistry.THUNDER_DRAGON), "thunder dragon cast"));
		ctx.runAtTick(132, () -> ctx.assertTrue(count(world, ModEntities.THUNDER_DRAGON, player.getPos(), 64) >= 1, "thunder dragon released after the charge"));

		ctx.runAtTick(150, () -> {
			for (ActiveCast cast : qi.getActiveCasts()) cast.onRelease();
			removePlayer(ctx, player);
			ctx.complete();
		});
	}

	@GameTest(templateName = EMPTY_STRUCTURE, tickLimit = 200)
	public void skillDamageScalesWithRealm(TestContext ctx) {
		// Pure function first.
		ctx.assertTrue(Math.abs(RealmPassives.skillDamageMultiplier(Realm.QI_REFINING) - 1.0F) < 1e-5F, "qi refining multiplier is 1");
		ctx.assertTrue(RealmPassives.skillDamageMultiplier(Realm.TRIBULATION) > RealmPassives.skillDamageMultiplier(Realm.GOLDEN_CORE), "multiplier grows with realm");

		ServerPlayerEntity player = spawnPlayer(ctx, Realm.QI_REFINING);
		ServerWorld world = ctx.getWorld();
		IronGolemEntity low = ctx.spawnMob(EntityType.IRON_GOLEM, 3.5F, 2.0F, 0.5F);
		IronGolemEntity high = ctx.spawnMob(EntityType.IRON_GOLEM, 5.5F, 2.0F, 0.5F);
		low.setAiDisabled(true);
		high.setAiDisabled(true);

		ctx.runAtTick(2, () -> {
			PlayerQi qi = QiHolder.get(player);
			float before = low.getHealth();
			low.damage(ModDamageTypes.source(world, ModDamageTypes.SWORD_QI, player), 10.0F);
			float lost = before - low.getHealth();
			float expected1 = 10.0F * CultivationStats.skillDamageMultiplier(qi, Element.SWORD);
			ctx.assertTrue(Math.abs(lost - expected1) < 0.01F, "qi refining skill hit follows the cultivation multiplier, lost " + lost + " expected " + expected1);
			// A metal root favours sword arts: the sword multiplier beats the unattuned one.
			ctx.assertTrue(CultivationStats.skillDamageMultiplier(qi, Element.SWORD) > CultivationStats.skillDamageMultiplier(qi, Element.FIRE), "metal root favours sword arts");

			qi.setRealm(Realm.TRIBULATION);
			qi.setStage(Stage.PEAK);
			float before2 = high.getHealth();
			high.damage(ModDamageTypes.source(world, ModDamageTypes.SWORD_QI, player), 10.0F);
			float lost2 = before2 - high.getHealth();
			float expected = 10.0F * CultivationStats.skillDamageMultiplier(qi, Element.SWORD);
			ctx.assertTrue(Math.abs(lost2 - expected) < 0.01F, "tribulation skill hit is amplified, lost " + lost2 + " expected " + expected);
			ctx.assertTrue(lost2 > lost * 1.5F, "peak tribulation hits much harder than early qi refining");

			// Non-skill damage from the same player is untouched.
			high.timeUntilRegen = 0;
			high.hurtTime = 0;
			float before3 = high.getHealth();
			high.damage(world.getDamageSources().playerAttack(player), 5.0F);
			float lost3 = before3 - high.getHealth();
			ctx.assertTrue(Math.abs(lost3 - 5.0F) < 0.01F, "plain melee damage is not scaled, lost " + lost3);
		});
		ctx.runAtTick(6, () -> {
			low.discard();
			high.discard();
			removePlayer(ctx, player);
			ctx.complete();
		});
	}

	@GameTest(templateName = EMPTY_STRUCTURE, tickLimit = 400)
	public void swordFlightMountsPlayer(TestContext ctx) {
		ServerPlayerEntity player = spawnPlayer(ctx, Realm.GOLDEN_CORE);
		PlayerQi qi = QiHolder.get(player);
		ctx.runAtEveryTick(() -> tickIfDetached(ctx, player));
		ctx.runAtTick(2, () -> ctx.assertTrue(SkillManager.cast(player, qi, SkillRegistry.SWORD_FLIGHT), "sword flight cast"));
		ctx.runAtTick(6, () -> ctx.assertTrue(player.getVehicle() != null && player.getVehicle().getType() == ModEntities.FLYING_SWORD, "player rides the flying sword"));
		// Casting again toggles the flight off.
		ctx.runAtTick(40, () -> SkillManager.cast(player, qi, SkillRegistry.SWORD_FLIGHT));
		ctx.runAtTick(60, () -> {
			ctx.assertTrue(!player.hasVehicle(), "player dismounted after toggling sword flight off");
			removePlayer(ctx, player);
			ctx.complete();
		});
	}

	@GameTest(templateName = EMPTY_STRUCTURE, tickLimit = 300)
	public void channelSkillStopsOnSecondPress(TestContext ctx) {
		ServerPlayerEntity player = spawnPlayer(ctx, Realm.TRIBULATION);
		PlayerQi qi = QiHolder.get(player);
		ctx.runAtEveryTick(() -> tickIfDetached(ctx, player));
		Skill beam = SkillRegistry.PURPLE_THUNDER_BEAM;
		ctx.runAtTick(2, () -> ctx.assertTrue(SkillManager.cast(player, qi, beam), "beam cast"));
		ctx.runAtTick(6, () -> {
			ctx.assertTrue(qi.hasActiveCast(beam.getId()), "beam is an active cast");
			ctx.assertTrue(qi.isChanneling(), "player is channeling");
			// Other skills are blocked while channeling.
			ctx.assertTrue(!SkillManager.cast(player, qi, SkillRegistry.FLAME_CLAW), "other skills blocked while channeling");
		});
		ctx.runAtTick(30, () -> ctx.assertTrue(SkillManager.cast(player, qi, beam), "second press accepted"));
		ctx.runAtTick(34, () -> {
			ctx.assertTrue(!qi.isChanneling(), "channel stopped after second press");
			removePlayer(ctx, player);
			ctx.complete();
		});
	}

	@GameTest(templateName = EMPTY_STRUCTURE, tickLimit = 100)
	public void forgetSkillRefundsScrollAndClearsSlot(TestContext ctx) {
		ServerPlayerEntity player = FakePlayer.get(ctx.getWorld());
		Vec3d pos = ctx.getAbsolute(new Vec3d(0.5, 2.0, 0.5));
		player.refreshPositionAndAngles(pos.x, pos.y, pos.z, 0.0F, 0.0F);
		player.getInventory().clear();
		PlayerQi qi = QiHolder.get(player);
		qi.interruptAllCasts();
		Skill skill = SkillRegistry.SWORD_QI_SLASH;
		qi.forget(skill.getId());
		for (int i = 0; i < PlayerQi.SLOT_COUNT; i++) qi.setSlot(i, null);
		qi.learn(skill.getId());
		ctx.assertTrue(skill.getId().equals(qi.getSlot(0)), "learning auto-binds the first free slot");
		ctx.assertTrue(!SkillManager.forget(player, SkillRegistry.FIRE_LOTUS.getId(), true), "forgetting an unknown art is refused");
		ctx.assertTrue(SkillManager.forget(player, skill.getId(), true), "forget accepted");
		ctx.assertTrue(!qi.hasLearned(skill.getId()), "art removed");
		ctx.assertTrue(qi.getSlot(0) == null, "slot cleared");
		boolean refunded = false;
		for (int i = 0; i < player.getInventory().size(); i++) {
			if (player.getInventory().getStack(i).getItem() instanceof com.ngoducduy.celestialarts.item.SkillScrollItem scroll && scroll.getSkill() == skill) refunded = true;
		}
		ctx.assertTrue(refunded, "the art's manual was handed back");
		ctx.assertTrue(!SkillManager.forget(player, skill.getId(), true), "cannot forget twice (no duplicate scrolls)");
		ctx.complete();
	}

	@GameTest(templateName = EMPTY_STRUCTURE, tickLimit = 200)
	public void qiCostAndCooldownApplied(TestContext ctx) {
		// FakePlayer is a survival ServerPlayerEntity outside the player list, so costs are not skipped.
		ServerPlayerEntity player = FakePlayer.get(ctx.getWorld());
		Vec3d pos = ctx.getAbsolute(new Vec3d(0.5, 2.0, 0.5));
		player.refreshPositionAndAngles(pos.x, pos.y, pos.z, 0.0F, 0.0F);
		PlayerQi qi = QiHolder.get(player);
		qi.setRealm(Realm.QI_REFINING);
		qi.setQi(qi.getMaxQi());
		qi.interruptAllCasts();
		Skill skill = SkillRegistry.SWORD_QI_SLASH;
		qi.learn(skill.getId());
		qi.setCooldown(skill.getId(), 0);
		float before = qi.getQi();
		ctx.assertTrue(!player.isCreative(), "fake player is not creative");
		ctx.assertTrue(SkillManager.cast(player, qi, skill), "cast accepted");
		ctx.assertTrue(Math.abs((before - qi.getQi()) - skill.getQiCost()) < 0.001F, "qi cost consumed, delta " + (before - qi.getQi()));
		ctx.assertTrue(qi.getCooldown(skill.getId()) == skill.getCooldownTicks(), "cooldown applied");
		ctx.assertTrue(!SkillManager.cast(player, qi, skill), "second cast rejected while on cooldown");
		// Not enough qi: refuse.
		qi.setCooldown(skill.getId(), 0);
		qi.setQi(skill.getQiCost() - 1.0F);
		ctx.assertTrue(!SkillManager.cast(player, qi, skill), "cast rejected without enough qi");
		// Cooldowns tick down.
		qi.setCooldown(skill.getId(), 5);
		for (int i = 0; i < 5; i++) qi.tickCooldowns();
		ctx.assertTrue(!qi.isOnCooldown(skill.getId()), "cooldown expired after ticking");
		ctx.complete();
	}

	@GameTest(templateName = EMPTY_STRUCTURE)
	public void realmPassivesScaleWithRealm(TestContext ctx) {
		ServerPlayerEntity player = FakePlayer.get(ctx.getWorld());
		Vec3d pos = ctx.getAbsolute(new Vec3d(0.5, 2.0, 0.5));
		player.refreshPositionAndAngles(pos.x, pos.y, pos.z, 0.0F, 0.0F);
		PlayerQi qi = QiHolder.get(player);
		qi.interruptAllCasts();

		qi.setRealm(Realm.QI_REFINING);
		RealmPassives.apply(player);
		ctx.assertTrue(Math.abs(player.getMaxHealth() - 20.0F) < 0.01F, "no bonus at Qi Refining, was " + player.getMaxHealth());

		qi.setRealm(Realm.GOLDEN_CORE);
		RealmPassives.apply(player);
		ctx.assertTrue(Math.abs(player.getMaxHealth() - 28.0F) < 0.01F, "+8 health at Golden Core, was " + player.getMaxHealth());
		RealmPassives.apply(player);
		ctx.assertTrue(Math.abs(player.getMaxHealth() - 28.0F) < 0.01F, "modifiers do not stack on re-apply, was " + player.getMaxHealth());

		DamageSource fall = ctx.getWorld().getDamageSources().fall();
		DamageSource fire = ctx.getWorld().getDamageSources().onFire();
		DamageSource drown = ctx.getWorld().getDamageSources().drown();
		ctx.assertTrue(RealmPassives.ignoresDamage(player, fall), "Golden Core ignores fall damage");
		ctx.assertTrue(!RealmPassives.ignoresDamage(player, fire), "Golden Core still burns");
		ctx.assertTrue(!RealmPassives.ignoresDamage(player, drown), "Golden Core still drowns");
		qi.setRealm(Realm.SPIRIT_TRANSFORMATION);
		ctx.assertTrue(RealmPassives.ignoresDamage(player, fire) && RealmPassives.ignoresDamage(player, drown), "Spirit Transformation ignores fire and drowning");

		qi.setRealm(Realm.QI_REFINING);
		RealmPassives.apply(player);
		ctx.assertTrue(Math.abs(player.getMaxHealth() - 20.0F) < 0.01F, "modifiers removed when realm drops, was " + player.getMaxHealth());
		qi.setQi(qi.getMaxQi());
		ctx.assertTrue(!RealmPassives.airJump(player), "no air jump at Qi Refining");

		qi.setRealm(Realm.FOUNDATION);
		qi.setQi(qi.getMaxQi());
		float before = qi.getQi();
		ctx.assertTrue(RealmPassives.airJump(player), "air jump at Foundation");
		ctx.assertTrue(Math.abs((before - qi.getQi()) - RealmPassives.AIR_JUMP_QI) < 0.001F, "air jump costs qi, delta " + (before - qi.getQi()));
		qi.setQi(1.0F);
		ctx.assertTrue(!RealmPassives.airJump(player), "air jump refused without qi");
		ctx.complete();
	}

	/**
	 * Crossing a major realm: a peak-stage cultivator with enough cultivation sits down, the
	 * tribulation runs its bolts (the mock player is invulnerable, so it always survives) and the
	 * realm advances to Foundation, early stage.
	 */
	@GameTest(templateName = EMPTY_STRUCTURE, tickLimit = 480)
	public void tribulationAdvancesRealm(TestContext ctx) {
		ServerPlayerEntity player = spawnPlayer(ctx, Realm.QI_REFINING);
		PlayerQi qi = QiHolder.get(player);
		qi.setStage(Stage.PEAK);
		qi.setExp(qi.getExpForBreakthrough() + 10);
		ctx.assertTrue(qi.canBreakthrough(), "enough exp to break through");
		ctx.assertTrue(qi.nextBreakthroughIsTribulation(), "peak stage means the next step is a tribulation");
		int bolts = CultivationStats.tribulationBolts(qi);
		ctx.assertTrue(bolts >= 5 && bolts <= 12, "bolt count in a sane range, was " + bolts);
		ctx.runAtEveryTick(() -> tickIfDetached(ctx, player));
		ctx.runAtTick(2, () -> {
			player.setOnGround(true);
			Breakthrough.tryBreakthrough(player);
			ctx.assertTrue(Meditation.isMeditating(player), "breakthrough seats the cultivator");
			ctx.assertTrue(Breakthrough.isInTribulation(qi), "tribulation cast started");
		});
		ctx.runAtTick(60, () -> ctx.assertTrue(Meditation.isMeditating(player) && Breakthrough.isInTribulation(qi), "still seated under the cloud"));
		int done = 80 + bolts * 20 + 60;
		ctx.runAtTick(done, () -> {
			ctx.assertTrue(qi.getRealm() == Realm.FOUNDATION, "realm advanced to Foundation, was " + qi.getRealm());
			ctx.assertTrue(qi.getStage() == Stage.EARLY, "new realm starts at early stage, was " + qi.getStage());
			ctx.assertTrue(qi.getExp() == 0, "cultivation spent by the tribulation, was " + qi.getExp());
			ctx.assertTrue(!Meditation.isMeditating(player), "trance ends with the breakthrough");
			ctx.assertTrue(!player.hasStatusEffect(com.ngoducduy.celestialarts.registry.ModEffects.QI_DEVIATION), "no qi deviation after success");
			removePlayer(ctx, player);
			ctx.complete();
		});
	}

	/** One press sits, gathers cultivation over time, a second press stands up. */
	@GameTest(templateName = EMPTY_STRUCTURE, tickLimit = 200)
	public void meditationTogglesAndGathersCultivation(TestContext ctx) {
		ServerPlayerEntity player = spawnPlayer(ctx, Realm.FOUNDATION);
		PlayerQi qi = QiHolder.get(player);
		qi.setExp(0);
		ctx.runAtEveryTick(() -> tickIfDetached(ctx, player));
		ctx.runAtTick(2, () -> {
			player.setOnGround(true);
			Meditation.toggle(player);
			ctx.assertTrue(Meditation.isMeditating(player), "first press seats the player");
			ctx.assertTrue(qi.isMeditating(), "qi state mirrors the seat");
			ctx.assertTrue(count(ctx.getWorld(), ModEntities.MEDITATION_SEAT, player.getPos(), 2.0) == 1, "one seat entity under the player");
		});
		ctx.runAtTick(90, () -> {
			ctx.assertTrue(Meditation.isMeditating(player), "still meditating without holding any key");
			ctx.assertTrue(qi.getExp() > 0, "cultivation accumulates while seated, exp " + qi.getExp());
			ctx.assertTrue(qi.getSpiritQi() > 0.0F, "spiritual qi of the spot was sampled");
			Meditation.toggle(player);
			ctx.assertTrue(!Meditation.isMeditating(player) && !qi.isMeditating(), "second press stands up");
		});
		ctx.runAtTick(120, () -> {
			ctx.assertTrue(count(ctx.getWorld(), ModEntities.MEDITATION_SEAT, player.getPos(), 4.0) == 0, "seat entity removed after standing up");
			removePlayer(ctx, player);
			ctx.complete();
		});
	}

	/** Pure numbers behind aptitude, breakthrough odds and tribulations. */
	@GameTest(templateName = EMPTY_STRUCTURE)
	public void cultivationStatsAreMonotonic(TestContext ctx) {
		SpiritRoot poor = new SpiritRoot(List.of(SpiritRoot.Kind.METAL, SpiritRoot.Kind.WOOD, SpiritRoot.Kind.EARTH), 1);
		SpiritRoot best = new SpiritRoot(List.of(SpiritRoot.Kind.THUNDER), 5);
		int aPoor = CultivationStats.aptitude(poor, Talent.CRIPPLED_MERIDIANS);
		int aBest = CultivationStats.aptitude(best, Talent.CHAOS_BODY);
		ctx.assertTrue(aPoor >= 1 && aPoor < 20, "poor aptitude is mortal tier, was " + aPoor);
		ctx.assertTrue(aBest > 80 && aBest <= 100, "best aptitude is peerless tier, was " + aBest);
		ctx.assertTrue(CultivationStats.aptitudeTier(aPoor).equals("mortal") && CultivationStats.aptitudeTier(aBest).equals("peerless"), "tier labels");

		PlayerQi weak = new PlayerQi();
		weak.setRoot(poor);
		weak.setTalent(Talent.CRIPPLED_MERIDIANS);
		PlayerQi strong = new PlayerQi();
		strong.setRoot(best);
		strong.setTalent(Talent.CHAOS_BODY);
		for (PlayerQi q : List.of(weak, strong)) {
			q.setRealm(Realm.GOLDEN_CORE);
			q.setStage(Stage.MIDDLE);
		}
		ctx.assertTrue(CultivationStats.powerMultiplier(strong) > CultivationStats.powerMultiplier(weak), "talent separates power within a realm");
		ctx.assertTrue(strong.getMaxQi() > weak.getMaxQi() && strong.getRegenPerTick() > weak.getRegenPerTick(), "better root regenerates and stores more qi");
		ctx.assertTrue(CultivationStats.skillDamageMultiplier(strong, Element.LIGHTNING) > CultivationStats.skillDamageMultiplier(strong, Element.EARTH), "thunder root favours lightning arts");
		ctx.assertTrue(CultivationStats.tribulationBolts(strong) > CultivationStats.tribulationBolts(weak), "heaven sends more lightning at greater talent");
		ctx.assertTrue(CultivationStats.tribulationBoltDamage(strong) > CultivationStats.tribulationBoltDamage(weak), "and heavier bolts");
		float cWeak = CultivationStats.breakthroughChance(weak, 0.5F);
		float cStrong = CultivationStats.breakthroughChance(strong, 2.5F);
		ctx.assertTrue(cWeak >= 0.2F && cWeak < cStrong && cStrong <= 0.98F, "breakthrough odds clamp and ordering, " + cWeak + " < " + cStrong);
		ctx.assertTrue(CultivationStats.breakthroughChance(weak, 2.5F) > CultivationStats.breakthroughChance(weak, 0.5F), "rich spiritual qi helps");
		ctx.assertTrue(CultivationStats.deviationTicks(weak, true) > CultivationStats.deviationTicks(weak, false), "tribulation deviation lasts longer");
		ctx.assertTrue(CultivationStats.deviationTicks(strong, false) > 0 && CultivationStats.deviationTicks(weak, false) > 0, "deviation positive");
		// Stage chain: 6 realms x 4 stages = 24 steps, peak of the last realm is the end.
		PlayerQi p = new PlayerQi();
		p.setRealm(Realm.TRIBULATION);
		p.setStage(Stage.PEAK);
		ctx.assertTrue(p.isAtPeakOfCultivation() && p.getRank() == 24 && !p.canBreakthrough(), "peak of cultivation");
		ctx.complete();
	}

	@GameTest(templateName = EMPTY_STRUCTURE)
	public void qiNbtRoundTrip(TestContext ctx) {
		PlayerQi qi = new PlayerQi();
		qi.setRoot(new SpiritRoot(List.of(SpiritRoot.Kind.FIRE, SpiritRoot.Kind.DARK), 4));
		qi.setTalent(Talent.DAO_HEART);
		qi.setRealm(Realm.NASCENT_SOUL);
		qi.setStage(Stage.LATE);
		qi.setQi(123.5F);
		qi.setExp(777);
		qi.learn(SkillRegistry.FIRE_LOTUS.getId());
		qi.learn(SkillRegistry.TAIJI_FORMATION.getId());
		qi.setSlot(0, SkillRegistry.FIRE_LOTUS.getId());
		qi.setSlot(5, SkillRegistry.TAIJI_FORMATION.getId());
		qi.setCooldown(SkillRegistry.FIRE_LOTUS.getId(), 42);

		NbtCompound nbt = qi.writeNbt(new NbtCompound());
		PlayerQi copy = new PlayerQi();
		copy.readNbt(nbt);

		ctx.assertTrue(copy.getRealm() == Realm.NASCENT_SOUL, "realm restored");
		ctx.assertTrue(copy.getStage() == Stage.LATE, "stage restored");
		ctx.assertTrue(copy.getRoot() != null && copy.getRoot().getGrade() == 4 && copy.getRoot().getKinds().equals(List.of(SpiritRoot.Kind.FIRE, SpiritRoot.Kind.DARK)), "spirit root restored");
		ctx.assertTrue(copy.getTalent() == Talent.DAO_HEART, "talent restored");
		ctx.assertTrue(copy.hasAwakened(), "awakened flag restored");
		ctx.assertTrue(Math.abs(copy.getQi() - 123.5F) < 0.001F, "qi restored");
		ctx.assertTrue(copy.getExp() == 777, "exp restored");
		ctx.assertTrue(copy.hasLearned(SkillRegistry.FIRE_LOTUS.getId()) && copy.hasLearned(SkillRegistry.TAIJI_FORMATION.getId()), "learned skills restored");
		ctx.assertTrue(SkillRegistry.FIRE_LOTUS.getId().equals(copy.getSlot(0)) && SkillRegistry.TAIJI_FORMATION.getId().equals(copy.getSlot(5)), "slots restored");
		ctx.assertTrue(copy.getCooldown(SkillRegistry.FIRE_LOTUS.getId()) == 42, "cooldown restored");
		ctx.assertTrue(!copy.hasQi(1000F) && copy.hasQi(100F), "hasQi sanity");
		ctx.complete();
	}

	/**
	 * Thiên Đạo Chi Thủ runs in its own batch: its 220-block domain would otherwise reach every other
	 * test's mobs. Checks the full timeline on a 400-health zombie: pinned + crushed during the descent,
	 * other skills locked out while channelling, extra damage from the slam/shock ring, clean finish.
	 */
	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "heaven_hand", tickLimit = 420)
	public void heavenHandSuppressesAndSlams(TestContext ctx) {
		ServerPlayerEntity player = spawnPlayer(ctx, Realm.TRIBULATION);
		PlayerQi qi = QiHolder.get(player);
		ZombieEntity zombie = ctx.spawnMob(EntityType.ZOMBIE, 0.5F, 2.0F, 4.5F);
		zombie.setAiDisabled(true);
		zombie.getAttributeInstance(net.minecraft.entity.attribute.EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(400.0);
		zombie.setHealth(400.0F);
		ctx.runAtEveryTick(() -> tickIfDetached(ctx, player));
		float[] beforeSlam = new float[1];

		ctx.runAtTick(2, () -> ctx.assertTrue(SkillManager.cast(player, qi, SkillRegistry.HEAVEN_HAND), "heaven hand cast accepted"));
		ctx.runAtTick(100, () -> {
			ctx.assertTrue(qi.isChanneling(), "caster is channelling during the ritual");
			ctx.assertTrue(!SkillManager.cast(player, qi, SkillRegistry.SWORD_QI_SLASH), "other skills are locked out while borrowing the heavens");
			ctx.assertTrue(player.isAlive(), "caster alive");
		});
		ctx.runAtTick(170, () -> {
			ctx.assertTrue(zombie.hasStatusEffect(com.ngoducduy.celestialarts.registry.ModEffects.SUPPRESSED), "victim is Trấn Áp (suppressed) during the descent");
			ctx.assertTrue(zombie.getHealth() < 400.0F, "victim is crushed for damage during the descent");
			zombie.setVelocity(0.0, 0.6, 0.0);
			zombie.velocityModified = true;
		});
		ctx.runAtTick(172, () -> ctx.assertTrue(zombie.getVelocity().y <= 0.05, "suppression forces the victim back down"));
		ctx.runAtTick(HeavenHandSkill.T_SLAM - 1 + 2, () -> beforeSlam[0] = zombie.getHealth());
		ctx.runAtTick(HeavenHandSkill.T_SLAM + 30, () -> {
			ctx.assertTrue(zombie.isAlive(), "400-health victim survives the slam at the edge of the core");
			ctx.assertTrue(beforeSlam[0] - zombie.getHealth() >= HeavenHandSkill.RING_DAMAGE_MIN - 0.01F, "slam / shock ring dealt extra damage: " + (beforeSlam[0] - zombie.getHealth()));
		});
		ctx.runAtTick(HeavenHandSkill.T_END + 12, () -> {
			ActiveCast cast = qi.getActiveCast(SkillRegistry.HEAVEN_HAND.getId());
			ctx.assertTrue(cast == null || cast.isFinished(), "cast finished on schedule");
			ctx.assertTrue(!qi.isChanneling(), "no longer channelling");
			ctx.assertTrue(player.isAlive(), "caster survived the ritual");
			removePlayer(ctx, player);
			ctx.complete();
		});
	}

	@GameTest(templateName = EMPTY_STRUCTURE)
	public void fxDataRoundTrip(TestContext ctx) {
		FxData original = FxData.follow(FxType.BEAM, 17, new Vec3d(1.5, 64.25, -3.75), 0xB57BFF, 0.55F, 120)
				.withExtra(30);
		PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
		original.write(buf);
		FxData decoded = FxData.read(buf);
		ctx.assertTrue(decoded.type() == FxType.BEAM, "type");
		ctx.assertTrue(decoded.pos().equals(original.pos()), "pos");
		ctx.assertTrue(decoded.color() == 0xB57BFF, "color");
		ctx.assertTrue(Math.abs(decoded.scale() - 0.55F) < 1.0E-6F, "scale");
		ctx.assertTrue(decoded.duration() == 120, "duration");
		ctx.assertTrue(decoded.extra() == 30, "extra");
		ctx.assertTrue(decoded.entityId() == 17, "entity id");
		ctx.complete();
	}

	@GameTest(templateName = EMPTY_STRUCTURE)
	public void dataPackContentLoaded(TestContext ctx) {
		MinecraftServer server = ctx.getWorld().getServer();
		String[] recipes = {"spirit_stone", "high_spirit_stone", "dao_manual", "foundation_pill", "nascent_pill",
				"immortal_sword", "scroll_sword_qi_slash", "scroll_flame_claw", "scroll_ice_arrows", "scroll_lightning_step",
				"scroll_wind_blade_dance", "scroll_tortoise_shield", "scroll_earth_shatter", "scroll_vajra_palm", "scroll_wind_dragon",
				"qi_pill", "heaven_pill"};
		for (String r : recipes) {
			Identifier id = CelestialArts.id(r);
			ctx.assertTrue(server.getRecipeManager().get(id).isPresent(), "recipe present: " + id);
		}
		String[] advancements = {"root", "learn_first_skill", "learn_all_skills", "realm/foundation", "realm/golden_core",
				"realm/nascent_soul", "realm/spirit_transformation", "realm/tribulation", "skills/sword_flight",
				"skills/nine_tribulations", "skills/heaven_sword", "skills/heaven_hand"};
		for (String a : advancements) {
			Identifier id = CelestialArts.id(a);
			ctx.assertTrue(server.getAdvancementLoader().get(id) != null, "advancement present: " + id);
		}
		ctx.assertTrue(ModItems.scrolls().size() == SkillRegistry.all().size(), "one manual per skill");
		ctx.complete();
	}

	@GameTest(templateName = EMPTY_STRUCTURE, tickLimit = 300)
	public void skillEntitiesTickWithoutCrashing(TestContext ctx) {
		ctx.spawnEntity(ModEntities.SWORD_QI, 0.5F, 3.0F, 0.5F);
		ctx.spawnEntity(ModEntities.ICE_SHARD, 0.5F, 3.0F, 0.5F);
		ctx.spawnEntity(ModEntities.FIRE_LOTUS, 0.5F, 3.0F, 0.5F);
		ctx.spawnEntity(ModEntities.SPIRIT_SWORD, 0.5F, 3.0F, 0.5F);
		ctx.spawnEntity(ModEntities.FLYING_SWORD, 0.5F, 3.0F, 0.5F);
		ctx.spawnEntity(ModEntities.ROCK_SPIKE, 0.5F, 2.0F, 0.5F);
		ctx.spawnEntity(ModEntities.HEAVEN_SWORD, 0.5F, 20.0F, 0.5F);
		ctx.spawnEntity(ModEntities.WIND_DRAGON, 0.5F, 2.0F, 0.5F);
		ctx.spawnEntity(ModEntities.THUNDER_DRAGON, 0.5F, 3.0F, 0.5F);
		ctx.runAtTick(250, ctx::complete);
	}
}
