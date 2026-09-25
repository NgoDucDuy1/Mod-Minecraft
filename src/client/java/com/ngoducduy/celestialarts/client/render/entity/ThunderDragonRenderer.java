package com.ngoducduy.celestialarts.client.render.entity;

import com.ngoducduy.celestialarts.CelestialArts;
import com.ngoducduy.celestialarts.client.render.FxTextures;
import com.ngoducduy.celestialarts.client.render.RenderUtil;
import com.ngoducduy.celestialarts.client.render.entity.model.DragonHeadModel;
import com.ngoducduy.celestialarts.client.render.entity.model.ModModelLayers;
import com.ngoducduy.celestialarts.client.render.layer.ModRenderLayers;
import com.ngoducduy.celestialarts.entity.ThunderDragonEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Lôi Long: horned head model at the front, followed by a body made of the entity's own
 * position history – a tapering purple ribbon with a white core and jittering side arcs.
 */
public class ThunderDragonRenderer extends GlowEntityRenderer<ThunderDragonEntity> {
	private static final Identifier TEXTURE = CelestialArts.id("textures/entity/thunder_dragon.png");
	private static final int PURPLE = 0xB57BFF;
	private static final int PALE = 0xF2E6FF;
	private final DragonHeadModel head;

	public ThunderDragonRenderer(EntityRendererFactory.Context ctx) {
		super(ctx);
		this.head = new DragonHeadModel(ctx.getPart(ModModelLayers.THUNDER_DRAGON));
	}

	@Override
	public void render(ThunderDragonEntity entity, float entityYaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vcp, int light) {
		float age = entity.age + tickDelta;
		Vec3d now = entity.getLerpedPos(tickDelta);
		Vec3d cam = this.dispatcher.camera.getPos();

		// ---- body ribbon in world space relative to the entity origin
		List<Vec3d> pts = new ArrayList<>();
		pts.add(Vec3d.ZERO);
		Deque<Vec3d> trail = entity.getTrail();
		for (Vec3d p : trail) pts.add(p.subtract(now));
		if (pts.size() < 2) {
			// Freshly spawned: fake a short tail behind the velocity.
			Vec3d v = entity.getVelocity();
			for (int i = 1; i <= 4; i++) pts.add(v.multiply(-i * 0.8));
		}
		Vec3d camRel = cam.subtract(now);
		VertexConsumer bolt = vcp.getBuffer(ModRenderLayers.lightning());
		Matrix4f m = matrices.peek().getPositionMatrix();
		int n = pts.size();
		for (int i = 0; i < n - 1; i++) {
			float t0 = (float) i / (n - 1);
			float t1 = (float) (i + 1) / (n - 1);
			Vec3d p0 = pts.get(i);
			Vec3d p1 = pts.get(i + 1);
			// Jitter every segment a little each frame so the body crackles.
			float j = 0.06F;
			Vec3d jit = new Vec3d(RenderUtil.hash(i, entity.age * 7) - 0.5F, RenderUtil.hash(i + 31, entity.age * 7) - 0.5F, RenderUtil.hash(i + 67, entity.age * 7) - 0.5F).multiply(j * (1 + i));
			p1 = p1.add(jit);
			float w0 = 0.9F * (1.0F - t0) + 0.1F;
			float w1 = 0.9F * (1.0F - t1) + 0.1F;
			float a0 = 0.85F * (1.0F - t0);
			float a1 = 0.85F * (1.0F - t1);
			RenderUtil.ribbon(bolt, m, p0, p1, camRel, w0 + w1, PURPLE, a0 * 0.6F, a1 * 0.6F);
			RenderUtil.ribbon(bolt, m, p0, p1, camRel, (w0 + w1) * 0.35F, PALE, a0, a1);
			// Side arcs on some segments.
			if (i % 3 == 1 && i < n - 2) {
				Vec3d mid = p0.add(p1).multiply(0.5);
				Vec3d off = new Vec3d(RenderUtil.hash(i, 5 + entity.age) - 0.5F, RenderUtil.hash(i, 9 + entity.age) - 0.5F, RenderUtil.hash(i, 13 + entity.age) - 0.5F).normalize().multiply(0.5 + 0.5 * (1 - t0));
				RenderUtil.ribbon(bolt, m, mid, mid.add(off), camRel, 0.08F, PALE, a0 * 0.8F, 0.0F);
			}
		}

		// ---- head
		matrices.push();
		matrices.translate(0.0, entity.getHeight() * 0.5, 0.0);
		applyProjectileRotation(matrices, lerpYaw(entity, tickDelta), lerpPitch(entity, tickDelta));
		// Model faces -Z; projectile convention travels along +Z.
		matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Y.rotationDegrees(180.0F));
		matrices.scale(2.4F, 2.4F, 2.4F);
		applyModelFlip(matrices);
		head.animate(age, 0.6F + 0.4F * MathHelper.sin(age * 0.9F));
		VertexConsumer body = vcp.getBuffer(RenderLayer.getEntityTranslucentEmissive(TEXTURE));
		head.render(matrices, body, FULL_LIGHT, OverlayTexture.DEFAULT_UV, 1.0F, 1.0F, 1.0F, 0.95F);
		matrices.scale(1.3F, 1.3F, 1.3F);
		VertexConsumer ghost = vcp.getBuffer(ModRenderLayers.additive(TEXTURE));
		head.render(matrices, ghost, FULL_LIGHT, OverlayTexture.DEFAULT_UV, r(PURPLE), g(PURPLE), b(PURPLE), 0.4F);
		matrices.pop();

		// Glow core at the head.
		matrices.push();
		matrices.translate(0.0, entity.getHeight() * 0.5, 0.0);
		matrices.multiply(this.dispatcher.getRotation());
		VertexConsumer glow = vcp.getBuffer(ModRenderLayers.additive(FxTextures.GLOW));
		RenderUtil.billboardQuad(glow, matrices.peek(), 1.3F + 0.15F * MathHelper.sin(age * 1.3F), PURPLE, 0.6F);
		RenderUtil.billboardQuad(glow, matrices.peek(), 0.45F, PALE, 0.7F);
		matrices.pop();

		super.render(entity, entityYaw, tickDelta, matrices, vcp, light);
	}

	@Override
	public Identifier getTexture(ThunderDragonEntity entity) {
		return TEXTURE;
	}
}
