package com.ngoducduy.celestialarts.skill.skills;

import com.ngoducduy.celestialarts.cultivation.Realm;
import com.ngoducduy.celestialarts.network.FxData;
import com.ngoducduy.celestialarts.network.FxType;
import com.ngoducduy.celestialarts.network.ModPackets;
import com.ngoducduy.celestialarts.registry.ModDamageTypes;
import com.ngoducduy.celestialarts.registry.ModParticles;
import com.ngoducduy.celestialarts.registry.ModSounds;
import com.ngoducduy.celestialarts.skill.Element;
import com.ngoducduy.celestialarts.skill.Skill;
import com.ngoducduy.celestialarts.skill.SkillContext;
import com.ngoducduy.celestialarts.skill.SkillType;
import com.ngoducduy.celestialarts.util.EntityUtil;
import com.ngoducduy.celestialarts.util.SkillFx;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;

/**
 * Kim Cương Chưởng – the Vajra Palm. A single golden palm strike: a seal of light flashes
 * in front of the hand and everything in the short cone is hurled away. Enemies that fly
 * into a wall take extra damage from the impact.
 */
public class VajraPalmSkill extends Skill {
	private static final double RANGE = 3.6;
	private static final double HALF_ANGLE = 40;
	private static final float DAMAGE = 8f;
	private static final int GOLD = 0xFFE9A8;

	public VajraPalmSkill() {
		super(Settings.of(Element.DAO, SkillType.MELEE, Realm.QI_REFINING, 11f, 50));
	}

	@Override
	public boolean activate(SkillContext ctx) {
		ServerPlayerEntity player = ctx.player();
		Vec3d look = ctx.look();
		Vec3d origin = player.getEyePos().add(0, -0.25, 0);
		// Seal a little further out so it does not fill the caster's whole screen in first person.
		Vec3d palm = origin.add(look.multiply(2.2));

		player.swingHand(Hand.OFF_HAND, true);
		// Golden seal standing upright in front of the palm, a burst of light and a gust.
		ModPackets.sendFx(ctx.world(), FxData.at(FxType.MAGIC_CIRCLE, palm, GOLD, 0.9f, 14).withExtra(1).withTarget(look));
		ModPackets.sendFx(ctx.world(), FxData.at(FxType.ENERGY_BURST, palm, 0xFFE9B8, 0.45f, 6));
		ModPackets.sendFx(ctx.world(), FxData.at(FxType.SHOCKWAVE_RING, palm.add(look.multiply(0.6)), GOLD, 2.2f, 7).withTarget(look));
		SkillFx.cone(ctx.world(), ModParticles.GOLDEN_LIGHT, palm, look, 30, 16, 0.5);
		SkillFx.windGust(ctx.world(), palm, look, 12, 0.6);
		ctx.world().playSound(null, player.getBlockPos(), ModSounds.PALM_STRIKE, SoundCategory.PLAYERS, 1.3f, 1.0f);

		boolean hitAny = false;
		for (LivingEntity target : EntityUtil.inCone(ctx.world(), player, origin, look, RANGE, HALF_ANGLE)) {
			if (target.damage(ModDamageTypes.source(ctx.world(), ModDamageTypes.HEAVEN, player), DAMAGE)) {
				hitAny = true;
				ctx.world().playSound(null, target.getBlockPos(), ModSounds.HIT_BLUNT, SoundCategory.PLAYERS, 1.2f, 0.9f);
				Vec3d push = look.multiply(1.6).add(0, 0.35, 0);
				target.addVelocity(push.x, push.y, push.z);
				target.velocityModified = true;
				target.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 60, 0, false, false, true), player);
				SkillFx.goldenLight(ctx.world(), target.getBoundingBox().getCenter(), 10, 0.5);
				// Impact against a wall directly behind the target.
				Vec3d behind = target.getBoundingBox().getCenter().add(look.multiply(1.2));
				if (!ctx.world().getBlockState(net.minecraft.util.math.BlockPos.ofFloored(behind)).getCollisionShape(ctx.world(), net.minecraft.util.math.BlockPos.ofFloored(behind)).isEmpty()) {
					target.damage(ModDamageTypes.source(ctx.world(), ModDamageTypes.EARTH, player), DAMAGE * 0.5f);
					SkillFx.rockDebris(ctx.world(), behind, 10, 0.3);
					ctx.world().playSound(null, target.getBlockPos(), ModSounds.EARTH_QUAKE, SoundCategory.PLAYERS, 0.7f, 1.3f);
				}
			}
		}
		if (hitAny) ModPackets.sendCameraShake(ctx.world(), palm, 12.0, 0.35f, 5);
		return true;
	}
}
