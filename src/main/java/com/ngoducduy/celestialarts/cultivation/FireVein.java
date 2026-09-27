package com.ngoducduy.celestialarts.cultivation;

import com.ngoducduy.celestialarts.alchemy.FlameTier;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.BiomeTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.Predicate;

/**
 * Hỏa Nguyên – wild pockets of fire qi where a Hỏa Linh (fire spirit) can be found. Exactly like
 * the spirit veins in {@link SpiritQi}, a site is a deterministic hotspot inside every ~320-block
 * cell, hashed from the world seed and dimension so it is stable for a world without being saved
 * anywhere. Which of the seven wild hỏa chủng (everything above Phàm Hỏa) a cell can hold – and
 * whether it manifests at all – depends on the biome rolled for that cell: Địa Hỏa in scorched
 * badlands and deserts, Linh Hỏa where spirit qi runs rich (cherry groves, lush caves, meadows),
 * Quỷ Hỏa in eerie places (deep dark, swamps), Nghiệp Hỏa in the Soul Sand Valley, Tam Muội Chân
 * Hỏa around basalt and crimson/warped forests, Thiên Hỏa in the End, and Dị Hỏa anywhere at all
 * (its legendary rarity comes purely from a tiny weight).
 */
public final class FireVein {
	private static final int CELL = 320;
	/** How close a hunter (or a spawning Hỏa Linh) must be to a site for it to count as "here". */
	public static final double RADIUS = 20.0;
	private static final int SAMPLE_Y = 64;

	private FireVein() {
	}

	public record Site(int x, int z, FlameTier tier) {
	}

	private record Candidate(FlameTier tier, int weight, @Nullable Predicate<RegistryEntry<Biome>> biome) {
	}

	/** Nearest deterministic site across the surrounding 3x3 cells – for the {@code /celestial firevein} locator. */
	public static Optional<Site> nearestSite(ServerWorld world, int x, int z) {
		int cx = Math.floorDiv(x, CELL);
		int cz = Math.floorDiv(z, CELL);
		Site best = null;
		double bestDist = Double.MAX_VALUE;
		for (int i = -1; i <= 1; i++) {
			for (int j = -1; j <= 1; j++) {
				Site s = siteInCell(world, cx + i, cz + j);
				if (s == null) continue;
				double d = Math.hypot(s.x() - x, s.z() - z);
				if (d < bestDist) {
					bestDist = d;
					best = s;
				}
			}
		}
		return Optional.ofNullable(best);
	}

	/**
	 * Tier of the site within {@link #RADIUS} of {@code pos}, checking only the cell {@code pos}
	 * itself sits in (a site is always well inside the borders of its own cell, so this alone is
	 * enough – and it never has to peek at a distant, possibly unloaded chunk). Used to gate the
	 * natural spawning of {@code FireSpiritEntity}.
	 */
	@Nullable
	public static FlameTier tierNear(ServerWorld world, BlockPos pos) {
		int cx = Math.floorDiv(pos.getX(), CELL);
		int cz = Math.floorDiv(pos.getZ(), CELL);
		Site s = siteInCell(world, cx, cz);
		if (s == null) return null;
		double d = Math.hypot(s.x() - pos.getX(), s.z() - pos.getZ());
		return d <= RADIUS ? s.tier() : null;
	}

	@Nullable
	private static Site siteInCell(ServerWorld world, int cx, int cz) {
		long seed = world.getSeed() ^ dimensionSalt(world);
		long h = hash(seed, cx, cz);
		int lx = (int) (((h >>> 8) & 0xFFFF) % (CELL - 40)) + 20;
		int lz = (int) (((h >>> 24) & 0xFFFF) % (CELL - 40)) + 20;
		int x = cx * CELL + lx;
		int z = cz * CELL + lz;
		FlameTier tier = pickTier(world, h, x, z);
		return tier == null ? null : new Site(x, z, tier);
	}

	private static long dimensionSalt(ServerWorld world) {
		return world.getRegistryKey().getValue().toString().hashCode() * 0x2545F4914F6CDD1DL;
	}

	@Nullable
	private static FlameTier pickTier(ServerWorld world, long h, int x, int z) {
		Candidate[] cands = candidatesFor(world);
		int total = 0;
		for (Candidate c : cands) total += c.weight();
		if (total <= 0) return null;
		int roll = (int) Long.remainderUnsigned(h, total);
		int acc = 0;
		for (Candidate c : cands) {
			acc += c.weight();
			if (roll < acc) {
				if (c.biome() == null) return c.tier();
				RegistryEntry<Biome> biome = world.getBiome(new BlockPos(x, SAMPLE_Y, z));
				return c.biome().test(biome) ? c.tier() : null;
			}
		}
		return null;
	}

	private static Candidate[] candidatesFor(ServerWorld world) {
		if (world.getRegistryKey().equals(World.NETHER)) {
			return new Candidate[]{
					new Candidate(FlameTier.KARMIC, 30, b -> b.matchesKey(BiomeKeys.SOUL_SAND_VALLEY)),
					new Candidate(FlameTier.SAMADHI, 18, b -> b.matchesKey(BiomeKeys.BASALT_DELTAS)
							|| b.matchesKey(BiomeKeys.CRIMSON_FOREST) || b.matchesKey(BiomeKeys.WARPED_FOREST)),
					new Candidate(FlameTier.STRANGE, 4, null),
			};
		}
		if (world.getRegistryKey().equals(World.END)) {
			return new Candidate[]{
					new Candidate(FlameTier.HEAVEN, 15, b -> b.isIn(BiomeTags.IS_END)),
					new Candidate(FlameTier.STRANGE, 2, null),
			};
		}
		return new Candidate[]{
				new Candidate(FlameTier.EARTH, 40, b -> b.isIn(BiomeTags.IS_BADLANDS) || b.matchesKey(BiomeKeys.DESERT) || b.isIn(BiomeTags.IS_SAVANNA)),
				new Candidate(FlameTier.SPIRIT, 22, b -> b.matchesKey(BiomeKeys.CHERRY_GROVE) || b.matchesKey(BiomeKeys.LUSH_CAVES)
						|| b.matchesKey(BiomeKeys.FLOWER_FOREST) || b.matchesKey(BiomeKeys.MEADOW)),
				new Candidate(FlameTier.GHOST, 22, b -> b.matchesKey(BiomeKeys.DEEP_DARK) || b.matchesKey(BiomeKeys.DRIPSTONE_CAVES)
						|| b.matchesKey(BiomeKeys.SWAMP) || b.matchesKey(BiomeKeys.MANGROVE_SWAMP)),
				new Candidate(FlameTier.STRANGE, 4, null),
		};
	}

	private static long hash(long seed, int cx, int cz) {
		long h = seed ^ (cx * 0x9E3779B97F4A7C15L) ^ (cz * 0xC2B2AE3D27D4EB4FL) ^ 0x9E3779B1L;
		h ^= (h >>> 33);
		h *= 0xFF51AFD7ED558CCDL;
		h ^= (h >>> 33);
		h *= 0xC4CEB9FE1A85EC53L;
		h ^= (h >>> 33);
		return h;
	}
}
