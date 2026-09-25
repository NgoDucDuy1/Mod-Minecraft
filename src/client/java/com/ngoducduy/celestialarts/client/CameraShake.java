package com.ngoducduy.celestialarts.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.MathHelper;

/**
 * Screen shake driven by impacts. Multiple shakes stack (the strongest wins, durations extend).
 * Applied to the camera in {@code CameraMixin}.
 */
public final class CameraShake {
	private static float strength;
	private static int ticksLeft;
	private static int totalTicks;
	private static float prevTime;
	private static long seed;

	private CameraShake() {
	}

	public static void add(float newStrength, int ticks) {
		if (newStrength <= 0.0F || ticks <= 0) return;
		strength = Math.max(strength * (ticksLeft / (float) Math.max(1, totalTicks)), newStrength);
		ticksLeft = Math.max(ticksLeft, ticks);
		totalTicks = ticksLeft;
		seed = System.nanoTime();
	}

	public static void tick(MinecraftClient client) {
		if (client.isPaused()) return;
		if (ticksLeft > 0) {
			ticksLeft--;
			if (ticksLeft == 0) {
				strength = 0.0F;
				totalTicks = 0;
			}
		}
	}

	public static void clear() {
		strength = 0.0F;
		ticksLeft = 0;
		totalTicks = 0;
	}

	public static boolean isActive() {
		return ticksLeft > 0 && strength > 0.0F;
	}

	/** Current amplitude in blocks, decaying quadratically. */
	public static float amplitude(float tickDelta) {
		if (!isActive()) return 0.0F;
		float t = (ticksLeft - tickDelta) / (float) Math.max(1, totalTicks);
		return strength * 0.18F * t * t;
	}

	public static float offsetX(float tickDelta) {
		float time = (float) ((System.nanoTime() - seed) / 1.0E7);
		return MathHelper.sin(time * 1.31F) * amplitude(tickDelta);
	}

	public static float offsetY(float tickDelta) {
		float time = (float) ((System.nanoTime() - seed) / 1.0E7);
		return MathHelper.cos(time * 1.77F + 1.3F) * amplitude(tickDelta) * 0.7F;
	}

	public static float roll(float tickDelta) {
		float time = (float) ((System.nanoTime() - seed) / 1.0E7);
		return MathHelper.sin(time * 0.93F + 0.7F) * amplitude(tickDelta) * 6.0F;
	}
}
