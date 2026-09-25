package com.ngoducduy.celestialarts.registry;

import com.ngoducduy.celestialarts.CelestialArts;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;

public final class ModItemGroups {
	public static final RegistryKey<ItemGroup> KEY = RegistryKey.of(RegistryKeys.ITEM_GROUP, CelestialArts.id("main"));

	private ModItemGroups() {
	}

	public static void register() {
		ItemGroup group = FabricItemGroup.builder()
				.icon(() -> new ItemStack(ModItems.DAO_MANUAL))
				.displayName(Text.translatable("itemGroup.celestialarts.main"))
				.entries((context, entries) -> {
					for (Item item : ModItems.all()) {
						entries.add(item);
					}
				})
				.build();
		Registry.register(Registries.ITEM_GROUP, KEY, group);
	}
}
