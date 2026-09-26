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
 * Crescent slash sweeping in front of the followed entity. Uses the entity's look at spawn time.
 * {@code extra}: 0 horizontal, 1 diagonal (top-left to bottom-right), 2 diagonal mirrored, 3 vertical.
 * {@code scale} = outer radius.
 */
public class SlashArcFx extends ClientFx {
	private static final float SWEEP = (float) Math.toRadians(130);
	private final float roll;
	private final boolean reverse;

	public SlashArcFx(FxData data, ClientWorld world) {
		super(data, world);
		this.roll = switch (data.extra()) {
			case 1 -> -50.0F;
			case 2 -> 50.0F;
			case 3 -> 90.0F;
			default -> 0.0F;
		};
		this.reverse = data.extra() == 2 || (data.extra() == 0 && rand(3) > 0.5F);
		// Free-standing slashes (no entity) face the direction given in {@code target}.
		if (entity() == null && data.target() != null && data.target().lengthSquared() > 1.0E-6) {
			Vec3d d = data.target().normalize();
			this.yaw = (float) Math.toDegrees(Math.atan2(-d.x, d.z));
			this.pitch = (float) -Math.toDegrees(Math.asin(MathHelper.clamp(d.y, -1.0, 1.0)));
		}
	}

	@Override
	protected Vec3d anchor(net.minecraft.entity.Entity e) {
		return e.getPos().add(0, e.getStandingEyeHeight() - 0.35, 0);
	}

	@Override
	public void render(MatrixStack matrices, VertexConsumerProvider consumers, Camera camera, float tickDelta) {
		float p = progress(tickDelta);
		// Head of the slash advances fast, tail follows, whole thing fades at the end.
		float head = RenderUtil.easeOutQuint(Math.min(1.0F, p / 0.55F));
		float tail = RenderUtil.easeInCubic(MathHelper.clamp((p - 0.25F) / 0.75F, 0.0F, 1.0F));
		float alpha = 1.0F - RenderUtil.easeInCubic(MathHelper.clamp((p - 0.45F) / 0.55F, 0.0F, 1.0F));
		if (head - tail < 0.01F || alpha <= 0.0F) return;

		Vec3d o = origin(tickDelta);
		matrices.translate(o.x, o.y, o.z);
		matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-yaw));
		matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(pitch));
		matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(roll));
		// Push forward a little so the arc clears the body.
		matrices.translate(0, 0, 0.45);
		// arcBand lives in the XZ plane around +Y; tilt so it sweeps in front (+Z) of the caster.
		matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-8.0F));

		float center = (float) Math.PI / 2.0F; // +Z
		float a0 = center - SWEEP / 2.0F;
		float a1 = center + SWEEP / 2.0F;
		if (reverse) {
			float tmp = a0;
			a0 = a1;
			a1 = tmp;
		}
		float start = MathHelper.lerp(tail, a0, a1);
		float end = MathHelper.lerp(head, a0, a1);
		int segs = Math.max(4, (int) (Math.abs(head - tail) * 28));

		MatrixStack.Entry e = matrices.peek();
		VertexConsumer vc = consumers.getBuffer(ModRenderLayers.additive(FxTextures.SLASH));
		// Wide coloured crescent.
		RenderUtil.arcBand(vc, e, scale * 0.25F, scale, start, end, segs, color, alpha * 0.25F, alpha);
		// Bright white core along the outer edge.
		RenderUtil.arcBand(vc, e, scale * 0.62F, scale * 0.98F, start, end, segs, RenderUtil.whiten(color, 0.8F), alpha * 0.2F, alpha * 0.9F);
		// Slight after-image below for volume.
		matrices.translate(0, -0.06, 0);
		RenderUtil.arcBand(vc, matrices.peek(), scale * 0.3F, scale * 1.06F, start, end, segs, color, 0.0F, alpha * 0.35F);
	}
}
