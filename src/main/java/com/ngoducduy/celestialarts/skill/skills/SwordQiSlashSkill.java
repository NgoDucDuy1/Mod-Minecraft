package com.ngoducduy.celestialarts.skill.skills;

import com.ngoducduy.celestialarts.cultivation.Realm;
import com.ngoducduy.celestialarts.entity.SwordQiEntity;
import com.ngoducduy.celestialarts.network.FxData;
import com.ngoducduy.celestialarts.network.FxType;
import com.ngoducduy.celestialarts.network.ModPackets;
import com.ngoducduy.celestialarts.registry.ModEffects;
import com.ngoducduy.celestialarts.registry.ModEntities;
import com.ngoducduy.celestialarts.registry.ModSounds;
import com.ngoducduy.celestialarts.skill.Element;
import com.ngoducduy.celestialarts.skill.Skill;
import com.ngoducduy.celestialarts.skill.SkillContext;
import com.ngoducduy.celestialarts.skill.SkillType;
import com.ngoducduy.celestialarts.util.SkillFx;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Kiếm Khí Trảm – swing the blade and release a crescent of sword qi.
 * Three consecutive casts form a combo: horizontal, diagonal, then a larger
 * vertical cut that pierces further and grants Sword Intent.
 */
public class SwordQiSlashSkill extends Skill {
	private static final int COMBO_WINDOW = 40;
	private final Map<UUID, int[]> combos = new HashMap<>();

	public SwordQiSlashSkill() {
		super(Settings.of(Element.SWORD, SkillType.PROJECTILE, Realm.QI_REFINING, 12f, 16));
	}

	@Override
	public boolean activate(SkillContext ctx) {
		ServerPlayerEntity player = ctx.player();
		int[] combo = combos.computeIfAbsent(player.getUuid(), k -> new int[]{0, -1000});
		int step = (player.age - combo[1] <= COMBO_WINDOW) ? (combo[0] + 1) % 3 : 0;
		combo[0] = step;
		combo[1] = player.age;

		float roll;
		float size;
		float damage;
		int arcStyle;
		switch (step) {
			case 1 -> {
				roll = 40f;
				size = 1.0f;
				damage = 9f;
				arcStyle = 1;
			}
			case 2 -> {
				roll = 90f;
				size = 1.4f;
				damage = 13f;
				arcStyle = 3;
			}
			default -> {
				roll = 0f;
				size = 1.0f;
				damage = 9f;
				arcStyle = 0;
			}
		}

		Vec3d look = ctx.look();
		Vec3d spawn = player.getEyePos().add(look.multiply(1.2)).add(0, -0.2, 0);
		SwordQiEntity qi = new SwordQiEntity(ModEntities.SWORD_QI, ctx.world());
		qi.setOwner(player);
		qi.setRoll(roll);
		qi.setSize(size);
		qi.setDamage(damage);
		qi.setPierce(step == 2 ? 6 : 3);
		qi.setMaxAge(step == 2 ? 55 : 42);
		qi.refreshPositionAndAngles(spawn.x, spawn.y, spawn.z, player.getYaw(), player.getPitch());
		qi.launch(look, step == 2 ? 1.5 : 1.3);
		ctx.world().spawnEntity(qi);

		// Visuals on the caster.
		player.swingHand(Hand.MAIN_HAND, true);
		ModPackets.sendFx(player, FxData.follow(FxType.SLASH_ARC, player.getId(), player.getPos(), getElement().getPrimary(), size * 1.6f, 8).withExtra(arcStyle));
		SkillFx.swordGlints(ctx.world(), spawn, 8, 0.15);
		ctx.world().playSound(null, player.getBlockPos(), ModSounds.SWORD_QI, SoundCategory.PLAYERS, 1.0f, 0.95f + step * 0.1f);

		if (step == 2) {
			player.addStatusEffect(new StatusEffectInstance(ModEffects.SWORD_INTENT, 100, 0, false, false, true));
			SkillFx.glowRing(ctx.world(), player.getPos().add(0, 0.1, 0), 1.2, getElement().getPrimary(), 20, 0.1);
		}
		return true;
	}
}
