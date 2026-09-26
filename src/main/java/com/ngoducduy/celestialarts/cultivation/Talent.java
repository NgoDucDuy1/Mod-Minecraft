package com.ngoducduy.celestialarts.cultivation;

import com.ngoducduy.celestialarts.skill.Element;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.random.Random;

/**
 * Thiên phú / thể chất – the innate constitution a cultivator is born with, rolled once together
 * with the {@link SpiritRoot}. Each talent has a rarity, a tier that feeds the aptitude score and
 * a bundle of passive effects consulted by {@link CultivationStats}, {@link RealmPassives},
 * {@link Breakthrough} and the skills.
 *
 * <p>Tiers: −1 (cursed) … 4 (legendary). Higher tiers mean more power at the same realm but also
 * a heavier heavenly tribulation – heaven is jealous of geniuses.</p>
 */
public enum Talent {
	/** Nothing special – the fate of most mortals. */
	MORTAL_BODY("mortal_body", Rarity.COMMON, 30, 0, 0xBFBFBF),
	/** Hard bones: +2 armour, +10% health. */
	IRON_BONE("iron_bone", Rarity.COMMON, 12, 1, 0xC9CFD6),
	/** Light body: +10% speed, wind arts +10%. */
	SWIFT_WIND("swift_wind", Rarity.COMMON, 12, 1, 0xB8FFD9),
	/** Meridians shaped like a blade: sword arts +15%, sword intent lasts longer. */
	SWORD_BONE("sword_bone", Rarity.UNCOMMON, 8, 2, 0x9FE8FF),
	/** A heart of crimson flame: fire arts +15%, immune to fire from Trúc Cơ. */
	FLAME_HEART("flame_heart", Rarity.UNCOMMON, 8, 2, 0xFF7A1A),
	/** Frost marrow: ice arts +15%, never freezes. */
	FROST_MARROW("frost_marrow", Rarity.UNCOMMON, 8, 2, 0x9BE4FF),
	/** Thunder body: lightning arts +15%, tribulation lightning −35%. */
	THUNDER_BODY("thunder_body", Rarity.UNCOMMON, 7, 2, 0xC98BFF),
	/** Body of the thick earth: earth arts +15%, +2 armour, +20% knockback resistance. */
	EARTH_SPIRIT("earth_spirit", Rarity.UNCOMMON, 7, 2, 0xD9B36A),
	/** Unshakable Dao heart: +15% breakthrough chance, qi deviation lasts half as long. */
	DAO_HEART("dao_heart", Rarity.RARE, 5, 3, 0xFFE9A8),
	/** Innate Dao body: +25% max qi, +30% qi regeneration. */
	HEAVENLY_MERIDIANS("heavenly_meridians", Rarity.RARE, 4, 3, 0xE0F7FF),
	/** Void spirit body: void arts +20%, all cooldowns −10%. */
	VOID_SPIRIT("void_spirit", Rarity.RARE, 3, 3, 0xD24BFF),
	/** Ancient saint body: +30% health, +2 attack, all arts +15%. */
	ANCIENT_SAINT_BODY("ancient_saint_body", Rarity.RARE, 3, 3, 0xFFD36B),
	/** Chaos body: every element +12%, the heaviest tribulations under heaven. */
	CHAOS_BODY("chaos_body", Rarity.LEGENDARY, 1, 4, 0xFFFFFF),
	/** Crippled meridians: −30% regeneration, −20% cultivation gain. Can be endured, not cured (yet). */
	CRIPPLED_MERIDIANS("crippled_meridians", Rarity.CURSED, 6, -1, 0x6E5A5A);

	public enum Rarity {
		CURSED("cursed", Formatting.DARK_GRAY),
		COMMON("common", Formatting.WHITE),
		UNCOMMON("uncommon", Formatting.GREEN),
		RARE("rare", Formatting.AQUA),
		LEGENDARY("legendary", Formatting.GOLD);

		private final String key;
		private final Formatting formatting;

		Rarity(String key, Formatting formatting) {
			this.key = key;
			this.formatting = formatting;
		}

		public String getKey() {
			return key;
		}

		public Formatting getFormatting() {
			return formatting;
		}

		public Text getName() {
			return Text.translatable("talent.celestialarts.rarity." + key).formatted(formatting);
		}
	}

	private final String key;
	private final Rarity rarity;
	private final int weight;
	private final int tier;
	private final int rgb;

	Talent(String key, Rarity rarity, int weight, int tier, int rgb) {
		this.key = key;
		this.rarity = rarity;
		this.weight = weight;
		this.tier = tier;
		this.rgb = rgb;
	}

