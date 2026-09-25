package com.ngoducduy.celestialarts.client.render.entity;

import com.ngoducduy.celestialarts.CelestialArts;
import com.ngoducduy.celestialarts.client.render.FxTextures;
import com.ngoducduy.celestialarts.client.render.RenderUtil;
import com.ngoducduy.celestialarts.client.render.entity.model.DragonHeadModel;
import com.ngoducduy.celestialarts.client.render.entity.model.ModModelLayers;
import com.ngoducduy.celestialarts.client.render.layer.ModRenderLayers;
import com.ngoducduy.celestialarts.entity.WindDragonEntity;
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
 * Phong Long: a spinning three-layer funnel of wind (translucent cylinders with scrolling
 * streak texture) with the dragon's head riding the rim, circling the eye of the storm.
 */
public class WindDragonRenderer extends GlowEntityRenderer<WindDragonEntity> {
	private static final Identifier TEXTURE = CelestialArts.id("textures/entity/wind_dragon.png");
	private static final int COLOR = 0xB8FFD9;
	private static final int PALE = 0xF0FFF6;
	private final DragonHeadModel head;

	public WindDragonRenderer(EntityRendererFactory.Context ctx) {
		super(ctx);
		this.head = new DragonHeadModel(ctx.getPart(ModModelLayers.WIND_DRAGON));
	}

	@Override
	public void render(WindDragonEntity entity, float entityYaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vcp, int light) {
		float age = entity.age + tickDelta;
		float grow = MathHelper.clamp(age / 8.0F, 0.0F, 1.0F);
		float radius = (float) WindDragonEntity.FUNNEL_RADIUS * grow;
		float height = WindDragonEntity.FUNNEL_HEIGHT * (0.6F + 0.4F * grow);

		matrices.push();
		// Funnel body: a translucent emissive cone of churning air (visible against any sky)...
		VertexConsumer funnel = vcp.getBuffer(ModRenderLayers.translucentGlow(FxTextures.BEAM));
		matrices.push();
		matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(age * 18.0F));
		RenderUtil.cylinder(funnel, matrices.peek(), 0.35F, radius * 0.9F, height, 16, 2.0F, 1.5F, -age * 0.05F, COLOR, 0.55F * grow, 0.18F * grow);
		matrices.pop();
		// ...wrapped in three nested layers of additive wind streaks spinning at different speeds.
		VertexConsumer wind = vcp.getBuffer(ModRenderLayers.additive(FxTextures.WIND_BLADE));
		for (int layer = 0; layer < 3; layer++) {
			matrices.push();
			float spin = age * (26.0F + layer * 9.0F) * (layer == 1 ? -1.0F : 1.0F);
			matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(spin));
			float rb = 0.25F + layer * 0.12F;
			float rt = radius * (0.75F + layer * 0.14F);
			float h = height * (0.85F + layer * 0.08F);
			float a = (layer == 0 ? 0.9F : 0.5F) * grow;
			RenderUtil.cylinder(wind, matrices.peek(), rb, rt, h, 14, 3.0F, 2.0F, -age * 0.08F, layer == 0 ? PALE : COLOR, a, a * 0.15F);
			matrices.pop();
		}
		// Dust skirt at the base.
		VertexConsumer glow = vcp.getBuffer(ModRenderLayers.additive(FxTextures.GLOW));
		matrices.push();
		matrices.translate(0.0, 0.05, 0.0);
		matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-age * 12.0F));
		RenderUtil.annulus(glow, matrices.peek(), radius * 0.4F, radius * 1.4F, 20, 1.0F, 0.0F, COLOR, 0.6F * grow, 0.0F);
		matrices.pop();

		// Dragon head circling the upper rim, looking along its circular path.
		float orbit = age * 26.0F;
		matrices.push();
		matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(orbit));
		matrices.translate(radius * 0.75F, height * 0.72F + MathHelper.sin(age * 0.3F) * 0.15F, 0.0);
		// The head model faces -Z, which is exactly the tangent of a +Y rotation at this point.
		matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(18.0F));
		matrices.scale(2.2F, 2.2F, 2.2F);
		applyModelFlip(matrices);
		head.animate(age, 0.5F + 0.5F * MathHelper.sin(age * 0.5F));
		VertexConsumer body = vcp.getBuffer(RenderLayer.getEntityTranslucentEmissive(TEXTURE));
		head.render(matrices, body, FULL_LIGHT, OverlayTexture.DEFAULT_UV, 1.0F, 1.0F, 1.0F, 0.9F * grow);
		matrices.scale(1.25F, 1.25F, 1.25F);
		VertexConsumer ghost = vcp.getBuffer(ModRenderLayers.additive(TEXTURE));
		head.render(matrices, ghost, FULL_LIGHT, OverlayTexture.DEFAULT_UV, r(COLOR), g(COLOR), b(COLOR), 0.35F * grow);
		matrices.pop();
		matrices.pop();
		super.render(entity, entityYaw, tickDelta, matrices, vcp, light);
	}

	@Override
	public Identifier getTexture(WindDragonEntity entity) {
		return TEXTURE;
	}
}
