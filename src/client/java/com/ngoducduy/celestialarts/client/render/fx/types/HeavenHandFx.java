package com.ngoducduy.celestialarts.client.render.fx.types;

import com.ngoducduy.celestialarts.client.render.FxTextures;
import com.ngoducduy.celestialarts.client.render.RenderUtil;
import com.ngoducduy.celestialarts.client.render.entity.model.HeavenHandModel;
import com.ngoducduy.celestialarts.client.render.entity.model.ModModelLayers;
import com.ngoducduy.celestialarts.client.render.fx.ClientFx;
import com.ngoducduy.celestialarts.client.render.layer.ModRenderLayers;
import com.ngoducduy.celestialarts.network.FxData;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;

/**
 * Thiên Đạo Chi Thủ – the whole world-scale spectacle in one effect so every part stays in sync:
 *
 * <ol>
 *   <li><b>Khai Thiên</b> (0–80 ticks): a formation of eight trigrams unfolds in the sky 120 blocks
 *       above the target; the ground beneath darkens.</li>
 *   <li><b>Giáng Lâm</b> (80–240): the hand of the Heavenly Dao – {@code scale} blocks across –
 *       pushes through the formation on an arm of light and sinks, slowly at first, then faster and
 *       faster. Its shadow swallows the ground, a column of pressure links palm and earth, glyph
 *       rings spin around the wrist.</li>
 *   <li><b>Trấn Áp</b> (240): the palm slams flat. White-out, a shock ring racing outward at six
 *       blocks per tick to the edge of the domain, a wall of dust, a dust column at the centre.</li>
 *   <li><b>Quy Thiên</b> (270–330): the hand dissolves into rising light and the formation breaks.</li>
 * </ol>
 *
 * <p>{@code pos} = ground centre, {@code target} = horizontal direction the fingers point,
 * {@code scale} = hand width in blocks, {@code extra} = domain radius the shock ring travels,
 * {@code duration} = 330. All layers use the fog-less variants so the hand remains visible from
 * anywhere inside the domain.</p>
 */
public class HeavenHandFx extends ClientFx {
	public static final int T_SUMMON = 80;
	public static final int T_SLAM = 240;
	public static final int T_DISSOLVE = 270;
	public static final int T_END = 330;
	public static final float SKY_HEIGHT = 120.0F;
	public static final float START_HEIGHT = 140.0F;
	public static final float RING_SPEED = 6.0F;

	private static final int GOLD = 0xFFD86B;
	private static final int PALE = 0xFFF3CC;
	private static final int DUST = 0xC9A66B;
	private static final int SHADOW = 0x000000;

	private final float yawDeg;
	private final float handScale;
	private final float domain;
	private HeavenHandModel model;

	public HeavenHandFx(FxData data, ClientWorld world) {
		super(data, world);
		Vec3d d = data.target();
		this.yawDeg = d.lengthSquared() < 1.0E-6 ? 0.0F : (float) Math.toDegrees(Math.atan2(d.x, d.z));
		this.handScale = Math.max(1.0F, scale) / (HeavenHandModel.WIDTH_UNITS / 16.0F);
		this.domain = data.extra() > 0 ? data.extra() : Math.max(1.0F, scale) * 2.0F;
	}

	private HeavenHandModel model() {
		if (model == null) {
			model = new HeavenHandModel(MinecraftClient.getInstance().getEntityModelLoader().getModelPart(ModModelLayers.HEAVEN_HAND));
		}
		return model;
	}

