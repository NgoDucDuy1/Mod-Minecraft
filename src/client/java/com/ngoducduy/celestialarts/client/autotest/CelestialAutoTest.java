package com.ngoducduy.celestialarts.client.autotest;

import com.ngoducduy.celestialarts.CelestialArts;
import com.ngoducduy.celestialarts.client.ClientPackets;
import com.ngoducduy.celestialarts.client.gui.SkillBookScreen;
import com.ngoducduy.celestialarts.client.render.fx.ClientFxManager;
import com.ngoducduy.celestialarts.cultivation.PlayerQi;
import com.ngoducduy.celestialarts.cultivation.QiHolder;
import com.ngoducduy.celestialarts.cultivation.Realm;
import com.ngoducduy.celestialarts.network.FxData;
import com.ngoducduy.celestialarts.network.FxType;
import com.ngoducduy.celestialarts.registry.GlowParticleEffect;
import com.ngoducduy.celestialarts.registry.ModEntities;
import com.ngoducduy.celestialarts.registry.ModParticles;
import com.ngoducduy.celestialarts.skill.Skill;
import com.ngoducduy.celestialarts.skill.SkillRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.AccessibilityOnboardingScreen;
import net.minecraft.client.gui.screen.MessageScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.entity.Entity;
import net.minecraft.registry.Registries;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.resource.DataConfiguration;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Difficulty;
import net.minecraft.world.GameMode;
import net.minecraft.world.GameRules;
import net.minecraft.world.gen.GeneratorOptions;
import net.minecraft.world.gen.WorldPresets;
import net.minecraft.world.level.LevelInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.MixinEnvironment;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Headless-friendly client smoke test, enabled with {@code -Dcelestialarts.autotest}.
 *
 * <p>Runs on its own thread and drives the real client through {@link MinecraftClient#submit}:
 * creates a flat creative world, learns every skill through {@code /celestial}, casts all of them
 * from the hotbar slots (exercising the C2S packets, entity spawn packets, renderers, particles and
 * the world FX pipeline), spawns every {@link FxType} and particle locally, opens the skill book,
 * takes screenshots and finally quits. Any exception on the render thread crashes the game, any
 * failed assertion is logged and turns the exit code non-zero, so CI can gate on it.
 */
public final class CelestialAutoTest {
	public static final String PROPERTY = "celestialarts.autotest";
	private static final Logger LOG = LoggerFactory.getLogger("CelestialArts/AutoTest");
	private static final List<String> FAILURES = Collections.synchronizedList(new ArrayList<>());
	private static final String WORLD_NAME = "celestial-autotest";
	private static int screenshots;

	private CelestialAutoTest() {
	}

	public static void init() {
		if (System.getProperty(PROPERTY) == null) return;
		LOG.info("[AutoTest] enabled, starting driver thread");
		Thread thread = new Thread(() -> {
			try {
				run();
			} catch (Throwable t) {
				LOG.error("[AutoTest] RESULT: FAILED with exception", t);
				System.exit(1);
			}
		}, "Celestial Auto Test");
		thread.setDaemon(true);
		thread.start();
	}

	private static void run() {
		waitFor("loading overlay to finish", c -> c.getOverlay() == null, Duration.ofMinutes(5));

		// First launch shows the accessibility onboarding; skip it.
		waitFor("title or onboarding screen", c -> c.currentScreen instanceof TitleScreen
				|| c.currentScreen instanceof AccessibilityOnboardingScreen, Duration.ofMinutes(2));
		submitAndWait(c -> {
			if (c.currentScreen instanceof AccessibilityOnboardingScreen) {
				c.options.onboardAccessibility = false;
				c.options.write();
				c.setScreen(new TitleScreen());
			}
			return null;
		});
		waitForScreen(TitleScreen.class);
		screenshot("01_title");

		LOG.info("[AutoTest] auditing mixins");
		MixinEnvironment.getCurrentEnvironment().audit();

		LOG.info("[AutoTest] creating flat creative world");
		submitAndWait(c -> {
			LevelInfo info = new LevelInfo(WORLD_NAME, GameMode.CREATIVE, false, Difficulty.PEACEFUL, true,
					new GameRules(), DataConfiguration.SAFE_MODE);
			GeneratorOptions options = new GeneratorOptions(20260925L, false, false);
			c.createIntegratedServerLoader().createAndStart(WORLD_NAME, info, options,
					registries -> registries.get(RegistryKeys.WORLD_PRESET).entryOf(WorldPresets.FLAT).value().createDimensionsRegistryHolder());
			return null;
		});

		waitFor("world + player", c -> c.world != null && c.player != null && c.currentScreen == null, Duration.ofMinutes(10));
		waitTicks(60);
		screenshot("02_in_game");

		// Prepare: daylight, max realm, every skill learned.
		command("time set noon");
		command("weather clear");
		command("gamerule doDaylightCycle false");
		command("celestial realm 6");
		command("celestial learn all");
		command("celestial qi 9999");
		waitTicks(20);

		PlayerQi qi = submitAndWait(c -> QiHolder.get(Objects.requireNonNull(c.player)));
		check(qi.getRealm() == Realm.TRIBULATION, "client received realm sync (got " + qi.getRealm() + ")");
		int learned = 0;
		for (Skill skill : SkillRegistry.all()) {
			if (qi.hasLearned(skill.getId())) learned++;
		}
		check(learned == SkillRegistry.all().size(), "client received learned skills sync (" + learned + "/" + SkillRegistry.all().size() + ")");

		// HUD + skill book.
		submitAndWait(c -> {
			c.setScreen(new SkillBookScreen());
			return null;
		});
		waitTicks(5);
		check(submitAndWait(c -> c.currentScreen instanceof SkillBookScreen), "skill book screen opened");
		screenshot("03_skill_book");
		submitAndWait(c -> {
			c.setScreen(null);
			return null;
		});

		// Local FX pipeline: one instance of every world effect in front of the player.
		LOG.info("[AutoTest] spawning every FxType locally");
		submitAndWait(c -> {
			Vec3d base = c.player.getPos();
			FxType[] types = FxType.values();
			for (int i = 0; i < types.length; i++) {
				double dx = (i % 6 - 2.5) * 3.0;
				double dz = 6.0 + (i / 6) * 4.0;
				Vec3d pos = base.add(dx, 0.0, dz);
				FxData data = FxData.at(types[i], pos, 0x9FE8FF, 1.0f, 200).withTarget(pos.add(0, 4, 0)).withExtra(8);
				ClientFxManager.spawn(data);
			}
			return null;
		});
		waitTicks(5);
		int fxCount = submitAndWait(c -> ClientFxManager.count());
		check(fxCount >= FxType.values().length, "all FX types alive after spawn (" + fxCount + ")");
		waitTicks(15);
		screenshot("04_fx_types");

		// Every custom particle.
		LOG.info("[AutoTest] spawning every particle type");
		submitAndWait(c -> {
			Vec3d p = c.player.getPos().add(0, 1.5, 3);
			List<ParticleEffect> effects = List.of(
					GlowParticleEffect.glow(0xFF7A1A, 1.2f, 40),
					GlowParticleEffect.spark(0xD9C7FF, 1.0f, 30),
					ModParticles.FLAME_WISP, ModParticles.EMBER, ModParticles.ICE_CRYSTAL, ModParticles.SNOWFLAKE,
					ModParticles.FROST_MIST, ModParticles.LIGHTNING_ARC, ModParticles.WIND_STREAK, ModParticles.VOID_SMOKE,
					ModParticles.LOTUS_PETAL, ModParticles.RUNE, ModParticles.SWORD_GLINT, ModParticles.ROCK_DEBRIS,
					ModParticles.GOLDEN_LIGHT);
			for (int i = 0; i < effects.size(); i++) {
				for (int n = 0; n < 12; n++) {
					c.world.addParticle(effects.get(i), p.x + (i - 7) * 0.4, p.y + n * 0.1, p.z,
							(c.world.random.nextDouble() - 0.5) * 0.05, 0.02, (c.world.random.nextDouble() - 0.5) * 0.05);
				}
			}
			return null;
		});
		waitTicks(10);
		screenshot("05_particles");
		// Clear the synthetic effects so the skill screenshots only show what the skills produce.
		submitAndWait(c -> {
			ClientFxManager.clear();
			return null;
		});

		// Cast every skill through the real hotbar packets, six at a time.
		List<Skill> skills = new ArrayList<>(SkillRegistry.all());
		for (int start = 0; start < skills.size(); start += PlayerQi.SLOT_COUNT) {
			List<Skill> batch = skills.subList(start, Math.min(skills.size(), start + PlayerQi.SLOT_COUNT));
			for (int i = 0; i < PlayerQi.SLOT_COUNT; i++) {
				final int slot = i;
				final Skill skill = i < batch.size() ? batch.get(i) : null;
				submitAndWait(c -> {
					ClientPackets.sendSetSlot(slot, skill == null ? null : skill.getId());
					return null;
				});
			}
			waitTicks(10);
			for (int i = 0; i < batch.size(); i++) {
				Skill skill = batch.get(i);
				check(submitAndWait(c -> QiHolder.get(c.player).getSlotSkill(0)) != null, "slot sync received for batch starting at " + start);
				castSlot(i, skill);
			}
		}

		// Sword flight is a toggle: player must actually be riding a flying sword.
		int flightSlot = -1;
		for (int i = 0; i < skills.size(); i++) {
			if (skills.get(i) == SkillRegistry.SWORD_FLIGHT) flightSlot = i % PlayerQi.SLOT_COUNT;
		}
		final int flightSlotIndex = flightSlot;
		int flightBatchStart = skills.indexOf(SkillRegistry.SWORD_FLIGHT) / PlayerQi.SLOT_COUNT * PlayerQi.SLOT_COUNT;
		for (int i = 0; i < PlayerQi.SLOT_COUNT; i++) {
			final int slot = i;
			final Skill skill = flightBatchStart + i < skills.size() ? skills.get(flightBatchStart + i) : null;
			submitAndWait(c -> {
				ClientPackets.sendSetSlot(slot, skill == null ? null : skill.getId());
				return null;
			});
		}
		waitTicks(10);
		submitAndWait(c -> {
			c.options.setPerspective(Perspective.THIRD_PERSON_BACK);
			ClientPackets.sendCast(flightSlotIndex);
			return null;
		});
		waitTicks(15);
		boolean riding = submitAndWait(c -> c.player.getVehicle() != null && c.player.getVehicle().getType() == ModEntities.FLYING_SWORD);
		check(riding, "player rides a flying sword after casting sword flight");
		screenshot("06_sword_flight_third_person");
		submitAndWait(c -> {
			ClientPackets.sendCast(flightSlotIndex);
			return null;
		});
		waitTicks(15);
		check(submitAndWait(c -> !c.player.hasVehicle()), "player dismounted after toggling sword flight off");
		submitAndWait(c -> {
			c.options.setPerspective(Perspective.FIRST_PERSON);
			return null;
		});

		// Breakthrough packet (at max realm the server must refuse gracefully, no crash).
		submitAndWait(c -> {
			ClientPackets.sendBreakthrough();
			return null;
		});
		waitTicks(20);
		screenshot("07_final");

		// Leave the world cleanly and quit.
		LOG.info("[AutoTest] returning to title");
		submitAndWait(c -> {
			if (c.world != null) c.world.disconnect();
			c.disconnect(new MessageScreen(Text.translatable("menu.savingLevel")));
			c.setScreen(new TitleScreen());
			return null;
		});
		waitForScreen(TitleScreen.class);

		if (FAILURES.isEmpty()) {
			LOG.info("[AutoTest] RESULT: PASSED ({} screenshots)", screenshots);
			submitAndWait(c -> {
				c.scheduleStop();
				return null;
			});
		} else {
			for (String f : FAILURES) LOG.error("[AutoTest] failed check: {}", f);
			LOG.error("[AutoTest] RESULT: FAILED ({} checks)", FAILURES.size());
			System.exit(1);
		}
	}

	/** Skills whose first-person view matters most (they surround or start at the camera). */
	private static final List<String> FIRST_PERSON_SHOTS = List.of("sword_qi_slash", "flame_claw", "ice_arrows", "purple_thunder_beam", "tortoise_shield", "wind_blade_dance", "frozen_domain", "thousand_swords");

	private static void castSlot(int slot, Skill skill) {
		LOG.info("[AutoTest] casting {} from slot {}", skill.getId(), slot);
		boolean fp = FIRST_PERSON_SHOTS.contains(skill.getId().getPath());
		submitAndWait(c -> {
			// Deterministic aim: look along +Z, slightly down, so projectiles fly away from the camera.
			c.player.setYaw(0.0F);
			c.player.setPitch(4.0F);
			c.player.setHeadYaw(0.0F);
			c.player.setBodyYaw(0.0F);
			c.options.setPerspective(fp ? Perspective.FIRST_PERSON : Perspective.THIRD_PERSON_BACK);
			ClientPackets.sendCast(slot);
			return null;
		});
		// Fast projectiles are gone within ~20 ticks: first-person shot early, third-person shortly after.
		if (fp) {
			waitTicks(3);
			screenshot("skill_" + skill.getId().getPath() + "_fp", false);
			submitAndWait(c -> {
				c.options.setPerspective(Perspective.THIRD_PERSON_BACK);
				return null;
			});
			waitTicks(3);
		} else {
			waitTicks(6);
		}
		screenshot("skill_" + skill.getId().getPath(), false);
		waitTicks(6);
		if (skill.isChannel()) {
			waitTicks(20);
			submitAndWait(c -> {
				ClientPackets.sendRelease(slot);
				return null;
			});
		}
		waitTicks(18);
		submitAndWait(c -> {
			// Sword flight is handled separately; make sure later skills are cast on foot.
			if (c.player.hasVehicle()) ClientPackets.sendCast(slot);
			return null;
		});
		waitTicks(5);
		// Let long-lived effects (formations, orbiting swords, domes) fade before the next skill so each
		// screenshot shows one skill only. Capped so a stuck effect cannot stall the run.
		waitFor("effects of " + skill.getId().getPath() + " to end", c -> ClientFxManager.count() == 0 && !modEntitiesPresent(c), Duration.ofSeconds(12), true);
	}

	private static boolean modEntitiesPresent(MinecraftClient c) {
		if (c.world == null) return false;
		for (Entity e : c.world.getEntities()) {
			if (Registries.ENTITY_TYPE.getId(e.getType()).getNamespace().equals(CelestialArts.MOD_ID)) return true;
		}
		return false;
	}

	private static void command(String command) {
		LOG.info("[AutoTest] /{}", command);
		submitAndWait(c -> {
			Objects.requireNonNull(c.player).networkHandler.sendChatCommand(command);
			return null;
		});
		waitTicks(3);
	}

	private static void check(boolean condition, String what) {
		if (condition) {
			LOG.info("[AutoTest] ok: {}", what);
		} else {
			LOG.error("[AutoTest] FAIL: {}", what);
			FAILURES.add(what);
		}
	}

	private static void screenshot(String name) {
		screenshot(name, true);
	}

	private static void screenshot(String name, boolean settle) {
		if (settle) sleep(Duration.ofMillis(500));
		submitAndWait(c -> {
			ScreenshotRecorder.saveScreenshot(c.runDirectory, name + ".png", c.getFramebuffer(), message -> {
			});
			return null;
		});
		screenshots++;
		LOG.info("[AutoTest] screenshot {}", name);
	}

	private static void waitTicks(long ticks) {
		long start = submitAndWait(c -> Objects.requireNonNull(c.world).getTime());
		waitFor(ticks + " world ticks", c -> c.world == null || c.world.getTime() >= start + ticks, Duration.ofMinutes(2));
	}

	private static void waitForScreen(Class<? extends Screen> type) {
		waitFor("screen " + type.getSimpleName(), c -> c.currentScreen != null && c.currentScreen.getClass() == type, Duration.ofMinutes(2));
	}

	private static void waitFor(String what, Predicate<MinecraftClient> predicate, Duration timeout) {
		waitFor(what, predicate, timeout, false);
	}

	private static void waitFor(String what, Predicate<MinecraftClient> predicate, Duration timeout, boolean lenient) {
		long end = System.currentTimeMillis() + timeout.toMillis();
		while (true) {
			if (submitAndWait(predicate::test)) return;
			if (System.currentTimeMillis() > end) {
				if (lenient) {
					LOG.info("[AutoTest] gave up waiting for {}", what);
					return;
				}
				throw new IllegalStateException("Timed out waiting for " + what);
			}
			sleep(Duration.ofMillis(100));
		}
	}

	private static <T> T submitAndWait(Function<MinecraftClient, T> function) {
		MinecraftClient client = MinecraftClient.getInstance();
		return client.submit(() -> function.apply(client)).join();
	}

	private static void sleep(Duration duration) {
		try {
			Thread.sleep(duration.toMillis());
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException(e);
		}
	}
}
