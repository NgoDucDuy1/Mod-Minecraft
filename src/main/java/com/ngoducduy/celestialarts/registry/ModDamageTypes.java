package com.ngoducduy.celestialarts.registry;

import com.ngoducduy.celestialarts.CelestialArts;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * Data-driven damage types (see data/celestialarts/damage_type/*.json) with helpers
 * to build sources. Each has its own death message.
 */
public final class ModDamageTypes {
	public static final RegistryKey<DamageType> SWORD_QI = of("sword_qi");
	public static final RegistryKey<DamageType> FLAME = of("flame");
	public static final RegistryKey<DamageType> FROST = of("frost");
	public static final RegistryKey<DamageType> THUNDER = of("thunder");
	public static final RegistryKey<DamageType> WIND = of("wind");
	public static final RegistryKey<DamageType> EARTH = of("earth");
	public static final RegistryKey<DamageType> VOID_DRAIN = of("void_drain");
	public static final RegistryKey<DamageType> HEAVEN = of("heaven");

	private ModDamageTypes() {
	}

	private static RegistryKey<DamageType> of(String path) {
		return RegistryKey.of(RegistryKeys.DAMAGE_TYPE, CelestialArts.id(path));
	}

	public static void init() {
	}

	/** True for every damage type declared by this mod. */
	public static boolean isSkillDamage(DamageSource source) {
		return source.isOf(SWORD_QI) || source.isOf(FLAME) || source.isOf(FROST) || source.isOf(THUNDER)
				|| source.isOf(WIND) || source.isOf(EARTH) || source.isOf(VOID_DRAIN) || source.isOf(HEAVEN);
	}

	/** Direct damage caused by an attacker (melee, area). */
	public static DamageSource source(World world, RegistryKey<DamageType> type, @Nullable Entity attacker) {
		return world.getDamageSources().create(type, attacker);
	}

	/** Indirect damage: projectile/entity {@code source} fired by {@code attacker}. */
	public static DamageSource projectile(World world, RegistryKey<DamageType> type, Entity source, @Nullable Entity attacker) {
		return world.getDamageSources().create(type, source, attacker);
	}
}
