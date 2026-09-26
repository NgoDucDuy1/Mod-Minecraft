package com.ngoducduy.celestialarts.registry;

import com.ngoducduy.celestialarts.CelestialArts;
import com.ngoducduy.celestialarts.screen.AlchemyFurnaceScreenHandler;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.screen.ScreenHandlerType;

public final class ModScreenHandlers {
	public static final ScreenHandlerType<AlchemyFurnaceScreenHandler> ALCHEMY_FURNACE = Registry.register(
			Registries.SCREEN_HANDLER, CelestialArts.id("alchemy_furnace"), new ExtendedScreenHandlerType<>(AlchemyFurnaceScreenHandler::new));

	private ModScreenHandlers() {
	}

	public static void register() {
	}
}
