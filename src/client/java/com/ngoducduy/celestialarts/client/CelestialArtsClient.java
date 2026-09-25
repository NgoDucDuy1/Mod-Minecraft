package com.ngoducduy.celestialarts.client;

import com.ngoducduy.celestialarts.CelestialArts;
import com.ngoducduy.celestialarts.client.autotest.CelestialAutoTest;
import com.ngoducduy.celestialarts.client.gui.SkillBookScreen;
import com.ngoducduy.celestialarts.client.gui.SkillHud;
import com.ngoducduy.celestialarts.client.particle.ModParticleFactories;
import com.ngoducduy.celestialarts.client.render.entity.ModEntityRenderers;
import com.ngoducduy.celestialarts.client.render.entity.model.ModModelLayers;
import com.ngoducduy.celestialarts.client.render.fx.ClientFxManager;
import com.ngoducduy.celestialarts.client.render.post.GlowPass;
import com.ngoducduy.celestialarts.client.render.post.ScreenOverlay;
import com.ngoducduy.celestialarts.cultivation.QiHolder;
import com.ngoducduy.celestialarts.item.DaoManualItem;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;

/** Client entrypoint: renderers, particles, HUD, key bindings, packets and the world FX pipeline. */
public final class CelestialArtsClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		CelestialArts.LOGGER.info("Celestial Arts client initialising");

		ModModelLayers.register();
		ModEntityRenderers.register();
		ModParticleFactories.register();
		ClientPackets.register();
		ModKeybinds.register();

		HudRenderCallback.EVENT.register(ScreenOverlay::render);
		HudRenderCallback.EVENT.register(SkillHud::render);
		GlowPass.init();
		WorldRenderEvents.AFTER_TRANSLUCENT.register(ClientFxManager::render);

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			ClientFxManager.tick(client);
			ScreenOverlay.tick(client);
			CameraShake.tick(client);
			ModKeybinds.tick(client);
			ClientMovement.tick(client);
			if (client.player != null && !client.isPaused()) {
				// Client-side cooldown prediction; the server re-syncs authoritative values.
				QiHolder.get(client.player).tickCooldowns();
			}
		});

		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			ClientFxManager.clear();
			ScreenOverlay.clear();
			CameraShake.clear();
		});

		DaoManualItem.OPENER = () -> {
			MinecraftClient client = MinecraftClient.getInstance();
			client.execute(() -> {
				if (client.player != null) client.setScreen(new SkillBookScreen());
			});
		};

		// Headless smoke test driver, only active with -Dcelestialarts.autotest (see CI workflow).
		CelestialAutoTest.init();
	}
}
