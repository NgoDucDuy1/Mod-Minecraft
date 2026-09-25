package com.ngoducduy.celestialarts.client.render.entity;

import com.ngoducduy.celestialarts.client.render.RenderUtil;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.client.util.math.MatrixStack;

/** Shared helpers for the mod's glowing skill entities. */
public abstract class GlowEntityRenderer<T extends Entity> extends EntityRenderer<T> {
	protected static final int FULL_LIGHT = LightmapTextureManager.MAX_LIGHT_COORDINATE;

	protected GlowEntityRenderer(EntityRendererFactory.Context ctx) {
		super(ctx);
		this.shadowRadius = 0.0F;
	}

	protected static float lerpYaw(Entity e, float tickDelta) {
		return MathHelper.lerpAngleDegrees(tickDelta, e.prevYaw, e.getYaw());
	}

	protected static float lerpPitch(Entity e, float tickDelta) {
		return MathHelper.lerp(tickDelta, e.prevPitch, e.getPitch());
	}

	/** Aligns model +Z with the projectile-convention rotation (yaw = atan2(vx, vz), pitch = atan2(vy, h)). */
	protected static void applyProjectileRotation(MatrixStack matrices, float yaw, float pitch) {
		matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(yaw));
		matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-pitch));
	}

	/** Aligns model +Z with the look-convention rotation used by living entities. */
	protected static void applyLookRotation(MatrixStack matrices, float yaw, float pitch) {
		matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-yaw));
		matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(pitch));
	}

	/** Standard model flip so models are authored y-down like every vanilla entity model. */
	protected static void applyModelFlip(MatrixStack matrices) {
		matrices.scale(-1.0F, -1.0F, 1.0F);
	}

	protected static float r(int rgb) {
		return RenderUtil.red(rgb);
	}

	protected static float g(int rgb) {
		return RenderUtil.green(rgb);
	}

	protected static float b(int rgb) {
		return RenderUtil.blue(rgb);
	}
}
