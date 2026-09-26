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

import net.minecraft.entity.Entity;

/**
 * Devouring void vortex hovering in front of the followed entity: spinning spiral discs,
 * a dark core sphere, and streams of energy being pulled in. {@code scale} = core radius.
 */
public class VortexFx extends ClientFx {
	private static final int STREAMS = 14;

	public VortexFx(FxData data, ClientWorld world) {
		super(data, world);
	}

	@Override
	protected Vec3d anchor(Entity e) {
		Vec3d flat = e.getRotationVec(1.0F);
		flat = new Vec3d(flat.x, 0, flat.z);
		if (flat.lengthSquared() < 1.0E-4) flat = new Vec3d(0, 0, 1);
		return e.getPos().add(flat.normalize().multiply(3.0)).add(0, 1.4, 0);
	}

	@Override
	public void render(MatrixStack matrices, VertexConsumerProvider consumers, Camera camera, float tickDelta) {
		Entity e = entity();
		float t = time(tickDelta);
		float alpha = fade(tickDelta, 8.0F, 6.0F);
		if (alpha <= 0.0F) return;
		float r = scale * (0.6F + 0.4F * RenderUtil.easeOutCubic(Math.min(1.0F, t / 8.0F)));
		Vec3d o = origin(tickDelta);
		Vec3d facing;
		if (e != null) {
			Vec3d look = e.getRotationVec(tickDelta);
			facing = new Vec3d(look.x, 0, look.z);
			if (facing.lengthSquared() < 1.0E-4) facing = new Vec3d(0, 0, 1);
			facing = facing.normalize();
		} else {
			facing = new Vec3d(0, 0, 1);
		}
		int white = RenderUtil.whiten(color, 0.6F);
		int deep = RenderUtil.lerpColor(0.7F, color, 0x05010A);

		// Dark core (normal blending so it actually darkens the scene).
		matrices.push();
		matrices.translate(o.x, o.y, o.z);
		VertexConsumer dark = consumers.getBuffer(ModRenderLayers.translucentGlow(FxTextures.GLOW));
		float coreR = r * 0.32F * (1.0F + 0.06F * MathHelper.sin(t * 0.9F));
		matrices.push();
		faceCamera(matrices, camera);
		RenderUtil.billboardQuad(dark, matrices.peek(), coreR * 2.4F, deep, alpha * 0.95F);
		matrices.pop();

		// Inward streams (camera facing ribbons spiralling to the centre).
		VertexConsumer lines = consumers.getBuffer(ModRenderLayers.lightning());
		var m = matrices.peek().getPositionMatrix();
		Vec3d cam = camera.getPos().subtract(o);
		Vec3d up = new Vec3d(0, 1, 0);
		Vec3d side = facing.crossProduct(up).normalize();
		for (int i = 0; i < STREAMS; i++) {
			float phase = (t * 0.06F + (float) i / STREAMS) % 1.0F;
			float baseAng = (float) (i * Math.PI * 2.0 / STREAMS) + t * 0.12F;
			Vec3d prev = null;
			int steps = 10;
			for (int k = 0; k <= steps; k++) {
				float f = (float) k / steps;
				float dist = r * 1.9F * (1.0F - f) * (1.0F - phase * 0.6F);
				float ang = baseAng + f * 2.6F;
				float depth = -r * 0.8F * f * f;
				Vec3d pt = side.multiply(Math.cos(ang) * dist).add(up.multiply(Math.sin(ang) * dist)).add(facing.multiply(depth));
				if (prev != null) {
					float a0 = alpha * 0.5F * (1.0F - f);
					RenderUtil.ribbon(lines, m, prev, pt, cam, 0.06F + 0.05F * f, color, a0, a0 * 0.8F);
				}
				prev = pt;
			}
		}
		matrices.pop();

		// Spiral discs facing the caster.
		matrices.push();
		matrices.translate(o.x, o.y, o.z);
		alignZ(matrices, facing.negate());
		VertexConsumer vortex = consumers.getBuffer(ModRenderLayers.additive(FxTextures.VORTEX));
		matrices.push();
		matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(t * 16.0F));
		RenderUtil.billboardQuad(vortex, matrices.peek(), r * 2.0F, color, alpha * 0.9F);
		matrices.pop();
		matrices.push();
		matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-t * 11.0F + 90.0F));
		matrices.translate(0, 0, -0.05);
		RenderUtil.billboardQuad(vortex, matrices.peek(), r * 1.4F, white, alpha * 0.5F);
		matrices.pop();
		// Event horizon ring.
		VertexConsumer ring = consumers.getBuffer(ModRenderLayers.additive(FxTextures.RING));
		float pulse = 1.0F + 0.08F * MathHelper.sin(t * 1.4F);
		RenderUtil.billboardQuad(ring, matrices.peek(), coreR * 1.9F * pulse, white, alpha * 0.85F);
		matrices.pop();
	}
}
