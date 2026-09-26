package com.ngoducduy.celestialarts.client.render.entity;

import com.ngoducduy.celestialarts.CelestialArts;
import com.ngoducduy.celestialarts.client.render.FxTextures;
import com.ngoducduy.celestialarts.client.render.RenderUtil;
import com.ngoducduy.celestialarts.client.render.entity.model.MeditationSeatModel;
import com.ngoducduy.celestialarts.client.render.entity.model.ModModelLayers;
import com.ngoducduy.celestialarts.client.render.layer.ModRenderLayers;
import com.ngoducduy.celestialarts.entity.MeditationSeatEntity;
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
 * The meditation cushion: a lit silk mat plus a soft, slowly turning rune ring in the colour of the
 * sitter's spirit root. The ring breathes with a 3-second period, like the cultivator's breath.
 */
public class MeditationSeatRenderer extends GlowEntityRenderer<MeditationSeatEntity> {
	private static final Identifier TEXTURE = CelestialArts.id("textures/entity/meditation_seat.png");
	private final MeditationSeatModel model;

	public MeditationSeatRenderer(EntityRendererFactory.Context ctx) {
		super(ctx);
		this.model = new MeditationSeatModel(ctx.getPart(ModModelLayers.MEDITATION_SEAT));
	}

	@Override
	public void render(MeditationSeatEntity entity, float entityYaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vcp, int light) {
		float t = entity.age + tickDelta;
		int color = entity.getColor();
		matrices.push();
		matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-entity.getYaw()));
		// Rune ring hovering just above the cushion.
		matrices.push();
		matrices.translate(0.0, 0.14, 0.0);
		matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(t * 0.6F));
		float breath = 0.22F + 0.10F * MathHelper.sin(t * (float) (Math.PI * 2.0 / 60.0));
		VertexConsumer ring = vcp.getBuffer(ModRenderLayers.additive(FxTextures.RING));
		RenderUtil.flatQuad(ring, matrices.peek(), 0.78F, color, breath);
		matrices.pop();

		matrices.push();
		applyModelFlip(matrices);
		VertexConsumer body = vcp.getBuffer(RenderLayer.getEntityCutoutNoCull(TEXTURE));
		model.render(matrices, body, light, OverlayTexture.DEFAULT_UV, 1.0F, 1.0F, 1.0F, 1.0F);
		matrices.pop();
		matrices.pop();
		super.render(entity, entityYaw, tickDelta, matrices, vcp, light);
	}

	@Override
	public Identifier getTexture(MeditationSeatEntity entity) {
		return TEXTURE;
	}
}
