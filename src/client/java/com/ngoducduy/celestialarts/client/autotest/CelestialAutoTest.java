package com.ngoducduy.celestialarts.client.autotest;

import com.ngoducduy.celestialarts.CelestialArts;
import net.minecraft.util.math.Direction;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.Hand;
import com.ngoducduy.celestialarts.screen.AlchemyFurnaceScreenHandler;
import com.ngoducduy.celestialarts.registry.ModRecipes;
import com.ngoducduy.celestialarts.client.gui.AlchemyFurnaceScreen;
import com.ngoducduy.celestialarts.block.entity.AlchemyFurnaceBlockEntity;
import com.ngoducduy.celestialarts.alchemy.AlchemyRecipe;
import com.ngoducduy.celestialarts.block.HerbBlock;
import com.ngoducduy.celestialarts.alchemy.Herbs;
import com.ngoducduy.celestialarts.alchemy.Herb;
import com.ngoducduy.celestialarts.client.ClientPackets;
import com.ngoducduy.celestialarts.client.gui.CultivationScreen;
import com.ngoducduy.celestialarts.client.gui.SkillBookScreen;
import com.ngoducduy.celestialarts.client.render.fx.ClientFxManager;
import com.ngoducduy.celestialarts.cultivation.CultivationStats;
import com.ngoducduy.celestialarts.cultivation.PlayerQi;
import com.ngoducduy.celestialarts.cultivation.QiHolder;
import com.ngoducduy.celestialarts.cultivation.Realm;
import com.ngoducduy.celestialarts.network.FxData;
import com.ngoducduy.celestialarts.network.FxType;
import com.ngoducduy.celestialarts.client.render.post.ScreenOverlay;
import com.ngoducduy.celestialarts.registry.GlowParticleEffect;
import com.ngoducduy.celestialarts.registry.ModEffects;
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
import net.minecraft.util.math.BlockPos;
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
import java.util.Locale;
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
		// Keeps "Teleported ..." feedback out of the cinematic screenshots.
		command("gamerule sendCommandFeedback false");
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
		int overlays = submitAndWait(c -> {
			Vec3d base = c.player.getPos();
			FxType[] types = FxType.values();
			for (int i = 0; i < types.length; i++) {
				double dx = (i % 6 - 2.5) * 3.0;
				double dz = 6.0 + (i / 6) * 4.0;
				Vec3d pos = base.add(dx, 0.0, dz);
				FxData data = FxData.at(types[i], pos, 0x9FE8FF, 1.0f, 200).withTarget(pos.add(0, 4, 0)).withExtra(8);
				if (types[i] == FxType.AFTERIMAGE) data = data.withEntity(c.player.getId());
				ClientFxManager.spawn(data);
			}
			// Checked right here: a screen flash is short-lived by design (clamped to ~1 s).
			return ScreenOverlay.count();
		});
		check(overlays >= 1, "screen overlay registered from SCREEN_FLASH fx");
		waitTicks(5);
		int fxCount = submitAndWait(c -> ClientFxManager.count());
		int worldFxTypes = (int) java.util.Arrays.stream(FxType.values()).filter(t -> t != FxType.SCREEN_FLASH).count();
		check(fxCount >= worldFxTypes, "all FX types alive after spawn (" + fxCount + "/" + worldFxTypes + ")");
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
		// The synthetic particles above live up to ~3 s; let them die so they do not leak into the
		// first skill screenshot.
		waitTicks(70);

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

		cultivationSequence();
		herbGarden();
		alchemySequence();

		// Breakthrough packet at the very top of cultivation: the server must refuse gracefully, no crash.
		command("celestial realm 6");
		command("celestial stage 3");
		waitTicks(5);
		submitAndWait(c -> {
			ClientPackets.sendBreakthrough();
			return null;
		});
		waitTicks(20);
		check(submitAndWait(c -> !c.player.hasVehicle()), "no meditation started by a refused breakthrough");
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
	private static final List<String> FIRST_PERSON_SHOTS = List.of("sword_qi_slash", "flame_claw", "ice_arrows", "purple_thunder_beam", "tortoise_shield", "wind_blade_dance", "frozen_domain", "vajra_palm", "thunder_dragon");
	/** Skills whose main visual only appears after a wind-up: extra ticks before the third-person shot. */
	/** Skills whose spectacle is behind the caster: shot from the front camera looking back at the player. */
	private static final List<String> FRONT_SHOTS = List.of("thousand_swords");
	private static final java.util.Map<String, Integer> SHOT_DELAY = java.util.Map.of("thunder_dragon", 18, "wind_dragon", 4, "golden_body", 4, "heaven_sword", 4, "thousand_swords", 32);
	/** Aim pitch per skill: projectiles fired straight ahead are hidden behind the player in third person, so tilt those up. */
	private static final java.util.Map<String, Float> AIM_PITCH = java.util.Map.of("thunder_dragon", -14.0F, "fire_lotus", -10.0F, "ice_arrows", -8.0F);
	/**
	 * The third-person camera shares the player's yaw and pitch, so anything fired straight along the look vector
	 * stays hidden behind the player's own head. For those skills the player turns away this many ticks after the
	 * cast (after the projectile has been released) so the camera looks at the projectile from the side.
	 */
	private static final java.util.Map<String, Integer> TURN_AT = java.util.Map.of("thunder_dragon", 20, "fire_lotus", 4, "ice_arrows", 4, "sword_qi_slash", 4, "wind_dragon", 6, "vajra_palm", 3);
	private static final float TURN_YAW = 55.0F;

	private static void castSlot(int slot, Skill skill) {
		LOG.info("[AutoTest] casting {} from slot {}", skill.getId(), slot);
		if (skill == SkillRegistry.HEAVEN_HAND) {
			castHeavenHand(slot);
			return;
		}
		boolean fp = FIRST_PERSON_SHOTS.contains(skill.getId().getPath());
		submitAndWait(c -> {
			// Deterministic aim: look along +Z, slightly down, so projectiles fly away from the camera.
			c.player.setYaw(0.0F);
			c.player.setPitch(AIM_PITCH.getOrDefault(skill.getId().getPath(), 4.0F));
			c.player.setHeadYaw(0.0F);
			c.player.setBodyYaw(0.0F);
			c.options.setPerspective(fp ? Perspective.FIRST_PERSON
					: FRONT_SHOTS.contains(skill.getId().getPath()) ? Perspective.THIRD_PERSON_FRONT : Perspective.THIRD_PERSON_BACK);
			ClientPackets.sendCast(slot);
			return null;
		});
		// Fast projectiles are gone within ~20 ticks: first-person shot early, third-person shortly after.
		if (fp) {
			// Projectiles must be caught before they leave; a channelled beam needs a couple of ticks for
			// the cast packet round trip plus its fade-in, otherwise the shot may show nothing at all.
			waitTicks(skill.isChannel() ? 6 : 3);
			screenshot("skill_" + skill.getId().getPath() + "_fp", false);
			submitAndWait(c -> {
				c.options.setPerspective(Perspective.THIRD_PERSON_BACK);
				return null;
			});
			waitTicks(3);
		} else {
			waitTicks(6);
		}
		int elapsed = 6;
		int shotAt = elapsed + SHOT_DELAY.getOrDefault(skill.getId().getPath(), 0);
		Integer turnAt = TURN_AT.get(skill.getId().getPath());
		if (turnAt != null) {
			int t = Math.max(elapsed, Math.min(turnAt, shotAt - 2));
			waitTicks(t - elapsed);
			elapsed = t;
			submitAndWait(c -> {
				c.player.setYaw(TURN_YAW);
				c.player.setHeadYaw(TURN_YAW);
				c.player.setBodyYaw(TURN_YAW);
				c.player.prevYaw = TURN_YAW;
				c.player.prevHeadYaw = TURN_YAW;
				c.player.prevBodyYaw = TURN_YAW;
				return null;
			});
		}
		waitTicks(Math.max(2, shotAt - elapsed));
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

	/**
	 * Thiên Đạo Chi Thủ is a 16.5-second world-scale ritual that cannot be released. The camera is
	 * directed like a film: first person from the caster for the sky formation and the hand breaking
	 * through it, a wide shot from 130 blocks beside the target for the descent / impact / shock ring,
	 * and a high angle over the palm print at the end. Every shot is scheduled on the world clock
	 * (not on accumulated waits) because a software-rendered screenshot costs the better part of a
	 * second and eight of them would otherwise drift the whole sequence by ~100 ticks.
	 */
	private static void castHeavenHand(int slot) {
		Vec3d home = submitAndWait(c -> c.player.getPos());
		long t0 = submitAndWait(c -> {
			c.player.setYaw(0.0F);
			c.player.setHeadYaw(0.0F);
			c.player.setBodyYaw(0.0F);
			c.player.setPitch(0.0F);
			c.options.setPerspective(Perspective.FIRST_PERSON);
			ClientPackets.sendCast(slot);
			return Objects.requireNonNull(c.world).getTime();
		});
		// Wide vantage: 155 blocks from the target point (72 blocks ahead of the caster) on the
		// front-left diagonal, hovering 6 blocks up, looking +X+Z – the hand (110 wide, ~180 long
		// with the fingers) is seen three-quarter on instead of edge-on, 175 blocks out and 28 up.
		String wide = String.format(Locale.ROOT, "tp @s %.2f %.2f %.2f -45 -6", home.x - 124.0, home.y + 28.0, home.z + 72.0 - 124.0);
		String high = String.format(Locale.ROOT, "tp @s %.2f %.2f %.2f -90 58", home.x - 60.0, home.y + 110.0, home.z + 72.0);
		String back = String.format(Locale.ROOT, "tp @s %.2f %.2f %.2f 0 4", home.x, home.y, home.z);

		// From the caster, looking up: the eight-trigram array unfolding 120 blocks up.
		lookAt(t0 + 45, -60.0F);
		screenshot("skill_heaven_hand_1_formation", false);
		// The hand has pushed through the array and begins its descent.
		lookAt(t0 + 125, -46.0F);
		screenshot("skill_heaven_hand_2_descent", false);

		waitUntil(t0 + 135);
		submitAndWait(c -> {
			c.player.getAbilities().flying = true;
			c.player.sendAbilitiesUpdate();
			return null;
		});
		command(wide);
		lookAt(t0 + 200, -10.0F);
		screenshot("skill_heaven_hand_3_approach", false);
		lookAt(t0 + 243, -4.0F);
		screenshot("skill_heaven_hand_4_impact", false);
		lookAt(t0 + 253, -4.0F);
		screenshot("skill_heaven_hand", false);
		lookAt(t0 + 266, -2.0F);
		screenshot("skill_heaven_hand_6_shock_ring", false);
		lookAt(t0 + 288, -6.0F);
		screenshot("skill_heaven_hand_7_dissolve", false);

		// High angle over the palm print and the ring of heaved rock once the hand has gone (the
		// teleport happens 50 ticks early so the chunks around the print are loaded by then).
		waitUntil(t0 + 291);
		command(high);
		lookAt(t0 + 342, 58.0F);
		screenshot("skill_heaven_hand_8_palm_print", false);

		// Return home on foot and drop the two-minute palm print so it does not leak into the
		// following screenshots.
		waitUntil(t0 + 352);
		command(back);
		submitAndWait(c -> {
			c.player.getAbilities().flying = false;
			c.player.sendAbilitiesUpdate();
			c.player.setPitch(4.0F);
			c.player.prevPitch = 4.0F;
			c.options.setPerspective(Perspective.THIRD_PERSON_BACK);
			ClientFxManager.clear();
			return null;
		});
		waitTicks(8);
	}

	/**
	 * 1.4.0 cultivation loop, end to end through the real packets: one press sits the player on a
	 * meditation seat, the Đạo Cơ panel opens, a real tribulation (cloud, bolts, pillar) carries a
	 * peak-stage Luyện Khí cultivator to Trúc Cơ, and the qi-deviation vignette is shown last.
	 */
	private static void cultivationSequence() {
		command("celestial realm 1");
		command("celestial stage 3");
		command("celestial root metal+fire 4");
		command("celestial talent dao_heart");
		waitTicks(5);
		waitFor("player back on the ground", c -> c.player != null && c.player.isOnGround(), Duration.ofSeconds(15), true);

		// Sit down. Third-person front so the seat, the array under it and the motes are all visible.
		submitAndWait(c -> {
			c.inGameHud.getChatHud().clear(false); // the seat sits exactly where the chat lines are
			c.options.setPerspective(Perspective.THIRD_PERSON_FRONT);
			c.player.setPitch(22.0F);
			c.player.prevPitch = 22.0F;
			ClientPackets.sendMeditate();
			return null;
		});
		waitTicks(70);
		check(submitAndWait(c -> c.player.getVehicle() != null && c.player.getVehicle().getType() == ModEntities.MEDITATION_SEAT), "one press seats the player on a meditation seat");
		check(submitAndWait(c -> QiHolder.get(c.player).isMeditating()), "meditating flag synced to the client");
		screenshot("08_meditation");

		// The cultivation panel over the seated player.
		submitAndWait(c -> {
			c.inGameHud.getChatHud().clear(false);
			c.setScreen(new CultivationScreen());
			return null;
		});
		waitTicks(5);
		screenshot("09_cultivation_screen");
		submitAndWait(c -> {
			c.setScreen(null);
			return null;
		});

		// Enough cultivation for the gate, then the tribulation. Creative players shrug the bolts off.
		command("celestial exp 99999");
		waitTicks(10);
		int bolts = submitAndWait(c -> CultivationStats.tribulationBolts(QiHolder.get(c.player)));
		check(bolts >= 7 && bolts <= 12, "tribulation bolt count from the synced root/talent (aptitude 81), was " + bolts);
		long t0 = submitAndWait(c -> {
			c.options.setPerspective(Perspective.THIRD_PERSON_BACK);
			ClientPackets.sendBreakthrough();
			return Objects.requireNonNull(c.world).getTime();
		});
		waitTicks(10);
		check(submitAndWait(c -> c.player.hasVehicle()), "player stays seated while the tribulation gathers");
		// Cloud 16 blocks up: from the third-person camera 4 blocks back that is ~75 degrees up.
		lookAt(t0 + 42, -60.0F);
		screenshot("10_tribulation_cloud", false);
		// Second bolt, mid-flash: bolts land at t0+80, +100, ...; the bolt FX lives 14 ticks.
		lookAt(t0 + 101, -40.0F);
		screenshot("11_tribulation_bolt", false);
		// Success at FIRST_BOLT + (bolts-1)*20 + 30: heaven pillar + burst + white ring.
		long success = t0 + 80 + (bolts - 1) * 20L + 30;
		lookAt(success + 12, -18.0F);
		screenshot("12_tribulation_success", false);
		waitUntil(success + 45);
		check(submitAndWait(c -> QiHolder.get(c.player).getRealm() == Realm.FOUNDATION), "tribulation carried the player to Foundation");
		check(submitAndWait(c -> !c.player.hasVehicle()), "trance ended after the breakthrough");

		// Tẩu hỏa nhập ma vignette, first person.
		submitAndWait(c -> {
			c.options.setPerspective(Perspective.FIRST_PERSON);
			c.player.setPitch(4.0F);
			c.player.prevPitch = 4.0F;
			return null;
		});
		command("effect give @s celestialarts:qi_deviation 20 0");
		waitTicks(48);
		check(submitAndWait(c -> c.player.hasStatusEffect(ModEffects.QI_DEVIATION)), "qi deviation effect applied");
		screenshot("13_qi_deviation", false);
		command("effect clear @s");
		submitAndWait(c -> {
			ClientFxManager.clear();
			return null;
		});
		waitTicks(10);
	}

	/**
	 * 1.5.0 alchemy, part 1: a bed of linh dược. One mature specimen of every species is set out
	 * in a grid on the flat world (grass, so only the soil-rooting species survive – the rest
	 * are placed on the ground they need) and photographed from a low front angle.
	 */
	private static void herbGarden() {
		Vec3d home = submitAndWait(c -> Objects.requireNonNull(c.player).getPos());
		int bx = (int) Math.floor(home.x) + 30;
		int by = (int) Math.floor(home.y);
		int bz = (int) Math.floor(home.z) + 30;
		List<Herb> herbs = Herbs.all();
		int cols = 13;
		for (int i = 0; i < herbs.size(); i++) {
			Herb herb = herbs.get(i);
			int x = bx + (i % cols) * 2;
			int z = bz + (i / cols) * 2;
			String ground = switch (herb.habitat().getGround()) {
				case SOIL, SOIL_OR_STONE, SOIL_OR_SNOW, SAND_OR_SOIL -> "minecraft:grass_block";
				case STONE -> "minecraft:stone";
				case NETHER -> "minecraft:crimson_nylium";
				case END -> "minecraft:end_stone";
			};
			command(String.format(Locale.ROOT, "setblock %d %d %d %s", x, by - 1, z, ground));
			command(String.format(Locale.ROOT, "setblock %d %d %d celestialarts:%s[age=2]", x, by, z, herb.blockKey()));
		}
		waitTicks(10);
		int placed = submitAndWait(c -> {
			int n = 0;
			for (int i = 0; i < herbs.size(); i++) {
				BlockPos pos = new BlockPos(bx + (i % cols) * 2, by, bz + (i / cols) * 2);
				if (c.world.getBlockState(pos).getBlock() instanceof HerbBlock) n++;
			}
			return n;
		});
		check(placed == herbs.size(), "all " + herbs.size() + " herb species placed and survived (" + placed + ")");
		// Camera in front of the bed, low and slightly above, looking along +Z over the rows.
		command(String.format(Locale.ROOT, "tp @s %.2f %.2f %.2f 0 18", bx + cols - 1.0, by + 2.2, bz - 5.5));
		submitAndWait(c -> {
			c.inGameHud.getChatHud().clear(false);
			c.options.setPerspective(Perspective.FIRST_PERSON);
			return null;
		});
		waitTicks(40);
		screenshot("14_herb_garden");
		// Close-up of the rare (grade 4–5) row corner.
		command(String.format(Locale.ROOT, "tp @s %.2f %.2f %.2f 35 22", bx + 2.0, by + 1.6, bz - 2.5));
		waitTicks(20);
		screenshot("15_herb_closeup");
		command(String.format(Locale.ROOT, "tp @s %.2f %.2f %.2f 0 4", home.x, home.y, home.z));
		waitTicks(10);
	}

	/**
	 * 1.5.0 alchemy, part 2: a Liệt Diễm Lô (Bảo cấp) is set down next to the player, loaded with
	 * the herbs of a grade-1 recipe and a Phàm Hỏa through /item, opened through a real block
	 * interaction, lit with the GUI button and steered to the end with the +/− fire and inject-qi
	 * buttons, exactly as a player would. Screenshots of the idle screen, mid-run and the result.
	 */
	private static void alchemySequence() {
		Vec3d home = submitAndWait(c -> Objects.requireNonNull(c.player).getPos());
		int fx = (int) Math.floor(home.x) + 2;
		int fy = (int) Math.floor(home.y);
		int fz = (int) Math.floor(home.z);
		command(String.format(Locale.ROOT, "setblock %d %d %d celestialarts:alchemy_furnace_fierce_treasure[facing=west]", fx, fy, fz));
		AlchemyRecipe recipe = submitAndWait(c -> c.world.getRecipeManager().listAllOfType(ModRecipes.ALCHEMY_TYPE).stream()
				.filter(r -> r.grade() == 1 && r.fireTier() == 1).findFirst().orElse(null));
		check(recipe != null, "client received the alchemy recipes");
		if (recipe == null) return;
		int slot = 0;
		for (AlchemyRecipe.IngredientStack ing : recipe.ingredientStacks()) {
			command(String.format(Locale.ROOT, "item replace block %d %d %d container.%d with %s %d", fx, fy, fz, slot++, Registries.ITEM.getId(ing.item()), ing.count()));
		}
		command(String.format(Locale.ROOT, "item replace block %d %d %d container.%d with celestialarts:flame_mortal 1", fx, fy, fz, AlchemyFurnaceBlockEntity.SLOT_FLAME));
		command(String.format(Locale.ROOT, "tp @s %.2f %.2f %.2f -90 25", home.x, home.y, home.z));
		waitTicks(10);
		screenshot("16_alchemy_furnace_block");
		// Open the furnace with a real right-click on the block.
		BlockPos furnacePos = new BlockPos(fx, fy, fz);
		submitAndWait(c -> {
			c.inGameHud.getChatHud().clear(false);
			BlockHitResult hit = new BlockHitResult(Vec3d.ofCenter(furnacePos), Direction.UP, furnacePos, false);
			c.interactionManager.interactBlock(c.player, Hand.MAIN_HAND, hit);
			return null;
		});
		waitFor("alchemy furnace screen", c -> c.currentScreen instanceof AlchemyFurnaceScreen, Duration.ofSeconds(10), true);
		waitTicks(10);
		screenshot("17_alchemy_gui_idle");
		AlchemyFurnaceScreenHandler handler = submitAndWait(c -> c.currentScreen instanceof AlchemyFurnaceScreen s ? s.getScreenHandler() : null);
		check(handler != null, "furnace screen handler open");
		if (handler == null) return;
		submitAndWait(c -> {
			c.interactionManager.clickButton(handler.syncId, AlchemyFurnaceBlockEntity.BTN_START);
			return null;
		});
		waitFor("refining state synced to the GUI", c -> handler.isRefining(), Duration.ofSeconds(5), true);
		check(handler.isRefining(), "refining started from the GUI button");
		// Steer: one button press per tick at most, like a quick-fingered player.
		int total = recipe.time();
		boolean midShot = false;
		for (int t = 0; t < total + 60; t++) {
			int state = handler.get(AlchemyFurnaceBlockEntity.P_STATE);
			if (state != AlchemyFurnaceBlockEntity.STATE_REFINING) break;
			float heat = handler.get(AlchemyFurnaceBlockEntity.P_HEAT) / 10.0F;
			float target = handler.get(AlchemyFurnaceBlockEntity.P_TARGET) / 10.0F;
			int level = handler.get(AlchemyFurnaceBlockEntity.P_FIRE_LEVEL);
			int qiMin = handler.get(AlchemyFurnaceBlockEntity.P_QI_PHASE);
			float qi = handler.get(AlchemyFurnaceBlockEntity.P_QI) / 10.0F;
			final int btn;
			if (qiMin >= 0 && qi < qiMin + 10) btn = AlchemyFurnaceBlockEntity.BTN_QI;
			else if (heat < target - 1 && level < AlchemyFurnaceBlockEntity.MAX_FIRE_LEVEL) btn = AlchemyFurnaceBlockEntity.BTN_MORE;
			else if (heat > target + 1 && level > 0) btn = AlchemyFurnaceBlockEntity.BTN_LESS;
			else btn = -1;
			if (btn >= 0) {
				submitAndWait(c -> {
					if (c.currentScreen instanceof AlchemyFurnaceScreen) c.interactionManager.clickButton(handler.syncId, btn);
					return null;
				});
			}
			if (!midShot && handler.get(AlchemyFurnaceBlockEntity.P_PROGRESS) >= 500) {
				midShot = true;
				screenshot("18_alchemy_refining", false);
			}
			waitTicks(1);
		}
		waitTicks(10);
		int finalState = handler.get(AlchemyFurnaceBlockEntity.P_STATE);
		LOG.info("[AutoTest] alchemy run ended in state {} (damage {}, score {})", finalState,
				handler.get(AlchemyFurnaceBlockEntity.P_DAMAGE) / 10.0F, handler.get(AlchemyFurnaceBlockEntity.P_SCORE) / 1000.0F);
		check(finalState != AlchemyFurnaceBlockEntity.STATE_REFINING, "alchemy run finished");
		check(handler.get(AlchemyFurnaceBlockEntity.P_DAMAGE) < 1000, "GUI steering kept the batch from ruin");
		screenshot("19_alchemy_result", false);
		submitAndWait(c -> {
			c.player.closeHandledScreen();
			return null;
		});
		waitTicks(5);
	}

	/** Waits for the world clock, then points the camera two ticks before the shot so interpolation has settled. */
	private static void lookAt(long worldTick, float pitch) {
		waitUntil(worldTick - 2);
		submitAndWait(c -> {
			c.player.setPitch(pitch);
			c.player.prevPitch = pitch;
			return null;
		});
		waitUntil(worldTick);
	}

	private static void waitUntil(long worldTick) {
		waitFor("world tick " + worldTick, c -> c.world == null || c.world.getTime() >= worldTick, Duration.ofMinutes(2));
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
