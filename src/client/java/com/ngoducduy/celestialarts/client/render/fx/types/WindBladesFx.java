package com.ngoducduy.celestialarts.client.render.fx.types;

import com.ngoducduy.celestialarts.client.render.FxTextures;
import com.ngoducduy.celestialarts.client.render.RenderUtil;
import com.ngoducduy.celestialarts.client.render.fx.ClientFx;
import com.ngoducduy.celestialarts.client.render.layer.ModRenderLayers;
import com.ngoducduy.celestialarts.network.FxData;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;

/**
 * Two counter-rotating tiers of crescent wind blades circling the followed entity.
 * {@code scale} = orbit radius.
 */
public class WindBladesFx extends ClientFx {
	private static final float BLADE_SPAN = (float) Math.toRadians(52);

	public WindBladesFx(FxData data, ClientWorld world) {
		super(data, world);
	}

	@Override
	public void render(MatrixStack matrices, VertexConsumerProvider consumers, Camera camera, float tickDelta) {
		float t = time(tickDelta);
		float alpha = fade(tickDelta, 5.0F, 6.0F);
		if (alpha <= 0.0F) return;
		Vec3d o = origin(tickDelta);
		matrices.translate(o.x, o.y, o.z);
		int white = RenderUtil.whiten(color, 0.75F);

		VertexConsumer vc = consumers.getBuffer(ModRenderLayers.additive(FxTextures.WIND_BLADE));
		// Lower tier: 3 blades, clockwise.
		tier(vc, matrices, 3, scale, 0.9F + 0.08F * MathHelper.sin(t * 0.4F), t * 14.0F, 12.0F, alpha, white);
		// Upper tier: 4 blades, counter-clockwise, smaller radius.
		tier(vc, matrices, 4, scale * 0.78F, 1.5F + 0.08F * MathHelper.cos(t * 0.5F), -t * 18.0F, -10.0F, alpha, white);

		// Spinning gust ring on the ground.
		VertexConsumer glow = consumers.getBuffer(ModRenderLayers.additive(FxTextures.GLOW));
		matrices.push();
		matrices.translate(0, 0.08, 0);
		RenderUtil.annulus(glow, matrices.peek(), scale * 0.7F, scale * 1.25F, 32, 1.0F, 0.0F, color, 0.0F, alpha * 0.35F);
		matrices.pop();
	}

	private void tier(VertexConsumer vc, MatrixStack matrices, int count, float radius, float height, float rotDeg, float tiltDeg, float alpha, int white) {
		matrices.push();
		matrices.translate(0, height, 0);
		matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(rotDeg));
		matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(tiltDeg));
		MatrixStack.Entry e = matrices.peek();
		for (int i = 0; i < count; i++) {
			float a0 = (float) (i * Math.PI * 2.0 / count);
			float a1 = a0 + BLADE_SPAN;
			RenderUtil.arcBand(vc, e, radius * 0.72F, radius, a0, a1, 8, color, alpha * 0.9F, alpha * 0.1F);
			RenderUtil.arcBand(vc, e, radius * 0.88F, radius * 0.98F, a0, a0 + BLADE_SPAN * 0.7F, 6, white, alpha * 0.9F, 0.0F);
			// Upright crescent "sail" on the outer edge: the flat arcs are almost invisible edge-on
			// (first person, or any camera near the blade plane), the sail keeps the blade readable.
			sail(vc, e, radius * 0.97F, 0.34F, a0, a0 + BLADE_SPAN * 0.85F, 8, color, alpha * 0.8F);
			sail(vc, e, radius * 0.99F, 0.14F, a0, a0 + BLADE_SPAN * 0.6F, 6, white, alpha * 0.8F);
		}
		matrices.pop();
	}

	/** Partial cylinder wall from angle {@code a0} to {@code a1}, {@code height} tall, fading towards {@code a1}. */
	private static void sail(VertexConsumer vc, MatrixStack.Entry e, float radius, float height, float a0, float a1, int segments, int rgb, float alpha) {
		float h = height * 0.5F;
		for (int i = 0; i < segments; i++) {
			float f0 = (float) i / segments, f1 = (float) (i + 1) / segments;
			float b0 = a0 + (a1 - a0) * f0, b1 = a0 + (a1 - a0) * f1;
			float x0 = MathHelper.cos(b0) * radius, z0 = MathHelper.sin(b0) * radius;
			float x1 = MathHelper.cos(b1) * radius, z1 = MathHelper.sin(b1) * radius;
			// Thin at both tips (crescent), full in the middle; alpha falls off towards the trailing tip.
			float k0 = MathHelper.sin(f0 * (float) Math.PI), k1 = MathHelper.sin(f1 * (float) Math.PI);
			float al0 = alpha * (1.0F - f0 * 0.8F), al1 = alpha * (1.0F - f1 * 0.8F);
			RenderUtil.vertex(vc, e, x0, -h * k0, z0, f0, 1.0F, rgb, al0);
			RenderUtil.vertex(vc, e, x1, -h * k1, z1, f1, 1.0F, rgb, al1);
			RenderUtil.vertex(vc, e, x1, h * k1, z1, f1, 0.0F, rgb, al1);
			RenderUtil.vertex(vc, e, x0, h * k0, z0, f0, 0.0F, rgb, al0);
		}
	}
}
