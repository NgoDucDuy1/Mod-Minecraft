package com.ngoducduy.celestialarts.cultivation;

import com.ngoducduy.celestialarts.network.FxData;
import com.ngoducduy.celestialarts.network.FxType;
import com.ngoducduy.celestialarts.network.ModPackets;
import com.ngoducduy.celestialarts.registry.ModParticles;
import com.ngoducduy.celestialarts.registry.ModSounds;
import com.ngoducduy.celestialarts.util.SkillFx;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.Vec3d;

import java.util.UUID;

/**
 * Passive benefits of the cultivator's body ("thể phách"). Each realm strengthens the body:
 *
 * <pre>
 * realm level   1        2         3            4             5                6
 *               Luyện Khí Trúc Cơ  Kim Đan      Nguyên Anh    Hóa Thần         Độ Kiếp
 * max health    +0       +4        +8           +12           +16              +20
 * attack        +0       +1        +2           +3            +4               +5
 * speed         +0%      +6%       +12%         +18%          +24%             +30%
 * toughness     +0       +1        +2           +3            +4               +5
 * knockback res +0       +0.08     +0.16        +0.24         +0.32            +0.40
 * abilities     –        air jump  no fall dmg  hover, no drown  fire immune   2 air jumps
 * </pre>
 *
 * Attribute modifiers use fixed UUIDs so they can be replaced idempotently and never stack.
 */
public final class RealmPassives {
	private static final UUID HEALTH_ID = UUID.fromString("2f3e5a10-6d2c-4d2f-9b0a-1c9e8e1c0001");
	private static final UUID ATTACK_ID = UUID.fromString("2f3e5a10-6d2c-4d2f-9b0a-1c9e8e1c0002");
	private static final UUID SPEED_ID = UUID.fromString("2f3e5a10-6d2c-4d2f-9b0a-1c9e8e1c0003");
	private static final UUID TOUGHNESS_ID = UUID.fromString("2f3e5a10-6d2c-4d2f-9b0a-1c9e8e1c0004");
	private static final UUID KNOCKBACK_ID = UUID.fromString("2f3e5a10-6d2c-4d2f-9b0a-1c9e8e1c0005");

	public static final float AIR_JUMP_QI = 6.0F;
	public static final double AIR_JUMP_VELOCITY = 0.62;
	public static final double HOVER_FALL_SPEED = -0.07;

	private RealmPassives() {
	}

	// --------------------------------------------------------------- rules

	public static int bonusHealth(Realm realm) {
		return (realm.getLevel() - 1) * 4;
	}

	public static float bonusAttack(Realm realm) {
		return realm.getLevel() - 1;
	}

	public static float bonusSpeed(Realm realm) {
		return (realm.getLevel() - 1) * 0.06F;
	}

	public static float bonusToughness(Realm realm) {
		return realm.getLevel() - 1;
	}

	public static float bonusKnockbackResistance(Realm realm) {
		return (realm.getLevel() - 1) * 0.08F;
	}

	/** Every skill hit is amplified by the caster's cultivation: +6% per realm above Luyện Khí. */
	public static float skillDamageMultiplier(Realm realm) {
		return 1.0F + (realm.getLevel() - 1) * 0.06F;
	}

	/** Extra jumps available while airborne (Lăng Không Bộ). */
	public static int airJumps(Realm realm) {
		int level = realm.getLevel();
		if (level >= 6) return 2;
		if (level >= 2) return 1;
		return 0;
	}

	public static boolean immuneToFall(Realm realm) {
		return realm.getLevel() >= 3;
	}

	/** Holding sneak in the air lets the cultivator drift down slowly (Ngự Không). */
	public static boolean canHover(Realm realm) {
		return realm.getLevel() >= 4;
	}

	public static boolean immuneToDrowning(Realm realm) {
		return realm.getLevel() >= 4;
	}

	public static boolean immuneToFire(Realm realm) {
		return realm.getLevel() >= 5;
	}

	/** Returns true when the body simply ignores this kind of harm. */
	public static boolean ignoresDamage(PlayerEntity player, DamageSource source) {
		Realm realm = QiHolder.get(player).getRealm();
		if (source.isIn(DamageTypeTags.IS_FALL)) return immuneToFall(realm);
		if (source.isIn(DamageTypeTags.IS_DROWNING)) return immuneToDrowning(realm);
		if (source.isIn(DamageTypeTags.IS_FIRE)) return immuneToFire(realm);
		return false;
	}

	// --------------------------------------------------------------- server

