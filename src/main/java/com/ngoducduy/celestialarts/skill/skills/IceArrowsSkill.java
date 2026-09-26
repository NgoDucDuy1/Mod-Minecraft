package com.ngoducduy.celestialarts.skill.skills;

import com.ngoducduy.celestialarts.cultivation.Realm;
import com.ngoducduy.celestialarts.entity.IceShardEntity;
import com.ngoducduy.celestialarts.network.FxData;
import com.ngoducduy.celestialarts.network.FxType;
import com.ngoducduy.celestialarts.network.ModPackets;
import com.ngoducduy.celestialarts.registry.ModEntities;
import com.ngoducduy.celestialarts.registry.ModSounds;
import com.ngoducduy.celestialarts.skill.Element;
import com.ngoducduy.celestialarts.skill.Skill;
import com.ngoducduy.celestialarts.skill.SkillContext;
import com.ngoducduy.celestialarts.skill.SkillType;
import com.ngoducduy.celestialarts.util.SkillFx;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;

/**
 * Hàn Băng Tiễn – condenses a fan of five icicles and fires them forward.
 * Chilled targets slow down; a third hit freezes them solid.
 */
public class IceArrowsSkill extends Skill {
	private static final int COUNT = 5;
	private static final double SPREAD_DEG = 7.0;

	public IceArrowsSkill() {
		super(Settings.of(Element.ICE, SkillType.PROJECTILE, Realm.QI_REFINING, 14f, 60));
	}

	@Override
	public boolean activate(SkillContext ctx) {
		ServerPlayerEntity player = ctx.player();
		Vec3d look = ctx.look();
		Vec3d[] basis = SkillFx.basis(look);
		Vec3d origin = player.getEyePos().add(look.multiply(0.8)).add(0, -0.15, 0);

		for (int i = 0; i < COUNT; i++) {
			double offset = (i - (COUNT - 1) / 2.0) * Math.toRadians(SPREAD_DEG);
			Vec3d dir = look.multiply(Math.cos(offset)).add(basis[0].multiply(Math.sin(offset))).normalize();
			Vec3d spawn = origin.add(basis[0].multiply(offset * 1.5));
			IceShardEntity shard = new IceShardEntity(ModEntities.ICE_SHARD, ctx.world());
			shard.setOwner(player);
			shard.refreshPositionAndAngles(spawn.x, spawn.y, spawn.z, player.getYaw(), player.getPitch());
			shard.launch(dir, 1.65);
			ctx.world().spawnEntity(shard);
		}

		player.swingHand(Hand.MAIN_HAND, true);
		SkillFx.frostBurst(ctx.world(), origin, 16, 0.12);
		ModPackets.sendFx(player, FxData.at(FxType.MAGIC_CIRCLE, origin, getElement().getPrimary(), 0.9f, 10).withExtra(3).withTarget(look));
		ctx.world().playSound(null, player.getBlockPos(), ModSounds.ICE_CAST, SoundCategory.PLAYERS, 1.0f, 1.0f);
		return true;
	}
}
