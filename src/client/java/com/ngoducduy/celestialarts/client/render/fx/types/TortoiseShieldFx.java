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
 * Hexagonal energy shell wrapped around the followed entity. Pulses gently and
 * rotates its hex pattern. {@code scale} multiplies the radius.
 */
public class TortoiseShieldFx extends ClientFx {
	public TortoiseShieldFx(FxData data, ClientWorld world) {
		super(data, world);
	}

	@Override
	protected Vec3d anchor(net.minecraft.entity.Entity e) {
		return e.getPos().add(0, e.getHeight() * 0.5, 0);
	}

	@Override
	public void render(MatrixStack matrices, VertexConsumerProvider consumers, Camera camera, float tickDelta) {
		float t = time(tickDelta);
		float in = RenderUtil.easeOutCubic(Math.min(1.0F, t / 5.0F));
		float out = MathHelper.clamp((duration - t) / 6.0F, 0.0F, 1.0F);
		float alpha = Math.min(in, out);
		if (alpha <= 0.0F) return;
		net.minecraft.entity.Entity e = entity();
		float base = e != null ? Math.max(e.getWidth(), e.getHeight() * 0.62F) : 1.2F;
		float radius = (base + 0.35F) * scale * (0.4F + 0.6F * in) * (1.0F + 0.02F * MathHelper.sin(t * 0.5F));
		Vec3d o = origin(tickDelta);
		int white = RenderUtil.whiten(color, 0.6F);

		matrices.translate(o.x, o.y, o.z);
		VertexConsumer hex = consumers.getBuffer(ModRenderLayers.additive(FxTextures.HEX_SHIELD));
		// Outer hex layer, slowly rotating.
		matrices.push();
		matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(t * 1.2F));
		RenderUtil.sphere(hex, matrices.peek(), radius, 12, 24, 6.0F, 3.0F, 0.0F, color, alpha * 0.55F, false);
		matrices.pop();
		// Inner layer, counter rotating, tilted for a moiré-free shimmer.
		matrices.push();
		matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(25.0F));
		matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-t * 0.9F));
		RenderUtil.sphere(hex, matrices.peek(), radius * 0.93F, 10, 20, 5.0F, 2.5F, 0.0F, white, alpha * 0.25F, false);
		matrices.pop();
		// Soft volume glow.
		VertexConsumer glow = consumers.getBuffer(ModRenderLayers.additive(FxTextures.GLOW));
		matrices.push();
		faceCamera(matrices, camera);
		RenderUtil.billboardQuad(glow, matrices.peek(), radius * 1.5F, color, alpha * 0.18F);
		matrices.pop();
		// Equatorial ring sweeping up and down.
		VertexConsumer ring = consumers.getBuffer(ModRenderLayers.additive(FxTextures.RING));
		float sweep = MathHelper.sin(t * 0.25F);
		float ry = sweep * radius * 0.8F;
		float rr = (float) Math.sqrt(Math.max(0.0F, radius * radius - ry * ry));
		matrices.push();
		matrices.translate(0, ry, 0);
		RenderUtil.flatQuad(ring, matrices.peek(), rr * 1.15F, white, alpha * 0.5F);
		matrices.pop();
	}
}
