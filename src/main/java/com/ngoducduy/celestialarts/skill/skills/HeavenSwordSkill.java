package com.ngoducduy.celestialarts.skill.skills;

import com.ngoducduy.celestialarts.cultivation.Realm;
import com.ngoducduy.celestialarts.entity.HeavenSwordEntity;
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
import com.ngoducduy.celestialarts.util.Targeting;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;

/**
 * Thiên Ngoại Phi Kiếm – the ultimate art. The cultivator points at the sky and a
 * colossal sword descends from beyond the heavens onto the chosen ground.
 */
public class HeavenSwordSkill extends Skill {
	private static final double TARGET_RANGE = 48.0;

	public HeavenSwordSkill() {
		super(Settings.of(Element.DAO, SkillType.ULTIMATE, Realm.TRIBULATION, 120f, 1800));
	}

	@Override
	public boolean activate(SkillContext ctx) {
		ServerPlayerEntity player = ctx.player();
		Vec3d ground = Targeting.groundPoint(player, TARGET_RANGE);

		HeavenSwordEntity sword = new HeavenSwordEntity(ModEntities.HEAVEN_SWORD, ctx.world());
		sword.init(player, ground.y, 42f + ctx.qi().getRealm().getLevel() * 2f);
		sword.refreshPositionAndAngles(ground.x, ground.y + HeavenSwordEntity.SPAWN_HEIGHT, ground.z, player.getYaw(), 0);
		ctx.world().spawnEntity(sword);

		player.swingHand(Hand.MAIN_HAND, true);
		ModPackets.sendFx(player, FxData.follow(FxType.QI_AURA, player.getId(), player.getPos(), 0xFFE9A8, 1.3f, HeavenSwordEntity.MATERIALISE_TICKS));
		ModPackets.sendFx(player, FxData.follow(FxType.MAGIC_CIRCLE, player.getId(), player.getPos(), 0xFFE9A8, 3.0f, HeavenSwordEntity.MATERIALISE_TICKS).withExtra(1));
		SkillFx.goldenLight(ctx.world(), player.getPos().add(0, 1, 0), 40, 1.5);
		ctx.world().playSound(null, player.getBlockPos(), ModSounds.QI_GATHER, SoundCategory.PLAYERS, 1.5f, 0.8f);
		ctx.world().playSound(null, player.getBlockPos(), ModSounds.SWORD_HUM, SoundCategory.PLAYERS, 1.5f, 0.6f);
		return true;
	}
}
