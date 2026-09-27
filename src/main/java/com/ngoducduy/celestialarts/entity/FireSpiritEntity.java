package com.ngoducduy.celestialarts.entity;

import com.ngoducduy.celestialarts.alchemy.FlameTier;
import com.ngoducduy.celestialarts.cultivation.FireVein;
import com.ngoducduy.celestialarts.item.FlameCapture;
import com.ngoducduy.celestialarts.registry.ModItems;
import com.ngoducduy.celestialarts.registry.ModSounds;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityData;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Difficulty;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * Hỏa Linh – a wandering fire spirit born of a Hỏa Nguyên ({@link FireVein}). Its {@code
 * FlameTier} is rolled from the vein it spawned near (Địa Hỏa in badlands, Linh Hỏa where spirit
 * qi is rich, Quỷ Hỏa in eerie places, Nghiệp Hỏa in the Soul Sand Valley, Tam Muội Chân Hỏa
 * around basalt/crimson forests, Thiên Hỏa in the End, Dị Hỏa vanishingly rarely anywhere).
 *
 * <p>It fights back (a burning claw, igniting whatever it touches), but the only way to actually
 * <i>take</i> its fire home is to beat it down to a quarter health – "khống chế" it into
 * submission – and then bind it with a Bình Hỏa Phách; see {@link FlameCapture}. Kill it outright
 * and the flame is lost with it.</p>
 */
public class FireSpiritEntity extends HostileEntity {
	private static final TrackedData<Integer> TIER = DataTracker.registerData(FireSpiritEntity.class, TrackedDataHandlerRegistry.INTEGER);
	/** How weak (as a fraction of max health) the spirit must be before it can be bound. */
	public static final float WEAKEN_THRESHOLD = 0.25F;

	private int enrageTicks;

	public FireSpiritEntity(EntityType<? extends FireSpiritEntity> type, World world) {
		super(type, world);
		this.experiencePoints = 10;
		initCustomGoals();
	}

	public static DefaultAttributeContainer.Builder createFireSpiritAttributes() {
		return HostileEntity.createHostileAttributes()
				.add(EntityAttributes.GENERIC_MAX_HEALTH, 32.0)
				.add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.26)
				.add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 6.0)
				.add(EntityAttributes.GENERIC_FOLLOW_RANGE, 24.0)
				.add(EntityAttributes.GENERIC_ARMOR, 2.0);
	}

	private void initCustomGoals() {
		this.goalSelector.add(1, new SwimGoal(this));
		this.goalSelector.add(2, new MeleeAttackGoal(this, 1.05, true));
		this.goalSelector.add(3, new WanderAroundFarGoal(this, 0.85));
		this.goalSelector.add(4, new LookAtEntityGoal(this, PlayerEntity.class, 10.0F));
		this.goalSelector.add(5, new LookAroundGoal(this));
		this.targetSelector.add(1, new RevengeGoal(this));
		this.targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
	}

	@Override
	protected void initDataTracker() {
		super.initDataTracker();
		this.dataTracker.startTracking(TIER, FlameTier.EARTH.ordinal());
	}

	public FlameTier getFlameTier() {
		FlameTier[] values = FlameTier.values();
		int ordinal = MathHelper.clamp(this.dataTracker.get(TIER), 0, values.length - 1);
		return values[ordinal];
	}

	/** Sets the tier, rescales attributes to match it and fully heals – for a freshly spawned spirit. */
	public void setFlameTier(FlameTier tier) {
		applyTierAttributes(tier);
		this.setHealth(this.getMaxHealth());
	}

	/** Same as {@link #setFlameTier}, but leaves current health alone (used when restoring from NBT, so a
	 * spirit saved mid-fight doesn't come back at full health just because the chunk reloaded). */
	private void applyTierAttributes(FlameTier tier) {
		this.dataTracker.set(TIER, tier.ordinal());
		int t = tier.getTier();
		setAttr(EntityAttributes.GENERIC_MAX_HEALTH, 18.0 + t * 7.0);
		setAttr(EntityAttributes.GENERIC_ATTACK_DAMAGE, 3.0 + t * 1.4);
		setAttr(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.22 + t * 0.008);
	}

	private void setAttr(EntityAttribute attribute, double value) {
		EntityAttributeInstance instance = this.getAttributeInstance(attribute);
		if (instance != null) instance.setBaseValue(value);
	}

	@Override
	public EntityData initialize(ServerWorldAccess world, LocalDifficulty difficulty, SpawnReason spawnReason, @Nullable EntityData entityData, @Nullable NbtCompound entityNbt) {
		entityData = super.initialize(world, difficulty, spawnReason, entityData, entityNbt);
		FlameTier tier = FireVein.tierNear(world.toServerWorld(), this.getBlockPos());
		setFlameTier(tier != null ? tier : FlameTier.EARTH);
		return entityData;
	}

	public static boolean canSpawn(EntityType<FireSpiritEntity> type, ServerWorldAccess world, SpawnReason reason, BlockPos pos, Random random) {
		ServerWorld serverWorld = world.toServerWorld();
		if (serverWorld == null || serverWorld.getDifficulty() == Difficulty.PEACEFUL) return false;
		BlockPos below = pos.down();
		if (!world.getBlockState(below).isOpaqueFullCube(world, below)) return false;
		return FireVein.tierNear(serverWorld, pos) != null;
	}

	@Override
	public boolean isFireImmune() {
		return true;
	}

	public boolean isWeakened() {
		return this.getHealth() <= this.getMaxHealth() * WEAKEN_THRESHOLD;
	}

	public boolean isEnraged() {
		return enrageTicks > 0;
	}

	public void enrage(int ticks) {
		this.enrageTicks = Math.max(this.enrageTicks, ticks);
	}

	@Override
	public void tick() {
		super.tick();
		if (!getWorld().isClient && enrageTicks > 0) enrageTicks--;
	}

	@Override
	public boolean tryAttack(Entity target) {
		boolean success = super.tryAttack(target);
		if (success) {
			target.setFireTicks(Math.max(target.getFireTicks(), 60 + getFlameTier().getTier() * 20));
		}
		return success;
	}

	/** Right-click hook: a Bình Hỏa Phách attempts to bind the spirit once it is weakened enough. */
	@Override
	protected ActionResult interactMob(PlayerEntity player, Hand hand) {
		ItemStack stack = player.getStackInHand(hand);
		if (!stack.isOf(ModItems.FLAME_CAPTURE_BOTTLE)) return super.interactMob(player, hand);
		if (getWorld() instanceof ServerWorld serverWorld) {
			return FlameCapture.attempt(serverWorld, player, this, stack, hand);
		}
		return ActionResult.SUCCESS;
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return ModSounds.FIRE_WHOOSH;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return ModSounds.HIT_FIRE;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return ModSounds.FIRE_EXPLOSION;
	}

	@Override
	public void writeCustomDataToNbt(NbtCompound nbt) {
		super.writeCustomDataToNbt(nbt);
		nbt.putString("FlameTier", getFlameTier().name());
	}

	@Override
	public void readCustomDataFromNbt(NbtCompound nbt) {
		super.readCustomDataFromNbt(nbt);
		if (nbt.contains("FlameTier")) {
			try {
				applyTierAttributes(FlameTier.valueOf(nbt.getString("FlameTier")));
			} catch (IllegalArgumentException ignored) {
				// Unknown/legacy value: keep whatever the tracked data already holds.
			}
		}
	}
}
