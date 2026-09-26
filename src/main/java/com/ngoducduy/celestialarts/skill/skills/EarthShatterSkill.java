package com.ngoducduy.celestialarts.skill.skills;

import com.ngoducduy.celestialarts.cultivation.Realm;
import com.ngoducduy.celestialarts.entity.RockSpikeEntity;
import com.ngoducduy.celestialarts.network.FxData;
import com.ngoducduy.celestialarts.network.FxType;
import com.ngoducduy.celestialarts.network.ModPackets;
import com.ngoducduy.celestialarts.registry.ModDamageTypes;
import com.ngoducduy.celestialarts.registry.ModEntities;
import com.ngoducduy.celestialarts.registry.ModParticles;
import com.ngoducduy.celestialarts.registry.ModSounds;
import com.ngoducduy.celestialarts.skill.Element;
import com.ngoducduy.celestialarts.skill.Skill;
import com.ngoducduy.celestialarts.skill.SkillContext;
import com.ngoducduy.celestialarts.skill.SkillType;
import com.ngoducduy.celestialarts.skill.cast.ActiveCast;
import com.ngoducduy.celestialarts.util.EntityUtil;
import com.ngoducduy.celestialarts.util.SkillFx;
import com.ngoducduy.celestialarts.util.Targeting;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;

/**
 * Đại Địa Liệt – the cultivator slams the ground; a fissure races forward and a
 * line of stone spikes erupts one after another, launching enemies skyward. The fissure
 * ends in a fan-shaped eruption, and the caster's skin hardens to stone for a few seconds.
 */
public class EarthShatterSkill extends Skill {
	private static final int SPIKES = 7;
	private static final double STEP = 1.9;
	private static final int FAN = 5;

	public EarthShatterSkill() {
		super(Settings.of(Element.EARTH, SkillType.AREA, Realm.FOUNDATION, 28f, 240));
	}

	@Override
	public boolean canUseWhileRiding() {
		return false;
	}

	@Override
	public boolean activate(SkillContext ctx) {
		ServerPlayerEntity player = ctx.player();
		Vec3d dir = ctx.flatLook();
		Vec3d origin = player.getPos();

		player.swingHand(Hand.MAIN_HAND, true);
		ModPackets.sendFx(ctx.world(), FxData.at(FxType.SHOCKWAVE_RING, origin.add(0, 0.1, 0), 0xC69C5B, 3.5f, 12));
		ModPackets.sendFx(ctx.world(), FxData.line(FxType.GROUND_CRACK, origin.add(0, 0.05, 0), origin.add(dir.multiply(SPIKES * STEP + 1)), 0xF2D9A6, 1.0f, 70));
		SkillFx.rockDebris(ctx.world(), origin, 20, 0.6);
		ModPackets.sendFx(ctx.world(), FxData.at(FxType.GROUND_DECAL, origin.add(0, 0.03, 0), 0xC69C5B, 2.6f, 500));
		ctx.world().playSound(null, player.getBlockPos(), ModSounds.EARTH_QUAKE, SoundCategory.PLAYERS, 1.4f, 1.0f);
		ctx.world().playSound(null, player.getBlockPos(), ModSounds.SUB_DROP, SoundCategory.PLAYERS, 1.2f, 1.0f);
		ModPackets.sendCameraShake(ctx.world(), origin, 24.0, 0.7f, 10);
		ModPackets.sendFx(ctx.world(), FxData.at(FxType.SCREEN_FLASH, origin, 0xC9A66B, 0.3f, 5));

		// Close range stomp.
		for (LivingEntity target : EntityUtil.inCylinder(ctx.world(), player, origin, 2.5, 2.0)) {
			if (target.damage(ModDamageTypes.source(ctx.world(), ModDamageTypes.EARTH, player), 6f)) {
				EntityUtil.knockback(target, origin, 0.7, 0.5);
			}
		}

		// Drawing on the earth hardens the caster's skin for a moment.
		player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 80, 0, false, false, true));
		SkillFx.shell(ctx.world(), ModParticles.ROCK_DEBRIS, origin.add(0, 1, 0), 0.9, 14, 0.02);

		ctx.qi().addActiveCast(new FissureCast(player, this, origin, dir));
		return true;
	}

	private static final class FissureCast extends ActiveCast {
		private final Vec3d origin;
		private final Vec3d dir;
		private int spawned;

		FissureCast(ServerPlayerEntity caster, Skill skill, Vec3d origin, Vec3d dir) {
			super(caster, skill, SPIKES * 2 + 2);
			this.origin = origin;
			this.dir = dir;
		}

		@Override
		protected void onTick() {
			if (age % 2 == 0 && spawned < SPIKES) {
				spawned++;
				Vec3d p = origin.add(dir.multiply(1.2 + spawned * STEP));
				// Slight zig-zag so the line reads as a fissure rather than a rail.
				Vec3d side = new Vec3d(-dir.z, 0, dir.x).multiply((spawned % 2 == 0 ? 1 : -1) * 0.45);
				p = p.add(side);
				Vec3d ground = Targeting.snapToGround(world, p.add(0, 1, 0), 6);
				float height = 1.8f + spawned * 0.18f;
				RockSpikeEntity.spawn(world, caster, ground, height, 8f + spawned * 0.5f, ModEntities.ROCK_SPIKE);
				ModPackets.sendFx(world, FxData.at(FxType.SHOCKWAVE_RING, ground.add(0, 0.1, 0), 0xC69C5B, 1.6f, 8));
				if (spawned == SPIKES) erupt(ground);
			}
		}

		/** The fissure ends in an eruption: a fan of spikes and a burst of debris. */
		private void erupt(Vec3d end) {
			Vec3d[] basis = SkillFx.basis(dir);
			for (int i = 0; i < FAN; i++) {
				double angle = Math.toRadians(-50 + i * (100.0 / (FAN - 1)));
				Vec3d d = dir.multiply(Math.cos(angle)).add(basis[0].multiply(Math.sin(angle)));
				Vec3d p = end.add(d.multiply(2.2));
				Vec3d ground = Targeting.snapToGround(world, p.add(0, 1, 0), 6);
				RockSpikeEntity.spawn(world, caster, ground, 2.6f, 9f, ModEntities.ROCK_SPIKE);
			}
			ModPackets.sendFx(world, FxData.at(FxType.SHOCKWAVE_RING, end.add(0, 0.1, 0), 0xF2D9A6, 4.0f, 12));
			SkillFx.rockDebris(world, end.add(0, 0.5, 0), 36, 0.9);
			world.playSound(null, net.minecraft.util.math.BlockPos.ofFloored(end), ModSounds.EARTH_QUAKE, SoundCategory.PLAYERS, 1.2f, 0.9f);
			world.playSound(null, net.minecraft.util.math.BlockPos.ofFloored(end), ModSounds.SUB_DROP, SoundCategory.PLAYERS, 1.5f, 0.9f);
			ModPackets.sendCameraShake(world, end, 20.0, 0.5f, 8);
			for (LivingEntity target : EntityUtil.inCylinder(world, caster, end, 3.2, 2.5)) {
				if (target.damage(ModDamageTypes.source(world, ModDamageTypes.EARTH, caster), 7f)) {
					EntityUtil.knockback(target, end, 0.5, 0.75);
					world.playSound(null, target.getBlockPos(), ModSounds.HIT_BLUNT, SoundCategory.PLAYERS, 1.0f, 0.85f);
				}
			}
		}
	}
}
