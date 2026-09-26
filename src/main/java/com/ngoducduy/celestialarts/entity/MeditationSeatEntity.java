package com.ngoducduy.celestialarts.entity;

import com.ngoducduy.celestialarts.cultivation.Breakthrough;
import com.ngoducduy.celestialarts.cultivation.Meditation;
import com.ngoducduy.celestialarts.cultivation.QiHolder;
import com.ngoducduy.celestialarts.registry.ModEntities;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * Bồ đoàn – the meditation cushion. An invisible-to-physics seat the cultivator rides while in
 * trance: riding it puts the player in the sitting pose, keeps them from walking off and ends
 * the trance automatically when they dismount (sneak). The cushion itself is rendered as a small
 * rune-etched mat that glows in the colour of the cultivator's spirit root.
 */
public class MeditationSeatEntity extends Entity {
	private static final TrackedData<Integer> OWNER_ID = DataTracker.registerData(MeditationSeatEntity.class, TrackedDataHandlerRegistry.INTEGER);
	private static final TrackedData<Integer> COLOR = DataTracker.registerData(MeditationSeatEntity.class, TrackedDataHandlerRegistry.INTEGER);

	/** How high above the ground the cushion sits the rider (player height offset is −0.35). */
	public static final double SEAT_OFFSET = 0.10;

	private int emptyTicks;

	public MeditationSeatEntity(EntityType<? extends MeditationSeatEntity> type, World world) {
		super(type, world);
		this.noClip = true;
		this.setNoGravity(true);
	}

	@Override
	protected void initDataTracker() {
		this.dataTracker.startTracking(OWNER_ID, -1);
		this.dataTracker.startTracking(COLOR, 0xFFE9A8);
	}

	public int getOwnerId() {
		return this.dataTracker.get(OWNER_ID);
	}

	public int getColor() {
		return this.dataTracker.get(COLOR);
	}

	@Nullable
	public PlayerEntity getRider() {
		Entity e = this.getFirstPassenger();
		return e instanceof PlayerEntity p ? p : null;
	}

	@Override
	public void tick() {
		super.tick();
		this.setVelocity(Vec3d.ZERO);
		if (this.getWorld().isClient) return;

		PlayerEntity rider = getRider();
		if (rider == null || !rider.isAlive() || rider.getId() != getOwnerId()) {
			// Give the mount packet a moment to arrive; then the seat is simply empty.
			if (++emptyTicks > 5) {
				if (rider != null) rider.stopRiding();
				this.discard();
				Entity owner = this.getWorld().getEntityById(getOwnerId());
				if (owner instanceof ServerPlayerEntity player) Meditation.onSeatLost(player);
			}
			return;
		}
		emptyTicks = 0;
		// A blow breaks the trance: hurtTime is set to 10 on the tick the damage lands. Not while a
		// breakthrough is running - the tribulation's own lightning must not unseat the cultivator,
		// and a Dao heart that has already committed does not flinch from a stray arrow either.
		if (rider.hurtTime == 9 && rider instanceof ServerPlayerEntity player && !Breakthrough.isBreakingThrough(QiHolder.get(player))) {
			Meditation.stop(player, Meditation.StopReason.HURT);
		}
	}

	@Override
	protected boolean canAddPassenger(Entity passenger) {
		return this.getPassengerList().isEmpty() && passenger instanceof PlayerEntity;
	}

	@Override
	public double getMountedHeightOffset() {
		return SEAT_OFFSET;
	}

	@Override
	protected void updatePassengerPosition(Entity passenger, Entity.PositionUpdater positionUpdater) {
		if (!this.hasPassenger(passenger)) return;
		positionUpdater.accept(passenger, this.getX(), this.getY() + getMountedHeightOffset() + passenger.getHeightOffset(), this.getZ());
		passenger.fallDistance = 0.0F;
	}

	@Override
	public Vec3d updatePassengerForDismount(LivingEntity passenger) {
		return new Vec3d(this.getX(), this.getY(), this.getZ());
	}

	@Override
	public boolean isCollidable() {
		return false;
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	public boolean canHit() {
		return false;
	}

	@Override
	public boolean isFireImmune() {
		return true;
	}

	@Override
	protected MoveEffect getMoveEffect() {
		return MoveEffect.NONE;
	}

	@Override
	public boolean shouldRender(double distance) {
		return distance < 96 * 96;
	}

	@Override
	protected void readCustomDataFromNbt(NbtCompound nbt) {
		this.discard();
	}

	@Override
	protected void writeCustomDataToNbt(NbtCompound nbt) {
	}

	/** Spawns a cushion under the player and seats them. Returns null when riding failed. */
	@Nullable
	public static MeditationSeatEntity seat(ServerWorld world, ServerPlayerEntity player, int color) {
		MeditationSeatEntity seat = new MeditationSeatEntity(ModEntities.MEDITATION_SEAT, world);
		seat.setPosition(player.getX(), player.getY(), player.getZ());
		seat.setYaw(player.getYaw());
		seat.dataTracker.set(OWNER_ID, player.getId());
		seat.dataTracker.set(COLOR, color);
		if (!world.spawnEntity(seat)) return null;
		if (!player.startRiding(seat, true)) {
			seat.discard();
			return null;
		}
		return seat;
	}
}
