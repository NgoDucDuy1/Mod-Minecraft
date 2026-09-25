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
 * Dark heavenly tribulation cloud with a rotating rune ring underneath and inner flashes.
 * {@code scale} = cloud radius. {@code pos} is the cloud centre (already high in the air).
 */
public class TribulationCloudFx extends ClientFx {
	public TribulationCloudFx(FxData data, ClientWorld world) {
		super(data, world);
	}

	@Override
	public void render(MatrixStack matrices, VertexConsumerProvider consumers, Camera camera, float tickDelta) {
		float t = time(tickDelta);
		float in = RenderUtil.easeOutCubic(Math.min(1.0F, t / 20.0F));
		float out = MathHelper.clamp((duration - t) / 20.0F, 0.0F, 1.0F);
		float alpha = Math.min(in, out);
		if (alpha <= 0.0F) return;
		float r = scale * (0.6F + 0.4F * in);
		Vec3d o = origin(tickDelta);
		int dark = RenderUtil.lerpColor(0.75F, color, 0x120A20);
		int mid = RenderUtil.lerpColor(0.45F, color, 0x120A20);

		matrices.translate(o.x, o.y, o.z);
		VertexConsumer cloud = consumers.getBuffer(ModRenderLayers.translucentGlow(FxTextures.CLOUD));
		layer(cloud, matrices, r * 1.0F, 0.0F, t * 0.6F, dark, alpha * 0.92F);
		layer(cloud, matrices, r * 0.85F, -0.7F, -t * 0.9F + 120.0F, mid, alpha * 0.9F);
		layer(cloud, matrices, r * 0.65F, -1.3F, t * 1.3F + 240.0F, mid, alpha * 0.85F);
		layer(cloud, matrices, r * 1.1F, 0.6F, -t * 0.4F + 60.0F, dark, alpha * 0.8F);

		// Rune ring under the cloud.
		VertexConsumer runes = consumers.getBuffer(ModRenderLayers.additive(FxTextures.GLYPHS));
		matrices.push();
		matrices.translate(0, -1.9F, 0);
		matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-t * 1.5F));
		RenderUtil.annulus(runes, matrices.peek(), r * 0.45F, r * 0.6F, 48, 6.0F, 0.0F, color, alpha * 0.8F, alpha * 0.8F);
		matrices.pop();
		VertexConsumer ring = consumers.getBuffer(ModRenderLayers.additive(FxTextures.RING));
		matrices.push();
		matrices.translate(0, -2.0F, 0);
		RenderUtil.flatQuad(ring, matrices.peek(), r * 0.75F, color, alpha * (0.35F + 0.15F * MathHelper.sin(t * 0.3F)));
		matrices.pop();

		// Internal lightning flashes.
		int flashSeed = seed + (age / 3);
		if (RenderUtil.hash(flashSeed, 1) < 0.35F) {
			float fx = (RenderUtil.hash(flashSeed, 2) - 0.5F) * r * 1.4F;
			float fz = (RenderUtil.hash(flashSeed, 3) - 0.5F) * r * 1.4F;
			float strength = 0.4F + 0.6F * RenderUtil.hash(flashSeed, 4);
			VertexConsumer glow = consumers.getBuffer(ModRenderLayers.additive(FxTextures.GLOW));
			matrices.push();
			matrices.translate(fx, -0.4F, fz);
			faceCamera(matrices, camera);
			RenderUtil.billboardQuad(glow, matrices.peek(), r * 0.5F * strength, RenderUtil.whiten(color, 0.5F), alpha * 0.8F * strength);
			matrices.pop();
		}
	}

	private static void layer(VertexConsumer vc, MatrixStack matrices, float radius, float y, float rotDeg, int rgb, float a) {
		matrices.push();
		matrices.translate(0, y, 0);
		matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(rotDeg));
		RenderUtil.flatQuad(vc, matrices.peek(), radius, rgb, a);
		matrices.pop();
	}
}
