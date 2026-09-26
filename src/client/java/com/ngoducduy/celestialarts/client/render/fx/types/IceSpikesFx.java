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
 * Ring of ice crystals thrusting out of the ground around {@code pos}. {@code scale} = area radius.
 * Each spike has its own delay, height, tilt and rises with a snappy ease; they sink back at the end.
 */
public class IceSpikesFx extends ClientFx {
	private final int count;

	public IceSpikesFx(FxData data, ClientWorld world) {
		super(data, world);
		this.count = Math.max(8, (int) (scale * 3.2F));
	}

	@Override
	public void render(MatrixStack matrices, VertexConsumerProvider consumers, Camera camera, float tickDelta) {
		float t = time(tickDelta);
		float outT = MathHelper.clamp((duration - t) / 10.0F, 0.0F, 1.0F);
		if (outT <= 0.0F) return;
		Vec3d o = origin(tickDelta);
		int white = RenderUtil.whiten(color, 0.55F);
		int solidColor = RenderUtil.whiten(color, 0.25F);

		matrices.translate(o.x, o.y, o.z);
		VertexConsumer solid = consumers.getBuffer(ModRenderLayers.solidGlow(FxTextures.ICE_SPIKE));
		VertexConsumer glow = consumers.getBuffer(ModRenderLayers.additive(FxTextures.ICE_SPIKE));
		for (int i = 0; i < count; i++) {
			float ang = (float) (i * Math.PI * 2.0 / count) + (rand(i) - 0.5F) * 0.5F;
			float dist = scale * (0.3F + 0.68F * rand(i + 30));
			float delay = rand(i + 60) * 8.0F;
			float rise = RenderUtil.easeOutQuint(MathHelper.clamp((t - delay) / 6.0F, 0.0F, 1.0F)) * RenderUtil.easeInOutSine(outT);
			if (rise <= 0.0F) continue;
			float height = (1.1F + 2.2F * rand(i + 90)) * rise;
			float width = (0.22F + 0.28F * rand(i + 120)) * (0.5F + 0.5F * rise);
			float tilt = 10.0F + 22.0F * rand(i + 150);
			matrices.push();
			matrices.translate(Math.cos(ang) * dist, -0.1, Math.sin(ang) * dist);
			// Tilt outward from the centre.
			matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float) Math.toDegrees(-ang)));
			matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-tilt));
			matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(rand(i + 180) * 90.0F));
			MatrixStack.Entry e = matrices.peek();
			RenderUtil.crossQuads(solid, e, width, height, solidColor, 0.88F * outT);
			RenderUtil.crossQuads(glow, e, width * 0.7F, height * 0.9F, white, 0.35F * outT);
			// Small secondary shard.
			matrices.translate(width * 0.6F, 0, width * 0.3F);
			matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(18.0F));
			RenderUtil.crossQuads(solid, matrices.peek(), width * 0.5F, height * 0.45F, solidColor, 0.85F * outT);
			matrices.pop();
		}
	}
}
