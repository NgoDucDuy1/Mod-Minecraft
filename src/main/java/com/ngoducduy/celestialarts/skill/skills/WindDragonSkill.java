package com.ngoducduy.celestialarts.skill.skills;

import com.ngoducduy.celestialarts.cultivation.Realm;
import com.ngoducduy.celestialarts.entity.WindDragonEntity;
import com.ngoducduy.celestialarts.network.FxData;
import com.ngoducduy.celestialarts.network.FxType;
import com.ngoducduy.celestialarts.network.ModPackets;
import com.ngoducduy.celestialarts.registry.ModEntities;
import com.ngoducduy.celestialarts.registry.ModParticles;
import com.ngoducduy.celestialarts.registry.ModSounds;
import com.ngoducduy.celestialarts.skill.Element;
import com.ngoducduy.celestialarts.skill.Skill;
import com.ngoducduy.celestialarts.skill.SkillContext;
import com.ngoducduy.celestialarts.skill.SkillType;
import com.ngoducduy.celestialarts.util.SkillFx;
import com.ngoducduy.celestialarts.util.Targeting;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;

/**
 * Phong Long Quyển – the cultivator sweeps a sleeve and a wind dragon coils out of the air,
 * rolling forward as a tornado that drags enemies in and lifts them before bursting apart.
 */
public class WindDragonSkill extends Skill {
	public WindDragonSkill() {
		super(Settings.of(Element.WIND, SkillType.PROJECTILE, Realm.FOUNDATION, 32f, 280));
	}

	@Override
	public boolean canUseWhileRiding() {
		return false;
	}

	@Override
	public boolean activate(SkillContext ctx) {
		ServerPlayerEntity player = ctx.player();
		Vec3d dir = ctx.flatLook();
		Vec3d start = Targeting.snapToGround(ctx.world(), player.getPos().add(dir.multiply(2.0)).add(0, 0.2, 0), 4);

		WindDragonEntity dragon = new WindDragonEntity(ModEntities.WIND_DRAGON, ctx.world());
		dragon.setOwner(player);
		dragon.refreshPositionAndAngles(start.x, start.y + 0.1, start.z, player.getYaw(), 0);
		dragon.launch(dir, 0.42);
		ctx.world().spawnEntity(dragon);

		player.swingHand(Hand.OFF_HAND, true);
		ModPackets.sendFx(player, FxData.follow(FxType.WIND_BLADES, player.getId(), player.getPos(), 0xB8FFD9, 2.0f, 14));
		ModPackets.sendFx(ctx.world(), FxData.at(FxType.SHOCKWAVE_RING, start.add(0, 0.1, 0), 0xB8FFD9, 3.0f, 10));
		SkillFx.helix(ctx.world(), ModParticles.WIND_STREAK, start, 1.2, 3.5, 30, 3, 0.0);
		SkillFx.windGust(ctx.world(), player.getEyePos(), dir, 20, 0.6);
		ctx.world().playSound(null, player.getBlockPos(), ModSounds.WIND_SLASH, SoundCategory.PLAYERS, 1.4f, 0.6f);
		ctx.world().playSound(null, player.getBlockPos(), ModSounds.QI_GATHER, SoundCategory.PLAYERS, 0.8f, 1.4f);
		return true;
	}
}
