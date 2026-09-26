package com.ngoducduy.celestialarts.cultivation;

import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.BiomeTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;

/**
 * Linh khí thiên địa – how much spiritual qi a place holds. Meditation (regeneration, cultivation
 * gain) and breakthrough odds scale with it, so <i>where</i> one cultivates matters:
 *
 * <ul>
 *   <li><b>Biome</b>: mountain peaks, cherry groves, flower forests, meadows, lush caves and the
 *       End are rich (1.4–1.8×); plains are the baseline; deserts, badlands, swamps, the Nether
 *       (demonic qi) and the deep dark are barren (0.3–0.7×).</li>
 *   <li><b>Spirit veins</b> (linh mạch): a deterministic hotspot every ~256 blocks; within 48
 *       blocks of one the qi climbs up to +80%. Finding a vein is worth building a cave on it.</li>
 *   <li><b>Altitude</b>: above y=100 the air thins into pure qi (+0.3 at the summit); deep
 *       underground it is sparse.</li>
 *   <li><b>Sky</b>: a roof between the cultivator and the heavens costs 30% – except in lush
 *       caves, which breathe on their own.</li>
 *   <li><b>Heaven's hours</b>: night +10%, full-moon night +30%, thunderstorms +25%.</li>
 * </ul>
 *
 * The result is clamped to 0.2 … 3.0. The value is a multiplier where 1.0 is "an ordinary plain
 * at noon".
 */
public final class SpiritQi {
	public static final float MIN = 0.2F;
	public static final float MAX = 3.0F;

	private static final int VEIN_CELL = 256;
	private static final double VEIN_RADIUS = 48.0;

	private SpiritQi() {
	}

	public static float density(World world, BlockPos pos) {
		RegistryEntry<Biome> biome = world.getBiome(pos);
		float d = biomeFactor(biome);
		d *= veinFactor(world.getSeed(), pos.getX(), pos.getZ());

		int y = pos.getY();
		if (y >= 100) d += Math.min(0.3F, (y - 100) * 0.003F);
		else if (y < 0) d *= 0.6F;
		else if (y < 40) d *= 0.8F;

		boolean lushCave = biome.matchesKey(BiomeKeys.LUSH_CAVES);
		if (!lushCave && !world.isSkyVisible(pos.up())) d *= 0.7F;

		long time = world.getTimeOfDay() % 24000L;
		boolean night = time >= 13000L && time <= 23000L;
		if (night) d *= world.getMoonPhase() == 0 ? 1.3F : 1.1F;
		if (world.isThundering()) d *= 1.25F;

		return MathHelper.clamp(d, MIN, MAX);
	}

	/** Biome base factor; see class comment. */
	public static float biomeFactor(RegistryEntry<Biome> biome) {
		if (biome.matchesKey(BiomeKeys.DEEP_DARK)) return 0.3F;
		if (biome.isIn(BiomeTags.IS_NETHER)) return 0.5F;
		if (biome.isIn(BiomeTags.IS_END)) return 1.8F;
		if (biome.matchesKey(BiomeKeys.CHERRY_GROVE)) return 1.5F;
		if (biome.matchesKey(BiomeKeys.LUSH_CAVES)) return 1.5F;
		if (biome.matchesKey(BiomeKeys.FLOWER_FOREST) || biome.matchesKey(BiomeKeys.MEADOW)) return 1.4F;
		if (biome.matchesKey(BiomeKeys.JAGGED_PEAKS) || biome.matchesKey(BiomeKeys.FROZEN_PEAKS)
				|| biome.matchesKey(BiomeKeys.STONY_PEAKS) || biome.matchesKey(BiomeKeys.SNOWY_SLOPES)
				|| biome.matchesKey(BiomeKeys.GROVE)) return 1.6F;
		if (biome.matchesKey(BiomeKeys.MUSHROOM_FIELDS)) return 1.3F;
		if (biome.matchesKey(BiomeKeys.DESERT)) return 0.6F;
		if (biome.matchesKey(BiomeKeys.SWAMP) || biome.matchesKey(BiomeKeys.MANGROVE_SWAMP)) return 0.7F;
		if (biome.matchesKey(BiomeKeys.DRIPSTONE_CAVES)) return 0.9F;
		if (biome.isIn(BiomeTags.IS_MOUNTAIN)) return 1.5F;
		if (biome.isIn(BiomeTags.IS_BADLANDS)) return 0.6F;
		if (biome.isIn(BiomeTags.IS_JUNGLE)) return 1.3F;
		if (biome.isIn(BiomeTags.IS_HILL)) return 1.2F;
		if (biome.isIn(BiomeTags.IS_FOREST)) return 1.15F;
		if (biome.isIn(BiomeTags.IS_TAIGA)) return 1.1F;
		if (biome.isIn(BiomeTags.IS_DEEP_OCEAN) || biome.isIn(BiomeTags.IS_OCEAN)) return 0.8F;
		if (biome.isIn(BiomeTags.IS_SAVANNA)) return 0.8F;
		if (biome.isIn(BiomeTags.IS_BEACH)) return 0.9F;
		return 1.0F;
	}