	/** Re-applies the attribute modifiers for the player's current realm. Cheap; safe to call every second. */
	public static void apply(ServerPlayerEntity player) {
		Realm realm = QiHolder.get(player).getRealm();
		boolean changed = false;
		changed |= set(player, EntityAttributes.GENERIC_MAX_HEALTH, HEALTH_ID, "celestialarts.realm_health", bonusHealth(realm), EntityAttributeModifier.Operation.ADDITION);
		changed |= set(player, EntityAttributes.GENERIC_ATTACK_DAMAGE, ATTACK_ID, "celestialarts.realm_attack", bonusAttack(realm), EntityAttributeModifier.Operation.ADDITION);
		changed |= set(player, EntityAttributes.GENERIC_MOVEMENT_SPEED, SPEED_ID, "celestialarts.realm_speed", bonusSpeed(realm), EntityAttributeModifier.Operation.MULTIPLY_TOTAL);
		changed |= set(player, EntityAttributes.GENERIC_ARMOR_TOUGHNESS, TOUGHNESS_ID, "celestialarts.realm_toughness", bonusToughness(realm), EntityAttributeModifier.Operation.ADDITION);
		changed |= set(player, EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, KNOCKBACK_ID, "celestialarts.realm_knockback", bonusKnockbackResistance(realm), EntityAttributeModifier.Operation.ADDITION);
		if (changed && player.getHealth() > player.getMaxHealth()) {
			player.setHealth(player.getMaxHealth());
		}
	}

	private static boolean set(ServerPlayerEntity player, EntityAttribute attribute, UUID id, String name, double value, EntityAttributeModifier.Operation op) {
		EntityAttributeInstance instance = player.getAttributeInstance(attribute);
		if (instance == null) return false;
		EntityAttributeModifier existing = instance.getModifier(id);
		if (existing != null && Math.abs(existing.getValue() - value) < 1.0E-6) return false;
		if (existing != null) instance.removeModifier(id);
		if (value != 0.0) instance.addPersistentModifier(new EntityAttributeModifier(id, name, value, op));
		return true;
	}

	/** Server side of an air jump: validates the realm, charges qi and shows the wind step. */
	public static boolean airJump(ServerPlayerEntity player) {
		PlayerQi qi = QiHolder.get(player);
		if (airJumps(qi.getRealm()) <= 0) return false;
		if (!player.isCreative() && !qi.hasQi(AIR_JUMP_QI)) return false;
		if (!player.isCreative()) qi.consumeQi(AIR_JUMP_QI);
		player.fallDistance = 0.0F;
		ServerWorld world = player.getServerWorld();
		Vec3d feet = player.getPos();
		ModPackets.sendFx(player, FxData.at(FxType.SHOCKWAVE_RING, feet.add(0, 0.05, 0), 0xBFF3FF, 0.9F, 10));
		SkillFx.windGust(world, feet, new Vec3d(0, -1, 0), 10, 0.25);
		SkillFx.glowBurst(world, feet.add(0, 0.2, 0), 0xDFFBFF, 6, 0.5F, 0.08);
		world.playSound(null, player.getBlockPos(), ModSounds.LIGHTNING_STEP, SoundCategory.PLAYERS, 0.7F, 1.3F + world.random.nextFloat() * 0.2F);
		qi.markDirty();
		return true;
	}

	/** Called every tick for online players: aura while meditating / hovering. */
	public static void tick(ServerPlayerEntity player, PlayerQi qi) {
		if (player.age % 20 == 0) apply(player);

		int med = qi.getMeditateTicks();
		if (med > 0) {
			ServerWorld world = player.getServerWorld();
			if (med % 200 == 40) {
				ModPackets.sendFx(player, FxData.follow(FxType.QI_AURA, player.getId(), player.getPos(), qi.getRealm().getRgb(), 1.0F, 200));
			}
			if (med > 40 && med % 12 == 0) {
				// Qi spiralling into the dantian.
				double a = med * 0.35;
				double r = 1.1;
				Vec3d p = player.getPos().add(Math.cos(a) * r, 0.2 + (med % 60) / 60.0 * 1.2, Math.sin(a) * r);
				Vec3d v = player.getPos().add(0, 0.9, 0).subtract(p).multiply(0.06);
				SkillFx.single(world, com.ngoducduy.celestialarts.registry.GlowParticleEffect.glow(qi.getRealm().getRgb(), 0.6F, 24), p, v);
				if (med % 48 == 0) SkillFx.single(world, ModParticles.RUNE, p.add(0, 0.4, 0), new Vec3d(0, 0.02, 0));
			}
		}

		if (canHover(qi.getRealm()) && player.isSneaking() && !player.isOnGround() && !player.hasVehicle()
				&& !player.getAbilities().flying && player.getVelocity().y < -0.05 && !player.isTouchingWater()) {
			player.fallDistance = 0.0F;
			if (player.age % 6 == 0) {
				SkillFx.single(player.getServerWorld(), ModParticles.WIND_STREAK, player.getPos().add((player.getRandom().nextDouble() - 0.5) * 0.8, 0.1, (player.getRandom().nextDouble() - 0.5) * 0.8), new Vec3d(0, -0.15, 0));
			}
		}
	}
}