	/** Height of the hand's origin above the ground at time {@code t}. */
	private float handHeight(float t) {
		float endHeight = HeavenHandModel.THICKNESS_UNITS / 16.0F * handScale * 0.32F + 2.0F;
		if (t < T_SUMMON) return START_HEIGHT;
		if (t >= T_SLAM) {
			if (t < T_DISSOLVE) {
				// A short shudder as the palm settles.
				float f = (t - T_SLAM) / (T_DISSOLVE - T_SLAM);
				return endHeight + 1.5F * (1.0F - f) * Math.abs(MathHelper.sin(t * 1.3F));
			}
			float f = MathHelper.clamp((t - T_DISSOLVE) / (float) (T_END - T_DISSOLVE), 0.0F, 1.0F);
			return endHeight + 60.0F * f * f;
		}
		float u = (t - T_SUMMON) / (float) (T_SLAM - T_SUMMON);
		return START_HEIGHT - (START_HEIGHT - endHeight) * (float) Math.pow(u, 2.4);
	}

	@Override
	public void render(MatrixStack matrices, VertexConsumerProvider consumers, Camera camera, float tickDelta) {
		float t = time(tickDelta);
		Vec3d o = origin(tickDelta);
		float w = Math.max(1.0F, scale);
		matrices.translate(o.x, o.y, o.z);

		renderFormation(matrices, consumers, t, w);
		renderGround(matrices, consumers, t, w);
		if (t >= T_SUMMON) renderHand(matrices, consumers, camera, t, w);
		if (t >= T_SLAM) renderSlam(matrices, consumers, t, w);
	}

