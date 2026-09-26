package com.ngoducduy.celestialarts.skill.skills;

import com.ngoducduy.celestialarts.cultivation.Realm;
import com.ngoducduy.celestialarts.entity.RockSpikeEntity;
import com.ngoducduy.celestialarts.network.FxData;
import com.ngoducduy.celestialarts.network.FxType;
import com.ngoducduy.celestialarts.network.ModPackets;
import com.ngoducduy.celestialarts.registry.ModDamageTypes;
import com.ngoducduy.celestialarts.registry.ModEffects;
import com.ngoducduy.celestialarts.registry.ModEntities;
import com.ngoducduy.celestialarts.registry.ModSounds;
import com.ngoducduy.celestialarts.skill.Element;
import com.ngoducduy.celestialarts.skill.Skill;
import com.ngoducduy.celestialarts.skill.SkillContext;
import com.ngoducduy.celestialarts.skill.SkillType;
import com.ngoducduy.celestialarts.skill.cast.ActiveCast;
import com.ngoducduy.celestialarts.util.EntityUtil;
import com.ngoducduy.celestialarts.util.SkillFx;
import com.ngoducduy.celestialarts.util.Targeting;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/**
 * Thiên Đạo Chi Thủ – the Hand of the Heavenly Dao. The pinnacle of the Tribulation realm and unlike
 * any other art: it is not an attack the caster throws, it is the caster borrowing the will of the
 * heavens for sixteen seconds.
 *
 * <ol>
 *   <li><b>Khai Thiên</b> (0–4 s): a formation of eight trigrams unfolds 120 blocks above the target
 *       point (72 blocks ahead of the caster) and the world darkens for everyone inside the
 *       {@value #R_DOMAIN}-block domain. The caster stands rooted, wrapped in heavenly light,
 *       nearly invulnerable but unable to move or cast anything else.</li>
 *   <li><b>Giáng Lâm</b> (4–12 s): a {@value #HAND_WIDTH}-block-wide palm pushes through the array and
 *       descends – slowly, then faster and faster. Everything living inside the domain is
 *       <i>Trấn Áp</i>: pressed to the ground, unable to jump or fly, dragged out of the air, and
 *       crushed for continuous heaven damage that grows the closer it is to the palm's centre.</li>
 *   <li><b>Trấn Áp</b> (12 s): the palm strikes. Within {@value #R_CORE} blocks of the centre the
 *       blow is almost always fatal ({@value #CORE_DAMAGE_MIN}–{@value #CORE_DAMAGE_MAX} heaven
 *       damage) and hurls victims outward; a shock ring then races to the edge of the domain at six
 *       blocks per tick, hitting every entity once for {@value #RING_DAMAGE_MIN}–{@value #RING_DAMAGE_MAX}
 *       damage and flinging it. Every player in the domain sees a white-out, feels the camera shake
 *       and hears the impact arrive with the correct delay for the speed of sound.</li>
 *   <li><b>Quy Thiên</b> (13.5–16.5 s): the hand dissolves into light; a golden palm print
 *       {@value #HAND_WIDTH} blocks across is seared into the ground for two minutes.</li>
 * </ol>
 */
public class HeavenHandSkill extends Skill {
	/** Distance in front of the caster where the palm centres. */
	public static final double TARGET_RANGE = 72.0;
	/** Radius of the suppression domain and of the shock ring. */
	public static final double R_DOMAIN = 220.0;
	/** Radius of the lethal core under the palm. */
	public static final double R_CORE = 40.0;
	/** Width of the hand in blocks (drives the client model scale and the palm print). */
	public static final float HAND_WIDTH = 110.0f;
	/** Everyone this far from the centre receives the world-scale effect packets. */
	public static final double FX_RANGE_FAR = 520.0;

	public static final int T_SUMMON = 80;
	public static final int T_SLAM = 240;
	public static final int T_END = 330;
	/** Rock spikes heaved up around the print after the slam (two per tick). */
	private static final int SPIKES = 12;
	public static final double RING_SPEED = 6.0;
	public static final int RING_TICKS = (int) Math.ceil(R_DOMAIN / RING_SPEED);

	public static final float CORE_DAMAGE_MIN = 60.0f;
	public static final float CORE_DAMAGE_MAX = 220.0f;
	public static final float RING_DAMAGE_MIN = 10.0f;
	public static final float RING_DAMAGE_MAX = 60.0f;

