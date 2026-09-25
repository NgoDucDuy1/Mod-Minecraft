package com.ngoducduy.celestialarts.skill.skills;

import com.ngoducduy.celestialarts.cultivation.Realm;
import com.ngoducduy.celestialarts.network.FxData;
import com.ngoducduy.celestialarts.network.FxType;
import com.ngoducduy.celestialarts.network.ModPackets;
import com.ngoducduy.celestialarts.registry.ModDamageTypes;
import com.ngoducduy.celestialarts.registry.ModSounds;
import com.ngoducduy.celestialarts.skill.Element;
import com.ngoducduy.celestialarts.skill.Skill;
import com.ngoducduy.celestialarts.skill.SkillContext;
import com.ngoducduy.celestialarts.skill.SkillType;
import com.ngoducduy.celestialarts.util.EntityUtil;
import com.ngoducduy.celestialarts.util.SkillFx;
import com.ngoducduy.celestialarts.util.Targeting;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.Vec3d;

/**
 * Lôi Ảnh Bộ – the cultivator becomes a bolt of lightning, blinking forward and
 * shocking everything on the path. Leaves a crackling afterimage trail.
 */
public class LightningStepSkill extends Skill {
	private static final double DISTANCE = 9.0;

	public LightningStepSkill() {
		super(Settings.of(Element.LIGHTNING, SkillType.MOVEMENT, Realm.QI_REFINING, 10f, 60));
	}

	@Override
	public boolean canUseWhileRiding() {
		return false;
	}

	@Override
	public boolean activate(SkillContext ctx) {
		ServerPlayerEntity player = ctx.player();
		Vec3d look = ctx.look();
		// On the ground dash horizontally; in the air follow the view.
		Vec3d dir = player.isOnGround() && look.y < 0.35 ? ctx.flatLook() : look;
		Vec3d start = player.getPos();
		Vec3d dest = Targeting.dashDestination(player, dir, DISTANCE);
		if (dest.squaredDistanceTo(start) < 1.0) return false;

		// Shock everything along the path.
		for (LivingEntity target : EntityUtil.alongLine(ctx.world(), player, start.add(0, 1, 0), dest.add(0, 1, 0), 1.2)) {
			if (target.damage(ModDamageTypes.source(ctx.world(), ModDamageTypes.THUNDER, player), 6f)) {
				target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 30, 2, false, false, true), player);
				SkillFx.thunderSparks(ctx.world(), target.getBoundingBox().getCenter(), 10, 0.3);
			}
		}

		ModPackets.sendFx(ctx.world(), FxData.line(FxType.LIGHTNING_BOLT, start.add(0, 1, 0), dest.add(0, 1, 0), getElement().getPrimary(), 0.9f, 9).withExtra(1));
		ModPackets.sendFx(ctx.world(), FxData.at(FxType.ENERGY_BURST, start.add(0, 1, 0), 0xE6D6FF, 0.9f, 6));
		SkillFx.thunderSparks(ctx.world(), start.add(0, 1, 0), 18, 0.35);
		ctx.world().playSound(null, player.getBlockPos(), ModSounds.LIGHTNING_STEP, SoundCategory.PLAYERS, 1.0f, 1.0f);

		player.fallDistance = 0;
		player.requestTeleport(dest.x, dest.y, dest.z);
		player.setVelocity(dir.multiply(0.4));
		player.velocityModified = true;

		SkillFx.thunderSparks(ctx.world(), dest.add(0, 1, 0), 18, 0.35);
		ModPackets.sendFx(ctx.world(), FxData.at(FxType.ENERGY_BURST, dest.add(0, 1, 0), 0xE6D6FF, 1.1f, 7));
		ModPackets.sendFx(ctx.world(), FxData.at(FxType.SHOCKWAVE_RING, dest.add(0, 0.1, 0), getElement().getPrimary(), 2.2f, 8));
		return true;
	}
}
