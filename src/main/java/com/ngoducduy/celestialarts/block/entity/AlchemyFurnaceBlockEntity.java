package com.ngoducduy.celestialarts.block.entity;

import com.ngoducduy.celestialarts.alchemy.AlchemyRecipe;
import com.ngoducduy.celestialarts.alchemy.FlameTier;
import com.ngoducduy.celestialarts.alchemy.FurnaceGrade;
import com.ngoducduy.celestialarts.alchemy.FurnaceType;
import com.ngoducduy.celestialarts.alchemy.PillQuality;
import com.ngoducduy.celestialarts.block.AlchemyFurnaceBlock;
import com.ngoducduy.celestialarts.cultivation.PlayerQi;
import com.ngoducduy.celestialarts.cultivation.QiHolder;
import com.ngoducduy.celestialarts.item.FlameItem;
import com.ngoducduy.celestialarts.item.PillItem;
import com.ngoducduy.celestialarts.network.ModPackets;
import com.ngoducduy.celestialarts.registry.GlowParticleEffect;
import com.ngoducduy.celestialarts.registry.ModBlockEntities;
import com.ngoducduy.celestialarts.registry.ModItems;
import com.ngoducduy.celestialarts.registry.ModRecipes;
import com.ngoducduy.celestialarts.registry.ModSounds;
import com.ngoducduy.celestialarts.screen.AlchemyFurnaceScreenHandler;
import com.ngoducduy.celestialarts.util.SkillFx;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * The refining simulation. Slots 0–5 herbs, 6 flame, 7 output. While a run is going the herbs
 * stay locked in their slots (so both sides can keep matching the recipe) and are consumed only
 * when the run ends.
 * <p>
 * Each tick heat rises with {@code fireLevel × flame power} and cools proportionally, plus a
 * random wobble scaled by the recipe's volatility and the flame's temperament. Distance outside
 * the recipe's tolerance band accrues damage; a hundred points ruin the batch. The share of ticks
 * spent close to the target is the quality score. In the recipe's qi window the injected-qi gauge
 * must be kept above its threshold or the pill does not condense (more damage, less score).
 */
public class AlchemyFurnaceBlockEntity extends BlockEntity implements Inventory, ExtendedScreenHandlerFactory {
	public static final int SLOT_FLAME = 6;
	public static final int SLOT_OUTPUT = 7;
	public static final int SIZE = 8;
	public static final int MAX_FIRE_LEVEL = 4;
	public static final float RUIN_DAMAGE = 100.0F;

	public static final int STATE_IDLE = 0;
	public static final int STATE_REFINING = 1;
	public static final int STATE_DONE = 2;
	public static final int STATE_RUINED = 3;

	// PropertyDelegate indices (all ints; ×10 for one decimal where noted)
	public static final int P_STATE = 0, P_PROGRESS = 1, P_HEAT = 2, P_QI = 3, P_DAMAGE = 4, P_FIRE_LEVEL = 5, P_TARGET = 6, P_TOL = 7,
			P_QI_PHASE = 8, P_QUALITY = 9, P_ERROR = 10, P_FLAME_TIER = 11, P_SCORE = 12, P_TYPE = 13, P_GRADE = 14;
	public static final int PROPERTY_COUNT = 15;

	public static final int ERR_NONE = 0, ERR_NO_RECIPE = 1, ERR_NO_FIRE = 2, ERR_WEAK_FIRE = 3, ERR_FURNACE_GRADE = 4, ERR_OUTPUT_FULL = 5;

	private final DefaultedList<ItemStack> items = DefaultedList.ofSize(SIZE, ItemStack.EMPTY);
	private int state = STATE_IDLE;
	private int tick;
	private int totalTicks;
	private float heat;
	private float qiGauge;
	private float damage;
	private float score;
	private int fireLevel = 2;
	private int error = ERR_NONE;
	private int lastQuality = -1;
	private int messageTicks;
	@Nullable
	private Identifier recipeId;
	@Nullable
	private AlchemyRecipe recipe;
	@Nullable
	private UUID lastOperator;
	private int errorTicks;