	private static final int GOLD = 0xFFD86B;
	private static final int PALE = 0xFFF3CC;
	private static final double SPEED_OF_SOUND = 17.0; // blocks per tick (≈340 m/s)

	public HeavenHandSkill() {
		super(Settings.of(Element.DAO, SkillType.ULTIMATE, Realm.TRIBULATION, 520f, 6000));
	}

	@Override
	public boolean activate(SkillContext ctx) {
		ServerPlayerEntity player = ctx.player();
		// Wide ground search: the palm must land on the terrain even when aimed across a valley or from a peak.
		Vec3d center = Targeting.snapToGround(player.getWorld(), Targeting.lookPoint(player, TARGET_RANGE), 64);
		Vec3d look = player.getRotationVec(1.0f);
		Vec3d dir = new Vec3d(look.x, 0.0, look.z);
		dir = dir.lengthSquared() < 1.0E-4 ? new Vec3d(0, 0, 1) : dir.normalize();
		player.swingHand(Hand.MAIN_HAND, true);
		ctx.qi().addActiveCast(new HeavenHandCast(player, this, center, dir));
		return true;
	}

	/** Players (spectators included) inside {@code range} of {@code center} – recipients of per-viewer overlays. */
	private static List<ServerPlayerEntity> viewers(net.minecraft.server.world.ServerWorld world, Vec3d center, double range) {
		List<ServerPlayerEntity> out = new ArrayList<>();
		for (ServerPlayerEntity p : world.getPlayers()) {
			if (p.getPos().squaredDistanceTo(center) <= range * range) out.add(p);
		}
		return out;
	}

	private static final class HeavenHandCast extends ActiveCast {
		private final Vec3d center;
		private final Vec3d dir;
		/** Entities already struck by the expanding shock ring. */
		private final Set<Integer> ringHit = new HashSet<>();
		/** Delayed impact sounds: entries of {@code {playerId, tick}} for the speed-of-sound rumble. */
		private final List<int[]> rumbles = new ArrayList<>();

		HeavenHandCast(ServerPlayerEntity caster, Skill skill, Vec3d center, Vec3d dir) {
			super(caster, skill, T_END);
			this.center = center;
			this.dir = dir;
		}

		@Override
		public boolean isChannel() {
			// Locks out every other skill while the heavens are borrowed; onRelease() is a no-op so the
			// cast cannot be interrupted by letting go of the key – only by the caster dying/leaving.
			return true;
		}

