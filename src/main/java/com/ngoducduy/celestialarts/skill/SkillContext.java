package com.ngoducduy.celestialarts.skill;

import com.ngoducduy.celestialarts.cultivation.PlayerQi;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;

/**
 * Everything a skill needs when it is activated on the server.
 */
public record SkillContext(ServerPlayerEntity player, ServerWorld world, PlayerQi qi) {
	public Vec3d eyePos() {
		return player.getEyePos();
	}

	public Vec3d look() {
		return player.getRotationVec(1.0f);
	}

	/** Horizontal (yaw only) look direction, normalised. */
	public Vec3d flatLook() {
		Vec3d l = look();
		Vec3d flat = new Vec3d(l.x, 0, l.z);
		return flat.lengthSquared() < 1.0E-6 ? new Vec3d(0, 0, 1) : flat.normalize();
	}

	public Vec3d pos() {
		return player.getPos();
	}
}
