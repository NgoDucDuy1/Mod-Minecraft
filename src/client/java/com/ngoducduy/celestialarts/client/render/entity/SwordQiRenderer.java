package com.ngoducduy.celestialarts.client.render.entity;

import com.ngoducduy.celestialarts.CelestialArts;
import com.ngoducduy.celestialarts.client.render.FxTextures;
import com.ngoducduy.celestialarts.client.render.RenderUtil;
import com.ngoducduy.celestialarts.client.render.entity.model.ModModelLayers;
import com.ngoducduy.celestialarts.client.render.entity.model.SwordQiModel;
import com.ngoducduy.celestialarts.client.render.layer.ModRenderLayers;
import com.ngoducduy.celestialarts.entity.SwordQiEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

/** Crescent sword-qi projectile: emissive blade model, additive ghost and a tapering energy trail. */
public class SwordQiRenderer extends GlowEntityRenderer<SwordQiEntity> {
	private static final Identifier TEXTURE = CelestialArts.id("textures/entity/sword_qi.png");
	private static final int COLOR = 0x9FE8FF;
	private final SwordQiModel model;

	public SwordQiRenderer(EntityRendererFactory.Context ctx) {
		super(ctx);
		this.model = new SwordQiModel(ctx.getPart(ModModelLayers.SWORD_QI));
	}

	@Override
	public void render(SwordQiEntity entity, float entityYaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vcp, int light) {
		float age = entity.age + tickDelta;
		float size = entity.getSize();
		float yaw = lerpYaw(entity, tickDelta);
		float pitch = lerpPitch(entity, tickDelta);

		matrices.push();
		matrices.translate(0.0, entity.getHeight() * 0.5, 0.0);
		// Core flash (camera-facing, so drawn before the blade rotation).
		matrices.push();
		matrices.multiply(this.dispatcher.getRotation());
		VertexConsumer glow = vcp.getBuffer(ModRenderLayers.additive(FxTextures.GLOW));
		RenderUtil.billboardQuad(glow, matrices.peek(), 0.9F * size, COLOR, 0.35F);
		// Wider soft halo so the thin crescent stays readable from behind the caster.
		RenderUtil.billboardQuad(glow, matrices.peek(), 1.5F * size, COLOR, 0.18F);
		matrices.pop();
		applyProjectileRotation(matrices, yaw, pitch);
		matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(entity.getRoll()));
		matrices.scale(size, size, size);

		// Energy trail behind the blade (model space: -Z is behind).
		matrices.push();
		matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-90.0F));
		VertexConsumer trail = vcp.getBuffer(ModRenderLayers.additive(FxTextures.GLOW));
		float pulse = 0.9F + 0.1F * MathHelper.sin(age * 1.5F);
		RenderUtil.cylinder(trail, matrices.peek(), 0.55F * pulse, 0.03F, 1.8F, 14, 1.0F, 0.0F, COLOR, 0.55F, 0.0F);
		matrices.pop();

		matrices.push();
		applyModelFlip(matrices);
		model.setAngles(age);
		// Solid emissive blade.
		VertexConsumer blade = vcp.getBuffer(RenderLayer.getEntityTranslucentEmissive(TEXTURE));
		model.render(matrices, blade, FULL_LIGHT, OverlayTexture.DEFAULT_UV, 1.0F, 1.0F, 1.0F, 1.0F);
		// Additive ghost for bloom.
		matrices.scale(1.22F, 2.4F, 1.12F);
		VertexConsumer ghost = vcp.getBuffer(ModRenderLayers.additive(TEXTURE));
		model.render(matrices, ghost, FULL_LIGHT, OverlayTexture.DEFAULT_UV, r(COLOR), g(COLOR), b(COLOR), 0.45F);
		matrices.pop();

		matrices.pop();
		super.render(entity, entityYaw, tickDelta, matrices, vcp, light);
	}

	@Override
	public Identifier getTexture(SwordQiEntity entity) {
		return TEXTURE;
	}
}
