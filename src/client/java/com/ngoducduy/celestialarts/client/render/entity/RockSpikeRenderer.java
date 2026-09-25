package com.ngoducduy.celestialarts.client.render.entity;

import com.ngoducduy.celestialarts.CelestialArts;
import com.ngoducduy.celestialarts.client.render.FxTextures;
import com.ngoducduy.celestialarts.client.render.RenderUtil;
import com.ngoducduy.celestialarts.client.render.entity.model.ModModelLayers;
import com.ngoducduy.celestialarts.client.render.entity.model.RockSpikeModel;
import com.ngoducduy.celestialarts.client.render.layer.ModRenderLayers;
import com.ngoducduy.celestialarts.entity.RockSpikeEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

/** Stone spike that thrusts out of the ground, lit by world light, with a brief earthen glow at its base. */
public class RockSpikeRenderer extends GlowEntityRenderer<RockSpikeEntity> {
	private static final Identifier TEXTURE = CelestialArts.id("textures/entity/rock_spike.png");
	private static final int COLOR = 0xF2D9A6;
	private final RockSpikeModel model;

	public RockSpikeRenderer(EntityRendererFactory.Context ctx) {
		super(ctx);
		this.model = new RockSpikeModel(ctx.getPart(ModModelLayers.ROCK_SPIKE));
	}

	@Override
	public void render(RockSpikeEntity entity, float entityYaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vcp, int light) {
		float rise = entity.getRise(tickDelta);
		if (rise <= 0.0F) return;
		float height = entity.getSpikeHeight();
		float yScale = height / RockSpikeModel.HEIGHT_BLOCKS;

		matrices.push();
		// Glow of disturbed earth at the base while the spike is rising.
		if (rise < 1.0F) {
			matrices.push();
			matrices.translate(0.0, 0.04, 0.0);
			VertexConsumer glow = vcp.getBuffer(ModRenderLayers.additive(FxTextures.RING));
			RenderUtil.flatQuad(glow, matrices.peek(), 0.9F + 0.4F * rise, COLOR, (1.0F - rise) * 0.7F);
			matrices.pop();
		}
		// Slide the whole spike up out of the ground.
		matrices.translate(0.0, -(1.0F - rise) * height, 0.0);
		matrices.scale(1.0F, yScale, 1.0F);
		applyModelFlip(matrices);
		model.setAngles((float) Math.toRadians(entity.getSpinSeed()));
		VertexConsumer body = vcp.getBuffer(RenderLayer.getEntityCutoutNoCull(TEXTURE));
		model.render(matrices, body, light, OverlayTexture.DEFAULT_UV, 1.0F, 1.0F, 1.0F, 1.0F);
		matrices.pop();
		super.render(entity, entityYaw, tickDelta, matrices, vcp, light);
	}

	@Override
	public Identifier getTexture(RockSpikeEntity entity) {
		return TEXTURE;
	}
}
