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
 * Expanding flat ring with a short dust wall – the classic impact "wave".
 * {@code scale} = final radius. If {@code target} holds a unit vector the ring is aligned to it
 * (used for wall impacts of beams).
 */
public class ShockwaveRingFx extends ClientFx {
	private final Vec3d normal;

	public ShockwaveRingFx(FxData data, ClientWorld world) {
		super(data, world);
		Vec3d t = data.target();
		double l = t.lengthSquared();
		this.normal = (l > 0.9 && l < 1.1 && !t.equals(data.pos())) ? t.normalize() : new Vec3d(0, 1, 0);
	}

	@Override
	public void render(MatrixStack matrices, VertexConsumerProvider consumers, Camera camera, float tickDelta) {
		float p = progress(tickDelta);
		float grow = RenderUtil.easeOutCubic(p);
		float radius = 0.15F + scale * grow;
		float alpha = (1.0F - p) * (1.0F - p);
		Vec3d o = origin(tickDelta);

		matrices.translate(o.x, o.y, o.z);
		alignY(matrices, normal);
		MatrixStack.Entry e = matrices.peek();

		// Main coloured ring (texture is a soft ring occupying ~85% of the quad).
		VertexConsumer ring = consumers.getBuffer(ModRenderLayers.additive(FxTextures.RING));
		RenderUtil.flatQuad(ring, e, radius * 1.18F, color, alpha * 0.95F);
		// Hot white leading edge, slightly smaller so it sits on the inner side of the ring.
		RenderUtil.flatQuad(ring, e, radius * 1.05F, RenderUtil.whiten(color, 0.7F), alpha * 0.6F);

		// Dust / energy wall rising from the ring edge.
		float wallH = (0.25F + 0.35F * scale) * (1.0F - p * 0.6F);
		VertexConsumer wall = consumers.getBuffer(ModRenderLayers.additive(FxTextures.GLOW));
		RenderUtil.cylinder(wall, e, radius * 0.92F, radius * 1.02F, wallH, 32, 1.0F, 0.0F, color, alpha * 0.55F, 0.0F);

		// Faint ground flash at the very beginning.
		if (p < 0.35F) {
			float f = 1.0F - p / 0.35F;
			RenderUtil.flatQuad(wall, e, scale * 0.6F, RenderUtil.whiten(color, 0.4F), f * f * 0.7F);
		}
	}
}
