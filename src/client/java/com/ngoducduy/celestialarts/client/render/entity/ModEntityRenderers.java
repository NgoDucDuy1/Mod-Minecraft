package com.ngoducduy.celestialarts.client.render.entity;

import com.ngoducduy.celestialarts.registry.ModEntities;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

/** Binds every custom entity to its renderer. */
public final class ModEntityRenderers {
	private ModEntityRenderers() {
	}

	public static void register() {
		EntityRendererRegistry.register(ModEntities.SWORD_QI, SwordQiRenderer::new);
		EntityRendererRegistry.register(ModEntities.ICE_SHARD, IceShardRenderer::new);
		EntityRendererRegistry.register(ModEntities.FIRE_LOTUS, FireLotusRenderer::new);
		EntityRendererRegistry.register(ModEntities.SPIRIT_SWORD, SpiritSwordRenderer::new);
		EntityRendererRegistry.register(ModEntities.FLYING_SWORD, FlyingSwordRenderer::new);
		EntityRendererRegistry.register(ModEntities.ROCK_SPIKE, RockSpikeRenderer::new);
		EntityRendererRegistry.register(ModEntities.HEAVEN_SWORD, HeavenSwordRenderer::new);
	}
}
