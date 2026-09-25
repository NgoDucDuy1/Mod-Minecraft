package com.ngoducduy.celestialarts.gametest;

import com.ngoducduy.celestialarts.CelestialArts;
import com.ngoducduy.celestialarts.cultivation.Breakthrough;
import com.ngoducduy.celestialarts.cultivation.PlayerQi;
import com.ngoducduy.celestialarts.cultivation.QiHolder;
import com.ngoducduy.celestialarts.cultivation.Realm;
import com.ngoducduy.celestialarts.network.FxData;
import com.ngoducduy.celestialarts.network.FxType;
import com.ngoducduy.celestialarts.registry.ModEntities;
import com.ngoducduy.celestialarts.registry.ModItems;
import com.ngoducduy.celestialarts.skill.Skill;
import com.ngoducduy.celestialarts.skill.SkillManager;
import com.ngoducduy.celestialarts.skill.SkillRegistry;
import com.ngoducduy.celestialarts.skill.cast.ActiveCast;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.ZombieEntity;
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
		qi.setRealm(realm);
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
		ctx.runAtTick(60, () -> ctx.assertTrue(count(world, ModEntities.SPIRIT_SWORD, player.getPos(), 24) >= 1, "spirit swords summoned"));

		ctx.runAtTick(62, () -> ctx.assertTrue(SkillManager.cast(player, qi, SkillRegistry.EARTH_SHATTER), "earth shatter cast"));
		ctx.runAtTick(80, () -> ctx.assertTrue(count(world, ModEntities.ROCK_SPIKE, player.getPos(), 24) >= 1, "rock spikes raised"));

		ctx.runAtTick(82, () -> ctx.assertTrue(SkillManager.cast(player, qi, SkillRegistry.HEAVEN_SWORD), "heaven sword cast"));
		ctx.runAtTick(100, () -> ctx.assertTrue(count(world, ModEntities.HEAVEN_SWORD, player.getPos(), 64) >= 1, "heaven sword descending"));

		ctx.runAtTick(150, () -> {
			for (ActiveCast cast : qi.getActiveCasts()) cast.onRelease();
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

	@GameTest(templateName = EMPTY_STRUCTURE, tickLimit = 400)
	public void breakthroughAdvancesRealm(TestContext ctx) {
		ServerPlayerEntity player = spawnPlayer(ctx, Realm.QI_REFINING);
		PlayerQi qi = QiHolder.get(player);
		qi.setExp(qi.getExpForBreakthrough() + 10);
		ctx.assertTrue(qi.canBreakthrough(), "enough exp to break through");
		ctx.runAtEveryTick(() -> tickIfDetached(ctx, player));
		ctx.runAtTick(2, () -> Breakthrough.tryBreakthrough(player));
		ctx.runAtTick(220, () -> {
			ctx.assertTrue(qi.getRealm() == Realm.FOUNDATION, "realm advanced to Foundation, was " + qi.getRealm());
			ctx.assertTrue(qi.getExp() == 10, "exp reduced by the breakthrough cost, was " + qi.getExp());
			removePlayer(ctx, player);
			ctx.complete();
		});
	}

	@GameTest(templateName = EMPTY_STRUCTURE)
	public void qiNbtRoundTrip(TestContext ctx) {
		PlayerQi qi = new PlayerQi();
		qi.setRealm(Realm.NASCENT_SOUL);
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
		ctx.assertTrue(Math.abs(copy.getQi() - 123.5F) < 0.001F, "qi restored");
		ctx.assertTrue(copy.getExp() == 777, "exp restored");
		ctx.assertTrue(copy.hasLearned(SkillRegistry.FIRE_LOTUS.getId()) && copy.hasLearned(SkillRegistry.TAIJI_FORMATION.getId()), "learned skills restored");
		ctx.assertTrue(SkillRegistry.FIRE_LOTUS.getId().equals(copy.getSlot(0)) && SkillRegistry.TAIJI_FORMATION.getId().equals(copy.getSlot(5)), "slots restored");
		ctx.assertTrue(copy.getCooldown(SkillRegistry.FIRE_LOTUS.getId()) == 42, "cooldown restored");
		ctx.assertTrue(!copy.hasQi(1000F) && copy.hasQi(100F), "hasQi sanity");
		ctx.complete();
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
				"scroll_wind_blade_dance", "scroll_tortoise_shield", "scroll_earth_shatter"};
		for (String r : recipes) {
			Identifier id = CelestialArts.id(r);
			ctx.assertTrue(server.getRecipeManager().get(id).isPresent(), "recipe present: " + id);
		}
		String[] advancements = {"root", "learn_first_skill", "learn_all_skills", "realm/foundation", "realm/golden_core",
				"realm/nascent_soul", "realm/spirit_transformation", "realm/tribulation", "skills/sword_flight",
				"skills/nine_tribulations", "skills/heaven_sword"};
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
		ctx.runAtTick(250, ctx::complete);
	}
}
