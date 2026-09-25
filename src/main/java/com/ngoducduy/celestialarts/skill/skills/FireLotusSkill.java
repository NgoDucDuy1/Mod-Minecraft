package com.ngoducduy.celestialarts.skill.skills;

import com.ngoducduy.celestialarts.cultivation.Realm;
import com.ngoducduy.celestialarts.entity.FireLotusEntity;
import com.ngoducduy.celestialarts.network.FxData;
import com.ngoducduy.celestialarts.network.FxType;
import com.ngoducduy.celestialarts.network.ModPackets;
import com.ngoducduy.celestialarts.registry.ModEntities;
import com.ngoducduy.celestialarts.registry.ModSounds;
import com.ngoducduy.celestialarts.skill.Element;
import com.ngoducduy.celestialarts.skill.Skill;
import com.ngoducduy.celestialarts.skill.SkillContext;
import com.ngoducduy.celestialarts.skill.SkillType;
import com.ngoducduy.celestialarts.skill.cast.ActiveCast;
import com.ngoducduy.celestialarts.util.SkillFx;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.Vec3d;

/**
 * Phật Nộ Hỏa Liên – the caster gathers different flames into a lotus in the palm.
 * The lotus blooms over ~2 seconds and is then hurled forward, detonating on impact.
 */
public class FireLotusSkill extends Skill {
	public FireLotusSkill() {
		super(Settings.of(Element.FIRE, SkillType.PROJECTILE, Realm.GOLDEN_CORE, 45f, 360));
	}

	@Override
	public boolean activate(SkillContext ctx) {
		ServerPlayerEntity player = ctx.player();
		Vec3d palm = FireLotusEntity.palmPos(player);

		FireLotusEntity lotus = new FireLotusEntity(ModEntities.FIRE_LOTUS, ctx.world());
		lotus.setOwner(player);
		lotus.setPower(1.0f + (ctx.qi().getRealm().getLevel() - Realm.GOLDEN_CORE.getLevel()) * 0.12f);
		lotus.refreshPositionAndAngles(palm.x, palm.y, palm.z, player.getYaw(), 0);
		ctx.world().spawnEntity(lotus);

		ModPackets.sendFx(player, FxData.follow(FxType.MAGIC_CIRCLE, player.getId(), player.getPos(), 0xFF7A1A, 2.2f, FireLotusEntity.CHARGE_TICKS).withExtra(1));
		ModPackets.sendFx(player, FxData.follow(FxType.QI_AURA, player.getId(), player.getPos(), 0xFF9A3C, 0.8f, FireLotusEntity.CHARGE_TICKS));
		ctx.world().playSound(null, player.getBlockPos(), ModSounds.QI_GATHER, SoundCategory.PLAYERS, 1.0f, 1.2f);
		ctx.qi().addActiveCast(new ChargeCast(player, this));
		return true;
	}

	/** Keeps the caster slowed and glowing while the lotus blooms. */
	private static final class ChargeCast extends ActiveCast {
		ChargeCast(ServerPlayerEntity caster, Skill skill) {
			super(caster, skill, FireLotusEntity.CHARGE_TICKS);
		}

		@Override
		protected void onTick() {
			if (age == 0) {
				caster.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, FireLotusEntity.CHARGE_TICKS, 2, false, false, false));
			}
			if (age % 6 == 0) {
				SkillFx.helix(world, com.ngoducduy.celestialarts.registry.GlowParticleEffect.glow(0xFF7A1A, 0.5f, 14),
						caster.getPos(), 1.1, 2.2, 8, 1, age * 0.3);
			}
		}
	}
}
