package com.ngoducduy.celestialarts.skill;

import com.ngoducduy.celestialarts.CelestialArts;
import com.ngoducduy.celestialarts.skill.skills.DevouringVortexSkill;
import com.ngoducduy.celestialarts.skill.skills.EarthShatterSkill;
import com.ngoducduy.celestialarts.skill.skills.FireLotusSkill;
import com.ngoducduy.celestialarts.skill.skills.FlameClawSkill;
import com.ngoducduy.celestialarts.skill.skills.FrozenDomainSkill;
import com.ngoducduy.celestialarts.skill.skills.HeavenSwordSkill;
import com.ngoducduy.celestialarts.skill.skills.IceArrowsSkill;
import com.ngoducduy.celestialarts.skill.skills.LightningStepSkill;
import com.ngoducduy.celestialarts.skill.skills.NineTribulationsSkill;
import com.ngoducduy.celestialarts.skill.skills.PurpleThunderBeamSkill;
import com.ngoducduy.celestialarts.skill.skills.SwordFlightSkill;
import com.ngoducduy.celestialarts.skill.skills.SwordQiSlashSkill;
import com.ngoducduy.celestialarts.skill.skills.TaijiFormationSkill;
import com.ngoducduy.celestialarts.skill.skills.ThousandSwordsSkill;
import com.ngoducduy.celestialarts.skill.skills.TortoiseShieldSkill;
import com.ngoducduy.celestialarts.skill.skills.WindBladeDanceSkill;
import com.ngoducduy.celestialarts.skill.skills.WindDragonSkill;
import com.ngoducduy.celestialarts.skill.skills.VajraPalmSkill;
import com.ngoducduy.celestialarts.skill.skills.GoldenBodySkill;
import com.ngoducduy.celestialarts.skill.skills.ThunderDragonSkill;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Simple ordered registry of all skills. Order matters for the skill book UI.
 */
public final class SkillRegistry {
	private static final Map<Identifier, Skill> SKILLS = new LinkedHashMap<>();

	// Realm 1 – Luyện Khí
	public static final SwordQiSlashSkill SWORD_QI_SLASH = register("sword_qi_slash", new SwordQiSlashSkill());
	public static final FlameClawSkill FLAME_CLAW = register("flame_claw", new FlameClawSkill());
	public static final IceArrowsSkill ICE_ARROWS = register("ice_arrows", new IceArrowsSkill());
	public static final LightningStepSkill LIGHTNING_STEP = register("lightning_step", new LightningStepSkill());
	public static final VajraPalmSkill VAJRA_PALM = register("vajra_palm", new VajraPalmSkill());
	// Realm 2 – Trúc Cơ
	public static final WindBladeDanceSkill WIND_BLADE_DANCE = register("wind_blade_dance", new WindBladeDanceSkill());
	public static final TortoiseShieldSkill TORTOISE_SHIELD = register("tortoise_shield", new TortoiseShieldSkill());
	public static final EarthShatterSkill EARTH_SHATTER = register("earth_shatter", new EarthShatterSkill());
	public static final WindDragonSkill WIND_DRAGON = register("wind_dragon", new WindDragonSkill());
	// Realm 3 – Kim Đan
	public static final FireLotusSkill FIRE_LOTUS = register("fire_lotus", new FireLotusSkill());
	public static final TaijiFormationSkill TAIJI_FORMATION = register("taiji_formation", new TaijiFormationSkill());
	public static final SwordFlightSkill SWORD_FLIGHT = register("sword_flight", new SwordFlightSkill());
	public static final FrozenDomainSkill FROZEN_DOMAIN = register("frozen_domain", new FrozenDomainSkill());
	// Realm 4 – Nguyên Anh
	public static final ThousandSwordsSkill THOUSAND_SWORDS = register("thousand_swords", new ThousandSwordsSkill());
	public static final PurpleThunderBeamSkill PURPLE_THUNDER_BEAM = register("purple_thunder_beam", new PurpleThunderBeamSkill());
	public static final DevouringVortexSkill DEVOURING_VORTEX = register("devouring_vortex", new DevouringVortexSkill());
	public static final GoldenBodySkill GOLDEN_BODY = register("golden_body", new GoldenBodySkill());
	// Realm 5 – Hóa Thần
	public static final NineTribulationsSkill NINE_TRIBULATIONS = register("nine_tribulations", new NineTribulationsSkill());
	public static final ThunderDragonSkill THUNDER_DRAGON = register("thunder_dragon", new ThunderDragonSkill());
	// Realm 6 – Độ Kiếp
	public static final HeavenSwordSkill HEAVEN_SWORD = register("heaven_sword", new HeavenSwordSkill());

	private SkillRegistry() {
	}

	private static <T extends Skill> T register(String path, T skill) {
		Identifier id = CelestialArts.id(path);
		skill.setId(id);
		if (SKILLS.putIfAbsent(id, skill) != null) {
			throw new IllegalStateException("Duplicate skill id " + id);
		}
		return skill;
	}

	/** Forces class initialisation from the mod initializer. */
	public static void registerAll() {
		// static initialiser did the work
	}

	@Nullable
	public static Skill get(@Nullable Identifier id) {
		return id == null ? null : SKILLS.get(id);
	}

	public static Collection<Skill> all() {
		return Collections.unmodifiableCollection(SKILLS.values());
	}

	public static boolean exists(Identifier id) {
		return SKILLS.containsKey(id);
	}
}
