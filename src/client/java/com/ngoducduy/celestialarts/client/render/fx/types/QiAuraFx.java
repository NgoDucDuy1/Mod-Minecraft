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
 * Column of spirit energy rising around the followed entity (meditation, charging, breakthrough).
 * {@code scale} multiplies the size.
 */
public class QiAuraFx extends ClientFx {
	public QiAuraFx(FxData data, ClientWorld world) {
		super(data, world);
	}

	@Override
	public void render(MatrixStack matrices, VertexConsumerProvider consumers, Camera camera, float tickDelta) {
		float t = time(tickDelta);
		float alpha = fade(tickDelta, 8.0F, 8.0F);
		if (alpha <= 0.0F) return;
		Vec3d o = origin(tickDelta);
		float s = scale;
		float pulse = 0.85F + 0.15F * MathHelper.sin(t * 0.3F);
		int white = RenderUtil.whiten(color, 0.6F);

		matrices.translate(o.x, o.y, o.z);
		VertexConsumer pillar = consumers.getBuffer(ModRenderLayers.additive(FxTextures.PILLAR));
		matrices.push();
		matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(t * 2.0F));
		RenderUtil.cylinder(pillar, matrices.peek(), 0.95F * s, 0.5F * s, 3.4F * s, 20, 3.0F, 1.0F, t * 0.05F, color, alpha * 0.45F * pulse, 0.0F);
		matrices.pop();
		matrices.push();
		matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-t * 3.0F + 40.0F));
		RenderUtil.cylinder(pillar, matrices.peek(), 0.75F * s, 0.35F * s, 2.8F * s, 16, 2.0F, 1.0F, t * 0.08F, white, alpha * 0.3F * pulse, 0.0F);
		matrices.pop();
		// Ground rings.
		VertexConsumer ring = consumers.getBuffer(ModRenderLayers.additive(FxTextures.RING));
		matrices.push();
		matrices.translate(0, 0.05, 0);
		RenderUtil.flatQuad(ring, matrices.peek(), 1.25F * s * pulse, color, alpha * 0.6F);
		float rise = (t * 0.03F) % 1.0F;
		matrices.translate(0, rise * 2.5F * s, 0);
		RenderUtil.flatQuad(ring, matrices.peek(), (1.1F - rise * 0.6F) * s, white, alpha * (1.0F - rise) * 0.5F);
		matrices.pop();
		// Soft glow around the body.
		VertexConsumer glow = consumers.getBuffer(ModRenderLayers.additive(FxTextures.GLOW));
		matrices.push();
		matrices.translate(0, 1.0F * s, 0);
		faceCamera(matrices, camera);
		RenderUtil.billboardQuad(glow, matrices.peek(), 1.6F * s * pulse, color, alpha * 0.25F);
		matrices.pop();
	}
}
