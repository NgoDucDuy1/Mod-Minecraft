package com.ngoducduy.celestialarts.skill.skills;

import com.ngoducduy.celestialarts.cultivation.PlayerQi;
import com.ngoducduy.celestialarts.cultivation.QiHolder;
import com.ngoducduy.celestialarts.cultivation.Realm;
import com.ngoducduy.celestialarts.entity.FlyingSwordEntity;
import com.ngoducduy.celestialarts.registry.ModEntities;
import com.ngoducduy.celestialarts.registry.ModSounds;
import com.ngoducduy.celestialarts.skill.Element;
import com.ngoducduy.celestialarts.skill.Skill;
import com.ngoducduy.celestialarts.skill.SkillContext;
import com.ngoducduy.celestialarts.skill.SkillType;
import com.ngoducduy.celestialarts.skill.cast.ActiveCast;
import com.ngoducduy.celestialarts.util.SkillFx;
import net.minecraft.util.math.Vec3d;
import net.minecraft.entity.LivingEntity;
import com.ngoducduy.celestialarts.util.Targeting;
import com.ngoducduy.celestialarts.util.EntityUtil;
import com.ngoducduy.celestialarts.registry.ModDamageTypes;
import com.ngoducduy.celestialarts.network.ModPackets;
import com.ngoducduy.celestialarts.network.FxType;
import com.ngoducduy.celestialarts.network.FxData;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Ngự Kiếm Phi Hành – summon a spirit sword underfoot and ride it through the sky.
 * Costs Qi continuously; pressing the key again (or sneaking) lands.
 */
public class SwordFlightSkill extends Skill {
	private static final float DRAIN_PER_TICK = 0.22f;

	public SwordFlightSkill() {
		super(Settings.of(Element.SWORD, SkillType.MOVEMENT, Realm.GOLDEN_CORE, 20f, 60));
	}

	@Override
	public boolean canUseWhileRiding() {
		return false;
	}

	@Override
	public boolean activate(SkillContext ctx) {
		ServerPlayerEntity player = ctx.player();
		FlyingSwordEntity sword = FlyingSwordEntity.summonFor(player, ModEntities.FLYING_SWORD);
		if (!player.hasVehicle()) {
			sword.discard();
			return false;
		}
		ctx.world().playSound(null, player.getBlockPos(), ModSounds.SWORD_FLIGHT, SoundCategory.PLAYERS, 1.0f, 1.0f);
		ctx.qi().addActiveCast(new FlightCast(player, this, sword));
		return true;
	}

	private static final class FlightCast extends ActiveCast {
		private static final double RAM_SPEED = 0.45;
		private final FlyingSwordEntity sword;
		private Vec3d lastPos;
		private Vec3d lastVelocity = Vec3d.ZERO;

		FlightCast(ServerPlayerEntity caster, Skill skill, FlyingSwordEntity sword) {
			super(caster, skill, 0);
			this.sword = sword;
			this.lastPos = sword.getPos();
		}

		@Override
		protected void onTick() {
			if (sword.isRemoved() || caster.getVehicle() != sword) {
				finish();
				return;
			}
			// The sword is steered by the rider's client; derive its speed from position deltas.
			Vec3d pos = sword.getPos();
			lastVelocity = pos.subtract(lastPos);
			lastPos = pos;
			// Kiếm Quang Xung: at speed the blade cuts through whatever it passes.
			if (age % 4 == 0 && lastVelocity.lengthSquared() > RAM_SPEED * RAM_SPEED) {
				for (LivingEntity target : EntityUtil.alongLine(world, caster, pos.subtract(lastVelocity.multiply(2)), pos.add(lastVelocity), 1.3)) {
					if (target == caster) continue;
					if (target.damage(ModDamageTypes.source(world, ModDamageTypes.SWORD_QI, caster), 6.0f)) {
						EntityUtil.knockback(target, pos, 0.5, 0.3);
						SkillFx.swordGlints(world, target.getBoundingBox().getCenter(), 10, 0.3);
						world.playSound(null, target.getBlockPos(), ModSounds.SWORD_QI, SoundCategory.PLAYERS, 0.8f, 1.3f);
					}
				}
			}
			PlayerQi qi = QiHolder.get(caster);
			if (!caster.isCreative()) {
				if (!qi.consumeQi(DRAIN_PER_TICK)) {
					caster.sendMessage(Text.translatable("message.celestialarts.qi_exhausted").formatted(Formatting.RED), true);
					finish();
					return;
				}
			}
			if (age % 40 == 0) {
				world.playSound(null, caster.getBlockPos(), ModSounds.SWORD_HUM, SoundCategory.PLAYERS, 0.35f, 1.0f);
			}
		}

		/** Pressing the key again lands the sword. */
		@Override
		public void onRelease() {
			finish();
		}

		@Override
		protected void onEnd(boolean cancelled) {
			if (caster.getVehicle() == sword) {
				caster.stopRiding();
			}
			if (!sword.isRemoved()) {
				SkillFx.swordGlints(world, sword.getPos(), 16, 0.2);
				sword.discard();
			}
			// Kiếm Lạc Cửu Thiên: dismounting while diving slams the ground with a sword-qi wave.
			if (!cancelled && lastVelocity.y < -0.35 && lastVelocity.length() > 0.6) {
				Vec3d ground = Targeting.snapToGround(world, caster.getPos().add(0, 0.5, 0), 6);
				float power = (float) Math.min(2.0, lastVelocity.length());
				ModPackets.sendFx(world, FxData.at(FxType.SHOCKWAVE_RING, ground.add(0, 0.1, 0), 0x9FE8FF, 3.0f + power * 2.0f, 12));
				ModPackets.sendFx(world, FxData.at(FxType.ENERGY_BURST, ground.add(0, 0.8, 0), 0xE8FBFF, 1.2f + power * 0.5f, 8));
				SkillFx.swordGlints(world, ground.add(0, 0.5, 0), 30, 0.5);
				world.playSound(null, caster.getBlockPos(), ModSounds.SWORD_QI, SoundCategory.PLAYERS, 1.3f, 0.7f);
				ModPackets.sendCameraShake(world, ground, 20.0, 0.5f, 8);
				caster.fallDistance = 0.0f;
				for (LivingEntity target : EntityUtil.inCylinder(world, caster, ground, 2.5 + power * 1.5, 2.5)) {
					if (target.damage(ModDamageTypes.source(world, ModDamageTypes.SWORD_QI, caster), 6.0f + power * 5.0f)) {
						EntityUtil.knockback(target, ground, 0.8, 0.4);
					}
				}
			}
		}
	}
}
