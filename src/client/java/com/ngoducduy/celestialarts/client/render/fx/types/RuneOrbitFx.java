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
 * Eight glowing dao glyphs orbiting a point or entity with a faint connecting ring.
 * {@code scale} = orbit radius.
 */
public class RuneOrbitFx extends ClientFx {
	private static final int GLYPHS = 8;

	public RuneOrbitFx(FxData data, ClientWorld world) {
		super(data, world);
	}

	@Override
	protected Vec3d anchor(net.minecraft.entity.Entity e) {
		return e.getPos().add(0, e.getHeight() * 0.6, 0);
	}

	@Override
	public void render(MatrixStack matrices, VertexConsumerProvider consumers, Camera camera, float tickDelta) {
		float t = time(tickDelta);
		float alpha = fade(tickDelta, 6.0F, 8.0F);
		if (alpha <= 0.0F) return;
		Vec3d o = origin(tickDelta);
		float r = scale * (0.5F + 0.5F * RenderUtil.easeOutCubic(Math.min(1.0F, t / 6.0F)));
		int white = RenderUtil.whiten(color, 0.5F);
		float glyphSize = 0.22F + 0.09F * scale;

		matrices.translate(o.x, o.y, o.z);
		VertexConsumer glyphs = consumers.getBuffer(ModRenderLayers.additive(FxTextures.GLYPHS));
		for (int i = 0; i < GLYPHS; i++) {
			float ang = (float) (i * Math.PI * 2.0 / GLYPHS) + t * 0.09F;
			float bob = MathHelper.sin(t * 0.25F + i * 0.8F) * 0.18F;
			float flick = 0.7F + 0.3F * MathHelper.sin(t * 0.7F + i * 2.1F);
			matrices.push();
			matrices.translate(Math.cos(ang) * r, bob, Math.sin(ang) * r);
			faceCamera(matrices, camera);
			MatrixStack.Entry e = matrices.peek();
			float u0 = (float) i / GLYPHS;
			float u1 = u0 + 1.0F / GLYPHS;
			glyph(glyphs, e, glyphSize, u0, u1, white, alpha * flick);
			glyph(glyphs, e, glyphSize * 1.5F, u0, u1, color, alpha * 0.35F * flick);
			matrices.pop();
		}
		// Connecting ring, slightly tilted and rotating.
		VertexConsumer glow = consumers.getBuffer(ModRenderLayers.additive(FxTextures.GLOW));
		matrices.push();
		matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(t * 2.0F));
		matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(8.0F));
		RenderUtil.annulus(glow, matrices.peek(), r * 0.92F, r * 1.08F, 40, 1.0F, 0.0F, color, alpha * 0.4F, alpha * 0.4F);
		matrices.pop();
	}

	private static void glyph(VertexConsumer vc, MatrixStack.Entry e, float half, float u0, float u1, int rgb, float a) {
		RenderUtil.vertex(vc, e, -half, -half, 0, u0, 1, rgb, a);
		RenderUtil.vertex(vc, e, half, -half, 0, u1, 1, rgb, a);
		RenderUtil.vertex(vc, e, half, half, 0, u1, 0, rgb, a);
		RenderUtil.vertex(vc, e, -half, half, 0, u0, 0, rgb, a);
	}
}
