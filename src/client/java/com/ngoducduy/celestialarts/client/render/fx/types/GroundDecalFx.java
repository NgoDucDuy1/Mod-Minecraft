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
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;

/**
 * Persistent ground mark (scorch / frost) left behind by an impact. Drawn with normal alpha blending
 * (it must be able to darken the ground), with a brief additive ember/ice glow that dies out much
 * sooner than the mark itself. {@code scale} = radius, {@code extra} = 0 scorch, 1 frost,
 * 2 the palm print of Thiên Đạo Chi Thủ (always flat on the ground; {@code target} is then the
 * horizontal direction the fingers point, the glow lingers for a minute and the print is drawn
 * without distance fog because it is 300 blocks across).
 */
public class GroundDecalFx extends ClientFx {
	private final boolean frost;
	private final boolean palm;
	private final float rotation;
	private final Vec3d normal;

	public GroundDecalFx(FxData data, ClientWorld world) {
		super(data, world);
		this.frost = data.extra() == 1;
		this.palm = data.extra() == 2;
		Vec3d t = data.target();
		double l = t.lengthSquared();
		if (palm) {
			this.rotation = l < 1.0E-6 ? 0.0F : (float) Math.toDegrees(Math.atan2(t.x, t.z));
			this.normal = new Vec3d(0, 1, 0);
		} else {
			this.rotation = rand(1) * 360.0F;
			this.normal = (l > 0.9 && l < 1.1 && !t.equals(data.pos())) ? t.normalize() : new Vec3d(0, 1, 0);
		}
	}

	@Override
	public void render(MatrixStack matrices, VertexConsumerProvider consumers, Camera camera, float tickDelta) {
		float t = time(tickDelta);
		float p = progress(tickDelta);
		float in = MathHelper.clamp(t / 4.0F, 0.0F, 1.0F);
		float out = MathHelper.clamp((1.0F - p) / 0.35F, 0.0F, 1.0F);
		float alpha = in * out;
		if (alpha <= 0.0F) return;
		Vec3d o = origin(tickDelta);
		matrices.translate(o.x, o.y + 0.02, o.z);
		alignY(matrices, normal);
		matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(rotation));
		MatrixStack.Entry e = matrices.peek();

		if (palm) {
			renderPalm(consumers, e, t, alpha);
			return;
		}
		Identifier tex = frost ? FxTextures.FROST_PATCH : FxTextures.SCORCH;
		// The mark itself: frost is tinted by the skill colour, scorch keeps its own dark pigment
		// (white tint) and the texture's ember cracks pick up the colour through the glow below.
		VertexConsumer mark = consumers.getBuffer(ModRenderLayers.translucentGlow(tex));
		RenderUtil.flatQuad(mark, e, scale, frost ? RenderUtil.whiten(color, 0.5F) : 0xFFFFFF, alpha * (frost ? 0.85F : 0.95F));

		// Residual heat / cold glow: strong at first, gone after ~3 seconds.
		float glowLife = MathHelper.clamp(1.0F - t / 60.0F, 0.0F, 1.0F);
		if (glowLife > 0.0F) {
			VertexConsumer glow = consumers.getBuffer(ModRenderLayers.additive(tex));
			float pulse = 0.8F + 0.2F * MathHelper.sin(t * 0.5F);
			RenderUtil.flatQuad(glow, e, scale, color, alpha * glowLife * glowLife * (frost ? 0.5F : 0.9F) * pulse);
		}
	}

	/** Palm print: dark pressed earth with molten golden seams that cool down over a minute. */
	private void renderPalm(VertexConsumerProvider consumers, MatrixStack.Entry e, float t, float alpha) {
		VertexConsumer mark = consumers.getBuffer(ModRenderLayers.translucentGlowFar(FxTextures.PALM_PRINT));
		RenderUtil.flatQuad(mark, e, scale, 0xFFFFFF, alpha * 0.8F);
		float glowLife = MathHelper.clamp(1.0F - t / 1200.0F, 0.0F, 1.0F);
		if (glowLife > 0.0F) {
			VertexConsumer glow = consumers.getBuffer(ModRenderLayers.additiveFar(FxTextures.PALM_PRINT));
			float pulse = 0.85F + 0.15F * MathHelper.sin(t * 0.15F);
			RenderUtil.flatQuad(glow, e, scale, color, alpha * (0.03F + 0.15F * glowLife * glowLife) * pulse);
		}
	}
}
