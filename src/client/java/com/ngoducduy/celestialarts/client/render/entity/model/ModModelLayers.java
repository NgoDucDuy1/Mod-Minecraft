package com.ngoducduy.celestialarts.client.render.entity.model;

import com.ngoducduy.celestialarts.CelestialArts;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.minecraft.client.render.entity.model.EntityModelLayer;

/** Model layer ids for every custom entity model. */
public final class ModModelLayers {
	public static final EntityModelLayer SWORD_QI = layer("sword_qi");
	public static final EntityModelLayer ICE_SHARD = layer("ice_shard");
	public static final EntityModelLayer FIRE_LOTUS = layer("fire_lotus");
	public static final EntityModelLayer SPIRIT_SWORD = layer("spirit_sword");
	public static final EntityModelLayer FLYING_SWORD = layer("flying_sword");
	public static final EntityModelLayer ROCK_SPIKE = layer("rock_spike");
	public static final EntityModelLayer HEAVEN_SWORD = layer("heaven_sword");
	public static final EntityModelLayer WIND_DRAGON = layer("wind_dragon");
	public static final EntityModelLayer THUNDER_DRAGON = layer("thunder_dragon");
	public static final EntityModelLayer FIRE_SPIRIT = layer("fire_spirit");
	/** The colossal palm of Thiên Đạo Chi Thủ (drawn by the client FX system, not an entity). */
	public static final EntityModelLayer HEAVEN_HAND = layer("heaven_hand");
	public static final EntityModelLayer MEDITATION_SEAT = layer("meditation_seat");

	private ModModelLayers() {
	}

	private static EntityModelLayer layer(String name) {
		return new EntityModelLayer(CelestialArts.id(name), "main");
	}

	public static void register() {
		EntityModelLayerRegistry.registerModelLayer(SWORD_QI, SwordQiModel::getTexturedModelData);
		EntityModelLayerRegistry.registerModelLayer(ICE_SHARD, IceShardModel::getTexturedModelData);
		EntityModelLayerRegistry.registerModelLayer(FIRE_LOTUS, FireLotusModel::getTexturedModelData);
		EntityModelLayerRegistry.registerModelLayer(SPIRIT_SWORD, SpiritSwordModel::getTexturedModelData);
		EntityModelLayerRegistry.registerModelLayer(FLYING_SWORD, FlyingSwordModel::getTexturedModelData);
		EntityModelLayerRegistry.registerModelLayer(ROCK_SPIKE, RockSpikeModel::getTexturedModelData);
		EntityModelLayerRegistry.registerModelLayer(HEAVEN_SWORD, HeavenSwordModel::getTexturedModelData);
		EntityModelLayerRegistry.registerModelLayer(WIND_DRAGON, DragonHeadModel::getTexturedModelData);
		EntityModelLayerRegistry.registerModelLayer(THUNDER_DRAGON, DragonHeadModel::getTexturedModelData);
		EntityModelLayerRegistry.registerModelLayer(FIRE_SPIRIT, FireSpiritModel::getTexturedModelData);
		EntityModelLayerRegistry.registerModelLayer(HEAVEN_HAND, HeavenHandModel::getTexturedModelData);
		EntityModelLayerRegistry.registerModelLayer(MEDITATION_SEAT, MeditationSeatModel::getTexturedModelData);
	}
}
