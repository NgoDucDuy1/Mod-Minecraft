package com.ngoducduy.celestialarts.registry;

import com.ngoducduy.celestialarts.cultivation.Realm;
import com.ngoducduy.celestialarts.item.SkillScrollItem;
import net.fabricmc.fabric.api.loot.v2.LootTableEvents;
import net.minecraft.loot.LootPool;
import net.minecraft.loot.condition.RandomChanceLootCondition;
import net.minecraft.loot.entry.ItemEntry;
import net.minecraft.loot.function.SetCountLootFunction;
import net.minecraft.loot.provider.number.ConstantLootNumberProvider;
import net.minecraft.loot.provider.number.UniformLootNumberProvider;
import net.minecraft.util.Identifier;

import java.util.Map;
import java.util.Set;

/**
 * Injects cultivation loot into vanilla structure chests: spirit stones almost everywhere, and skill
 * manuals whose realm requirement scales with how dangerous the structure is.
 */
public final class ModLootTables {
	/** Chest loot tables grouped by the highest skill realm they may contain. */
	private static final Map<Realm, Set<Identifier>> TIERS = Map.of(
			Realm.QI_REFINING, Set.of(
					chest("simple_dungeon"), chest("abandoned_mineshaft"), chest("village/village_temple"),
					chest("shipwreck_treasure"), chest("pillager_outpost"), chest("igloo_chest")),
			Realm.FOUNDATION, Set.of(
					chest("desert_pyramid"), chest("jungle_temple"), chest("buried_treasure"),
					chest("underwater_ruin_big"), chest("ruined_portal")),
			Realm.GOLDEN_CORE, Set.of(
					chest("stronghold_library"), chest("stronghold_corridor"), chest("woodland_mansion"),
					chest("nether_bridge")),
			Realm.NASCENT_SOUL, Set.of(
					chest("bastion_treasure"), chest("bastion_other"), chest("ancient_city")),
			Realm.SPIRIT_TRANSFORMATION, Set.of(
					chest("end_city_treasure"), chest("ancient_city_ice_box"))
	);

	private ModLootTables() {
	}

	private static Identifier chest(String path) {
		return new Identifier("minecraft", "chests/" + path);
	}

	public static void register() {
		LootTableEvents.MODIFY.register((resourceManager, lootManager, id, tableBuilder, source) -> {
			if (!source.isBuiltin()) return;
			Realm maxRealm = null;
			for (Map.Entry<Realm, Set<Identifier>> e : TIERS.entrySet()) {
				if (e.getValue().contains(id)) {
					maxRealm = e.getKey();
					break;
				}
			}
			if (maxRealm == null) return;

			// Spirit stones: common in every tier, high-grade ones from Golden Core tier upwards.
			LootPool.Builder stones = LootPool.builder()
					.rolls(UniformLootNumberProvider.create(0.0F, 2.0F))
					.with(ItemEntry.builder(ModItems.SPIRIT_STONE).weight(8)
							.apply(SetCountLootFunction.builder(UniformLootNumberProvider.create(1.0F, 4.0F))));
			if (maxRealm.getLevel() >= Realm.GOLDEN_CORE.getLevel()) {
				stones.with(ItemEntry.builder(ModItems.HIGH_SPIRIT_STONE).weight(3)
						.apply(SetCountLootFunction.builder(UniformLootNumberProvider.create(1.0F, 2.0F))));
			}
			if (maxRealm.getLevel() >= Realm.FOUNDATION.getLevel()) {
				stones.with(ItemEntry.builder(ModItems.FOUNDATION_PILL).weight(1));
			}
			if (maxRealm.getLevel() >= Realm.NASCENT_SOUL.getLevel()) {
				stones.with(ItemEntry.builder(ModItems.NASCENT_PILL).weight(1));
			}
			if (maxRealm.getLevel() >= Realm.FOUNDATION.getLevel()) {
				stones.with(ItemEntry.builder(ModItems.QI_PILL).weight(2));
			}
			if (maxRealm.getLevel() >= Realm.TRIBULATION.getLevel()) {
				stones.with(ItemEntry.builder(ModItems.HEAVEN_PILL).weight(1));
			}
			tableBuilder.pool(stones);

			// Skill manuals: one roll with a chance that grows with the tier, weighted towards lower realms.
			float chance = 0.18F + 0.08F * maxRealm.getLevel();
			LootPool.Builder scrolls = LootPool.builder()
					.rolls(ConstantLootNumberProvider.create(1.0F))
					.conditionally(RandomChanceLootCondition.builder(Math.min(0.6F, chance)));
			boolean any = false;
			for (SkillScrollItem scroll : ModItems.scrolls()) {
				Realm req = scroll.getSkill().getRealm();
				if (req.getLevel() > maxRealm.getLevel()) continue;
				// Manuals of the tier's own realm are rarest; the lowest realm manuals most common.
				int weight = 1 + 2 * (maxRealm.getLevel() - req.getLevel());
				scrolls.with(ItemEntry.builder(scroll).weight(weight));
				any = true;
			}
			if (any) tableBuilder.pool(scrolls);
		});
	}
}
