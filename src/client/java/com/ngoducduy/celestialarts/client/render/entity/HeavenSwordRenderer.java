package com.ngoducduy.celestialarts.client.render.entity;

import com.ngoducduy.celestialarts.CelestialArts;
import com.ngoducduy.celestialarts.client.render.FxTextures;
import com.ngoducduy.celestialarts.client.render.RenderUtil;
import com.ngoducduy.celestialarts.client.render.entity.model.HeavenSwordModel;
import com.ngoducduy.celestialarts.client.render.entity.model.ModModelLayers;
import com.ngoducduy.celestialarts.client.render.layer.ModRenderLayers;
import com.ngoducduy.celestialarts.entity.HeavenSwordEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

/**
 * Twelve-block heaven sword. Materialises from golden light while hovering point-down, plunges,
 * then stands embedded in the ground as its radiance fades.
 */
public class HeavenSwordRenderer extends GlowEntityRenderer<HeavenSwordEntity> {
	private static final Identifier TEXTURE = CelestialArts.id("textures/entity/heaven_sword.png");
	private static final int GOLD = 0xFFE9A8;
	private static final int DEEP_GOLD = 0xFFC94A;
	private final HeavenSwordModel model;

	public HeavenSwordRenderer(EntityRendererFactory.Context ctx) {
		super(ctx);
		this.model = new HeavenSwordModel(ctx.getPart(ModModelLayers.HEAVEN_SWORD));
	}

	@Override
	public boolean shouldRender(HeavenSwordEntity entity, net.minecraft.client.render.Frustum frustum, double x, double y, double z) {
		return true;
	}

	@Override
	public void render(HeavenSwordEntity entity, float entityYaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vcp, int light) {
		int phase = entity.getPhase();
		float phaseAge = entity.getPhaseAge() + tickDelta;
		float age = entity.age + tickDelta;
		float bladeLen = 12.0F;

		float alpha;
		float glow;
		float bob = 0.0F;
		float spin = 0.0F;
		switch (phase) {
			case HeavenSwordEntity.PHASE_MATERIALISE -> {
				float t = MathHelper.clamp(phaseAge / HeavenSwordEntity.MATERIALISE_TICKS, 0.0F, 1.0F);
				alpha = RenderUtil.easeOutCubic(t);
				glow = 0.5F + 0.5F * t;
				bob = MathHelper.sin(age * 0.2F) * 0.25F;
				spin = age * 1.5F;
			}
			case HeavenSwordEntity.PHASE_FALL -> {
				alpha = 1.0F;
				glow = 1.0F;
				spin = 0.0F;
			}
			default -> {
				alpha = 1.0F;
				glow = Math.max(0.0F, 1.0F - phaseAge / 80.0F);
			}
		}

		matrices.push();
		matrices.translate(0.0, bob, 0.0);

		// Radiant aura around the blade while materialising / falling.
		if (glow > 0.0F) {
			VertexConsumer aura = vcp.getBuffer(ModRenderLayers.additive(FxTextures.PILLAR));
			matrices.push();
			matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(age * 2.0F));
			RenderUtil.cylinder(aura, matrices.peek(), 1.6F, 1.0F, bladeLen + 3.0F, 20, 3.0F, 2.0F, age * 0.04F, GOLD, 0.45F * glow * alpha, 0.05F * glow);
			matrices.pop();
			matrices.push();
			matrices.translate(0.0, bladeLen * 0.5, 0.0);
			matrices.multiply(this.dispatcher.getRotation());
			VertexConsumer soft = vcp.getBuffer(ModRenderLayers.additive(FxTextures.GLOW));
			RenderUtil.billboardQuad(soft, matrices.peek(), 6.0F, DEEP_GOLD, 0.25F * glow * alpha);
			matrices.pop();
		}

		// Sword: model +Z (blade) rotated to point straight down, tip at the entity origin.
		matrices.translate(0.0, bladeLen, 0.0);
		matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(spin));
		matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90.0F));
		float s = HeavenSwordModel.RENDER_SCALE;
		matrices.scale(s, s, s);
		applyModelFlip(matrices);
		VertexConsumer body = vcp.getBuffer(RenderLayer.getEntityTranslucentEmissive(TEXTURE));
		model.render(matrices, body, FULL_LIGHT, OverlayTexture.DEFAULT_UV, 1.0F, 1.0F, 1.0F, alpha);
		if (glow > 0.0F) {
			matrices.scale(1.25F, 1.9F, 1.03F);
			VertexConsumer ghost = vcp.getBuffer(ModRenderLayers.additive(TEXTURE));
			model.render(matrices, ghost, FULL_LIGHT, OverlayTexture.DEFAULT_UV, r(GOLD), g(GOLD), b(GOLD), 0.45F * glow * alpha);
		}
		matrices.pop();
		super.render(entity, entityYaw, tickDelta, matrices, vcp, light);
	}

	@Override
	public Identifier getTexture(HeavenSwordEntity entity) {
		return TEXTURE;
	}
}
