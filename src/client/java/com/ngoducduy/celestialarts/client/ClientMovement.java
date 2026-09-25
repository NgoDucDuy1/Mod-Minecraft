package com.ngoducduy.celestialarts.client;

import com.ngoducduy.celestialarts.cultivation.PlayerQi;
import com.ngoducduy.celestialarts.cultivation.QiHolder;
import com.ngoducduy.celestialarts.cultivation.Realm;
import com.ngoducduy.celestialarts.cultivation.RealmPassives;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/**
 * Client half of the body passives: air jumps (Lăng Không Bộ) and sneak-hover (Ngự Không).
 * Player movement is client authoritative, so the impulse is applied here and the server is told
 * to charge qi and play the effect; the server independently refuses illegal requests.
 */
public final class ClientMovement {
	private static boolean jumpHeld;
	private static int airJumpsUsed;
	private static int airTicks;

	private ClientMovement() {
	}

	public static void tick(MinecraftClient client) {
		ClientPlayerEntity p = client.player;
		if (p == null || client.isPaused()) return;
		PlayerQi qi = QiHolder.get(p);
		Realm realm = qi.getRealm();

		boolean grounded = p.isOnGround() || p.isTouchingWater() || p.isInLava() || p.hasVehicle()
				|| p.getAbilities().flying || p.isFallFlying() || p.isClimbing();
		if (grounded) {
			airJumpsUsed = 0;
			airTicks = 0;
		} else {
			airTicks++;
		}

		boolean jumpDown = client.options.jumpKey.isPressed() && client.currentScreen == null;
		if (jumpDown && !jumpHeld && !grounded && airTicks >= 3 && airJumpsUsed < RealmPassives.airJumps(realm)
				&& (p.isCreative() || qi.hasQi(RealmPassives.AIR_JUMP_QI))) {
			airJump(p);
			airJumpsUsed++;
			ClientPackets.sendAirJump();
		}
		jumpHeld = jumpDown;

		if (RealmPassives.canHover(realm) && p.isSneaking() && !grounded && airTicks >= 2) {
			Vec3d v = p.getVelocity();
			// travel() subtracts gravity (0.08) before moving, so aim one gravity step above the target speed.
			double target = RealmPassives.HOVER_FALL_SPEED + 0.08;
			if (v.y < target) p.setVelocity(v.x, target, v.z);
			p.fallDistance = 0.0F;
		}
	}

	private static void airJump(ClientPlayerEntity p) {
		float forward = p.input.movementForward;
		float sideways = p.input.movementSideways;
		Vec3d v = p.getVelocity();
		double bx = 0.0;
		double bz = 0.0;
		if (Math.abs(forward) > 0.01F || Math.abs(sideways) > 0.01F) {
			float yaw = p.getYaw() * MathHelper.RADIANS_PER_DEGREE;
			double sin = MathHelper.sin(yaw);
			double cos = MathHelper.cos(yaw);
			// Same basis as Entity.movementInputToVelocity.
			bx = (sideways * cos - forward * sin) * 0.38;
			bz = (forward * cos + sideways * sin) * 0.38;
		}
		p.setVelocity(v.x * 0.6 + bx, RealmPassives.AIR_JUMP_VELOCITY, v.z * 0.6 + bz);
		p.fallDistance = 0.0F;
	}
}
