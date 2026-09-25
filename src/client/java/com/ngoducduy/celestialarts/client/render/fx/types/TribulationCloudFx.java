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
		// Flat strata give the underside its shape...
		layer(cloud, matrices, r * 1.0F, 0.0F, t * 0.6F, dark, alpha * 0.92F);
		layer(cloud, matrices, r * 0.85F, -0.7F, -t * 0.9F + 120.0F, mid, alpha * 0.9F);
		layer(cloud, matrices, r * 0.65F, -1.3F, t * 1.3F + 240.0F, mid, alpha * 0.85F);
		layer(cloud, matrices, r * 1.1F, 0.6F, -t * 0.4F + 60.0F, dark, alpha * 0.8F);
		layer(cloud, matrices, r * 0.8F, 1.2F, t * 0.5F + 300.0F, dark, alpha * 0.7F);
		// ...and a ring of camera-facing puffs gives it volume when seen from the side / below.
		int puffs = 10;
		for (int i = 0; i < puffs; i++) {
			float ang = (float) (i * Math.PI * 2 / puffs) + t * 0.004F * (i % 2 == 0 ? 1 : -1);
			float pr = r * (0.55F + 0.25F * RenderUtil.hash(seed, i));
			float py = (RenderUtil.hash(seed, 40 + i) - 0.5F) * 1.6F + 0.2F;
			float size = r * (0.42F + 0.2F * RenderUtil.hash(seed, 80 + i)) * (0.9F + 0.1F * MathHelper.sin(t * 0.07F + i));
			matrices.push();
			matrices.translate(MathHelper.cos(ang) * pr, py, MathHelper.sin(ang) * pr);
			faceCamera(matrices, camera);
			matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(RenderUtil.hash(seed, 120 + i) * 360.0F + t * 0.3F));
			RenderUtil.billboardQuad(cloud, matrices.peek(), size, i % 3 == 0 ? mid : dark, alpha * 0.8F);
			matrices.pop();
		}
		// Central dome of cloud so the top is not hollow.
		matrices.push();
		matrices.translate(0, 0.9F, 0);
		faceCamera(matrices, camera);
		RenderUtil.billboardQuad(cloud, matrices.peek(), r * 0.9F, dark, alpha * 0.85F);
		matrices.pop();

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

		// Internal lightning flashes: a glow inside the cloud plus a jagged tendril crawling along the underside.
		int flashSeed = seed + (age / 3);
		if (RenderUtil.hash(flashSeed, 1) < 0.4F) {
			float fx = (RenderUtil.hash(flashSeed, 2) - 0.5F) * r * 1.4F;
			float fz = (RenderUtil.hash(flashSeed, 3) - 0.5F) * r * 1.4F;
			float strength = 0.4F + 0.6F * RenderUtil.hash(flashSeed, 4);
			VertexConsumer glow = consumers.getBuffer(ModRenderLayers.additive(FxTextures.GLOW));
			matrices.push();
			matrices.translate(fx, -0.4F, fz);
			faceCamera(matrices, camera);
			RenderUtil.billboardQuad(glow, matrices.peek(), r * 0.6F * strength, RenderUtil.whiten(color, 0.5F), alpha * 0.8F * strength);
			matrices.pop();

			VertexConsumer bolt = consumers.getBuffer(ModRenderLayers.lightning());
			org.joml.Matrix4f m = matrices.peek().getPositionMatrix();
			Vec3d camRel = camera.getPos().subtract(o);
			Vec3d p = new Vec3d(fx, -1.5F, fz);
			float ang = RenderUtil.hash(flashSeed, 5) * (float) Math.PI * 2;
			Vec3d step = new Vec3d(MathHelper.cos(ang), 0, MathHelper.sin(ang)).multiply(r * 0.22F);
			int white = RenderUtil.whiten(color, 0.85F);
			for (int k = 0; k < 6; k++) {
				Vec3d q = p.add(step).add((RenderUtil.hash(flashSeed, 10 + k) - 0.5F) * r * 0.2F, (RenderUtil.hash(flashSeed, 20 + k) - 0.5F) * 0.8F, (RenderUtil.hash(flashSeed, 30 + k) - 0.5F) * r * 0.2F);
				float a0 = alpha * strength * (1.0F - k / 6.0F);
				RenderUtil.ribbon(bolt, m, p, q, camRel, 0.16F * strength, color, a0 * 0.6F, a0 * 0.5F);
				RenderUtil.ribbon(bolt, m, p, q, camRel, 0.06F * strength, white, a0, a0 * 0.8F);
				p = q;
			}
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
