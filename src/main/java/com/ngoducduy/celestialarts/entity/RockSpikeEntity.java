package com.ngoducduy.celestialarts.entity;

import com.ngoducduy.celestialarts.registry.ModDamageTypes;
import com.ngoducduy.celestialarts.registry.ModSounds;
import com.ngoducduy.celestialarts.util.EntityUtil;
import com.ngoducduy.celestialarts.util.SkillFx;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * Địa Thứ – a jagged stone spike that erupts from the ground, launches anything above
 * it into the air, stays for a moment and sinks back.
 */
public class RockSpikeEntity extends Entity {
	private static final TrackedData<Float> HEIGHT = DataTracker.registerData(RockSpikeEntity.class, TrackedDataHandlerRegistry.FLOAT);
	private static final TrackedData<Integer> OWNER_ID = DataTracker.registerData(RockSpikeEntity.class, TrackedDataHandlerRegistry.INTEGER);

	private static final int RISE_TICKS = 6;
	private static final int HOLD_TICKS = 26;
	private static final int SINK_TICKS = 10;

	private LivingEntity owner;
	private float damage = 8.0f;
	private boolean hitDone;
	private final float spinSeed;

	public RockSpikeEntity(EntityType<? extends RockSpikeEntity> type, World world) {
		super(type, world);
		this.noClip = true;
		this.setNoGravity(true);
		this.spinSeed = world.random.nextFloat() * 360f;
	}

	public void init(LivingEntity owner, float height, float damage) {
		this.owner = owner;
		this.damage = damage;
		this.dataTracker.set(OWNER_ID, owner.getId());
		this.dataTracker.set(HEIGHT, height);
	}

	@Override
	protected void initDataTracker() {
		this.dataTracker.startTracking(HEIGHT, 2.2f);
		this.dataTracker.startTracking(OWNER_ID, -1);
	}

	public float getSpikeHeight() {
		return this.dataTracker.get(HEIGHT);
	}

	public float getSpinSeed() {
		return spinSeed;
	}

	/** 0 = fully underground, 1 = fully risen. */
	public float getRise(float tickDelta) {
		float a = this.age + tickDelta;
		if (a < RISE_TICKS) return easeOut(a / RISE_TICKS);
		if (a < RISE_TICKS + HOLD_TICKS) return 1f;
		return 1f - MathHelper.clamp((a - RISE_TICKS - HOLD_TICKS) / SINK_TICKS, 0f, 1f);
	}

	private static float easeOut(float t) {
		return 1f - (1f - t) * (1f - t) * (1f - t);
	}

	@Override
	public void tick() {
		super.tick();
		if (this.getWorld().isClient) return;

		if (owner == null) {
			Entity e = this.getWorld().getEntityById(this.dataTracker.get(OWNER_ID));
			if (e instanceof LivingEntity l) owner = l;
		}

		if (this.age == 0 && this.getWorld() instanceof ServerWorld sw) {
			SkillFx.rockDebris(sw, this.getPos(), 14, 0.5);
			sw.playSound(null, this.getBlockPos(), ModSounds.EARTH_QUAKE, SoundCategory.PLAYERS, 0.9f, 1.1f + random.nextFloat() * 0.3f);
		}

		if (!hitDone && this.age >= 2) {
			hitDone = true;
			float h = getSpikeHeight();
			Box box = new Box(this.getX() - 0.8, this.getY() - 0.2, this.getZ() - 0.8, this.getX() + 0.8, this.getY() + h, this.getZ() + 0.8);
			Entity source = owner == null ? this : owner;
			for (LivingEntity target : this.getWorld().getEntitiesByClass(LivingEntity.class, box, EntityUtil.targetPredicate(source))) {
				if (target.damage(ModDamageTypes.projectile(this.getWorld(), ModDamageTypes.EARTH, this, owner), damage)) {
					target.addVelocity(0, 0.7 + h * 0.08, 0);
					target.velocityModified = true;
				}
			}
		}

		if (this.age > RISE_TICKS + HOLD_TICKS + SINK_TICKS) {
			this.discard();
		}
	}

	@Override
	public boolean shouldRender(double distance) {
		return distance < 128 * 128;
	}

	@Override
	public boolean isAttackable() {
		return false;
	}

	@Override
	public boolean canHit() {
		return false;
	}

	@Override
	protected void readCustomDataFromNbt(NbtCompound nbt) {
		this.discard();
	}

	@Override
	protected void writeCustomDataToNbt(NbtCompound nbt) {
	}

	public static void spawn(ServerWorld world, LivingEntity owner, Vec3d pos, float height, float damage, EntityType<RockSpikeEntity> type) {
		RockSpikeEntity spike = new RockSpikeEntity(type, world);
		spike.init(owner, height, damage);
		spike.refreshPositionAndAngles(pos.x, pos.y, pos.z, world.random.nextFloat() * 360f, 0);
		world.spawnEntity(spike);
	}
}
