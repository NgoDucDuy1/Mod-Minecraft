package com.ngoducduy.celestialarts.registry;

import com.ngoducduy.celestialarts.CelestialArts;
import com.ngoducduy.celestialarts.entity.FireLotusEntity;
import com.ngoducduy.celestialarts.entity.FlyingSwordEntity;
import com.ngoducduy.celestialarts.entity.HeavenSwordEntity;
import com.ngoducduy.celestialarts.entity.ThunderDragonEntity;
import com.ngoducduy.celestialarts.entity.WindDragonEntity;
import com.ngoducduy.celestialarts.entity.IceShardEntity;
import com.ngoducduy.celestialarts.entity.RockSpikeEntity;
import com.ngoducduy.celestialarts.entity.SpiritSwordEntity;
import com.ngoducduy.celestialarts.entity.SwordQiEntity;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public final class ModEntities {
	public static final EntityType<SwordQiEntity> SWORD_QI = register("sword_qi",
			FabricEntityTypeBuilder.<SwordQiEntity>create(SpawnGroup.MISC, SwordQiEntity::new)
					.dimensions(EntityDimensions.fixed(1.6f, 0.6f))
					.trackRangeBlocks(96).trackedUpdateRate(1).forceTrackedVelocityUpdates(true)
					.disableSaving().disableSummon()
					.build());

	public static final EntityType<IceShardEntity> ICE_SHARD = register("ice_shard",
			FabricEntityTypeBuilder.<IceShardEntity>create(SpawnGroup.MISC, IceShardEntity::new)
					.dimensions(EntityDimensions.fixed(0.35f, 0.35f))
					.trackRangeBlocks(96).trackedUpdateRate(1).forceTrackedVelocityUpdates(true)
					.disableSaving().disableSummon()
					.build());

	public static final EntityType<FireLotusEntity> FIRE_LOTUS = register("fire_lotus",
			FabricEntityTypeBuilder.<FireLotusEntity>create(SpawnGroup.MISC, FireLotusEntity::new)
					.dimensions(EntityDimensions.fixed(1.2f, 1.2f))
					.trackRangeBlocks(128).trackedUpdateRate(1).forceTrackedVelocityUpdates(true)
					.fireImmune().disableSaving().disableSummon()
					.build());

	public static final EntityType<SpiritSwordEntity> SPIRIT_SWORD = register("spirit_sword",
			FabricEntityTypeBuilder.<SpiritSwordEntity>create(SpawnGroup.MISC, SpiritSwordEntity::new)
					.dimensions(EntityDimensions.fixed(0.5f, 0.5f))
					.trackRangeBlocks(96).trackedUpdateRate(1).forceTrackedVelocityUpdates(true)
					.disableSaving().disableSummon()
					.build());

	public static final EntityType<FlyingSwordEntity> FLYING_SWORD = register("flying_sword",
			FabricEntityTypeBuilder.<FlyingSwordEntity>create(SpawnGroup.MISC, FlyingSwordEntity::new)
					.dimensions(EntityDimensions.fixed(1.2f, 0.3f))
					.trackRangeBlocks(128).trackedUpdateRate(1).forceTrackedVelocityUpdates(true)
					.fireImmune().disableSaving().disableSummon()
					.build());

	public static final EntityType<RockSpikeEntity> ROCK_SPIKE = register("rock_spike",
			FabricEntityTypeBuilder.<RockSpikeEntity>create(SpawnGroup.MISC, RockSpikeEntity::new)
					.dimensions(EntityDimensions.fixed(1.0f, 2.5f))
					.trackRangeBlocks(96).trackedUpdateRate(4)
					.disableSaving().disableSummon()
					.build());

	public static final EntityType<WindDragonEntity> WIND_DRAGON = register("wind_dragon",
			FabricEntityTypeBuilder.<WindDragonEntity>create(SpawnGroup.MISC, WindDragonEntity::new)
					.dimensions(EntityDimensions.fixed(1.2f, 1.2f))
					.trackRangeBlocks(96).trackedUpdateRate(1).forceTrackedVelocityUpdates(true)
					.disableSaving().disableSummon()
					.build());

	public static final EntityType<ThunderDragonEntity> THUNDER_DRAGON = register("thunder_dragon",
			FabricEntityTypeBuilder.<ThunderDragonEntity>create(SpawnGroup.MISC, ThunderDragonEntity::new)
					.dimensions(EntityDimensions.fixed(0.8f, 0.8f))
					.trackRangeBlocks(128).trackedUpdateRate(1).forceTrackedVelocityUpdates(true)
					.fireImmune().disableSaving().disableSummon()
					.build());

	public static final EntityType<HeavenSwordEntity> HEAVEN_SWORD = register("heaven_sword",
			FabricEntityTypeBuilder.<HeavenSwordEntity>create(SpawnGroup.MISC, HeavenSwordEntity::new)
					.dimensions(EntityDimensions.fixed(3.0f, 12.0f))
					.trackRangeBlocks(256).trackedUpdateRate(1)
					.fireImmune().disableSaving().disableSummon()
					.build());

	private ModEntities() {
	}

	private static <T extends net.minecraft.entity.Entity> EntityType<T> register(String path, EntityType<T> type) {
		return Registry.register(Registries.ENTITY_TYPE, CelestialArts.id(path), type);
	}

	public static void register() {
	}
}