	private final PropertyDelegate properties = new PropertyDelegate() {
		@Override
		public int get(int index) {
			return switch (index) {
				case P_STATE -> state;
				case P_PROGRESS -> totalTicks <= 0 ? 0 : Math.round(1000.0F * tick / totalTicks);
				case P_HEAT -> Math.round(heat * 10.0F);
				case P_QI -> Math.round(qiGauge * 10.0F);
				case P_DAMAGE -> Math.round(damage * 10.0F);
				case P_FIRE_LEVEL -> fireLevel;
				case P_TARGET -> recipe == null ? 0 : Math.round(recipe.targetHeat(progress()) * 10.0F);
				case P_TOL -> recipe == null ? 0 : Math.round(recipe.tolerance(progress()) * getFurnaceType().getToleranceMul() * 10.0F);
				case P_QI_PHASE -> recipe != null && state == STATE_REFINING && recipe.qiWindow().contains(progress()) ? Math.round(recipe.qiWindow().min()) : -1;
				case P_QUALITY -> lastQuality;
				case P_ERROR -> error;
				case P_FLAME_TIER -> flame() == null ? 0 : flame().getTier();
				case P_SCORE -> Math.round(currentScore() * 1000.0F);
				case P_TYPE -> getFurnaceType().ordinal();
				case P_GRADE -> getFurnaceGrade().ordinal();
				default -> 0;
			};
		}

		@Override
		public void set(int index, int value) {
			switch (index) {
				case P_STATE -> state = value;
				case P_FIRE_LEVEL -> fireLevel = value;
				case P_ERROR -> error = value;
				case P_QUALITY -> lastQuality = value;
				default -> {
				}
			}
		}

		@Override
		public int size() {
			return PROPERTY_COUNT;
		}
	};

