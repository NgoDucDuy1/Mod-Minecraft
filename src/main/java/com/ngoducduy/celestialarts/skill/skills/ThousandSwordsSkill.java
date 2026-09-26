package com.ngoducduy.celestialarts.skill.skills;

import com.ngoducduy.celestialarts.cultivation.Realm;
import com.ngoducduy.celestialarts.entity.SpiritSwordEntity;
import com.ngoducduy.celestialarts.network.FxData;
import com.ngoducduy.celestialarts.network.FxType;
import com.ngoducduy.celestialarts.network.ModPackets;
import com.ngoducduy.celestialarts.registry.ModEntities;
import com.ngoducduy.celestialarts.registry.ModSounds;
import com.ngoducduy.celestialarts.skill.Element;
import com.ngoducduy.celestialarts.skill.Skill;
import com.ngoducduy.celestialarts.skill.SkillContext;
import com.ngoducduy.celestialarts.skill.SkillType;
import com.ngoducduy.celestialarts.skill.cast.ActiveCast;
import com.ngoducduy.celestialarts.util.SkillFx;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;

/**
 * Vạn Kiếm Quy Tông – the cultivator stands still while a wall of spirit swords condenses behind
 * their back: four rising, widening rows (48 swords) with every tip pointing where the cultivator
 * faces. The rows unfold from the inside out over half a second, hum for a moment, then the swords
 * shoot forward one after another – past the caster – and home in on whatever stands ahead (or fly
 * to the point being looked at and shatter there).
 */
public class ThousandSwordsSkill extends Skill {
	private static final int ROWS = 4;
	private static final int PER_ROW = 12;
	public static final int SWORDS = ROWS * PER_ROW;
	/** Swords materialised per tick while the wall unfolds. */
	private static final int SPAWN_PER_TICK = 4;
	/** Global tick at which the first sword launches; the rest follow one per tick. */
	private static final int FIRST_LAUNCH = 34;
	private static final double HALF_SPREAD_DEG = 75.0;

	public ThousandSwordsSkill() {
		super(Settings.of(Element.SWORD, SkillType.SUMMON, Realm.NASCENT_SOUL, 55f, 440));
	}

	@Override
	public boolean activate(SkillContext ctx) {
		ServerPlayerEntity player = ctx.player();
		ctx.qi().addActiveCast(new FormationCast(player, this, 4f + 0.5f * ctx.qi().getRealm().getLevel()));

		player.swingHand(Hand.MAIN_HAND, true);
		int color = getElement().getPrimary();
		ModPackets.sendFx(player, FxData.follow(FxType.MAGIC_CIRCLE, player.getId(), player.getPos(), color, 3.4f, FIRST_LAUNCH + SWORDS).withExtra(1));
		ModPackets.sendFx(player, FxData.follow(FxType.QI_AURA, player.getId(), player.getPos(), color, 1.0f, FIRST_LAUNCH + 20));
		ModPackets.sendFx(player, FxData.follow(FxType.RUNE_ORBIT, player.getId(), player.getPos(), color, 2.2f, FIRST_LAUNCH + 10));
		ctx.world().playSound(null, player.getBlockPos(), ModSounds.SWORD_HUM, SoundCategory.PLAYERS, 1.6f, 0.9f);
		ctx.world().playSound(null, player.getBlockPos(), ModSounds.QI_GATHER, SoundCategory.PLAYERS, 1.0f, 1.2f);
		return true;
	}

	/** Position of sword {@code i} in the owner's frame (right, up, behind) plus its pitch. */
	static Vec3d formationOffset(int i) {
		int row = i / PER_ROW;
		int k = centreOut(i % PER_ROW);
		double radius = 1.5 + row * 0.55;
		double height = 0.55 + row * 0.6;
		double theta = Math.toRadians(-HALF_SPREAD_DEG + k * (2.0 * HALF_SPREAD_DEG / (PER_ROW - 1)));
		return new Vec3d(Math.sin(theta) * radius, height, Math.cos(theta) * radius);
	}

	static float formationPitch(int i) {
		return 3.0f + (i / PER_ROW) * 3.0f;
	}

	/** 0..n-1 reordered from the middle outwards, alternating sides: 5,6,4,7,3,8,... */
	private static int centreOut(int n) {
		int half = PER_ROW / 2;
		return n % 2 == 0 ? half - 1 - n / 2 : half + n / 2;
	}

	/** Keeps the caster rooted and materialises the wall row by row. */
	private static final class FormationCast extends ActiveCast {
		private final float damage;
		private boolean launchSoundPlayed;

		FormationCast(ServerPlayerEntity caster, Skill skill, float damage) {
			super(caster, skill, FIRST_LAUNCH + 6);
			this.damage = damage;
		}

		@Override
		protected void onTick() {
			if (age == 0) {
				caster.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, FIRST_LAUNCH + SWORDS / 2, 3, false, false, false));
			}
			// Unfold: SPAWN_PER_TICK swords per tick, inner row first.
			int first = age * SPAWN_PER_TICK;
			for (int i = first; i < Math.min(SWORDS, first + SPAWN_PER_TICK); i++) {
				Vec3d off = formationOffset(i);
				Vec3d forward = Vec3d.fromPolar(0.0f, caster.getYaw());
				Vec3d right = new Vec3d(-forward.z, 0, forward.x);
				// Born at the caster's back, then slides out to its slot.
				Vec3d start = caster.getPos().add(0, 1.0, 0).subtract(forward.multiply(0.6)).add(right.multiply(off.x * 0.2));
				SpiritSwordEntity sword = new SpiritSwordEntity(ModEntities.SPIRIT_SWORD, world);
				int launchTick = FIRST_LAUNCH + i;
				sword.initFormation(caster, i, off, formationPitch(i), launchTick - age, damage);
				sword.refreshPositionAndAngles(start.x, start.y, start.z, caster.getYaw(), 0.0f);
				world.spawnEntity(sword);
				if (i % 6 == 0) {
					SkillFx.swordGlints(world, start, 4, 0.2);
				}
			}
			if (age % 4 == 0 && age < FIRST_LAUNCH) {
				SkillFx.gatherQi(world, caster.getPos().add(0, 1.3, 0), 0x9FE8FF, 3.2, 5);
			}
			if (age == FIRST_LAUNCH && !launchSoundPlayed) {
				launchSoundPlayed = true;
				world.playSound(null, caster.getBlockPos(), ModSounds.SWORD_LAUNCH, SoundCategory.PLAYERS, 1.4f, 0.8f);
				ModPackets.sendFx(world, FxData.at(FxType.SHOCKWAVE_RING, caster.getPos().add(0, 0.1, 0), 0xBFF0FF, 3.0f, 10));
				ModPackets.sendFx(world, FxData.at(FxType.SCREEN_FLASH, caster.getPos(), 0xBFF0FF, 0.3f, 4));
			}
		}
	}
}
