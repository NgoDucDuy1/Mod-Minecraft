package com.ngoducduy.celestialarts.registry;

import com.ngoducduy.celestialarts.CelestialArts;
import com.ngoducduy.celestialarts.alchemy.Herb;
import com.ngoducduy.celestialarts.alchemy.Herbs;
import com.ngoducduy.celestialarts.block.HerbBlock;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.MapColor;
import net.minecraft.block.enums.Instrument;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ModBlocks {
	private static final List<Block> ALL = new ArrayList<>();
	private static final Map<Herb, HerbBlock> HERBS = new LinkedHashMap<>();

	static {
		for (Herb herb : Herbs.all()) {
			HERBS.put(herb, register(herb.blockKey(), new HerbBlock(herb, herbSettings(herb))));
		}
	}

	private ModBlocks() {
	}

	private static AbstractBlock.Settings herbSettings(Herb herb) {
		boolean nether = herb.habitat() == Herb.Habitat.NETHER;
		boolean cave = herb.habitat().getPlacement() != Herb.Placement.SURFACE;
		AbstractBlock.Settings s = AbstractBlock.Settings.create()
				.mapColor(nether ? MapColor.DARK_RED : MapColor.DARK_GREEN)
				.instrument(Instrument.FLUTE)
				.noCollision()
				.breakInstantly()
				.sounds(nether ? BlockSoundGroup.ROOTS : cave ? BlockSoundGroup.CAVE_VINES : BlockSoundGroup.GRASS)
				.offset(AbstractBlock.OffsetType.XZ)
				.pistonBehavior(PistonBehavior.DESTROY);
		if (herb.grade() >= 4 || nether) {
			// Precious and hellish species give off a little spirit light.
			int lum = nether ? 7 : 4 + herb.grade();
			s = s.luminance(state -> HerbBlock.isMature(state) ? lum : lum / 2);
		}
		return s;
	}

	private static <T extends Block> T register(String path, T block) {
		Registry.register(Registries.BLOCK, CelestialArts.id(path), block);
		ALL.add(block);
		return block;
	}

	public static HerbBlock herb(Herb herb) {
		return HERBS.get(herb);
	}

	public static Map<Herb, HerbBlock> herbs() {
		return Collections.unmodifiableMap(HERBS);
	}

	public static List<Block> all() {
		return Collections.unmodifiableList(ALL);
	}

	public static void register() {
	}
}
