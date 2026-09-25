package com.ngoducduy.celestialarts.registry;

import com.ngoducduy.celestialarts.CelestialArts;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

/**
 * Custom sound events. The .ogg files are procedurally designed (see tools/gen_sounds.py)
 * and layered with vanilla sounds in sounds.json.
 */
public final class ModSounds {
	public static final SoundEvent SWORD_QI = register("skill.sword_qi");
	public static final SoundEvent SWORD_HUM = register("skill.sword_hum");
	public static final SoundEvent SWORD_LAUNCH = register("skill.sword_launch");
	public static final SoundEvent FIRE_WHOOSH = register("skill.fire_whoosh");
	public static final SoundEvent FIRE_EXPLOSION = register("skill.fire_explosion");
	public static final SoundEvent ICE_CAST = register("skill.ice_cast");
	public static final SoundEvent ICE_SHATTER = register("skill.ice_shatter");
	public static final SoundEvent THUNDER_STRIKE = register("skill.thunder_strike");
	public static final SoundEvent THUNDER_CHARGE = register("skill.thunder_charge");
	public static final SoundEvent LIGHTNING_STEP = register("skill.lightning_step");
	public static final SoundEvent WIND_SLASH = register("skill.wind_slash");
	public static final SoundEvent EARTH_QUAKE = register("skill.earth_quake");
	public static final SoundEvent VOID_DRAIN = register("skill.void_drain");
	public static final SoundEvent SHIELD_UP = register("skill.shield_up");
	public static final SoundEvent SHIELD_HIT = register("skill.shield_hit");
	public static final SoundEvent FORMATION = register("skill.formation");
	public static final SoundEvent BEAM_LOOP = register("skill.beam_loop");
	public static final SoundEvent QI_GATHER = register("skill.qi_gather");
	public static final SoundEvent BREAKTHROUGH = register("skill.breakthrough");
	public static final SoundEvent HEAVEN_SWORD_FALL = register("skill.heaven_sword_fall");
	public static final SoundEvent HEAVEN_SWORD_IMPACT = register("skill.heaven_sword_impact");
	public static final SoundEvent SWORD_FLIGHT = register("skill.sword_flight");
	public static final SoundEvent LEARN_SKILL = register("item.learn_skill");
	public static final SoundEvent SPIRIT_STONE = register("item.spirit_stone");

	private ModSounds() {
	}

	private static SoundEvent register(String path) {
		Identifier id = CelestialArts.id(path);
		return Registry.register(Registries.SOUND_EVENT, id, SoundEvent.of(id));
	}

	public static void register() {
		// class-load
	}
}
