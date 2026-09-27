package com.ngoducduy.celestialarts.registry;

import com.ngoducduy.celestialarts.CelestialArts;
import com.ngoducduy.celestialarts.alchemy.Herb;
import com.ngoducduy.celestialarts.alchemy.Herbs;
import com.ngoducduy.celestialarts.alchemy.FlameTier;
import com.ngoducduy.celestialarts.alchemy.Pill;
import com.ngoducduy.celestialarts.alchemy.Pills;
import com.ngoducduy.celestialarts.block.AlchemyFurnaceBlock;
import com.ngoducduy.celestialarts.item.FlameCaptureBottleItem;
import com.ngoducduy.celestialarts.item.FlameItem;
import com.ngoducduy.celestialarts.item.PillItem;
import com.ngoducduy.celestialarts.item.PillSlagItem;
import net.minecraft.item.BlockItem;
import com.ngoducduy.celestialarts.item.HerbItem;
import com.ngoducduy.celestialarts.item.DaoManualItem;
import com.ngoducduy.celestialarts.item.ImmortalSwordItem;
import com.ngoducduy.celestialarts.item.SkillScrollItem;
import com.ngoducduy.celestialarts.item.SpiritStoneItem;
import com.ngoducduy.celestialarts.skill.Skill;
import com.ngoducduy.celestialarts.skill.SkillRegistry;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Rarity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ModItems {
	private static final List<Item> ALL = new ArrayList<>();
	private static final List<SkillScrollItem> SCROLLS = new ArrayList<>();
	private static final List<HerbItem> HERBS = new ArrayList<>();
	private static final List<PillItem> PILLS = new ArrayList<>();
	private static final List<FlameItem> FLAMES = new ArrayList<>();

	/** Đan tra – residue of a ruined batch. */
	public static final Item PILL_SLAG = register("pill_slag", new PillSlagItem(new Item.Settings().maxCount(64)));

	public static final Item DAO_MANUAL = register("dao_manual", new DaoManualItem(new Item.Settings().maxCount(1).rarity(Rarity.UNCOMMON)));
	public static final Item IMMORTAL_SWORD = register("immortal_sword", new ImmortalSwordItem(new Item.Settings().rarity(Rarity.RARE).fireproof()));

	public static final Item SPIRIT_STONE = register("spirit_stone", new SpiritStoneItem(60f, 15, 0x9FE8FF, new Item.Settings().maxCount(64)));
	public static final Item HIGH_SPIRIT_STONE = register("high_spirit_stone", new SpiritStoneItem(200f, 80, 0xFFD36B, new Item.Settings().maxCount(64).rarity(Rarity.UNCOMMON)));
	public static final Item FOUNDATION_PILL = register("foundation_pill", new SpiritStoneItem(100f, 400, 0x7CFFB0, new Item.Settings().maxCount(16).rarity(Rarity.RARE)));
	public static final Item NASCENT_PILL = register("nascent_pill", new SpiritStoneItem(300f, 2000, 0xD98BFF, new Item.Settings().maxCount(16).rarity(Rarity.EPIC)));
	/** Pure qi restoration, no cultivation experience – the cheap "mana potion". */
	public static final Item QI_PILL = register("qi_pill", new SpiritStoneItem(200f, 0, 0x50DCC8, new Item.Settings().maxCount(16).rarity(Rarity.UNCOMMON)));
	/** End-game pill: fills a Tribulation cultivator's qi and grants a large chunk of experience. */
	public static final Item HEAVEN_PILL = register("heaven_pill", new SpiritStoneItem(620f, 6000, 0xFFD75A, new Item.Settings().maxCount(16).rarity(Rarity.EPIC)));

	/** Bình Hỏa Phách – khống chế lửa: binds a weakened Hỏa Linh's flame; see B3. */
	public static final Item FLAME_CAPTURE_BOTTLE = register("flame_capture_bottle", new FlameCaptureBottleItem(new Item.Settings().maxCount(16)));

	static {
		// One manual per skill, registered in skill order.
		for (Skill skill : SkillRegistry.all()) {
			String path = "scroll_" + skill.getId().getPath();
			SkillScrollItem item = new SkillScrollItem(() -> skill, new Item.Settings().maxCount(16));
			register(path, item);
			SCROLLS.add(item);
		}
		// One item per herb species, in table order (element, then grade).
		for (Herb herb : Herbs.all()) {
			HerbItem item = new HerbItem(ModBlocks.herb(herb), new Item.Settings().maxCount(64).rarity(herb.grade() >= 5 ? Rarity.EPIC : herb.grade() >= 4 ? Rarity.RARE : herb.grade() >= 3 ? Rarity.UNCOMMON : Rarity.COMMON));
			register(herb.blockKey(), item);
			HERBS.add(item);
		}
		for (AlchemyFurnaceBlock block : ModBlocks.furnaces()) {
			register(ModBlocks.furnaceKey(block.getFurnaceType(), block.getFurnaceGrade()),
					new BlockItem(block, new Item.Settings().rarity(block.getFurnaceGrade().ordinal() >= 3 ? Rarity.EPIC : block.getFurnaceGrade().ordinal() >= 2 ? Rarity.RARE : Rarity.COMMON)));
		}
		for (FlameTier tier : FlameTier.values()) {
			FlameItem item = new FlameItem(tier, new Item.Settings().maxCount(1).fireproof().rarity(tier.getTier() >= 5 ? Rarity.EPIC : tier.getTier() >= 3 ? Rarity.RARE : Rarity.UNCOMMON));
			register(tier.getKey(), item);
			FLAMES.add(item);
		}
		for (Pill pill : Pills.all()) {
			PillItem item = new PillItem(pill, new Item.Settings().maxCount(16).rarity(pill.grade() >= 5 ? Rarity.EPIC : pill.grade() >= 4 ? Rarity.RARE : pill.grade() >= 3 ? Rarity.UNCOMMON : Rarity.COMMON));
			register(pill.itemKey(), item);
			PILLS.add(item);
		}
	}

	private ModItems() {
	}

	private static <T extends Item> T register(String path, T item) {
		Registry.register(Registries.ITEM, CelestialArts.id(path), item);
		ALL.add(item);
		return item;
	}

	public static List<Item> all() {
		return Collections.unmodifiableList(ALL);
	}

	public static List<PillItem> pills() {
		return Collections.unmodifiableList(PILLS);
	}

	public static List<FlameItem> flames() {
		return Collections.unmodifiableList(FLAMES);
	}

	/** The vessel item for a given hỏa chủng (flames are declared in {@link FlameTier} enum order). */
	public static FlameItem flameFor(FlameTier tier) {
		return FLAMES.get(tier.ordinal());
	}

	public static List<HerbItem> herbs() {
		return Collections.unmodifiableList(HERBS);
	}

	public static List<SkillScrollItem> scrolls() {
		return Collections.unmodifiableList(SCROLLS);
	}

	public static void register() {
	}
}
