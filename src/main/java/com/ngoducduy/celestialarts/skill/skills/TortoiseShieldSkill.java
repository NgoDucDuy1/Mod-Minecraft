package com.ngoducduy.celestialarts.skill.skills;

import com.ngoducduy.celestialarts.cultivation.Realm;
import com.ngoducduy.celestialarts.skill.Element;
import com.ngoducduy.celestialarts.skill.Skill;
import com.ngoducduy.celestialarts.skill.SkillContext;
import com.ngoducduy.celestialarts.skill.SkillType;
import com.ngoducduy.celestialarts.skill.cast.ShieldCast;
import com.ngoducduy.celestialarts.util.SkillFx;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Huyền Vũ Hộ Thể – the Black Tortoise's protection. A hexagonal energy shell that
 * absorbs damage for twelve seconds and reflects a share of melee blows. Press again to
 * detonate it as a repulsion wave; if it survives, the leftover force becomes absorption.
 */
public class TortoiseShieldSkill extends Skill {
	public TortoiseShieldSkill() {
		super(Settings.of(Element.EARTH, SkillType.BUFF, Realm.FOUNDATION, 30f, 400));
	}

	@Override
	public boolean activate(SkillContext ctx) {
		float pool = 30f + ctx.qi().getRealm().getLevel() * 6f;
		ctx.qi().addActiveCast(new TortoiseCast(ctx.player(), this, pool));
		return true;
	}

	/** Standard shell; when it outlasts its duration the unused force hardens into absorption hearts. */
	private static final class TortoiseCast extends ShieldCast {
		TortoiseCast(ServerPlayerEntity caster, Skill skill, float pool) {
			super(caster, skill, 240, pool, 0.25f, 0x7CFFB0);
		}

		@Override
		protected void onEnd(boolean cancelled) {
			if (cancelled || getPool() <= 0.5f) return;
			int hearts = Math.min(4, Math.max(1, Math.round(getPool() / 10f)));
			caster.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, 300, hearts - 1, false, false, true));
			SkillFx.glowRing(world, caster.getPos().add(0, 0.2, 0), 1.2, 0x7CFFB0, 20, -0.1);
		}
	}
}
