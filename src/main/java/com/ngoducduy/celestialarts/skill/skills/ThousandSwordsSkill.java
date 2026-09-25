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
import com.ngoducduy.celestialarts.util.SkillFx;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;

/**
 * Vạn Kiếm Quy Tông – a ring of spirit swords condenses around the cultivator,
 * hums for a moment, then the swords launch one after another at nearby enemies.
 */
public class ThousandSwordsSkill extends Skill {
	private static final int SWORDS = 12;

	public ThousandSwordsSkill() {
		super(Settings.of(Element.SWORD, SkillType.SUMMON, Realm.NASCENT_SOUL, 55f, 440));
	}

	@Override
	public boolean activate(SkillContext ctx) {
		ServerPlayerEntity player = ctx.player();
		float damage = 7f + ctx.qi().getRealm().getLevel();
		for (int i = 0; i < SWORDS; i++) {
			float offset = i * (360f / SWORDS);
			double a = Math.toRadians(offset);
			Vec3d start = player.getPos().add(Math.cos(a) * 0.6, 1.2, Math.sin(a) * 0.6);
			SpiritSwordEntity sword = new SpiritSwordEntity(ModEntities.SPIRIT_SWORD, ctx.world());
			sword.init(player, offset, 24 + i * 3, damage);
			sword.refreshPositionAndAngles(start.x, start.y, start.z, 0, 0);
			ctx.world().spawnEntity(sword);
		}

		player.swingHand(Hand.MAIN_HAND, true);
		ModPackets.sendFx(player, FxData.follow(FxType.MAGIC_CIRCLE, player.getId(), player.getPos(), getElement().getPrimary(), 3.2f, 40).withExtra(1));
		ModPackets.sendFx(player, FxData.follow(FxType.RUNE_ORBIT, player.getId(), player.getPos(), getElement().getPrimary(), 2.6f, 50));
		ModPackets.sendFx(player, FxData.follow(FxType.QI_AURA, player.getId(), player.getPos(), getElement().getPrimary(), 0.9f, 40));
		SkillFx.swordGlints(ctx.world(), player.getPos().add(0, 1.2, 0), 30, 0.3);
		ctx.world().playSound(null, player.getBlockPos(), ModSounds.SWORD_HUM, SoundCategory.PLAYERS, 1.5f, 1.0f);
		ctx.world().playSound(null, player.getBlockPos(), ModSounds.QI_GATHER, SoundCategory.PLAYERS, 1.0f, 1.3f);
		return true;
	}
}