	public String getKey() {
		return key;
	}

	public Rarity getRarity() {
		return rarity;
	}

	public int getWeight() {
		return weight;
	}

	/** −1 cursed, 0 plain, 1 common, 2 uncommon, 3 rare, 4 legendary. */
	public int getTier() {
		return tier;
	}

	public int getRgb() {
		return rgb;
	}

	public Text getName() {
		return Text.translatable("talent.celestialarts." + key).formatted(rarity.getFormatting());
	}

	public Text getDescription() {
		return Text.translatable("talent.celestialarts." + key + ".desc").formatted(Formatting.GRAY);
	}

	// ------------------------------------------------------------- effects

	/** Multiplier on skill damage of the given element (1 = none). */
	public float damageMultiplier(Element element) {
		return switch (this) {
			case SWIFT_WIND -> element == Element.WIND ? 1.10F : 1.0F;
			case SWORD_BONE -> element == Element.SWORD ? 1.15F : 1.0F;
			case FLAME_HEART -> element == Element.FIRE ? 1.15F : 1.0F;
			case FROST_MARROW -> element == Element.ICE ? 1.15F : 1.0F;
			case THUNDER_BODY -> element == Element.LIGHTNING ? 1.15F : 1.0F;
			case EARTH_SPIRIT -> element == Element.EARTH ? 1.15F : 1.0F;
			case VOID_SPIRIT -> element == Element.VOID ? 1.20F : 1.0F;
			case ANCIENT_SAINT_BODY -> 1.15F;
			case CHAOS_BODY -> 1.12F;
			default -> 1.0F;
		};
	}

	public float maxQiMultiplier() {
		return this == HEAVENLY_MERIDIANS ? 1.25F : 1.0F;
	}

	public float regenMultiplier() {
		return switch (this) {
			case HEAVENLY_MERIDIANS -> 1.30F;
			case CRIPPLED_MERIDIANS -> 0.70F;
			default -> 1.0F;
		};
	}

	/** Multiplier on cultivation experience gained from meditation and kills. */
	public float expMultiplier() {
		return this == CRIPPLED_MERIDIANS ? 0.80F : 1.0F;
	}

	public float cooldownMultiplier() {
		return this == VOID_SPIRIT ? 0.90F : 1.0F;
	}

	/** Additive bonus to the chance of a small breakthrough. */
	public float breakthroughBonus() {
		return this == DAO_HEART ? 0.15F : 0.0F;
	}

	/** Multiplier on the duration of qi deviation after a failed breakthrough. */
	public float deviationMultiplier() {
		return this == DAO_HEART ? 0.5F : 1.0F;
	}

	/** Multiplier on damage taken from tribulation lightning. */
	public float tribulationDamageMultiplier() {
		return this == THUNDER_BODY ? 0.65F : 1.0F;
	}

	/** Extra tribulation bolts: heaven tests great talents harder. */
	public int extraTribulationBolts() {
		return switch (this) {
			case CHAOS_BODY -> 4;
			case ANCIENT_SAINT_BODY, HEAVENLY_MERIDIANS -> 1;
			default -> 0;
		};
	}

	public float healthMultiplier() {
		return switch (this) {
			case IRON_BONE -> 1.10F;
			case ANCIENT_SAINT_BODY -> 1.30F;
			default -> 1.0F;
		};
	}

	public float bonusArmor() {
		return switch (this) {
			case IRON_BONE, EARTH_SPIRIT -> 2.0F;
			default -> 0.0F;
		};
	}

	public float bonusAttack() {
		return this == ANCIENT_SAINT_BODY ? 2.0F : 0.0F;
	}

	public float bonusSpeed() {
		return this == SWIFT_WIND ? 0.10F : 0.0F;
	}

	public float bonusKnockbackResistance() {
		return this == EARTH_SPIRIT ? 0.20F : 0.0F;
	}

	public boolean immuneToFire() {
		return this == FLAME_HEART;
	}

	public boolean immuneToFreezing() {
		return this == FROST_MARROW;
	}

	// ---------------------------------------------------------------- roll

	public static Talent roll(Random random) {
		int total = 0;
		for (Talent t : values()) total += t.weight;
		int pick = random.nextInt(total);
		for (Talent t : values()) {
			pick -= t.weight;
			if (pick < 0) return t;
		}
		return MORTAL_BODY;
	}

	public static Talent byKey(String key) {
		for (Talent t : values()) {
			if (t.key.equals(key)) return t;
		}
		return MORTAL_BODY;
	}
}
