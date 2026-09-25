package com.ngoducduy.celestialarts.skill.skills;

import com.ngoducduy.celestialarts.cultivation.Realm;
import com.ngoducduy.celestialarts.skill.Element;
import com.ngoducduy.celestialarts.skill.Skill;
import com.ngoducduy.celestialarts.skill.SkillContext;
import com.ngoducduy.celestialarts.skill.SkillType;
import com.ngoducduy.celestialarts.skill.cast.ShieldCast;

/**
 * Huyền Vũ Hộ Thể – the Black Tortoise's protection. A hexagonal energy shell that
 * absorbs damage for twelve seconds and reflects a share of melee blows.
 */
public class TortoiseShieldSkill extends Skill {
	public TortoiseShieldSkill() {
		super(Settings.of(Element.EARTH, SkillType.BUFF, Realm.FOUNDATION, 30f, 400));
	}

	@Override
	public boolean activate(SkillContext ctx) {
		float pool = 30f + ctx.qi().getRealm().getLevel() * 6f;
		ctx.qi().addActiveCast(new ShieldCast(ctx.player(), this, 240, pool, 0.25f, 0x7CFFB0));
		return true;
	}
}
