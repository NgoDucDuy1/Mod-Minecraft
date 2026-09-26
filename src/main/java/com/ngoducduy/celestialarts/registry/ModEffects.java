package com.ngoducduy.celestialarts.registry;

import com.ngoducduy.celestialarts.CelestialArts;
import com.ngoducduy.celestialarts.effect.FrozenEffect;
import com.ngoducduy.celestialarts.effect.QiBurnEffect;
import com.ngoducduy.celestialarts.effect.SuppressedEffect;
import com.ngoducduy.celestialarts.effect.SwordIntentEffect;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public final class ModEffects {
	/** Target is encased in ice: cannot move or jump, takes bonus damage. */
	public static final StatusEffect FROZEN = register("frozen", new FrozenEffect());
	/** Burning with spirit fire: damage over time that ignores fire resistance of mobs. */
	public static final StatusEffect QI_BURN = register("qi_burn", new QiBurnEffect());
	/** Caster buff after sword skills: attack damage & speed. */
	public static final StatusEffect SWORD_INTENT = register("sword_intent", new SwordIntentEffect());
	/** Trấn Áp – pinned to the ground beneath Thiên Đạo Chi Thủ; cannot jump or fly, dragged down when airborne. */
	public static final StatusEffect SUPPRESSED = register("suppressed", new SuppressedEffect());

	private ModEffects() {
	}

	private static StatusEffect register(String path, StatusEffect effect) {
		return Registry.register(Registries.STATUS_EFFECT, CelestialArts.id(path), effect);
	}

	public static void register() {
	}
}
