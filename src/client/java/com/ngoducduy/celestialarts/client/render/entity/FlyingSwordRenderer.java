package com.ngoducduy.celestialarts.client.render.entity;

import com.ngoducduy.celestialarts.CelestialArts;
import com.ngoducduy.celestialarts.client.render.FxTextures;
import com.ngoducduy.celestialarts.client.render.RenderUtil;
import com.ngoducduy.celestialarts.client.render.entity.model.FlyingSwordModel;
import com.ngoducduy.celestialarts.client.render.entity.model.ModModelLayers;
import com.ngoducduy.celestialarts.client.render.layer.ModRenderLayers;
import com.ngoducduy.celestialarts.entity.FlyingSwordEntity;
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
 * Rideable flying sword: lit metal body (real object), emissive rune fuller, banking on turns and a
 * speed-dependent qi wake behind the blade.
 */
public class FlyingSwordRenderer extends GlowEntityRenderer<FlyingSwordEntity> {
	private static final Identifier TEXTURE = CelestialArts.id("textures/entity/flying_sword.png");
	private static final Identifier GLOW_TEXTURE = CelestialArts.id("textures/entity/flying_sword_glow.png");
	private static final int COLOR = 0x9FE8FF;
	private final FlyingSwordModel model;

	public FlyingSwordRenderer(EntityRendererFactory.Context ctx) {
		super(ctx);
		this.model = new FlyingSwordModel(ctx.getPart(ModModelLayers.FLYING_SWORD));
		this.shadowRadius = 0.5F;
	}

	@Override
	public void render(FlyingSwordEntity entity, float entityYaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vcp, int light) {
		float age = entity.age + tickDelta;
		float yaw = lerpYaw(entity, tickDelta);
		float pitch = lerpPitch(entity, tickDelta);
		float speed = entity.getSpeedFactor();
		// Bank into turns based on yaw change per tick.
		float yawDelta = MathHelper.wrapDegrees(entity.getYaw() - entity.prevYaw);
		float bank = MathHelper.clamp(-yawDelta * 2.5F, -35.0F, 35.0F);
		float hover = MathHelper.sin(age * 0.15F) * 0.03F * (1.0F - speed);

		matrices.push();
		matrices.translate(0.0, entity.getHeight() * 0.5 + hover, 0.0);
		applyLookRotation(matrices, yaw, pitch);
		matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(bank));
		float scale = 1.35F;
		matrices.scale(scale, scale, scale);

		// Qi wake behind the sword, grows with speed.
		if (speed > 0.05F) {
			matrices.push();
			matrices.translate(0.0, 0.0, -0.6);
			matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-90.0F));
			VertexConsumer trail = vcp.getBuffer(ModRenderLayers.additive(FxTextures.BEAM));
			RenderUtil.cylinder(trail, matrices.peek(), 0.35F, 0.05F, 1.2F + 2.5F * speed, 12, 2.0F, -age * 0.3F, COLOR, 0.55F * speed, 0.0F);
			matrices.pop();
		}

		matrices.push();
		applyModelFlip(matrices);
		// Real lit metal.
		VertexConsumer body = vcp.getBuffer(RenderLayer.getEntityCutoutNoCull(TEXTURE));
		model.render(matrices, body, light, OverlayTexture.DEFAULT_UV, 1.0F, 1.0F, 1.0F, 1.0F);
		// Emissive runes / edge (separate glow texture is mostly transparent).
		VertexConsumer runes = vcp.getBuffer(RenderLayer.getEntityTranslucentEmissive(GLOW_TEXTURE));
		float flick = 0.8F + 0.2F * MathHelper.sin(age * 0.4F);
		model.render(matrices, runes, FULL_LIGHT, OverlayTexture.DEFAULT_UV, 1.0F, 1.0F, 1.0F, flick);
		// Soft additive bloom around the blade.
		matrices.scale(1.08F, 1.5F, 1.03F);
		VertexConsumer ghost = vcp.getBuffer(ModRenderLayers.additive(GLOW_TEXTURE));
		model.render(matrices, ghost, FULL_LIGHT, OverlayTexture.DEFAULT_UV, r(COLOR), g(COLOR), b(COLOR), 0.3F + 0.3F * speed);
		matrices.pop();

		// Under-glow disc (sword riding light).
		matrices.push();
		matrices.translate(0.0, -0.08, 0.2);
		VertexConsumer glow = vcp.getBuffer(ModRenderLayers.additive(FxTextures.GLOW));
		RenderUtil.flatQuad(glow, matrices.peek(), 0.9F, COLOR, 0.25F + 0.2F * speed);
		matrices.pop();
		matrices.pop();
		super.render(entity, entityYaw, tickDelta, matrices, vcp, light);
	}

	@Override
	public Identifier getTexture(FlyingSwordEntity entity) {
		return TEXTURE;
	}
}
