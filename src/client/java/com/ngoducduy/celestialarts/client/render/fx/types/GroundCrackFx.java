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

import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

/**
 * Glowing fissure on the ground from {@code pos} towards {@code target}, with side branches.
 * Reveals along its length, glows, then cools down and fades. {@code scale} = width multiplier.
 */
public class GroundCrackFx extends ClientFx {
	private record Strand(List<Vec3d> points, float width) {
	}

	private final List<Strand> strands = new ArrayList<>();
	private final float totalLength;

	public GroundCrackFx(FxData data, ClientWorld world) {
		super(data, world);
		Vec3d a = data.pos();
		Vec3d b = data.target();
		Vec3d flat = new Vec3d(b.x - a.x, 0, b.z - a.z);
		this.totalLength = (float) flat.length();
		if (totalLength < 0.3F) return;
		Vec3d dir = flat.normalize();
		Vec3d side = new Vec3d(-dir.z, 0, dir.x);
		List<Vec3d> main = strand(a, dir, side, totalLength, 0.32F * (float) Math.min(1.5, totalLength / 5.0), seed);
		strands.add(new Strand(main, 0.55F * scale));
		int branches = 2 + (int) (rand(11) * 3);
		for (int i = 0; i < branches; i++) {
			int idx = 1 + (int) (rand(20 + i) * (main.size() - 2));
			Vec3d start = main.get(idx);
			double ang = (rand(40 + i) - 0.5) * Math.PI * 0.9 + (rand(60 + i) > 0.5 ? 0.6 : -0.6);
			Vec3d bd = new Vec3d(dir.x * Math.cos(ang) - dir.z * Math.sin(ang), 0, dir.x * Math.sin(ang) + dir.z * Math.cos(ang));
			Vec3d bs = new Vec3d(-bd.z, 0, bd.x);
			float len = totalLength * (0.15F + 0.25F * rand(80 + i));
			strands.add(new Strand(strand(start, bd, bs, len, 0.2F, seed + 97 * (i + 1)), 0.3F * scale));
		}
	}

	private List<Vec3d> strand(Vec3d start, Vec3d dir, Vec3d side, float length, float jitter, int s) {
		int n = Math.max(2, (int) (length / 0.6F));
		List<Vec3d> pts = new ArrayList<>(n + 1);
		for (int i = 0; i <= n; i++) {
			double t = (double) i / n;
			double off = i == 0 ? 0.0 : (RenderUtil.hash(s, i) - 0.5) * 2.0 * jitter;
			Vec3d p = start.add(dir.multiply(t * length)).add(side.multiply(off));
			pts.add(new Vec3d(p.x, groundY(p, start.y), p.z));
		}
		return pts;
	}

	/** Finds the top surface near the given point so cracks hug uneven terrain. */
	private double groundY(Vec3d p, double fallback) {
		BlockPos.Mutable m = new BlockPos.Mutable(MathHelper.floor(p.x), MathHelper.floor(fallback + 1.5), MathHelper.floor(p.z));
		for (int i = 0; i < 6; i++) {
			if (!world.getBlockState(m).getCollisionShape(world, m).isEmpty()) {
				double top = m.getY() + world.getBlockState(m).getCollisionShape(world, m).getMax(net.minecraft.util.math.Direction.Axis.Y);
				return top + 0.03;
			}
			m.move(0, -1, 0);
		}
		return fallback + 0.03;
	}

	@Override
	public void render(MatrixStack matrices, VertexConsumerProvider consumers, Camera camera, float tickDelta) {
		if (strands.isEmpty()) return;
		float p = progress(tickDelta);
		float reveal = RenderUtil.easeOutCubic(Math.min(1.0F, p / 0.18F));
		float heat = 1.0F - RenderUtil.easeInCubic(MathHelper.clamp((p - 0.35F) / 0.65F, 0.0F, 1.0F));
		if (heat <= 0.0F) return;
		float flicker = 0.9F + 0.1F * MathHelper.sin(time(tickDelta) * 1.3F + seed);
		int hot = RenderUtil.whiten(color, 0.5F);
		int cool = RenderUtil.lerpColor(0.6F, color, 0x3A2A1A);
		int rgb = RenderUtil.lerpColor(1.0F - heat, cool, hot);
		MatrixStack.Entry e = matrices.peek();
		VertexConsumer vc = consumers.getBuffer(ModRenderLayers.additive(FxTextures.CRACK));
		VertexConsumer glow = consumers.getBuffer(ModRenderLayers.additive(FxTextures.GLOW));

		for (int s = 0; s < strands.size(); s++) {
			Strand st = strands.get(s);
			List<Vec3d> pts = st.points();
			int visible = (int) Math.ceil((pts.size() - 1) * (s == 0 ? reveal : Math.max(0.0F, reveal * 1.3F - 0.3F)));
			for (int i = 0; i < Math.min(visible, pts.size() - 1); i++) {
				float f0 = (float) i / (pts.size() - 1);
				float f1 = (float) (i + 1) / (pts.size() - 1);
				float w0 = st.width() * (1.0F - f0 * 0.8F);
				float w1 = st.width() * (1.0F - f1 * 0.8F);
				segment(vc, e, pts.get(i), pts.get(i + 1), w0, w1, i, rgb, heat * flicker);
				// Soft glow bloom over the crack.
				segment(glow, e, pts.get(i), pts.get(i + 1), w0 * 3.0F, w1 * 3.0F, i, color, heat * 0.25F);
			}
		}
	}

	private static void segment(VertexConsumer vc, MatrixStack.Entry e, Vec3d a, Vec3d b, float w0, float w1, int i, int rgb, float alpha) {
		Vec3d d = new Vec3d(b.x - a.x, 0, b.z - a.z);
		if (d.lengthSquared() < 1.0E-6) return;
		Vec3d n = new Vec3d(-d.z, 0, d.x).normalize();
		float u0 = i * 0.5F;
		float u1 = u0 + 0.5F;
		RenderUtil.vertex(vc, e, (float) (a.x - n.x * w0), (float) a.y, (float) (a.z - n.z * w0), u0, 0, rgb, alpha);
		RenderUtil.vertex(vc, e, (float) (a.x + n.x * w0), (float) a.y, (float) (a.z + n.z * w0), u0, 1, rgb, alpha);
		RenderUtil.vertex(vc, e, (float) (b.x + n.x * w1), (float) b.y, (float) (b.z + n.z * w1), u1, 1, rgb, alpha);
		RenderUtil.vertex(vc, e, (float) (b.x - n.x * w1), (float) b.y, (float) (b.z - n.z * w1), u1, 0, rgb, alpha);
	}
}
