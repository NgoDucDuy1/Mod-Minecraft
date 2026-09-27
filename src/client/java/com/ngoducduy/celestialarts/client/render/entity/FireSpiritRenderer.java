package com.ngoducduy.celestialarts.client.render.entity;

import com.ngoducduy.celestialarts.CelestialArts;
import com.ngoducduy.celestialarts.client.render.FxTextures;
import com.ngoducduy.celestialarts.client.render.RenderUtil;
import com.ngoducduy.celestialarts.client.render.entity.model.FireSpiritModel;
import com.ngoducduy.celestialarts.client.render.entity.model.ModModelLayers;
import com.ngoducduy.celestialarts.client.render.layer.ModRenderLayers;
import com.ngoducduy.celestialarts.entity.FireSpiritEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

/** Hỏa Linh: a bobbing flame core, colour tinted by its hỏa chủng, dimming once weakened enough to bind. */
public class FireSpiritRenderer extends GlowEntityRenderer<FireSpiritEntity> {
	private static final Identifier TEXTURE = CelestialArts.id("textures/entity/fire_spirit.png");
	private final FireSpiritModel model;

	public FireSpiritRenderer(EntityRendererFactory.Context ctx) {
		super(ctx);
		this.model = new FireSpiritModel(ctx.getPart(ModModelLayers.FIRE_SPIRIT));
		this.shadowRadius = 0.4F;
	}

	@Override
	public void render(FireSpiritEntity entity, float entityYaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vcp, int light) {
		float age = entity.age + tickDelta;
		int rgb = entity.getFlameTier().getRgb();
		boolean enraged = entity.isEnraged();
		float bob = 0.06F * MathHelper.sin(age * 0.12F);

		matrices.push();
		matrices.translate(0.0, entity.getHeight() * 0.55 + bob, 0.0);

		matrices.push();
		matrices.multiply(this.dispatcher.getRotation());
		VertexConsumer glow = vcp.getBuffer(ModRenderLayers.additive(FxTextures.GLOW));
		float pulse = 1.0F + 0.12F * MathHelper.sin(age * 0.5F);
		RenderUtil.billboardQuad(glow, matrices.peek(), 0.55F * pulse, rgb, enraged ? 0.9F : 0.55F);
		matrices.pop();

		applyLookRotation(matrices, lerpYaw(entity, tickDelta), 0.0F);
		matrices.push();
		applyModelFlip(matrices);
		float scale = 0.85F + 0.03F * entity.getFlameTier().getTier();
		matrices.scale(scale, scale, scale);
		model.setAngles(age, enraged);
		VertexConsumer body = vcp.getBuffer(RenderLayer.getEntityTranslucentEmissive(TEXTURE));
		float shade = entity.isWeakened() ? 0.55F : 1.0F;
		model.render(matrices, body, FULL_LIGHT, OverlayTexture.DEFAULT_UV, shade, shade, shade, 1.0F);
		matrices.pop();
		matrices.pop();

		super.render(entity, entityYaw, tickDelta, matrices, vcp, light);
	}

	@Override
	public Identifier getTexture(FireSpiritEntity entity) {
		return TEXTURE;
	}
}
