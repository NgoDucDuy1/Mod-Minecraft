package com.ngoducduy.celestialarts;

import com.ngoducduy.celestialarts.command.CelestialCommand;
import com.ngoducduy.celestialarts.cultivation.CultivationEvents;
import com.ngoducduy.celestialarts.network.ModPackets;
import com.ngoducduy.celestialarts.registry.ModDamageTypes;
import com.ngoducduy.celestialarts.registry.ModEffects;
import com.ngoducduy.celestialarts.registry.ModEntities;
import com.ngoducduy.celestialarts.registry.ModItemGroups;
import com.ngoducduy.celestialarts.registry.ModLootTables;
import com.ngoducduy.celestialarts.registry.ModItems;
import com.ngoducduy.celestialarts.registry.ModParticles;
import com.ngoducduy.celestialarts.registry.ModSounds;
import com.ngoducduy.celestialarts.skill.SkillRegistry;
import net.fabricmc.api.ModInitializer;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Celestial Arts – Tiên Đạo Thần Thông.
 *
 * <p>A Fabric 1.20.1 mod that adds a cultivation (tu tiên) skill system with
 * donghua-style visual effects: sword qi, fire lotus, heavenly tribulation,
 * sword flight, formations and more. Every visual is custom made (particles,
 * entity models, procedural geometry rendered through custom render layers).</p>
 */
public final class CelestialArts implements ModInitializer {
	public static final String MOD_ID = "celestialarts";
	public static final Logger LOGGER = LoggerFactory.getLogger("CelestialArts");

	public static Identifier id(String path) {
		return new Identifier(MOD_ID, path);
	}

	@Override
	public void onInitialize() {
		LOGGER.info("Khai mở Tiên Đạo... (Celestial Arts initialising)");

		ModSounds.register();
		ModParticles.register();
		ModEffects.register();
		ModDamageTypes.init();
		ModEntities.register();
		SkillRegistry.registerAll();
		ModItems.register();
		ModItemGroups.register();
		ModLootTables.register();
		ModPackets.registerServerReceivers();
		CultivationEvents.register();
		CelestialCommand.register();

		LOGGER.info("Celestial Arts loaded {} skills.", SkillRegistry.all().size());
	}
}
