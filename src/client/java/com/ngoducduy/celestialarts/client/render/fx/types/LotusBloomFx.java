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
 * Blooming lotus of fire (or light): two rings of petals opening outward from a glowing heart.
 * {@code scale} = full bloom radius.
 */
public class LotusBloomFx extends ClientFx {
	private static final int OUTER = 9;
	private static final int INNER = 6;

	public LotusBloomFx(FxData data, ClientWorld world) {
		super(data, world);
	}

	@Override
	public void render(MatrixStack matrices, VertexConsumerProvider consumers, Camera camera, float tickDelta) {
		float p = progress(tickDelta);
		float t = time(tickDelta);
		float open = RenderUtil.easeOutCubic(Math.min(1.0F, p / 0.55F));
		float fade = 1.0F - RenderUtil.easeInCubic(MathHelper.clamp((p - 0.6F) / 0.4F, 0.0F, 1.0F));
		if (fade <= 0.0F) return;
		Vec3d o = origin(tickDelta);
		int yellow = RenderUtil.lerpColor(0.5F, color, 0xFFE27A);
		int white = RenderUtil.whiten(color, 0.85F);

		matrices.translate(o.x, o.y, o.z);
		matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(t * 1.5F));
		VertexConsumer petal = consumers.getBuffer(ModRenderLayers.translucentGlow(FxTextures.PETAL));
		VertexConsumer petalGlow = consumers.getBuffer(ModRenderLayers.additive(FxTextures.PETAL));
		ring(petal, petalGlow, matrices, OUTER, scale, 78.0F * open, 0.0F, color, fade);
		matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F / INNER));
		ring(petal, petalGlow, matrices, INNER, scale * 0.7F, 55.0F * open, 0.15F, yellow, fade);

		// Heart glow.
		VertexConsumer glow = consumers.getBuffer(ModRenderLayers.additive(FxTextures.GLOW));
		matrices.push();
		matrices.translate(0, 0.4F * scale, 0);
		faceCamera(matrices, camera);
		float pulse = 1.0F + 0.12F * MathHelper.sin(t * 1.1F);
		RenderUtil.billboardQuad(glow, matrices.peek(), scale * 0.7F * pulse, white, fade * 0.9F);
		RenderUtil.billboardQuad(glow, matrices.peek(), scale * 1.6F * pulse, color, fade * 0.4F);
		matrices.pop();
		// Ground ring.
		VertexConsumer ringVc = consumers.getBuffer(ModRenderLayers.additive(FxTextures.RING));
		RenderUtil.flatQuad(ringVc, matrices.peek(), scale * (0.8F + 0.6F * open), yellow, fade * 0.5F);
	}

	private void ring(VertexConsumer petal, VertexConsumer glowVc, MatrixStack matrices, int count, float radius, float tiltDeg, float lift, int rgb, float alpha) {
		float petalH = radius * 0.95F;
		float petalW = radius * 0.28F;
		for (int i = 0; i < count; i++) {
			matrices.push();
			matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(360.0F * i / count));
			matrices.translate(0, lift, radius * 0.12F);
			matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(tiltDeg));
			MatrixStack.Entry e = matrices.peek();
			RenderUtil.verticalQuad(petal, e, petalW, petalH, 0.0F, 1.0F, rgb, alpha * 0.95F);
			RenderUtil.verticalQuad(glowVc, e, petalW * 0.8F, petalH * 0.9F, 0.0F, 1.0F, RenderUtil.whiten(rgb, 0.4F), alpha * 0.35F);
			matrices.pop();
		}
	}
}
