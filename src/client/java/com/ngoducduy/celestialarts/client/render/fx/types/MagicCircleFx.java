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

import net.minecraft.util.Identifier;

/**
 * Formation array / magic circle. {@code extra} chooses the artwork:
 * 0 taiji, 1 runes, 2 thunder, 3 ice. {@code scale} = radius.
 * If {@code target} holds a unit vector the circle is oriented to face it.
 */
public class MagicCircleFx extends ClientFx {
	private final Identifier texture;
	private final Vec3d normal;
	private final float spin;

	public MagicCircleFx(FxData data, ClientWorld world) {
		super(data, world);
		this.texture = switch (data.extra()) {
			case 0 -> FxTextures.CIRCLE_TAIJI;
			case 2 -> FxTextures.CIRCLE_THUNDER;
			case 3 -> FxTextures.CIRCLE_ICE;
			default -> FxTextures.CIRCLE_RUNES;
		};
		Vec3d t = data.target();
		double l = t.lengthSquared();
		this.normal = (l > 0.9 && l < 1.1 && !t.equals(data.pos())) ? t.normalize() : new Vec3d(0, 1, 0);
		this.spin = switch (data.extra()) {
			case 0 -> 0.9F;
			case 2 -> 2.6F;
			case 3 -> 0.6F;
			default -> 1.6F;
		};
	}

	@Override
	protected Vec3d anchor(net.minecraft.entity.Entity e) {
		return e.getPos().add(0, 0.06, 0);
	}

	@Override
	public void render(MatrixStack matrices, VertexConsumerProvider consumers, Camera camera, float tickDelta) {
		float t = time(tickDelta);
		float in = MathHelper.clamp(t / 6.0F, 0.0F, 1.0F);
		float out = duration > 8 ? MathHelper.clamp((duration - t) / 8.0F, 0.0F, 1.0F) : MathHelper.clamp((duration - t) / 2.0F, 0.0F, 1.0F);
		float alpha = Math.min(RenderUtil.easeOutCubic(in), out);
		float size = scale * (0.55F + 0.45F * RenderUtil.easeOutQuint(in));
		float pulse = 0.85F + 0.15F * MathHelper.sin(t * 0.35F);
		Vec3d o = origin(tickDelta);

		matrices.translate(o.x, o.y, o.z);
		alignY(matrices, normal);

		VertexConsumer vc = consumers.getBuffer(ModRenderLayers.additive(texture));
		// Main disc.
		matrices.push();
		matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(t * spin));
		RenderUtil.flatQuad(vc, matrices.peek(), size, color, alpha * pulse);
		matrices.pop();
		// Counter-rotating ghost copy, larger and dimmer, gives the layered array look.
		matrices.push();
		matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-t * spin * 0.6F + 45.0F));
		matrices.translate(0, 0.02, 0);
		RenderUtil.flatQuad(vc, matrices.peek(), size * 1.12F, RenderUtil.whiten(color, 0.3F), alpha * 0.35F);
		matrices.pop();

		// Outer ring pulse + rim light wall.
		VertexConsumer ring = consumers.getBuffer(ModRenderLayers.additive(FxTextures.RING));
		MatrixStack.Entry e = matrices.peek();
		RenderUtil.flatQuad(ring, e, size * 1.22F, color, alpha * 0.5F * pulse);
		VertexConsumer glow = consumers.getBuffer(ModRenderLayers.additive(FxTextures.GLOW));
		float wallH = 0.12F + 0.06F * size;
		RenderUtil.cylinder(glow, e, size * 0.98F, size * 0.98F, wallH * (1.0F + 0.3F * MathHelper.sin(t * 0.5F)), 40, 1.0F, 0.0F, color, alpha * 0.45F, 0.0F);
	}
}
