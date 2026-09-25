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
 * Translucent hemispherical ice dome with a frozen floor. {@code scale} = radius.
 * Grows in quickly, shimmers, then cracks and fades at the end.
 */
public class FrostDomeFx extends ClientFx {
	public FrostDomeFx(FxData data, ClientWorld world) {
		super(data, world);
	}

	@Override
	public void render(MatrixStack matrices, VertexConsumerProvider consumers, Camera camera, float tickDelta) {
		float t = time(tickDelta);
		float in = RenderUtil.easeOutCubic(Math.min(1.0F, t / 8.0F));
		float outT = MathHelper.clamp((duration - t) / 10.0F, 0.0F, 1.0F);
		float alpha = Math.min(in, outT);
		if (alpha <= 0.0F) return;
		float radius = scale * (0.2F + 0.8F * in) * (1.0F + (1.0F - outT) * 0.12F);
		Vec3d o = origin(tickDelta);
		int white = RenderUtil.whiten(color, 0.6F);

		matrices.translate(o.x, o.y + 0.02, o.z);
		// Icy shell.
		VertexConsumer shell = consumers.getBuffer(ModRenderLayers.translucentGlow(FxTextures.FROST));
		matrices.push();
		matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(t * 0.4F));
		RenderUtil.sphere(shell, matrices.peek(), radius, 10, 32, 6.0F, 2.0F, 0.0F, color, alpha * 0.45F, true);
		matrices.pop();
		// Shimmering hex energy layer just outside the ice.
		VertexConsumer hex = consumers.getBuffer(ModRenderLayers.additive(FxTextures.HEX_SHIELD));
		float shimmer = 0.10F + 0.06F * MathHelper.sin(t * 0.6F);
		RenderUtil.sphere(hex, matrices.peek(), radius * 1.03F, 8, 32, 8.0F, 3.0F, t * 0.01F, white, alpha * shimmer, true);
		// Frozen floor.
		VertexConsumer floor = consumers.getBuffer(ModRenderLayers.translucentGlow(FxTextures.FROST));
		RenderUtil.flatQuad(floor, matrices.peek(), radius, white, alpha * 0.35F);
		// Rim light on the ground.
		VertexConsumer ring = consumers.getBuffer(ModRenderLayers.additive(FxTextures.RING));
		RenderUtil.flatQuad(ring, matrices.peek(), radius * 1.18F, color, alpha * 0.5F);
	}
}
