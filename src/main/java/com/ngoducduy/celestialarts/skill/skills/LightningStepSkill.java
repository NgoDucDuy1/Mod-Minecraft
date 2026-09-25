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
 * shocking everything on the path. On arrival the leftover charge chains between the
 * closest enemies, each arc weaker than the last.
 */
public class LightningStepSkill extends Skill {
	private static final double DISTANCE = 9.0;
	private static final int CHAIN_HOPS = 3;
	private static final double CHAIN_RANGE = 6.0;

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
		// Tàn ảnh: ghosts of the caster smeared along the step.
		ModPackets.sendFx(ctx.world(), FxData.line(FxType.AFTERIMAGE, start, dest, 0xC9B8FF, 6, 14).withEntity(player.getId()));
		SkillFx.thunderSparks(ctx.world(), start.add(0, 1, 0), 18, 0.35);
		ctx.world().playSound(null, player.getBlockPos(), ModSounds.LIGHTNING_STEP, SoundCategory.PLAYERS, 1.0f, 1.0f);

		player.fallDistance = 0;
		player.requestTeleport(dest.x, dest.y, dest.z);
		player.setVelocity(dir.multiply(0.4));
		player.velocityModified = true;

		SkillFx.thunderSparks(ctx.world(), dest.add(0, 1, 0), 18, 0.35);
		ModPackets.sendFx(ctx.world(), FxData.at(FxType.ENERGY_BURST, dest.add(0, 1, 0), 0xE6D6FF, 1.1f, 7));
		ModPackets.sendFx(ctx.world(), FxData.at(FxType.SCREEN_FLASH, dest, 0xE6D6FF, 0.55f, 5));
		ModPackets.sendFx(ctx.world(), FxData.at(FxType.SHOCKWAVE_RING, dest.add(0, 0.1, 0), getElement().getPrimary(), 2.2f, 8));

		// The stored charge discharges into nearby foes: arcs jump from the arrival point to the
		// closest target, then from that target to the next, weakening with each hop.
		Vec3d from = dest.add(0, 1.2, 0);
		java.util.Set<LivingEntity> struck = new java.util.HashSet<>();
		float dmg = 5f;
		for (int hop = 0; hop < CHAIN_HOPS; hop++) {
			LivingEntity next = null;
			double best = CHAIN_RANGE * CHAIN_RANGE;
			for (LivingEntity e : EntityUtil.inSphere(ctx.world(), player, from, CHAIN_RANGE)) {
				if (struck.contains(e)) continue;
				double d = e.getBoundingBox().getCenter().squaredDistanceTo(from);
				if (d < best) {
					best = d;
					next = e;
				}
			}
			if (next == null) break;
			struck.add(next);
			Vec3d hit = next.getBoundingBox().getCenter();
			ModPackets.sendFx(ctx.world(), FxData.line(FxType.LIGHTNING_BOLT, from, hit, 0xE6D6FF, 0.55f, 7).withExtra(1));
			if (next.damage(ModDamageTypes.source(ctx.world(), ModDamageTypes.THUNDER, player), dmg)) {
				next.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 25, 3, false, false, true), player);
				SkillFx.thunderSparks(ctx.world(), hit, 8, 0.25);
			}
			ctx.world().playSound(null, next.getBlockPos(), ModSounds.THUNDER_STRIKE, SoundCategory.PLAYERS, 0.5f, 1.5f + hop * 0.15f);
			from = hit;
			dmg *= 0.7f;
		}
		return true;
	}
}