	public AlchemyFurnaceBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.ALCHEMY_FURNACE, pos, state);
	}

	public FurnaceType getFurnaceType() {
		return getCachedState().getBlock() instanceof AlchemyFurnaceBlock b ? b.getFurnaceType() : FurnaceType.STEADY;
	}

	public FurnaceGrade getFurnaceGrade() {
		return getCachedState().getBlock() instanceof AlchemyFurnaceBlock b ? b.getFurnaceGrade() : FurnaceGrade.MORTAL;
	}

	public PropertyDelegate getProperties() {
		return properties;
	}

	public int getState() {
		return state;
	}

	public float getHeat() {
		return heat;
	}

	public float getDamage() {
		return damage;
	}

	public int getFireLevel() {
		return fireLevel;
	}

	public float getQiGauge() {
		return qiGauge;
	}

	@Nullable
	public AlchemyRecipe getRecipe() {
		return recipe;
	}

	public float progress() {
		return totalTicks <= 0 ? 0.0F : MathHelper.clamp((float) tick / totalTicks, 0.0F, 1.0F);
	}

	public float currentScore() {
		return tick <= 0 ? 0.0F : score / tick;
	}

	@Nullable
	public FlameTier flame() {
		return items.get(SLOT_FLAME).getItem() instanceof FlameItem f ? f.getTier() : null;
	}

	// ------------------------------------------------------------------ controls

	/** Button ids from the screen handler. */
	public static final int BTN_LESS = 0, BTN_MORE = 1, BTN_QI = 2, BTN_START = 3, BTN_ABORT = 4;

	public void onButton(ServerPlayerEntity player, int id) {
		lastOperator = player.getUuid();
		switch (id) {
			case BTN_LESS -> setFireLevel(fireLevel - 1);
			case BTN_MORE -> setFireLevel(fireLevel + 1);
			case BTN_QI -> injectQi(player);
			case BTN_START -> start(player);
			case BTN_ABORT -> abort();
			default -> {
			}
		}
		markDirty();
	}

	public void setFireLevel(int level) {
		fireLevel = MathHelper.clamp(level, 0, MAX_FIRE_LEVEL);
	}

	private void injectQi(ServerPlayerEntity player) {
		if (state != STATE_REFINING || recipe == null) return;
		PlayerQi qi = QiHolder.get(player);
		float cost = 8.0F + 4.0F * recipe.grade();
		if (!qi.consumeQi(cost)) return;
		ModPackets.sendSync(player, qi);
		qiGauge = Math.min(100.0F, qiGauge + 25.0F * getFurnaceType().getQiMul());
		if (world instanceof ServerWorld sw) {
			SkillFx.gatherQi(sw, Vec3d.ofCenter(pos, 0.4), 0x7AE8FF, 1.2, 10);
			sw.playSound(null, pos, ModSounds.QI_GATHER, SoundCategory.BLOCKS, 0.6F, 1.3F);
		}
	}

	private int checkStart() {
		if (!items.get(SLOT_OUTPUT).isEmpty()) return ERR_OUTPUT_FULL;
		AlchemyRecipe r = findRecipe();
		if (r == null) return ERR_NO_RECIPE;
		FlameTier flame = flame();
		if (flame == null) return ERR_NO_FIRE;
		if (flame.getTier() < r.fireTier()) return ERR_WEAK_FIRE;
		if (r.grade() > getFurnaceGrade().getMaxPillGrade()) return ERR_FURNACE_GRADE;
		return ERR_NONE;
	}

	/** Recipe matched by the herbs currently in the slots (server: through the recipe manager). */
	@Nullable
	public AlchemyRecipe findRecipe() {
		if (world == null) return null;
		return world.getRecipeManager().getFirstMatch(ModRecipes.ALCHEMY_TYPE, this, world).orElse(null);
	}

	private void start(ServerPlayerEntity player) {
		if (state == STATE_REFINING) return;
		int err = checkStart();
		error = err;
		errorTicks = 80;
		if (err != ERR_NONE) return;
		recipe = findRecipe();
		if (recipe == null) return;
		recipeId = recipe.getId();
		state = STATE_REFINING;
		tick = 0;
		totalTicks = recipe.time();
		heat = Math.max(heat, 5.0F);
		qiGauge = 0.0F;
		damage = 0.0F;
		score = 0.0F;
		lastQuality = -1;
		if (fireLevel == 0) fireLevel = 2;
		if (world instanceof ServerWorld sw) {
			sw.playSound(null, pos, SoundEvents.ITEM_FIRECHARGE_USE, SoundCategory.BLOCKS, 0.8F, 0.9F);
			SkillFx.glowBurst(sw, Vec3d.ofCenter(pos, 0.5), flame() == null ? 0xFF9A3A : flame().getRgb(), 16, 0.6F, 0.1);
		}
		setLit(true);
		sync();
	}

	private void abort() {
		if (state != STATE_REFINING) return;
		state = STATE_IDLE;
		recipe = null;
		recipeId = null;
		tick = 0;
		if (world instanceof ServerWorld sw) sw.playSound(null, pos, SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.BLOCKS, 0.7F, 1.0F);
		setLit(false);
		sync();
	}

	private void setLit(boolean lit) {
		if (world == null) return;
		BlockState s = getCachedState();
		if (s.contains(AlchemyFurnaceBlock.LIT) && s.get(AlchemyFurnaceBlock.LIT) != lit) {
			world.setBlockState(pos, s.with(AlchemyFurnaceBlock.LIT, lit), 3);
		}
	}

	private void sync() {
		markDirty();
		if (world != null) world.updateListeners(pos, getCachedState(), getCachedState(), 3);
	}

	// ------------------------------------------------------------------ simulation

	public static void serverTick(World world, BlockPos pos, BlockState state, AlchemyFurnaceBlockEntity be) {
		be.tickServer((ServerWorld) world);
	}

	private void tickServer(ServerWorld world) {
		if (errorTicks > 0 && --errorTicks == 0) error = ERR_NONE;
		if (messageTicks > 0 && --messageTicks == 0 && (state == STATE_RUINED || state == STATE_DONE)) {
			state = STATE_IDLE;
			sync();
		}
		if (state != STATE_REFINING) {
			// Cool down when idle.
			if (heat > 0.0F) heat = Math.max(0.0F, heat - 0.4F);
			if (qiGauge > 0.0F) qiGauge = Math.max(0.0F, qiGauge - 0.5F);
			return;
		}
		if (recipe == null && recipeId != null) {
			recipe = world.getRecipeManager().get(recipeId).filter(r -> r instanceof AlchemyRecipe).map(r -> (AlchemyRecipe) r).orElse(null);
		}
		FlameTier flame = flame();
		if (recipe == null || flame == null || !recipe.matches(this, world)) {
			// Herbs or fire were taken away (should not happen through the GUI, but hoppers / breaking).
			ruin(world);
			return;
		}
		FurnaceType type = getFurnaceType();
		float progress = progress();
		float target = recipe.targetHeat(progress);
		float tol = recipe.tolerance(progress) * type.getToleranceMul();

		// Heat: fire adds, cooling subtracts, nature wobbles.
		float gain = fireLevel * flame.getPower() * type.getHeatMul();
		float cooling = heat * 0.012F + 0.05F;
		float wobble = (world.random.nextFloat() - 0.5F) * 2.0F * recipe.volatility() * flame.getVolatility() * 0.9F;
		heat = MathHelper.clamp(heat + gain - cooling + wobble, 0.0F, 100.0F);

		// Qi gauge decays; Tụ Linh Lô holds it longer.
		qiGauge = Math.max(0.0F, qiGauge - (type == FurnaceType.SPIRIT ? 0.22F : 0.35F));

		float dev = Math.abs(heat - target);
		float tickScore = MathHelper.clamp(1.0F - dev / tol, 0.0F, 1.0F);
		if (dev > tol) damage += (dev - tol) * 0.025F * type.getDamageMul();
		boolean inQi = recipe.qiWindow().contains(progress);
		if (inQi && qiGauge < recipe.qiWindow().min()) {
			damage += 0.12F * type.getDamageMul();
			tickScore *= 0.5F;
		}
		score += tickScore;
		tick++;

		if (tick % 5 == 0) {
			int rgb = flame.getRgb();
			Vec3d top = Vec3d.ofCenter(pos, 0.55);
			world.spawnParticles(GlowParticleEffect.glow(rgb, 0.25F + heat / 250.0F, 12), top.x, top.y, top.z, 1, 0.15, 0.05, 0.15, 0.01);
			if (dev > tol) world.spawnParticles(ParticleTypes.SMOKE, top.x, top.y, top.z, 2, 0.15, 0.05, 0.15, 0.02);
		}
		if (damage >= RUIN_DAMAGE) {
			ruin(world);
			return;
		}
		if (tick >= totalTicks) finish(world);
	}

	private void ruin(ServerWorld world) {
		if (recipe != null) recipe.consume(this);
		state = STATE_RUINED;
		messageTicks = 100;
		lastQuality = -1;
		recipe = null;
		recipeId = null;
		ItemStack slag = new ItemStack(ModItems.PILL_SLAG, 1);
		if (!insertOutput(slag)) drop(world, slag);
		world.playSound(null, pos, SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.BLOCKS, 0.5F, 1.4F);
		Vec3d c = Vec3d.ofCenter(pos, 0.5);
		world.spawnParticles(ParticleTypes.LARGE_SMOKE, c.x, c.y, c.z, 30, 0.3, 0.3, 0.3, 0.05);
		heat = Math.min(heat, 40.0F);
		setLit(false);
		sync();
	}

	private void finish(ServerWorld world) {
		if (recipe == null) return;
		FlameTier flame = flame();
		FurnaceType type = getFurnaceType();
		float quality = MathHelper.clamp(currentScore() + type.getQualityBonus(), 0.0F, 1.0F);
		float chance = 0.45F + 0.45F * quality - 0.15F * (damage / RUIN_DAMAGE) + getFurnaceGrade().getSuccessBonus();
		if (flame != null) chance += flame.successBonusPerTier() * Math.max(0, flame.getTier() - recipe.fireTier());
		ServerPlayerEntity operator = lastOperator == null ? null : world.getServer().getPlayerManager().getPlayer(lastOperator);
		if (operator != null) chance += 0.02F * QiHolder.get(operator).getRealm().ordinal();
		chance = MathHelper.clamp(chance, 0.05F, 0.98F);
		recipe.consume(this);
		Vec3d c = Vec3d.ofCenter(pos, 0.6);
		if (world.random.nextFloat() >= chance) {
			state = STATE_RUINED;
			messageTicks = 100;
			lastQuality = -1;
			ItemStack slag = new ItemStack(ModItems.PILL_SLAG, 1);
			if (!insertOutput(slag)) drop(world, slag);
			world.playSound(null, pos, SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.BLOCKS, 0.8F, 0.8F);
			world.spawnParticles(ParticleTypes.LARGE_SMOKE, c.x, c.y, c.z, 20, 0.3, 0.3, 0.3, 0.04);
		} else {
			PillQuality q = PillQuality.fromScore(quality);
			ItemStack out = recipe.craft(this, world.getRegistryManager());
			int count = out.getCount() + (recipe.grade() <= 2 ? 1 : 0) + (q == PillQuality.SUPREME ? 1 : 0);
			out.setCount(count);
			PillItem.withQuality(out, q);
			lastQuality = q.ordinal();
			state = STATE_DONE;
			messageTicks = 200;
			if (!insertOutput(out)) drop(world, out);
			world.playSound(null, pos, ModSounds.BREAKTHROUGH, SoundCategory.BLOCKS, 0.7F, 1.4F);
			SkillFx.glowBurst(world, c, 0xFFE9A8, 28, 0.8F, 0.18);
			SkillFx.runes(world, c, 12, 0.8);
			if (operator != null) {
				PlayerQi qi = QiHolder.get(operator);
				qi.addExp(recipe.grade() * recipe.grade() * 20);
				qi.markDirty();
				ModPackets.sendSync(operator, qi);
				operator.sendMessage(Text.translatable("furnace.celestialarts.state.done", q.getName()), true);
			}
		}
		recipe = null;
		recipeId = null;
		setLit(false);
		sync();
	}

	private boolean insertOutput(ItemStack stack) {
		ItemStack cur = items.get(SLOT_OUTPUT);
		if (cur.isEmpty()) {
			items.set(SLOT_OUTPUT, stack);
			return true;
		}
		if (ItemStack.canCombine(cur, stack) && cur.getCount() + stack.getCount() <= cur.getMaxCount()) {
			cur.increment(stack.getCount());
			return true;
		}
		return false;
	}

	private void drop(ServerWorld world, ItemStack stack) {
		ItemEntity e = new ItemEntity(world, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, stack);
		world.spawnEntity(e);
	}

	// ------------------------------------------------------------------ inventory

	@Override
	public int size() {
		return SIZE;
	}

	@Override
	public boolean isEmpty() {
		for (ItemStack s : items) if (!s.isEmpty()) return false;
		return true;
	}

	@Override
	public ItemStack getStack(int slot) {
		return items.get(slot);
	}

	@Override
	public ItemStack removeStack(int slot, int amount) {
		ItemStack r = Inventories.splitStack(items, slot, amount);
		if (!r.isEmpty()) markDirty();
		return r;
	}

	@Override
	public ItemStack removeStack(int slot) {
		return Inventories.removeStack(items, slot);
	}

	@Override
	public void setStack(int slot, ItemStack stack) {
		items.set(slot, stack);
		if (stack.getCount() > getMaxCountPerStack()) stack.setCount(getMaxCountPerStack());
		markDirty();
	}

	@Override
	public boolean canPlayerUse(PlayerEntity player) {
		return Inventory.canPlayerUse(this, player);
	}

	@Override
	public void clear() {
		items.clear();
	}

	public DefaultedList<ItemStack> getItems() {
		return items;
	}

	// ------------------------------------------------------------------ persistence / sync

	@Override
	protected void writeNbt(NbtCompound nbt) {
		super.writeNbt(nbt);
		Inventories.writeNbt(nbt, items);
		nbt.putInt("State", state);
		nbt.putInt("Tick", tick);
		nbt.putInt("Total", totalTicks);
		nbt.putFloat("Heat", heat);
		nbt.putFloat("QiGauge", qiGauge);
		nbt.putFloat("Damage", damage);
		nbt.putFloat("Score", score);
		nbt.putInt("FireLevel", fireLevel);
		nbt.putInt("LastQuality", lastQuality);
		if (recipeId != null) nbt.putString("Recipe", recipeId.toString());
		if (lastOperator != null) nbt.putUuid("Operator", lastOperator);
	}

	@Override
	public void readNbt(NbtCompound nbt) {
		super.readNbt(nbt);
		items.clear();
		Inventories.readNbt(nbt, items);
		state = nbt.getInt("State");
		tick = nbt.getInt("Tick");
		totalTicks = nbt.getInt("Total");
		heat = nbt.getFloat("Heat");
		qiGauge = nbt.getFloat("QiGauge");
		damage = nbt.getFloat("Damage");
		score = nbt.getFloat("Score");
		fireLevel = nbt.contains("FireLevel") ? nbt.getInt("FireLevel") : 2;
		lastQuality = nbt.contains("LastQuality") ? nbt.getInt("LastQuality") : -1;
		recipeId = nbt.contains("Recipe") ? new Identifier(nbt.getString("Recipe")) : null;
		recipe = null;
		lastOperator = nbt.containsUuid("Operator") ? nbt.getUuid("Operator") : null;
	}

	@Override
	public NbtCompound toInitialChunkDataNbt() {
		return createNbt();
	}

	@Nullable
	@Override
	public net.minecraft.network.packet.Packet<net.minecraft.network.listener.ClientPlayPacketListener> toUpdatePacket() {
		return net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket.create(this);
	}

	// ------------------------------------------------------------------ screen

	@Override
	public void writeScreenOpeningData(ServerPlayerEntity player, PacketByteBuf buf) {
		buf.writeBlockPos(pos);
	}

	@Override
	public Text getDisplayName() {
		return Text.translatable("container.celestialarts.alchemy_furnace");
	}

	@Nullable
	@Override
	public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
		return new AlchemyFurnaceScreenHandler(syncId, playerInventory, this, properties, pos);
	}
}
