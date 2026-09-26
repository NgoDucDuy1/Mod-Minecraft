package com.ngoducduy.celestialarts.block;

import com.ngoducduy.celestialarts.alchemy.Herb;
import com.ngoducduy.celestialarts.cultivation.SpiritQi;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.Fertilizable;
import net.minecraft.block.PlantBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;

/**
 * A linh dược growing in the world. Three growth stages ({@link #AGE} 0 sprout, 1 half grown,
 * 2 mature); only a mature plant yields more than one herb. Growth is a random tick roll whose
 * odds scale with the local spirit-qi density, so herb gardens do best on a spirit vein.
 * Bone meal (and, later, alchemists' qi) advances a stage. Each species roots only on the ground
 * of its habitat – see {@link Herb.Ground}.
 */
public class HerbBlock extends PlantBlock implements Fertilizable {
	public static final int MAX_AGE = 2;
	public static final IntProperty AGE = IntProperty.of("age", 0, MAX_AGE);
	private static final VoxelShape[] SHAPES = {
			Block.createCuboidShape(5.0, 0.0, 5.0, 11.0, 6.0, 11.0),
			Block.createCuboidShape(4.0, 0.0, 4.0, 12.0, 10.0, 12.0),
			Block.createCuboidShape(3.0, 0.0, 3.0, 13.0, 14.0, 13.0),
	};
	/** Base odds of advancing a stage per random tick at ordinary spirit-qi density (≈ a wheat crop). */
	private static final float BASE_GROWTH_CHANCE = 0.11F;

	private final Herb herb;

	public HerbBlock(Herb herb, Settings settings) {
		super(settings);
		this.herb = herb;
		setDefaultState(getStateManager().getDefaultState().with(AGE, 0));
	}

	public Herb getHerb() {
		return herb;
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(AGE);
	}

	@Override
	public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		Vec3d off = state.getModelOffset(world, pos);
		return SHAPES[state.get(AGE)].offset(off.x, off.y, off.z);
	}

	@Override
	protected boolean canPlantOnTop(BlockState floor, BlockView world, BlockPos pos) {
		return canRootOn(herb.habitat().getGround(), floor);
	}

	/** Which blocks a herb of the given ground family accepts as footing. */
	public static boolean canRootOn(Herb.Ground ground, BlockState floor) {
		boolean soil = floor.isIn(BlockTags.DIRT) || floor.isOf(Blocks.FARMLAND) || floor.isOf(Blocks.MOSS_BLOCK) || floor.isOf(Blocks.CLAY);
		boolean stone = floor.isIn(BlockTags.BASE_STONE_OVERWORLD) || floor.isOf(Blocks.GRAVEL) || floor.isOf(Blocks.CALCITE)
				|| floor.isOf(Blocks.DRIPSTONE_BLOCK) || floor.isOf(Blocks.COBBLESTONE) || floor.isOf(Blocks.MOSSY_COBBLESTONE)
				|| floor.isOf(Blocks.COBBLED_DEEPSLATE) || floor.isOf(Blocks.SCULK) || floor.isOf(Blocks.MOSS_BLOCK) || floor.isIn(BlockTags.DIRT);
		return switch (ground) {
			case SOIL -> soil;
			case SOIL_OR_STONE -> soil || stone;
			case SOIL_OR_SNOW -> soil || floor.isIn(BlockTags.SNOW) || floor.isOf(Blocks.PACKED_ICE) || floor.isOf(Blocks.BLUE_ICE) || floor.isIn(BlockTags.BASE_STONE_OVERWORLD);
			case SAND_OR_SOIL -> soil || floor.isIn(BlockTags.SAND) || floor.isIn(BlockTags.TERRACOTTA) || floor.isOf(Blocks.SANDSTONE) || floor.isOf(Blocks.RED_SANDSTONE);
			case STONE -> stone;
			case NETHER -> floor.isIn(BlockTags.NYLIUM) || floor.isIn(BlockTags.BASE_STONE_NETHER) || floor.isOf(Blocks.SOUL_SOIL)
					|| floor.isOf(Blocks.SOUL_SAND) || floor.isOf(Blocks.MAGMA_BLOCK) || floor.isIn(BlockTags.DIRT);
			case END -> floor.isOf(Blocks.END_STONE) || floor.isIn(BlockTags.DIRT) || floor.isOf(Blocks.OBSIDIAN);
		};
	}

	public static boolean isMature(BlockState state) {
		return state.get(AGE) >= MAX_AGE;
	}

	@Override
	public boolean hasRandomTicks(BlockState state) {
		return !isMature(state);
	}

	@Override
	public void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
		if (isMature(state)) return;
		float density = SpiritQi.density(world, pos);
		float chance = BASE_GROWTH_CHANCE * MathHelper.clamp(density, 0.25F, 3.0F);
		if (world.getBaseLightLevel(pos, 0) >= 9 || herb.habitat().getPlacement() != Herb.Placement.SURFACE) {
			if (random.nextFloat() < chance) {
				world.setBlockState(pos, state.with(AGE, state.get(AGE) + 1), Block.NOTIFY_LISTENERS);
			}
		}
	}

	@Override
	public boolean isFertilizable(WorldView world, BlockPos pos, BlockState state, boolean isClient) {
		return !isMature(state);
	}

	@Override
	public boolean canGrow(World world, Random random, BlockPos pos, BlockState state) {
		return true;
	}

	@Override
	public void grow(ServerWorld world, Random random, BlockPos pos, BlockState state) {
		world.setBlockState(pos, state.with(AGE, Math.min(MAX_AGE, state.get(AGE) + 1)), Block.NOTIFY_LISTENERS);
	}

	@Override
	public ItemStack getPickStack(BlockView world, BlockPos pos, BlockState state) {
		return new ItemStack(this);
	}
}
