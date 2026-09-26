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
 * Spherical explosion core: flash, expanding light sphere, radial rays and a ground ring.
 * {@code scale} = radius.
 */
public class EnergyBurstFx extends ClientFx {
	private static final int RAYS = 14;

	public EnergyBurstFx(FxData data, ClientWorld world) {
		super(data, world);
	}

	@Override
	public void render(MatrixStack matrices, VertexConsumerProvider consumers, Camera camera, float tickDelta) {
		float p = progress(tickDelta);
		float grow = RenderUtil.easeOutQuint(p);
		float fadeOut = (float) Math.pow(1.0F - p, 1.5);
		if (fadeOut <= 0.0F) return;
		float r = scale * (0.15F + 0.85F * grow);
		Vec3d o = origin(tickDelta);
		int white = RenderUtil.whiten(color, 0.85F);

		matrices.translate(o.x, o.y, o.z);
		// Expanding sphere of light.
		VertexConsumer glow = consumers.getBuffer(ModRenderLayers.additive(FxTextures.GLOW));
		matrices.push();
		matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(time(tickDelta) * 6.0F));
		RenderUtil.sphere(glow, matrices.peek(), r, 8, 16, 1.0F, 1.0F, 0.0F, color, fadeOut * 0.6F, false);
		matrices.pop();
		// Central flash + wide halo.
		matrices.push();
		faceCamera(matrices, camera);
		float flash = (float) Math.pow(1.0F - p, 2.5);
		RenderUtil.billboardQuad(glow, matrices.peek(), scale * 1.1F, white, flash);
		RenderUtil.billboardQuad(glow, matrices.peek(), r * 2.4F, color, fadeOut * 0.45F);
		matrices.pop();
		// Radial rays.
		VertexConsumer lines = consumers.getBuffer(ModRenderLayers.lightning());
		var m = matrices.peek().getPositionMatrix();
		Vec3d cam = camera.getPos().subtract(o);
		float rayLen = scale * (1.4F + 1.4F * grow);
		for (int i = 0; i < RAYS; i++) {
			double th = rand(i) * Math.PI * 2.0;
			double ph = Math.acos(2.0 * rand(i + 50) - 1.0);
			Vec3d d = new Vec3d(Math.sin(ph) * Math.cos(th), Math.cos(ph), Math.sin(ph) * Math.sin(th));
			float len = rayLen * (0.6F + 0.4F * rand(i + 100));
			RenderUtil.ribbon(lines, m, d.multiply(r * 0.5), d.multiply(len), cam, 0.05F + 0.08F * scale, white, fadeOut * 0.9F, 0.0F);
		}
		// Horizontal ring.
		VertexConsumer ring = consumers.getBuffer(ModRenderLayers.additive(FxTextures.RING));
		RenderUtil.flatQuad(ring, matrices.peek(), r * 1.9F, color, fadeOut * 0.7F);
	}
}