	/** Eight-trigram formation in the sky, unfolding at the start and breaking after the slam. */
	private void renderFormation(MatrixStack matrices, VertexConsumerProvider consumers, float t, float w) {
		float in = RenderUtil.easeOutCubic(MathHelper.clamp(t / 40.0F, 0.0F, 1.0F));
		float out = 1.0F - RenderUtil.easeInCubic(MathHelper.clamp((t - (T_SLAM + 10)) / 70.0F, 0.0F, 1.0F));
		float env = Math.min(in, out);
		if (env <= 0.0F) return;
		float radius = Math.max(w * 0.9F, domain * 0.45F) * (0.6F + 0.4F * in) * (1.0F + 0.35F * (1.0F - out));
		matrices.push();
		matrices.translate(0.0F, SKY_HEIGHT, 0.0F);
		MatrixStack.Entry flat = matrices.peek();
		// Soft golden haze so the array reads against a bright sky as well as a dark one.
		VertexConsumer glow = consumers.getBuffer(ModRenderLayers.additiveFar(FxTextures.GLOW));
		RenderUtil.flatQuad(glow, flat, radius * 1.1F, GOLD, env * 0.015F);

		matrices.push();
		matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(t * 0.12F));
		VertexConsumer circle = consumers.getBuffer(ModRenderLayers.additiveCrispFar(FxTextures.CIRCLE_HEAVEN));
		RenderUtil.flatQuad(circle, matrices.peek(), radius, PALE, env * 0.9F);
		matrices.pop();
		// Counter-rotating glyph bands.
		VertexConsumer glyphs = consumers.getBuffer(ModRenderLayers.additiveCrispFar(FxTextures.GLYPHS));
		RenderUtil.annulus(glyphs, flat, radius * 0.86F, radius * 0.93F, 96, 48.0F, -t * 0.004F, PALE, env * 0.8F, env * 0.8F);
		RenderUtil.annulus(glyphs, flat, radius * 0.50F, radius * 0.56F, 96, 32.0F, t * 0.006F, GOLD, env * 0.7F, env * 0.7F);
		VertexConsumer ring = consumers.getBuffer(ModRenderLayers.additiveFar(FxTextures.RING));
		RenderUtil.flatQuad(ring, flat, radius * 1.04F, PALE, env * 0.25F);
		// Light bleeding down from the array.
		float veil = env * (t < T_SUMMON ? 0.03F : 0.015F);
		VertexConsumer beam = consumers.getBuffer(ModRenderLayers.additiveFar(FxTextures.BEAM));
		matrices.push();
		matrices.translate(0.0F, -SKY_HEIGHT, 0.0F);
		RenderUtil.cylinder(beam, matrices.peek(), radius * 0.55F, radius * 0.45F, SKY_HEIGHT, 48, 6.0F, 2.0F, -t * 0.01F, GOLD, veil * 0.3F, veil);
		matrices.pop();
		matrices.pop();
	}

	/** Shadow of the descending hand and the pressure column beneath the palm. */
	private void renderGround(MatrixStack matrices, VertexConsumerProvider consumers, float t, float w) {
		if (t < 10.0F) return;
		float u = MathHelper.clamp((t - T_SUMMON) / (float) (T_SLAM - T_SUMMON), 0.0F, 1.0F);
		float gone = 1.0F - MathHelper.clamp((t - T_DISSOLVE) / 40.0F, 0.0F, 1.0F);
		float dark = (0.10F + 0.35F * u) * gone;
		matrices.push();
		matrices.translate(0.0F, 0.06F, 0.0F);
		MatrixStack.Entry e = matrices.peek();
		VertexConsumer shadow = consumers.getBuffer(ModRenderLayers.translucentGlowFar(FxTextures.GLOW));
		RenderUtil.flatQuad(shadow, e, w * (0.40F + 0.12F * u), SHADOW, dark);
		if (t >= T_SUMMON && t < T_SLAM) {
			// Golden pressure pouring down from the palm onto the ground.
			VertexConsumer beam = consumers.getBuffer(ModRenderLayers.additiveFar(FxTextures.BEAM));
			float h = handHeight(t);
			RenderUtil.cylinder(beam, e, w * 0.34F, w * 0.30F, h, 40, 5.0F, 3.0F, -t * 0.08F, GOLD, 0.05F + 0.16F * u * u, 0.02F);
			VertexConsumer ring = consumers.getBuffer(ModRenderLayers.additiveFar(FxTextures.RING));
			// Rings of pressure tightening on the ground as the hand nears.
			for (int i = 0; i < 3; i++) {
				float f = (t * 0.02F + i / 3.0F) % 1.0F;
				RenderUtil.flatQuad(ring, e, w * (0.55F - 0.25F * f), GOLD, (0.10F + 0.25F * u) * (1.0F - f) * f * 4.0F);
			}
		}
		matrices.pop();
	}

	/** The hand itself: solid golden jade, an additive glow pass, wrist glyph rings and the arm of light. */
	private void renderHand(MatrixStack matrices, VertexConsumerProvider consumers, Camera camera, float t, float w) {
		float h = handHeight(t);
		float dissolve = MathHelper.clamp((t - T_DISSOLVE) / (float) (T_END - T_DISSOLVE), 0.0F, 1.0F);
		float appear = MathHelper.clamp((t - T_SUMMON) / 12.0F, 0.0F, 1.0F);
		float body = appear * (1.0F - dissolve);
		if (body <= 0.001F) return;
		float u = MathHelper.clamp((t - T_SUMMON) / (float) (T_SLAM - T_SUMMON), 0.0F, 1.0F);
		// Fingers relax on the way down, snap flat on impact, curl again while the hand rises.
		float curl;
		float tremble;
		if (t < T_SLAM) {
			curl = 0.55F - 0.25F * u;
			tremble = 0.4F + 0.6F * u;
		} else if (t < T_DISSOLVE) {
			curl = 0.05F;
			tremble = 1.0F - (t - T_SLAM) / 30.0F;
		} else {
			curl = 0.05F + 0.6F * dissolve;
			tremble = 0.0F;
		}
		HeavenHandModel m = model();
		m.pose(curl, tremble, t);

		matrices.push();
		matrices.translate(0.0F, h, 0.0F);
		matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(yawDeg + MathHelper.sin(t * 0.03F) * 1.5F));

		// Arm of light rising from the wrist into the sky formation.
		float armTop = SKY_HEIGHT + 40.0F - h;
		if (armTop > 5.0F) {
			matrices.push();
			matrices.translate(0.0F, 0.0F, -0.95F * handScale);
			VertexConsumer arm = consumers.getBuffer(ModRenderLayers.additiveFar(FxTextures.BEAM));
			RenderUtil.cylinder(arm, matrices.peek(), 0.42F * handScale, 0.30F * handScale, armTop, 40, 4.0F, 3.0F, t * 0.05F, GOLD, body * 0.07F, 0.0F);
			VertexConsumer core = consumers.getBuffer(ModRenderLayers.additiveFar(FxTextures.BEAM_CORE));
			RenderUtil.cylinder(core, matrices.peek(), 0.22F * handScale, 0.12F * handScale, armTop, 24, 1.0F, 4.0F, t * 0.09F, PALE, body * 0.06F, 0.0F);
			matrices.pop();
		}

		// Glyph rings spinning around the wrist.
		matrices.push();
		matrices.translate(0.0F, 0.0F, -0.98F * handScale);
		matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90.0F));
		VertexConsumer glyphs = consumers.getBuffer(ModRenderLayers.additiveCrispFar(FxTextures.GLYPHS));
		RenderUtil.annulus(glyphs, matrices.peek(), 0.62F * handScale, 0.70F * handScale, 64, 24.0F, t * 0.01F, PALE, body * 0.85F, body * 0.85F);
		RenderUtil.annulus(glyphs, matrices.peek(), 0.78F * handScale, 0.84F * handScale, 64, 32.0F, -t * 0.007F, GOLD, body * 0.7F, body * 0.7F);
		matrices.pop();

		// The hand model, in blocks (ModelPart divides cuboid units by 16 itself).
		matrices.push();
		matrices.scale(handScale, handScale, handScale);
		// Lit, opaque golden jade (vanilla entity shading gives the palm and fingers real volume);
		// only the dissolution thins it out.
		float solidA = appear * (1.0F - dissolve) * (1.0F - dissolve);
		VertexConsumer solid = consumers.getBuffer(ModRenderLayers.entityFar(FxTextures.HEAVEN_HAND));
		m.render(matrices, solid, 0xF000F0, OverlayTexture.DEFAULT_UV, 1.0F, 0.96F, 0.84F, solidA);
		// Dao seams and seals burn from within (emissive mask), pulsing faster as the palm nears the
		// ground; the whole hand only blazes as it returns to light.
		float pulse = 0.8F + 0.2F * MathHelper.sin(t * (0.25F + 0.5F * u));
		float seamA = body * (0.55F + 0.35F * u) * pulse;
		VertexConsumer seams = consumers.getBuffer(ModRenderLayers.additiveFar(FxTextures.HEAVEN_HAND_GLOW));
		matrices.push();
		matrices.scale(1.004F, 1.004F, 1.004F);
		m.render(matrices, seams, 0xF000F0, OverlayTexture.DEFAULT_UV, 1.0F, 0.95F, 0.75F, seamA);
		matrices.pop();
		float glowA = appear * dissolve * (1.0F - dissolve) * 0.6F;
		if (glowA > 0.003F) {
			VertexConsumer glow = consumers.getBuffer(ModRenderLayers.additiveFar(FxTextures.HEAVEN_HAND));
			matrices.scale(1.015F, 1.015F, 1.015F);
			m.render(matrices, glow, 0xF000F0, OverlayTexture.DEFAULT_UV, 1.0F, 0.9F, 0.6F, glowA);
		}
		matrices.pop();

		matrices.pop();

		// Motes of golden light falling from the fingertips to the ground (billboards, so they are
		// placed in the un-rotated frame and the finger offsets are rotated by hand).
		if (t < T_SLAM) {
			VertexConsumer mote = consumers.getBuffer(ModRenderLayers.additiveFar(FxTextures.SPARKLE));
			float yawRad = (float) Math.toRadians(yawDeg);
			float sin = MathHelper.sin(yawRad), cos = MathHelper.cos(yawRad);
			for (int i = 0; i < 6; i++) {
				float f = ((t * 0.03F) + i * (1.0F / 6.0F)) % 1.0F;
				float lx = (i % 4 - 1.5F) * 0.5F * handScale;
				float lz = (2.4F - (i / 4) * 3.2F) * handScale;
				float x = lx * cos + lz * sin;
				float z = -lx * sin + lz * cos;
				matrices.push();
				matrices.translate(x, h - 0.2F * handScale - f * (h - 0.2F * handScale), z);
				faceCamera(matrices, camera);
				RenderUtil.billboardQuad(mote, matrices.peek(), (0.03F + 0.02F * (1.0F - f)) * handScale, PALE, body * (1.0F - f) * f * 3.0F);
				matrices.pop();
			}
		}
	}

	/** Impact: white-out, shock ring + dust wall racing outward, dust column. */
	private void renderSlam(MatrixStack matrices, VertexConsumerProvider consumers, float t, float w) {
		float s = t - T_SLAM;
		matrices.push();
		matrices.translate(0.0F, 0.12F, 0.0F);
		MatrixStack.Entry e = matrices.peek();
		VertexConsumer glow = consumers.getBuffer(ModRenderLayers.additiveFar(FxTextures.GLOW));
		if (s < 16.0F) {
			float f = 1.0F - s / 16.0F;
			RenderUtil.flatQuad(glow, e, w * 0.6F, 0xFFFFFF, f * f * 0.5F);
		}
		float ringR = s * RING_SPEED;
		if (ringR < domain + 12.0F) {
			float f = MathHelper.clamp(ringR / domain, 0.0F, 1.0F);
			float a = (1.0F - f) * (1.0F - f);
			VertexConsumer ring = consumers.getBuffer(ModRenderLayers.additiveFar(FxTextures.RING));
			RenderUtil.flatQuad(ring, e, ringR * 1.12F, GOLD, a * 0.55F + 0.03F);
			RenderUtil.flatQuad(ring, e, ringR * 1.03F, 0xFFFFFF, a * 0.3F);
			// Wall of dust and light lifted by the shock.
			VertexConsumer wall = consumers.getBuffer(ModRenderLayers.translucentGlowFar(FxTextures.CLOUD));
			float wallH = 14.0F + 30.0F * (1.0F - f);
			RenderUtil.cylinder(wall, e, ringR * 0.90F, ringR * 1.05F, wallH, 96, 40.0F, 1.0F, s * 0.01F, DUST, (0.45F - 0.3F * f), 0.0F);
			VertexConsumer wallGlow = consumers.getBuffer(ModRenderLayers.additiveFar(FxTextures.GLOW));
			RenderUtil.cylinder(wallGlow, e, ringR * 0.93F, ringR * 1.0F, wallH * 0.6F, 96, 1.0F, 0.0F, GOLD, a * 0.16F, 0.0F);
		}
		// Slower second wave of pure light.
		float ring2 = s * 3.5F;
		if (ring2 < domain) {
			float f = ring2 / domain;
			VertexConsumer ring = consumers.getBuffer(ModRenderLayers.additiveFar(FxTextures.RING));
			RenderUtil.flatQuad(ring, e, ring2 * 1.1F, PALE, (1.0F - f) * 0.3F);
		}
		// Dust column at the centre of the print.
		float colLife = MathHelper.clamp(1.0F - s / 90.0F, 0.0F, 1.0F);
		if (colLife > 0.0F) {
			float rise = Math.min(1.0F, s / 25.0F);
			VertexConsumer cloud = consumers.getBuffer(ModRenderLayers.translucentGlowFar(FxTextures.CLOUD));
			RenderUtil.cylinder(cloud, e, w * 0.18F, w * (0.26F + 0.1F * (1.0F - colLife)), 70.0F * rise, 48, 8.0F, 2.0F, -s * 0.02F, DUST, colLife * 0.55F, 0.0F);
			RenderUtil.cylinder(glow, e, w * 0.16F, w * 0.22F, 40.0F * rise, 32, 1.0F, 0.0F, GOLD, colLife * colLife * 0.12F, 0.0F);
		}
		matrices.pop();
	}
}
