package com.ngoducduy.celestialarts.client.render.entity;

import com.ngoducduy.celestialarts.CelestialArts;
import com.ngoducduy.celestialarts.client.render.FxTextures;
import com.ngoducduy.celestialarts.client.render.RenderUtil;
import com.ngoducduy.celestialarts.client.render.entity.model.IceShardModel;
import com.ngoducduy.celestialarts.client.render.entity.model.ModModelLayers;
import com.ngoducduy.celestialarts.client.render.layer.ModRenderLayers;
import com.ngoducduy.celestialarts.entity.IceShardEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;

/** Spinning ice crystal with a frosty glow and a short mist trail. */
public class IceShardRenderer extends GlowEntityRenderer<IceShardEntity> {
	private static final Identifier TEXTURE = CelestialArts.id("textures/entity/ice_shard.png");
	private static final int COLOR = 0x9BE4FF;
	private final IceShardModel model;

	public IceShardRenderer(EntityRendererFactory.Context ctx) {
		super(ctx);
		this.model = new IceShardModel(ctx.getPart(ModModelLayers.ICE_SHARD));
	}

	@Override
	public void render(IceShardEntity entity, float entityYaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vcp, int light) {
		float age = entity.age + tickDelta;
		matrices.push();
		matrices.translate(0.0, entity.getHeight() * 0.5, 0.0);
		applyProjectileRotation(matrices, lerpYaw(entity, tickDelta), lerpPitch(entity, tickDelta));
		matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(age * 22.0F));

		matrices.push();
		matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-90.0F));
		VertexConsumer trail = vcp.getBuffer(ModRenderLayers.additive(FxTextures.GLOW));
		RenderUtil.cylinder(trail, matrices.peek(), 0.16F, 0.02F, 0.9F, 10, 1.0F, 0.0F, COLOR, 0.5F, 0.0F);
		matrices.pop();

		matrices.push();
		applyModelFlip(matrices);
		VertexConsumer body = vcp.getBuffer(RenderLayer.getEntityTranslucentEmissive(TEXTURE));
		model.render(matrices, body, FULL_LIGHT, OverlayTexture.DEFAULT_UV, 1.0F, 1.0F, 1.0F, 0.95F);
		matrices.scale(1.35F, 1.35F, 1.1F);
		VertexConsumer ghost = vcp.getBuffer(ModRenderLayers.additive(TEXTURE));
		model.render(matrices, ghost, FULL_LIGHT, OverlayTexture.DEFAULT_UV, r(COLOR), g(COLOR), b(COLOR), 0.4F);
		matrices.pop();
		matrices.pop();
		super.render(entity, entityYaw, tickDelta, matrices, vcp, light);
	}

	@Override
	public Identifier getTexture(IceShardEntity entity) {
		return TEXTURE;
	}
}
