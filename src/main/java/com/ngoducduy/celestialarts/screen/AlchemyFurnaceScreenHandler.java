package com.ngoducduy.celestialarts.screen;

import com.ngoducduy.celestialarts.alchemy.AlchemyRecipe;
import com.ngoducduy.celestialarts.block.entity.AlchemyFurnaceBlockEntity;
import com.ngoducduy.celestialarts.item.FlameItem;
import com.ngoducduy.celestialarts.item.HerbItem;
import com.ngoducduy.celestialarts.registry.ModRecipes;
import com.ngoducduy.celestialarts.registry.ModScreenHandlers;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ArrayPropertyDelegate;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/** Container for the furnace: 6 herb slots, flame, output, player inventory; live numbers via the property delegate. */
public class AlchemyFurnaceScreenHandler extends ScreenHandler {
	public static final int HERB_X = 9, HERB_Y = 19, FLAME_X = 18, FLAME_Y = 82, OUTPUT_X = 18, OUTPUT_Y = 112, INV_X = 20, INV_Y = 140;

	private final Inventory inventory;
	private final PropertyDelegate properties;
	private final BlockPos pos;
	@Nullable
	private final AlchemyFurnaceBlockEntity blockEntity;

	/** Client constructor. */
	public AlchemyFurnaceScreenHandler(int syncId, PlayerInventory playerInventory, PacketByteBuf buf) {
		this(syncId, playerInventory, new SimpleInventory(AlchemyFurnaceBlockEntity.SIZE), new ArrayPropertyDelegate(AlchemyFurnaceBlockEntity.PROPERTY_COUNT), buf.readBlockPos());
	}

	public AlchemyFurnaceScreenHandler(int syncId, PlayerInventory playerInventory, Inventory inventory, PropertyDelegate properties, BlockPos pos) {
		super(ModScreenHandlers.ALCHEMY_FURNACE, syncId);
		checkSize(inventory, AlchemyFurnaceBlockEntity.SIZE);
		this.inventory = inventory;
		this.properties = properties;
		this.pos = pos;
		this.blockEntity = inventory instanceof AlchemyFurnaceBlockEntity be ? be : null;
		inventory.onOpen(playerInventory.player);

		for (int r = 0; r < 3; r++) {
			for (int c = 0; c < 2; c++) {
				addSlot(new LockedSlot(inventory, r * 2 + c, HERB_X + c * 18, HERB_Y + r * 18) {
					@Override
					public boolean canInsert(ItemStack stack) {
						return stack.getItem() instanceof HerbItem && !isRefining();
					}
				});
			}
		}
		addSlot(new LockedSlot(inventory, AlchemyFurnaceBlockEntity.SLOT_FLAME, FLAME_X, FLAME_Y) {
			@Override
			public boolean canInsert(ItemStack stack) {
				return stack.getItem() instanceof FlameItem && !isRefining();
			}

			@Override
			public int getMaxItemCount() {
				return 1;
			}
		});
		addSlot(new Slot(inventory, AlchemyFurnaceBlockEntity.SLOT_OUTPUT, OUTPUT_X, OUTPUT_Y) {
			@Override
			public boolean canInsert(ItemStack stack) {
				return false;
			}
		});
		for (int r = 0; r < 3; r++) {
			for (int c = 0; c < 9; c++) {
				addSlot(new Slot(playerInventory, c + r * 9 + 9, INV_X + c * 18, INV_Y + r * 18));
			}
		}
		for (int c = 0; c < 9; c++) {
			addSlot(new Slot(playerInventory, c, INV_X + c * 18, INV_Y + 58));
		}
		addProperties(properties);
	}

	/** Herbs and the flame stay put while the furnace runs. */
	private class LockedSlot extends Slot {
		LockedSlot(Inventory inventory, int index, int x, int y) {
			super(inventory, index, x, y);
		}

		@Override
		public boolean canTakeItems(PlayerEntity player) {
			return !isRefining();
		}
	}

	public boolean isRefining() {
		return properties.get(AlchemyFurnaceBlockEntity.P_STATE) == AlchemyFurnaceBlockEntity.STATE_REFINING;
	}

	public int get(int property) {
		return properties.get(property);
	}

	public BlockPos getPos() {
		return pos;
	}

	public Inventory getInventory() {
		return inventory;
	}

	/** The recipe the herbs in the slots form, matched locally (works on both sides since the herbs stay in place). */
	public Optional<AlchemyRecipe> matchRecipe(World world) {
		return world.getRecipeManager().getFirstMatch(ModRecipes.ALCHEMY_TYPE, inventory, world);
	}

	@Override
	public boolean onButtonClick(PlayerEntity player, int id) {
		if (player instanceof ServerPlayerEntity sp && blockEntity != null) {
			blockEntity.onButton(sp, id);
			return true;
		}
		return false;
	}

	@Override
	public ItemStack quickMove(PlayerEntity player, int index) {
		ItemStack result = ItemStack.EMPTY;
		Slot slot = slots.get(index);
		if (slot.hasStack()) {
			ItemStack stack = slot.getStack();
			result = stack.copy();
			int containerSlots = AlchemyFurnaceBlockEntity.SIZE;
			if (index < containerSlots) {
				if (!insertItem(stack, containerSlots, slots.size(), true)) return ItemStack.EMPTY;
			} else if (stack.getItem() instanceof HerbItem && !isRefining()) {
				if (!insertItem(stack, 0, AlchemyRecipe.HERB_SLOTS, false)) return ItemStack.EMPTY;
			} else if (stack.getItem() instanceof FlameItem && !isRefining()) {
				if (!insertItem(stack, AlchemyFurnaceBlockEntity.SLOT_FLAME, AlchemyFurnaceBlockEntity.SLOT_FLAME + 1, false)) return ItemStack.EMPTY;
			} else {
				return ItemStack.EMPTY;
			}
			if (stack.isEmpty()) slot.setStack(ItemStack.EMPTY);
			else slot.markDirty();
		}
		return result;
	}

	@Override
	public boolean canUse(PlayerEntity player) {
		return inventory.canPlayerUse(player);
	}

	@Override
	public void onClosed(PlayerEntity player) {
		super.onClosed(player);
		inventory.onClose(player);
	}
}
