package com.ngoducduy.celestialarts.client.render.entity;

import com.ngoducduy.celestialarts.CelestialArts;
import com.ngoducduy.celestialarts.client.render.FxTextures;
import com.ngoducduy.celestialarts.client.render.RenderUtil;
import com.ngoducduy.celestialarts.client.render.entity.model.FireLotusModel;
import com.ngoducduy.celestialarts.client.render.entity.model.ModModelLayers;
import com.ngoducduy.celestialarts.client.render.layer.ModRenderLayers;
import com.ngoducduy.celestialarts.entity.FireLotusEntity;
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
 * Fire lotus: petals open while charging in the caster's palm, then the whole flower spins
 * through the air trailing flame.
 */
public class FireLotusRenderer extends GlowEntityRenderer<FireLotusEntity> {
	private static final Identifier TEXTURE = CelestialArts.id("textures/entity/fire_lotus.png");
	private static final int COLOR = 0xFF7A1A;
	private static final int HEART = 0xFFE27A;
	private final FireLotusModel model;

	public FireLotusRenderer(EntityRendererFactory.Context ctx) {
		super(ctx);
		this.model = new FireLotusModel(ctx.getPart(ModModelLayers.FIRE_LOTUS));
	}

	@Override
	public void render(FireLotusEntity entity, float entityYaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vcp, int light) {
		float age = entity.age + tickDelta;
		float growth = entity.getGrowth();
		boolean charging = entity.isCharging();
		float size = 0.45F + 0.95F * growth;
		float spin = age * (charging ? 0.05F : 0.35F);

		matrices.push();
		matrices.translate(0.0, entity.getHeight() * 0.5, 0.0);
		// Heart glow (camera-facing, drawn before any rotation).
		matrices.push();
		matrices.multiply(this.dispatcher.getRotation());
		VertexConsumer glow = vcp.getBuffer(ModRenderLayers.additive(FxTextures.GLOW));
		float pulse = 1.0F + 0.12F * MathHelper.sin(age * 0.8F);
		RenderUtil.billboardQuad(glow, matrices.peek(), 0.45F * size * pulse, HEART, 0.8F);
		RenderUtil.billboardQuad(glow, matrices.peek(), 1.1F * size * pulse, COLOR, 0.35F);
		matrices.pop();
		if (charging) {
			applyLookRotation(matrices, lerpYaw(entity, tickDelta), 0.0F);
			// Bud faces forward, slightly tilted up towards the target.
			matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-70.0F));
		} else {
			applyProjectileRotation(matrices, lerpYaw(entity, tickDelta), lerpPitch(entity, tickDelta));
			matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-90.0F));
			// Flame trail behind (model +Y after the tilt points backwards).
			matrices.push();
			VertexConsumer trail = vcp.getBuffer(ModRenderLayers.additive(FxTextures.FLAME_COLUMN));
			RenderUtil.cylinder(trail, matrices.peek(), 0.5F * size, 0.05F, 2.2F * size, 12, 2.0F, -age * 0.15F, COLOR, 0.7F, 0.0F);
			matrices.pop();
		}
		matrices.scale(size, size, size);

		matrices.push();
		applyModelFlip(matrices);
		model.setAngles(growth, spin);
		VertexConsumer body = vcp.getBuffer(RenderLayer.getEntityTranslucentEmissive(TEXTURE));
		model.render(matrices, body, FULL_LIGHT, OverlayTexture.DEFAULT_UV, 1.0F, 1.0F, 1.0F, 1.0F);
		matrices.scale(1.12F, 1.12F, 1.12F);
		VertexConsumer ghost = vcp.getBuffer(ModRenderLayers.additive(TEXTURE));
		model.render(matrices, ghost, FULL_LIGHT, OverlayTexture.DEFAULT_UV, r(COLOR), g(COLOR), b(COLOR), 0.35F + 0.25F * growth);
		matrices.pop();

		matrices.pop();
		super.render(entity, entityYaw, tickDelta, matrices, vcp, light);
	}

	@Override
	public Identifier getTexture(FireLotusEntity entity) {
		return TEXTURE;
	}
}
