package com.ngoducduy.celestialarts.block;

import com.ngoducduy.celestialarts.alchemy.FurnaceGrade;
import com.ngoducduy.celestialarts.alchemy.FurnaceType;
import com.ngoducduy.celestialarts.block.entity.AlchemyFurnaceBlockEntity;
import com.ngoducduy.celestialarts.registry.ModBlockEntities;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Lò luyện đan – one block per (type, grade). All logic lives in the block entity. */
public class AlchemyFurnaceBlock extends BlockWithEntity {
	public static final DirectionProperty FACING = HorizontalFacingBlock.FACING;
	public static final BooleanProperty LIT = Properties.LIT;
	private static final VoxelShape SHAPE = VoxelShapes.union(
			Block.createCuboidShape(3, 0, 3, 13, 3, 13),
			Block.createCuboidShape(2, 3, 2, 14, 13, 14),
			Block.createCuboidShape(4, 13, 4, 12, 14, 12),
			Block.createCuboidShape(7, 14, 7, 9, 16, 9));

	private final FurnaceType furnaceType;
	private final FurnaceGrade furnaceGrade;

	public AlchemyFurnaceBlock(FurnaceType type, FurnaceGrade grade, Settings settings) {
		super(settings);
		this.furnaceType = type;
		this.furnaceGrade = grade;
		setDefaultState(getStateManager().getDefaultState().with(FACING, net.minecraft.util.math.Direction.NORTH).with(LIT, false));
	}

	public FurnaceType getFurnaceType() {
		return furnaceType;
	}

	public FurnaceGrade getFurnaceGrade() {
		return furnaceGrade;
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(FACING, LIT);
	}

	@Override
	public BlockState getPlacementState(ItemPlacementContext ctx) {
		return getDefaultState().with(FACING, ctx.getHorizontalPlayerFacing().getOpposite());
	}

	@Override
	public BlockState rotate(BlockState state, BlockRotation rotation) {
		return state.with(FACING, rotation.rotate(state.get(FACING)));
	}

	@Override
	public BlockState mirror(BlockState state, BlockMirror mirror) {
		return state.rotate(mirror.getRotation(state.get(FACING)));
	}

	@Override
	public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return SHAPE;
	}

	@Override
	public BlockRenderType getRenderType(BlockState state) {
		return BlockRenderType.MODEL;
	}

	@Nullable
	@Override
	public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
		return new AlchemyFurnaceBlockEntity(pos, state);
	}

	@Nullable
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
		return world.isClient ? null : checkType(type, ModBlockEntities.ALCHEMY_FURNACE, AlchemyFurnaceBlockEntity::serverTick);
	}

	@Override
	public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
		if (world.isClient) return ActionResult.SUCCESS;
		if (world.getBlockEntity(pos) instanceof AlchemyFurnaceBlockEntity be) {
			player.openHandledScreen(be);
		}
		return ActionResult.CONSUME;
	}

	@Override
	public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
		if (!state.isOf(newState.getBlock()) && world.getBlockEntity(pos) instanceof AlchemyFurnaceBlockEntity be) {
			ItemScatterer.spawn(world, pos, be.getItems());
			world.updateComparators(pos, this);
		}
		super.onStateReplaced(state, world, pos, newState, moved);
	}

	@Override
	public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
		if (!state.get(LIT)) return;
		double x = pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.3;
		double y = pos.getY() + 0.95;
		double z = pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.3;
		world.addParticle(ParticleTypes.SMOKE, x, y, z, 0.0, 0.03, 0.0);
		if (random.nextInt(3) == 0) world.addParticle(ParticleTypes.FLAME, x, y - 0.05, z, 0.0, 0.01, 0.0);
	}

	@Override
	public void appendTooltip(ItemStack stack, @Nullable BlockView world, List<Text> tooltip, TooltipContext options) {
		tooltip.add(Text.translatable("furnace.celestialarts.tooltip.grade", furnaceGrade.getName(),
				Text.translatable("herb.celestialarts.grade." + furnaceGrade.getMaxPillGrade())).formatted(Formatting.GRAY));
		tooltip.add(furnaceType.getDescription().copy().formatted(Formatting.DARK_AQUA, Formatting.ITALIC));
	}
}
