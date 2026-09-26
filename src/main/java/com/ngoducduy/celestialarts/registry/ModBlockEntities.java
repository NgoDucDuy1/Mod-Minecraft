package com.ngoducduy.celestialarts.registry;

import com.ngoducduy.celestialarts.CelestialArts;
import com.ngoducduy.celestialarts.block.entity.AlchemyFurnaceBlockEntity;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public final class ModBlockEntities {
	public static final BlockEntityType<AlchemyFurnaceBlockEntity> ALCHEMY_FURNACE = Registry.register(
			Registries.BLOCK_ENTITY_TYPE, CelestialArts.id("alchemy_furnace"),
			FabricBlockEntityTypeBuilder.create(AlchemyFurnaceBlockEntity::new, ModBlocks.furnaces().toArray(new Block[0])).build());

	private ModBlockEntities() {
	}

	public static void register() {
	}
}
