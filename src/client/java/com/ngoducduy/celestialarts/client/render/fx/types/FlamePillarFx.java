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
 * Column of flame erupting from the ground. {@code scale} = base radius.
 */
public class FlamePillarFx extends ClientFx {
	public FlamePillarFx(FxData data, ClientWorld world) {
		super(data, world);
	}

	@Override
	public void render(MatrixStack matrices, VertexConsumerProvider consumers, Camera camera, float tickDelta) {
		float p = progress(tickDelta);
		float t = time(tickDelta);
		float rise = RenderUtil.easeOutCubic(Math.min(1.0F, p / 0.22F));
		float fade = 1.0F - RenderUtil.easeInCubic(MathHelper.clamp((p - 0.6F) / 0.4F, 0.0F, 1.0F));
		if (fade <= 0.0F) return;
		Vec3d o = origin(tickDelta);
		float r = scale;
		float height = Math.max(4.0F, r * 5.0F) * rise;
		int yellow = RenderUtil.lerpColor(0.6F, color, 0xFFE27A);
		int white = RenderUtil.whiten(color, 0.85F);

		matrices.translate(o.x, o.y, o.z);
		VertexConsumer flame = consumers.getBuffer(ModRenderLayers.additive(FxTextures.FLAME_COLUMN));
		// Outer flame sheet.
		matrices.push();
		matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(t * 4.0F));
		RenderUtil.cylinder(flame, matrices.peek(), r * 1.15F, r * 0.55F, height, 20, 2.0F, -t * 0.09F, color, fade * 0.9F, 0.0F);
		matrices.pop();
		// Inner sheet spinning the other way, hotter colour.
		matrices.push();
		matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-t * 6.0F + 60.0F));
		RenderUtil.cylinder(flame, matrices.peek(), r * 0.85F, r * 0.4F, height * 0.92F, 16, 3.0F, -t * 0.14F, yellow, fade * 0.9F, 0.0F);
		matrices.pop();
		// White core.
		VertexConsumer core = consumers.getBuffer(ModRenderLayers.additive(FxTextures.BEAM_CORE));
		RenderUtil.cylinder(core, matrices.peek(), r * 0.4F, r * 0.1F, height * 0.8F, 12, 1.0F, -t * 0.2F, white, fade * 0.9F, 0.0F);
		// Base glow disc + rising heat ring.
		VertexConsumer glow = consumers.getBuffer(ModRenderLayers.additive(FxTextures.GLOW));
		RenderUtil.flatQuad(glow, matrices.peek(), r * 2.2F, color, fade * 0.7F);
		VertexConsumer ring = consumers.getBuffer(ModRenderLayers.additive(FxTextures.RING));
		float ringT = (t * 0.08F) % 1.0F;
		matrices.push();
		matrices.translate(0, ringT * height * 0.7F, 0);
		RenderUtil.flatQuad(ring, matrices.peek(), r * (1.6F - ringT * 0.8F), yellow, fade * (1.0F - ringT) * 0.6F);
		matrices.pop();
	}
}
