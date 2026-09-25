package com.ngoducduy.celestialarts.entity;

import com.ngoducduy.celestialarts.network.FxData;
import com.ngoducduy.celestialarts.network.FxType;
import com.ngoducduy.celestialarts.network.ModPackets;
import com.ngoducduy.celestialarts.registry.GlowParticleEffect;
import com.ngoducduy.celestialarts.registry.ModDamageTypes;
import com.ngoducduy.celestialarts.registry.ModParticles;
import com.ngoducduy.celestialarts.registry.ModSounds;
import com.ngoducduy.celestialarts.util.EntityUtil;
import com.ngoducduy.celestialarts.util.SkillFx;
import com.ngoducduy.celestialarts.util.Targeting;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * Phong Long – a wind dragon coiled into a travelling tornado. It hugs the ground, climbs
 * single steps, drags enemies into its funnel, lifts them and finally bursts apart in a gale.
 */
public class WindDragonEntity extends SkillProjectileEntity {
	public static final double FUNNEL_RADIUS = 2.4;
	public static final float FUNNEL_HEIGHT = 4.2F;
	private static final float TICK_DAMAGE = 2.5F;
	private static final float BURST_DAMAGE = 7.0F;

	public WindDragonEntity(EntityType<? extends WindDragonEntity> type, World world) {
		super(type, world);
		this.damage = TICK_DAMAGE;
		this.maxAge = 70;
		this.pierce = Integer.MAX_VALUE / 2;
	}

	@Override
	public void tick() {
		super.tick();
		if (this.isRemoved()) return;
		World world = this.getWorld();
		if (world.isClient) return;

		// Follow the terrain: sink to the ground when floating, climb when the floor rises.
		Vec3d probe = this.getPos().add(0, 1.0, 0);
		Vec3d snapped = Targeting.snapToGround(world, probe, 5);
		double dy = snapped.y + 0.1 - this.getY();
		if (snapped != probe && Math.abs(dy) > 0.05) {
			this.setPosition(this.getX(), this.getY() + Math.signum(dy) * Math.min(Math.abs(dy), 0.35), this.getZ());
		}

		if (!(world instanceof ServerWorld sw)) return;
		Entity owner = this.getOwner();
		Vec3d center = this.getPos().add(0, FUNNEL_HEIGHT * 0.45, 0);
		for (LivingEntity target : EntityUtil.inCylinder(sw, owner == null ? this : owner, this.getPos(), FUNNEL_RADIUS + 1.0, FUNNEL_HEIGHT)) {
			// Drag toward the eye, then lift.
			EntityUtil.pull(target, center, 0.28);
			Vec3d v = target.getVelocity();
			if (target.getY() < this.getY() + FUNNEL_HEIGHT - 1.0) {
				target.setVelocity(v.x, Math.min(v.y + 0.14, 0.45), v.z);
			} else {
				target.setVelocity(v.x, Math.min(v.y, 0.05), v.z);
			}
			target.velocityModified = true;
			target.fallDistance = 0.0F;
			if (this.age % 8 == 0) {
				target.damage(ModDamageTypes.projectile(sw, ModDamageTypes.WIND, this, owner), damage);
				SkillFx.windGust(sw, target.getBoundingBox().getCenter(), new Vec3d(0, 1, 0), 4, 0.2);
			}
		}
		if (this.age % 10 == 0) {
			sw.playSound(null, this.getBlockPos(), ModSounds.WIND_SLASH, SoundCategory.PLAYERS, 0.9F, 0.6F + random.nextFloat() * 0.1F);
		}
	}

	@Override
	protected boolean hitTarget(Entity target) {
		// Damage is applied by the funnel every few ticks; contact itself does nothing.
		return false;
	}

	@Override
	protected void onBlockHit(BlockHitResult hit) {
		// Climb a single block; anything taller stops the dragon.
		BlockPos above = hit.getBlockPos().up();
		if (this.getWorld().getBlockState(above).getCollisionShape(this.getWorld(), above).isEmpty()
				&& this.getWorld().getBlockState(above.up()).getCollisionShape(this.getWorld(), above.up()).isEmpty()) {
			this.setPosition(this.getX(), above.getY() + 0.05, this.getZ());
			return;
		}
		super.onBlockHit(hit);
	}

	@Override
	protected void onDissipate(Vec3d pos) {
		burst();
	}

	@Override
	protected void onExpire() {
		burst();
	}

	private boolean burst;

	/** Phong Bạo: the dragon unwinds in a gale that hurls everything outward and upward. */
	private void burst() {
		if (burst || !(this.getWorld() instanceof ServerWorld sw)) return;
		burst = true;
		Entity owner = this.getOwner();
		Vec3d c = this.getPos();
		for (LivingEntity target : EntityUtil.inCylinder(sw, owner == null ? this : owner, c, FUNNEL_RADIUS + 2.0, FUNNEL_HEIGHT + 1)) {
			if (target.damage(ModDamageTypes.projectile(sw, ModDamageTypes.WIND, this, owner), BURST_DAMAGE)) {
				EntityUtil.knockback(target, c, 1.0, 0.7);
			}
		}
		ModPackets.sendFx(sw, FxData.at(FxType.SHOCKWAVE_RING, c.add(0, 0.1, 0), 0xB8FFD9, (float) FUNNEL_RADIUS * 2.2F, 12));
		ModPackets.sendFx(sw, FxData.at(FxType.ENERGY_BURST, c.add(0, FUNNEL_HEIGHT * 0.5, 0), 0xE8FFF3, 1.8F, 8));
		SkillFx.ring(sw, ModParticles.WIND_STREAK, c.add(0, 0.5, 0), 1.0, 40, 0.6, 0.2);
		SkillFx.shell(sw, ModParticles.WIND_STREAK, c.add(0, FUNNEL_HEIGHT * 0.5, 0), 1.5, 30, 0.45);
		sw.playSound(null, this.getBlockPos(), ModSounds.WIND_SLASH, SoundCategory.PLAYERS, 1.6F, 0.5F);
	}

	@Override
	protected void clientTick() {
		World world = this.getWorld();
		Vec3d p = this.getPos();
		for (int i = 0; i < 3; i++) {
			double h = random.nextDouble() * FUNNEL_HEIGHT;
			double r = 0.4 + h / FUNNEL_HEIGHT * FUNNEL_RADIUS;
			double a = random.nextDouble() * Math.PI * 2;
			Vec3d q = p.add(Math.cos(a) * r, h, Math.sin(a) * r);
			Vec3d v = new Vec3d(-Math.sin(a), 0.08, Math.cos(a)).multiply(0.3);
			world.addParticle(ModParticles.WIND_STREAK, q.x, q.y, q.z, v.x, v.y, v.z);
		}
		if (random.nextInt(2) == 0) {
			Vec3d q = p.add(random.nextGaussian() * 0.4, 0.1, random.nextGaussian() * 0.4);
			world.addParticle(GlowParticleEffect.glow(0xB8FFD9, 0.5F, 10), q.x, q.y, q.z, 0, 0.15, 0);
		}
		if (random.nextInt(3) == 0) {
			Vec3d q = p.add(random.nextGaussian() * 1.2, 0.05, random.nextGaussian() * 1.2);
			world.addParticle(ModParticles.ROCK_DEBRIS, q.x, q.y, q.z, 0, 0.25, 0);
		}
	}
}
