package com.ngoducduy.celestialarts.cultivation;

import com.ngoducduy.celestialarts.skill.Element;
import net.minecraft.util.math.MathHelper;

/**
 * Pure functions that turn a cultivator's realm, stage, spirit root and talent into numbers:
 * aptitude, power, qi pool, regeneration, breakthrough odds and tribulation weight.
 *
 * <pre>
 * aptitude (tư chất, 1–100) = 10 + 12·(grade−1) + 8·(3−roots) + 9·talentTier
 *   grade 1 triple root, plain talent  → 10       (a mortal who somehow found a manual)
 *   grade 3 double root, uncommon      → 10+24+8+18 = 60
 *   grade 5 single root, legendary     → 10+48+16+36 = 100+ (clamped)
 *
 * power  = 0.85 + 0.35·apt/100   (skill damage & attack within the same realm: 0.85 … 1.20)
 * exp    = 0.80 + 0.50·apt/100   (cultivation gain)
 * odds   = stageBase + 0.25·apt/100 + talent + 0.05·(spiritQi−1), clamped 0.20 … 0.98
 * bolts  = 4 + realm + apt/25 + talentExtra   (tribulation lightning count)
 * </pre>
 */
public final class CultivationStats {
	private CultivationStats() {
	}

	// ------------------------------------------------------------ aptitude

	public static int aptitude(SpiritRoot root, Talent talent) {
		int grade = root == null ? 1 : root.getGrade();
		int roots = root == null ? 3 : root.getKinds().size();
		int apt = 10 + 12 * (grade - 1) + 8 * (3 - roots) + 9 * talent.getTier();
		return MathHelper.clamp(apt, 1, 100);
	}

	public static int aptitude(PlayerQi qi) {
		return aptitude(qi.getRoot(), qi.getTalent());
	}

	/** Aptitude tier label key: "mortal", "ordinary", "good", "excellent", "peerless". */
	public static String aptitudeTier(int aptitude) {
		if (aptitude < 20) return "mortal";
		if (aptitude < 40) return "ordinary";
		if (aptitude < 60) return "good";
		if (aptitude < 80) return "excellent";
		return "peerless";
	}

	// --------------------------------------------------------------- power

	/** Power at the same realm: 0.85 for the dullest, 1.20 for a peerless genius. */
	public static float powerMultiplier(PlayerQi qi) {
		return 0.85F + 0.35F * aptitude(qi) / 100.0F;
	}

	/** Stage multiplier: +4% per minor stage (viên mãn = +12%). */
	public static float stageMultiplier(Stage stage) {
		return 1.0F + 0.04F * stage.getIndex();
	}

	/**
	 * Everything that scales a skill hit of the given element: realm, stage, aptitude, spirit root
	 * attunement and talent.
	 */
	public static float skillDamageMultiplier(PlayerQi qi, Element element) {
		float m = RealmPassives.skillDamageMultiplier(qi.getRealm()) * stageMultiplier(qi.getStage()) * powerMultiplier(qi);
		SpiritRoot root = qi.getRoot();
		if (root != null && element != null) m *= root.damageMultiplier(element);
		if (element != null) m *= qi.getTalent().damageMultiplier(element);
		return m;
	}

	public static float qiCostMultiplier(PlayerQi qi, Element element) {
		SpiritRoot root = qi.getRoot();
		return root == null || element == null ? 1.0F : root.qiCostMultiplier(element);
	}

	public static float cooldownMultiplier(PlayerQi qi) {
		return qi.getTalent().cooldownMultiplier();
	}

	// ------------------------------------------------------------------ qi

	public static float maxQi(PlayerQi qi) {
		float base = qi.getRealm().getMaxQi() * (1.0F + 0.06F * qi.getStage().getIndex());
		return base * qi.getTalent().maxQiMultiplier() * (0.9F + 0.2F * aptitude(qi) / 100.0F);
	}

	public static float regenPerTick(PlayerQi qi) {
		float base = qi.getRealm().getRegenPerTick() * (1.0F + 0.05F * qi.getStage().getIndex());
		SpiritRoot root = qi.getRoot();
		if (root != null) base *= root.purityRegenMultiplier();
		return base * qi.getTalent().regenMultiplier();
	}

	/** Cultivation experience multiplier from aptitude and talent. */
	public static float expMultiplier(PlayerQi qi) {
		return (0.8F + 0.5F * aptitude(qi) / 100.0F) * qi.getTalent().expMultiplier();
	}

	// ------------------------------------------------------- breakthrough

	/** Chance (0–1) that a small breakthrough out of the current stage succeeds. */
	public static float breakthroughChance(PlayerQi qi, float spiritQi) {
		Stage stage = qi.getStage();
		if (stage.isPeak()) return 1.0F;
		float chance = stage.baseBreakthroughChance()
				+ 0.25F * aptitude(qi) / 100.0F
				+ qi.getTalent().breakthroughBonus()
				+ 0.05F * (spiritQi - 1.0F);
		return MathHelper.clamp(chance, 0.20F, 0.98F);
	}

	/** Number of lightning bolts in the tribulation that guards the gate to the next realm. */
	public static int tribulationBolts(PlayerQi qi) {
		return 4 + qi.getRealm().getLevel() + aptitude(qi) / 25 + qi.getTalent().extraTribulationBolts();
	}

	/** Damage of a single tribulation bolt before the target's own mitigation. */
	public static float tribulationBoltDamage(PlayerQi qi) {
		float base = 3.0F + 1.5F * qi.getRealm().getLevel();
		return base * (0.7F + 0.6F * aptitude(qi) / 100.0F);
	}

	/** Duration in ticks of qi deviation after a failed small breakthrough. */
	public static int deviationTicks(PlayerQi qi, boolean tribulation) {
		int base = tribulation ? 20 * 120 : 20 * 60;
		return Math.round(base * qi.getTalent().deviationMultiplier());
	}
}
