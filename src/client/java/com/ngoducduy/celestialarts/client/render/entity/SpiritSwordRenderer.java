package com.ngoducduy.celestialarts.client.render.entity;

import com.ngoducduy.celestialarts.CelestialArts;
import com.ngoducduy.celestialarts.client.render.FxTextures;
import com.ngoducduy.celestialarts.client.render.RenderUtil;
import com.ngoducduy.celestialarts.client.render.entity.model.ModModelLayers;
import com.ngoducduy.celestialarts.client.render.entity.model.SpiritSwordModel;
import com.ngoducduy.celestialarts.client.render.layer.ModRenderLayers;
import com.ngoducduy.celestialarts.entity.SpiritSwordEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

/** Spirit sword of the Thousand Swords technique: glowing blade with an aura and a launch trail. */
public class SpiritSwordRenderer extends GlowEntityRenderer<SpiritSwordEntity> {
	private static final Identifier TEXTURE = CelestialArts.id("textures/entity/spirit_sword.png");
	private static final int COLOR = 0x9FE8FF;
	private final SpiritSwordModel model;

	public SpiritSwordRenderer(EntityRendererFactory.Context ctx) {
		super(ctx);
		this.model = new SpiritSwordModel(ctx.getPart(ModModelLayers.SPIRIT_SWORD));
	}

	@Override
	public void render(SpiritSwordEntity entity, float entityYaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vcp, int light) {
		float age = entity.age + tickDelta;
		int phase = entity.getPhase();
		float appear = Math.min(1.0F, age / 6.0F);
		float scale = 1.15F * (0.3F + 0.7F * appear);

		matrices.push();
		matrices.translate(0.0, entity.getHeight() * 0.5, 0.0);
		// Aura halo.
		matrices.push();
		matrices.multiply(this.dispatcher.getRotation());
		VertexConsumer glow = vcp.getBuffer(ModRenderLayers.additive(FxTextures.GLOW));
		float pulse = 1.0F + 0.1F * MathHelper.sin(age * 0.6F + entity.getOrbitOffset());
		RenderUtil.billboardQuad(glow, matrices.peek(), 0.6F * pulse * appear, COLOR, 0.35F);
		matrices.pop();

		applyProjectileRotation(matrices, lerpYaw(entity, tickDelta), lerpPitch(entity, tickDelta));
		if (phase == SpiritSwordEntity.PHASE_ORBIT) {
			// Slow roll while orbiting so the blade catches the light.
			matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(MathHelper.sin(age * 0.12F + entity.getOrbitOffset()) * 25.0F));
		} else if (phase == SpiritSwordEntity.PHASE_LAUNCH) {
			matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(age * 30.0F));
			matrices.push();
			matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-90.0F));
			VertexConsumer trail = vcp.getBuffer(ModRenderLayers.additive(FxTextures.GLOW));
			RenderUtil.cylinder(trail, matrices.peek(), 0.2F, 0.02F, 1.6F, 10, 1.0F, 0.0F, COLOR, 0.6F, 0.0F);
			matrices.pop();
		}
		matrices.scale(scale, scale, scale);
		applyModelFlip(matrices);
		VertexConsumer body = vcp.getBuffer(RenderLayer.getEntityTranslucentEmissive(TEXTURE));
		model.render(matrices, body, FULL_LIGHT, OverlayTexture.DEFAULT_UV, 1.0F, 1.0F, 1.0F, 0.6F + 0.4F * appear);
		matrices.scale(1.5F, 1.5F, 1.06F);
		VertexConsumer ghost = vcp.getBuffer(ModRenderLayers.additive(TEXTURE));
		model.render(matrices, ghost, FULL_LIGHT, OverlayTexture.DEFAULT_UV, r(COLOR), g(COLOR), b(COLOR), 0.4F * appear);
		matrices.pop();
		super.render(entity, entityYaw, tickDelta, matrices, vcp, light);
	}

	@Override
	public Identifier getTexture(SpiritSwordEntity entity) {
		return TEXTURE;
	}
}