	/**
	 * Spirit-vein multiplier at a column: 1.0 far from every vein, up to 1.8 on top of one. Veins
	 * are placed by hashing the surrounding 256-block cells with the world seed, so they are stable
	 * for a world and different between worlds.
	 */
	public static float veinFactor(long seed, int x, int z) {
		int cx = Math.floorDiv(x, VEIN_CELL);
		int cz = Math.floorDiv(z, VEIN_CELL);
		double best = Double.MAX_VALUE;
		for (int i = -1; i <= 1; i++) {
			for (int j = -1; j <= 1; j++) {
				long h = hash(seed, cx + i, cz + j);
				double vx = (cx + i) * (double) VEIN_CELL + 24 + ((h >>> 8) & 0xFF) / 255.0 * (VEIN_CELL - 48);
				double vz = (cz + j) * (double) VEIN_CELL + 24 + ((h >>> 24) & 0xFF) / 255.0 * (VEIN_CELL - 48);
				double dx = vx - x;
				double dz = vz - z;
				best = Math.min(best, Math.sqrt(dx * dx + dz * dz));
			}
		}
		double t = Math.max(0.0, 1.0 - best / VEIN_RADIUS);
		return (float) (1.0 + 0.8 * t * t);
	}

	/** Distance in blocks to the nearest spirit vein (for the HUD hint and the command). */
	public static double nearestVeinDistance(long seed, int x, int z) {
		int cx = Math.floorDiv(x, VEIN_CELL);
		int cz = Math.floorDiv(z, VEIN_CELL);
		double best = Double.MAX_VALUE;
		for (int i = -1; i <= 1; i++) {
			for (int j = -1; j <= 1; j++) {
				long h = hash(seed, cx + i, cz + j);
				double vx = (cx + i) * (double) VEIN_CELL + 24 + ((h >>> 8) & 0xFF) / 255.0 * (VEIN_CELL - 48);
				double vz = (cz + j) * (double) VEIN_CELL + 24 + ((h >>> 24) & 0xFF) / 255.0 * (VEIN_CELL - 48);
				best = Math.min(best, Math.hypot(vx - x, vz - z));
			}
		}
		return best;
	}

	private static long hash(long seed, int cx, int cz) {
		long h = seed ^ (cx * 0x9E3779B97F4A7C15L) ^ (cz * 0xC2B2AE3D27D4EB4FL);
		h ^= (h >>> 33);
		h *= 0xFF51AFD7ED558CCDL;
		h ^= (h >>> 33);
		h *= 0xC4CEB9FE1A85EC53L;
		h ^= (h >>> 33);
		return h;
	}

	/** Label key for a density value: barren / thin / ordinary / rich / dense / vein. */
	public static String label(float density) {
		if (density < 0.6F) return "barren";
		if (density < 0.9F) return "thin";
		if (density < 1.2F) return "ordinary";
		if (density < 1.6F) return "rich";
		if (density < 2.2F) return "dense";
		return "vein";
	}

	/** Colour for the label (grey → white → green → aqua → gold). */
	public static int labelRgb(float density) {
		if (density < 0.6F) return 0x8A7F7F;
		if (density < 0.9F) return 0xBDBDBD;
		if (density < 1.2F) return 0xFFFFFF;
		if (density < 1.6F) return 0x9CFFB0;
		if (density < 2.2F) return 0x8AF3FF;
		return 0xFFE08A;
	}
}
