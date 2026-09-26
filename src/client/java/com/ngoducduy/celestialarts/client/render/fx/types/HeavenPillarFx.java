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
 * Pillar of heavenly light descending from the sky with rings falling along it and a ground burst.
 * {@code scale} = pillar radius.
 */
public class HeavenPillarFx extends ClientFx {
	private static final float HEIGHT = 64.0F;
	private static final int RINGS = 5;

	public HeavenPillarFx(FxData data, ClientWorld world) {
		super(data, world);
	}

	@Override
	public void render(MatrixStack matrices, VertexConsumerProvider consumers, Camera camera, float tickDelta) {
		float t = time(tickDelta);
		float p = progress(tickDelta);
		float in = RenderUtil.easeOutQuint(Math.min(1.0F, t / 4.0F));
		float out = 1.0F - RenderUtil.easeInCubic(MathHelper.clamp((p - 0.65F) / 0.35F, 0.0F, 1.0F));
		float env = Math.min(in, out);
		if (env <= 0.0F) return;
		Vec3d o = origin(tickDelta);
		float r = scale * (0.7F + 0.3F * in) * (1.0F + 0.05F * MathHelper.sin(t * 0.9F));
		int white = RenderUtil.whiten(color, 0.9F);
		// Standing inside the pillar (caster in first person, or a third-person camera clipped against
		// the ground) must not white the screen out: every shell (including the white core) and the
		// camera-facing flash fade with camera proximity while the falling rings and the ground burst stay.
		float near = nearFade(camera, o.add(0.0, 1.5, 0.0), r * 1.2F, r * 4.0F);
		float shell = near;

		matrices.translate(o.x, o.y, o.z);
		VertexConsumer pillar = consumers.getBuffer(ModRenderLayers.additive(FxTextures.PILLAR));
		MatrixStack.Entry e = matrices.peek();
		// Main shaft (texture scrolls downward = light pouring from the heavens).
		RenderUtil.cylinder(pillar, e, r, r, HEIGHT, 24, 3.0F, HEIGHT / 6.0F, t * 0.12F, color, env * 0.75F * shell, env * 0.75F * shell);
		matrices.push();
		matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(t * 3.0F));
		RenderUtil.cylinder(pillar, matrices.peek(), r * 1.5F, r * 1.5F, HEIGHT, 24, 4.0F, HEIGHT / 8.0F, t * 0.2F, color, env * 0.3F * shell, env * 0.3F * shell);
		matrices.pop();
		VertexConsumer core = consumers.getBuffer(ModRenderLayers.additive(FxTextures.BEAM_CORE));
		RenderUtil.cylinder(core, e, r * 0.45F, r * 0.45F, HEIGHT, 12, 1.0F, HEIGHT / 4.0F, t * 0.3F, white, env * shell, env * shell);
		VertexConsumer glow = consumers.getBuffer(ModRenderLayers.additive(FxTextures.GLOW));
		RenderUtil.cylinder(glow, e, r * 2.6F, r * 2.6F, HEIGHT, 16, 1.0F, 1.0F, 0.0F, color, env * 0.18F * near, env * 0.18F * near);

		// Rings falling along the shaft.
		VertexConsumer ring = consumers.getBuffer(ModRenderLayers.additive(FxTextures.RING));
		for (int i = 0; i < RINGS; i++) {
			float f = ((t * 0.02F) + (float) i / RINGS) % 1.0F;
			float y = (1.0F - f) * 30.0F;
			float a = env * (1.0F - f) * 0.7F * (0.15F + 0.85F * near);
			matrices.push();
			matrices.translate(0, y, 0);
			RenderUtil.flatQuad(ring, matrices.peek(), r * (3.0F - 1.2F * f), i % 2 == 0 ? white : color, a);
			matrices.pop();
		}
		// Ground burst.
		RenderUtil.flatQuad(glow, e, r * 4.0F, color, env * 0.7F);
		RenderUtil.flatQuad(ring, e, r * 3.4F * (1.0F + 0.15F * MathHelper.sin(t * 0.5F)), white, env * 0.6F);
		// Sky-piercing top flash.
		matrices.push();
		matrices.translate(0, 1.5F, 0);
		faceCamera(matrices, camera);
		if (near > 0.0F) RenderUtil.billboardQuad(glow, matrices.peek(), r * 3.5F, white, env * 0.6F * near);
		matrices.pop();
	}
}
