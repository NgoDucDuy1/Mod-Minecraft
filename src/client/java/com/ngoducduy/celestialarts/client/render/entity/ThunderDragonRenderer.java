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
		if (pts.size() < ThunderDragonEntity.TRAIL_LENGTH) {
			// Freshly spawned: the serpent is already full length, coiled straight behind its heading.
			Vec3d v = entity.getVelocity();
			if (v.lengthSquared() < 1.0E-4) v = Vec3d.fromPolar(entity.getPitch(), entity.getYaw()).multiply(0.8);
			Vec3d last = pts.get(pts.size() - 1);
			int missing = ThunderDragonEntity.TRAIL_LENGTH - pts.size();
			for (int i = 1; i <= missing; i++) pts.add(last.add(v.multiply(-i)));
		}
		Vec3d camRel = cam.subtract(now);
		VertexConsumer bolt = vcp.getBuffer(ModRenderLayers.lightning());
		Matrix4f m = matrices.peek().getPositionMatrix();
		int n = pts.size();
		// Per-point displacement: a slow serpentine undulation plus a small per-frame crackle. Computing it
		// once per point (instead of per segment) keeps the body continuous.
		Vec3d heading = pts.size() > 1 ? pts.get(0).subtract(pts.get(1)) : entity.getVelocity();
		Vec3d[] basis = com.ngoducduy.celestialarts.util.SkillFx.basis(heading.lengthSquared() < 1.0E-6 ? new Vec3d(0, 0, 1) : heading.normalize());
		Vec3d[] body = new Vec3d[n];
		float[] widths = new float[n];
		float[] alphas = new float[n];
		int frame = entity.age / 2;
		Vec3d lift = new Vec3d(0, entity.getHeight() * 0.5, 0); // body runs through the head centre, not the feet
		for (int i = 0; i < n; i++) {
			float t = (float) i / (n - 1);
			double wave = Math.sin(i * 0.55 - age * 0.45) * 0.35 * t;
			double sway = Math.cos(i * 0.4 - age * 0.3) * 0.25 * t;
			double jx = (RenderUtil.hash(i, frame) - 0.5F) * 0.12 * (0.3 + t);
			double jy = (RenderUtil.hash(i + 31, frame) - 0.5F) * 0.12 * (0.3 + t);
			double jz = (RenderUtil.hash(i + 67, frame) - 0.5F) * 0.12 * (0.3 + t);
			body[i] = (i == 0 ? pts.get(0) : pts.get(i).add(basis[1].multiply(wave)).add(basis[0].multiply(sway)).add(jx, jy, jz)).add(lift);
			// Thick behind the head, tapering to a whip at the tail.
			float bulge = (float) Math.sin(Math.min(1.0, t * 4.0) * Math.PI * 0.5);
			widths[i] = (0.28F + 0.62F * bulge) * (1.0F - t * 0.85F);
			alphas[i] = 1.0F - t * t;
		}
		strip(bolt, m, body, widths, alphas, camRel, 1.6F, 0x3A0F80, 0.35F);
		strip(bolt, m, body, widths, alphas, camRel, 1.0F, PURPLE, 0.7F);
		strip(bolt, m, body, widths, alphas, camRel, 0.28F, PALE, 0.85F);
		// Crackling side arcs that jump off the body.
		for (int i = 2; i < n - 1; i += 3) {
			float t = (float) i / (n - 1);
			if (RenderUtil.hash(i, frame + 7) < 0.45F) continue;
			Vec3d root = body[i];
			Vec3d off = new Vec3d(RenderUtil.hash(i, 5 + frame) - 0.5F, RenderUtil.hash(i, 9 + frame) - 0.5F, RenderUtil.hash(i, 13 + frame) - 0.5F).normalize().multiply(0.6 + 0.7 * (1 - t));
			Vec3d mid = root.add(off.multiply(0.5)).add((RenderUtil.hash(i, 21 + frame) - 0.5F) * 0.3, (RenderUtil.hash(i, 23 + frame) - 0.5F) * 0.3, (RenderUtil.hash(i, 25 + frame) - 0.5F) * 0.3);
			RenderUtil.ribbon(bolt, m, root, mid, camRel, 0.1F, PALE, alphas[i] * 0.9F, alphas[i] * 0.6F);
			RenderUtil.ribbon(bolt, m, mid, root.add(off), camRel, 0.07F, PALE, alphas[i] * 0.6F, 0.0F);
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
		VertexConsumer headVc = vcp.getBuffer(RenderLayer.getEntityTranslucentEmissive(TEXTURE));
		head.render(matrices, headVc, FULL_LIGHT, OverlayTexture.DEFAULT_UV, 1.0F, 1.0F, 1.0F, 0.95F);
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

	/** Camera-facing continuous strip through {@code pts}; width and alpha are per point. */
	private static void strip(VertexConsumer vc, Matrix4f m, Vec3d[] pts, float[] widths, float[] alphas, Vec3d camRel, float widthScale, int rgb, float alphaScale) {
		int n = pts.length;
		if (n < 2) return;
		float r = RenderUtil.red(rgb), g = RenderUtil.green(rgb), b = RenderUtil.blue(rgb);
		Vec3d[] side = new Vec3d[n];
		for (int i = 0; i < n; i++) {
			Vec3d dir = (i == 0 ? pts[1].subtract(pts[0]) : pts[i].subtract(pts[i - 1]));
			if (i > 0 && i < n - 1) dir = dir.add(pts[i + 1].subtract(pts[i]));
			Vec3d toCam = camRel.subtract(pts[i]);
			Vec3d sd = dir.crossProduct(toCam);
			if (sd.lengthSquared() < 1.0E-8) sd = new Vec3d(0, 1, 0);
			side[i] = sd.normalize().multiply(widths[i] * widthScale * 0.5F);
		}
		for (int i = 0; i < n - 1; i++) {
			Vec3d a0 = pts[i].add(side[i]), a1 = pts[i].subtract(side[i]);
			Vec3d b0 = pts[i + 1].add(side[i + 1]), b1 = pts[i + 1].subtract(side[i + 1]);
			float aa = alphas[i] * alphaScale, ab = alphas[i + 1] * alphaScale;
			// Same winding as RenderUtil.ribbon (p0-s, p0+s, p1+s, p1-s) so culling behaves identically.
			vc.vertex(m, (float) a1.x, (float) a1.y, (float) a1.z).color(r, g, b, aa).next();
			vc.vertex(m, (float) a0.x, (float) a0.y, (float) a0.z).color(r, g, b, aa).next();
			vc.vertex(m, (float) b0.x, (float) b0.y, (float) b0.z).color(r, g, b, ab).next();
			vc.vertex(m, (float) b1.x, (float) b1.y, (float) b1.z).color(r, g, b, ab).next();
		}
	}

	@Override
	public Identifier getTexture(ThunderDragonEntity entity) {
		return TEXTURE;
	}
}
