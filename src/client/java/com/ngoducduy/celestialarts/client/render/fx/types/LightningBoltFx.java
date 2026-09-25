package com.ngoducduy.celestialarts.client.render.fx.types;

import com.ngoducduy.celestialarts.client.render.FxTextures;
import com.ngoducduy.celestialarts.client.render.RenderUtil;
import com.ngoducduy.celestialarts.client.render.fx.ClientFx;
import com.ngoducduy.celestialarts.client.render.layer.ModRenderLayers;
import com.ngoducduy.celestialarts.network.FxData;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

/**
 * Jagged, flickering lightning bolt from {@code pos} to {@code target}. Regenerates its path every
 * two ticks and has secondary branches. {@code scale} = width. {@code extra}: 0 default, 1 dash
 * (thin, horizontal), 2 tribulation, 3 final tribulation strike (thicker, longer flash).
 */
public class LightningBoltFx extends ClientFx {
	private final List<Vec3d[]> strands = new ArrayList<>();
	private final Vec3d from;
	private final Vec3d to;
	private int pathSeed;

	public LightningBoltFx(FxData data, ClientWorld world) {
		super(data, world);
		this.from = data.pos();
		this.to = data.target();
		this.pathSeed = seed;
		rebuild();
	}

	@Override
	protected void onTick() {
		if (age % 2 == 0) {
			pathSeed += 977;
			rebuild();
		}
	}

	private void rebuild() {
		strands.clear();
		Vec3d main = to.subtract(from);
		double len = main.length();
		if (len < 0.05) return;
		int segments = Math.max(4, (int) (len / 0.55));
		float jitter = (float) Math.min(1.2, 0.12 + len * 0.045) * (data.extra() == 1 ? 0.6F : 1.0F);
		Vec3d[] pts = buildStrand(from, to, segments, jitter, pathSeed);
		strands.add(pts);
		// Branches.
		int branches = data.extra() == 3 ? 5 : data.extra() == 2 ? 3 : 2;
		for (int b = 0; b < branches; b++) {
			int idx = 1 + (int) (RenderUtil.hash(pathSeed, 100 + b) * (segments - 2));
			Vec3d start = pts[idx];
			Vec3d dir = main.normalize().multiply(len * (0.15 + 0.2 * RenderUtil.hash(pathSeed, 200 + b)));
			Vec3d off = new Vec3d(RenderUtil.hash(pathSeed, 300 + b) - 0.5, RenderUtil.hash(pathSeed, 400 + b) - 0.5, RenderUtil.hash(pathSeed, 500 + b) - 0.5).multiply(len * 0.35);
			Vec3d end = start.add(dir).add(off);
			strands.add(buildStrand(start, end, Math.max(3, segments / 3), jitter * 0.7F, pathSeed + 31 * (b + 1)));
		}
	}

	private static Vec3d[] buildStrand(Vec3d a, Vec3d b, int segments, float jitter, int s) {
		Vec3d dir = b.subtract(a);
		Vec3d n1 = dir.crossProduct(Math.abs(dir.y) > 0.9 * dir.length() ? new Vec3d(1, 0, 0) : new Vec3d(0, 1, 0)).normalize();
		Vec3d n2 = dir.crossProduct(n1).normalize();
		Vec3d[] pts = new Vec3d[segments + 1];
		pts[0] = a;
		pts[segments] = b;
		for (int i = 1; i < segments; i++) {
			double t = (double) i / segments;
			double fall = Math.sin(t * Math.PI) * 0.5 + 0.5;
			double o1 = (RenderUtil.hash(s, i * 2) - 0.5) * 2.0 * jitter * fall;
			double o2 = (RenderUtil.hash(s, i * 2 + 1) - 0.5) * 2.0 * jitter * fall;
			pts[i] = a.add(dir.multiply(t)).add(n1.multiply(o1)).add(n2.multiply(o2));
		}
		return pts;
	}

	@Override
	public void render(MatrixStack matrices, VertexConsumerProvider consumers, Camera camera, float tickDelta) {
		float t = time(tickDelta);
		float env = t < 2.0F ? 1.0F : (float) Math.pow(1.0F - (t - 2.0F) / Math.max(1, duration - 2), 1.6);
		if (env <= 0.0F) return;
		float flicker = 0.75F + 0.25F * RenderUtil.hash(seed + age, 7);
		float width = scale * (data.extra() == 3 ? 1.5F : 1.0F);
		Vec3d cam = camera.getPos();
		VertexConsumer vc = consumers.getBuffer(ModRenderLayers.lightning());
		var m = matrices.peek().getPositionMatrix();
		int glowColor = color;
		int coreColor = RenderUtil.whiten(color, 0.85F);

		for (int s = 0; s < strands.size(); s++) {
			Vec3d[] pts = strands.get(s);
			float wMul = s == 0 ? 1.0F : 0.45F;
			float aMul = s == 0 ? 1.0F : 0.6F;
			for (int i = 0; i < pts.length - 1; i++) {
				RenderUtil.ribbon(vc, m, pts[i], pts[i + 1], cam, width * 0.55F * wMul, glowColor, env * 0.35F * aMul * flicker, env * 0.35F * aMul * flicker);
				RenderUtil.ribbon(vc, m, pts[i], pts[i + 1], cam, width * 0.16F * wMul, coreColor, env * aMul * flicker, env * aMul * flicker);
			}
		}

		// Impact flash + origin glow (billboards).
		VertexConsumer glow = consumers.getBuffer(ModRenderLayers.additive(FxTextures.GLOW));
		float flash = env * env;
		matrices.push();
		matrices.translate(to.x, to.y, to.z);
		faceCamera(matrices, camera);
		RenderUtil.billboardQuad(glow, matrices.peek(), (1.2F + width * 1.6F) * (data.extra() == 3 ? 1.6F : 1.0F), RenderUtil.whiten(color, 0.5F), flash * 0.9F);
		matrices.pop();
		matrices.push();
		matrices.translate(from.x, from.y, from.z);
		faceCamera(matrices, camera);
		RenderUtil.billboardQuad(glow, matrices.peek(), 0.6F + width, color, flash * 0.5F);
		matrices.pop();
	}
}