		@Override
		protected void onTick() {
			if (age == 0) summon();
			// The caster is rooted and shielded by heaven's light for the whole ritual.
			if (age % 20 == 0) {
				caster.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 45, 3, false, false, false), caster);
				caster.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 45, 6, false, false, false), caster);
			}
			if (age % 5 == 0) SkillFx.goldenLight(world, caster.getEyePos().add(0, 0.6, 0), 4, 1.2);

			if (age >= T_SUMMON && age < T_SLAM) {
				if ((age - T_SUMMON) % 10 == 0) suppress(age);
				if ((age - T_SUMMON) % 40 == 0) {
					world.playSound(null, BlockPos.ofFloored(center), ModSounds.HEAVEN_HAND_PRESSURE, SoundCategory.PLAYERS, 6.0f, 0.9f + 0.2f * ((age - T_SUMMON) / (float) (T_SLAM - T_SUMMON)));
				}
			}
			if (age == T_SLAM) slam();
			if (age > T_SLAM && age <= T_SLAM + RING_TICKS) ring(age - T_SLAM);
			if (age > T_SLAM) heaveEarth(age - T_SLAM);
			if (!rumbles.isEmpty()) playRumbles();
		}

		private void summon() {
			// extra carries the domain radius so the client shock ring travels exactly as far as the damage does.
			ModPackets.sendFx(world, FxData.at(FxType.HEAVEN_HAND, center, GOLD, HAND_WIDTH, T_END).withTarget(dir).withExtra((int) R_DOMAIN), FX_RANGE_FAR);
			ModPackets.sendFx(world, FxData.follow(FxType.HEAVEN_PILLAR, caster.getId(), caster.getPos(), PALE, 2.2f, T_END));
			ModPackets.sendFx(world, FxData.follow(FxType.QI_AURA, caster.getId(), caster.getPos(), GOLD, 1.6f, T_END));
			ModPackets.sendFx(world, FxData.at(FxType.MAGIC_CIRCLE, caster.getPos().add(0, 0.06, 0), GOLD, 4.0f, T_END).withExtra(1));
			// The sky darkens for everyone in the domain – sent per player at their own eyes so the
			// 24-block falloff of SCREEN_FLASH never applies.
			for (ServerPlayerEntity p : viewers(world, center, R_DOMAIN + 40)) {
				ModPackets.sendFxTo(p, FxData.at(FxType.SCREEN_FLASH, p.getEyePos(), 0x120C04, 0.55f, T_SLAM + 20).withExtra(1));
			}
			world.playSound(null, caster.getBlockPos(), ModSounds.HEAVEN_HAND_SUMMON, SoundCategory.PLAYERS, 8.0f, 1.0f);
			world.playSound(null, caster.getBlockPos(), ModSounds.RISER, SoundCategory.PLAYERS, 2.0f, 0.8f);
			SkillFx.runes(world, caster.getPos().add(0, 0.3, 0), 24, 3.5);
			ModPackets.sendCameraShake(world, center, R_DOMAIN, 0.35f, 60);
		}

		/** Trấn Áp: pin and crush everything living inside the domain. */
		private void suppress(int now) {
			float u = (now - T_SUMMON) / (float) (T_SLAM - T_SUMMON);
			List<LivingEntity> victims = EntityUtil.inSphere(world, caster, center, R_DOMAIN);
			victims.sort(Comparator.comparingDouble(e -> e.squaredDistanceTo(center)));
			int shown = 0;
			for (LivingEntity target : victims) {
				double d = Math.sqrt(target.squaredDistanceTo(center));
				float p = (float) MathHelper.clamp(1.0 - d / R_DOMAIN, 0.0, 1.0);
				int amplifier = d < R_CORE ? 3 : d < R_DOMAIN * 0.5 ? 2 : d < R_DOMAIN * 0.8 ? 1 : 0;
				// Effect refreshed a little beyond the next pulse so it never flickers off.
				target.addStatusEffect(new StatusEffectInstance(ModEffects.SUPPRESSED, 30, amplifier, false, false, true), caster);
				if ((now - T_SUMMON) % 20 == 0) {
					float dmg = 1.5f + 4.5f * p * (0.5f + 0.5f * u);
					target.damage(ModDamageTypes.source(world, ModDamageTypes.HEAVEN, caster), dmg);
				}
				if (shown < 24) {
					shown++;
					Vec3d c = target.getBoundingBox().getCenter();
					SkillFx.goldenLight(world, c.add(0, target.getHeight() * 0.6, 0), 3, target.getWidth() * 0.8);
					if ((now - T_SUMMON) % 20 == 0) {
						ModPackets.sendFx(world, FxData.at(FxType.SHOCKWAVE_RING, target.getPos().add(0, 0.05, 0), GOLD, 1.2f + target.getWidth(), 8));
					}
				}
			}
		}

		/** The palm strikes the earth. */
		private void slam() {
			// Lethal core.
			for (LivingEntity target : EntityUtil.inSphere(world, caster, center, R_CORE)) {
				double d = Math.sqrt(target.squaredDistanceTo(center));
				float f = (float) Math.pow(MathHelper.clamp(1.0 - d / R_CORE, 0.0, 1.0), 1.5);
				float dmg = CORE_DAMAGE_MIN + (CORE_DAMAGE_MAX - CORE_DAMAGE_MIN) * f;
				ringHit.add(target.getId());
				if (target.damage(ModDamageTypes.source(world, ModDamageTypes.HEAVEN, caster), dmg)) {
					EntityUtil.knockback(target, center, 1.8 + 1.2 * f, 0.9 + 0.6 * f);
					ModPackets.sendFx(world, FxData.at(FxType.ENERGY_BURST, target.getBoundingBox().getCenter(), PALE, 2.4f, 10));
				}
			}
			// Visuals that live outside the hand effect: the print, a ring for near viewers, the flash.
			ModPackets.sendFx(world, FxData.at(FxType.GROUND_DECAL, center.add(0, 0.04, 0), GOLD, HAND_WIDTH * 0.5f, 2400).withExtra(2).withTarget(dir), FX_RANGE_FAR);
			ModPackets.sendFx(world, FxData.at(FxType.SHOCKWAVE_RING, center.add(0, 0.1, 0), PALE, (float) R_CORE * 1.5f, 24), FX_RANGE_FAR);
			ModPackets.sendCameraShake(world, center, FX_RANGE_FAR, 4.0f, 45);
			for (ServerPlayerEntity p : viewers(world, center, FX_RANGE_FAR)) {
				double d = Math.sqrt(p.getPos().squaredDistanceTo(center));
				float strength = (float) MathHelper.clamp(1.15 - d / FX_RANGE_FAR, 0.35, 1.0);
				ModPackets.sendFxTo(p, FxData.at(FxType.SCREEN_FLASH, p.getEyePos(), 0xFFFFFF, strength, 18));
				// The slam itself is heard at once (skill audio); the deep rumble arrives at the speed of sound.
				rumbles.add(new int[]{p.getId(), age + (int) (d / SPEED_OF_SOUND)});
			}
			world.playSound(null, BlockPos.ofFloored(center), ModSounds.HEAVEN_HAND_SLAM, SoundCategory.PLAYERS, 16.0f, 1.0f);
			world.playSound(null, BlockPos.ofFloored(center), ModSounds.SUB_DROP, SoundCategory.PLAYERS, 6.0f, 0.7f);
			SkillFx.rockDebris(world, center, 80, 1.6);
			SkillFx.goldenLight(world, center.add(0, 2.0, 0), 60, R_CORE * 0.6);
		}

		/**
		 * The earth heaved up around the palm: a broken ring of rock spikes erupts just outside the
		 * print (staggered over the ticks after the slam, nearest the fingers first) so the impact
		 * leaves real geometry behind, not only light.
		 */
		private void heaveEarth(int sinceSlam) {
			int i = sinceSlam - 1;
			if (i < 0 || i >= SPIKES) return;
			for (int k = 0; k < 2; k++) {
				int n = i * 2 + k;
				double a = Math.atan2(dir.x, dir.z) + n * (Math.PI * 2 / (SPIKES * 2)) + world.random.nextGaussian() * 0.04;
				double r = HAND_WIDTH * (0.58 + 0.10 * world.random.nextDouble());
				Vec3d p = center.add(Math.sin(a) * r, 0, Math.cos(a) * r);
				Vec3d ground = Targeting.snapToGround(world, p.add(0, 2, 0), 12);
				float height = 9.0f + 7.0f * world.random.nextFloat();
				RockSpikeEntity.spawn(world, caster, ground, height, 14f, ModEntities.ROCK_SPIKE);
				ModPackets.sendFx(world, FxData.at(FxType.SHOCKWAVE_RING, ground.add(0, 0.1, 0), 0xC69C5B, 3.0f, 10), FX_RANGE_FAR);
			}
		}

		/** Expanding shock ring: hits each entity once when the front passes it. */
		private void ring(int sinceSlam) {
			double rNow = sinceSlam * RING_SPEED;
			double rPrev = (sinceSlam - 1) * RING_SPEED;
			for (LivingEntity target : EntityUtil.inSphere(world, caster, center, Math.min(rNow + 2.0, R_DOMAIN))) {
				double d = Math.sqrt(target.squaredDistanceTo(center));
				if (d < rPrev - 2.0 || !ringHit.add(target.getId())) continue;
				float p = (float) MathHelper.clamp(1.0 - d / R_DOMAIN, 0.0, 1.0);
				float dmg = RING_DAMAGE_MIN + (RING_DAMAGE_MAX - RING_DAMAGE_MIN) * p;
				if (target.damage(ModDamageTypes.source(world, ModDamageTypes.HEAVEN, caster), dmg)) {
					EntityUtil.knockback(target, center, 1.2 + 1.0 * p, 0.5 + 0.4 * p);
					target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 60, 2, false, false, true), caster);
					SkillFx.rockDebris(world, target.getPos(), 6, 0.5);
				}
			}
		}

		private void playRumbles() {
			for (int i = rumbles.size() - 1; i >= 0; i--) {
				int[] r = rumbles.get(i);
				if (r[1] > age) continue;
				rumbles.remove(i);
				net.minecraft.entity.Entity e = world.getEntityById(r[0]);
				if (e instanceof ServerPlayerEntity p) {
					world.playSound(null, p.getBlockPos(), ModSounds.HEAVEN_HAND_RUMBLE, SoundCategory.PLAYERS, 3.0f, 1.0f);
				}
			}
		}

		@Override
		protected void onEnd(boolean cancelled) {
			caster.removeStatusEffect(StatusEffects.SLOWNESS);
			if (cancelled && age < T_SLAM) {
				// The heavens withdraw: nothing left to slam, so at least release the pinned victims.
				for (LivingEntity target : EntityUtil.inSphere(world, caster, center, R_DOMAIN)) {
					target.removeStatusEffect(ModEffects.SUPPRESSED);
				}
			}
		}
	}
}
