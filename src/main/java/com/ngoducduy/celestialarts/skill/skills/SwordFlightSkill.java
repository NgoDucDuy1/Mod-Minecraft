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
		private final FlyingSwordEntity sword;

		FlightCast(ServerPlayerEntity caster, Skill skill, FlyingSwordEntity sword) {
			super(caster, skill, 0);
			this.sword = sword;
		}

		@Override
		protected void onTick() {
			if (sword.isRemoved() || caster.getVehicle() != sword) {
				finish();
				return;
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
		}
	}
}
