package com.ngoducduy.celestialarts.client.render;

import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * Low level geometry helpers used by every effect renderer.
 *
 * <p>All methods emit QUADS into a {@link VertexConsumer} obtained from one of the
 * {@link com.ngoducduy.celestialarts.client.render.layer.ModRenderLayers}. Colors are packed
 * 0xRRGGBB ints plus a separate alpha in [0,1].</p>
 */
public final class RenderUtil {
	public static final int FULL_LIGHT = LightmapTextureManager.MAX_LIGHT_COORDINATE;
	private static final float TAU = (float) (Math.PI * 2.0);

	private RenderUtil() {
	}

	// ----------------------------------------------------------------- colors

	public static float red(int rgb) {
		return ((rgb >> 16) & 0xFF) / 255.0F;
	}

	public static float green(int rgb) {
		return ((rgb >> 8) & 0xFF) / 255.0F;
	}

	public static float blue(int rgb) {
		return (rgb & 0xFF) / 255.0F;
	}

	/** Linear interpolation between two packed colors. */
	public static int lerpColor(float t, int a, int b) {
		t = MathHelper.clamp(t, 0.0F, 1.0F);
		int r = (int) MathHelper.lerp(t, (a >> 16) & 0xFF, (b >> 16) & 0xFF);
		int g = (int) MathHelper.lerp(t, (a >> 8) & 0xFF, (b >> 8) & 0xFF);
		int bl = (int) MathHelper.lerp(t, a & 0xFF, b & 0xFF);
		return (r << 16) | (g << 8) | bl;
	}

	/** Brightens a color towards white by {@code amount} in [0,1]. */
	public static int whiten(int rgb, float amount) {
		return lerpColor(amount, rgb, 0xFFFFFF);
	}

	// ---------------------------------------------------------------- easing

	public static float easeOutCubic(float t) {
		t = MathHelper.clamp(t, 0.0F, 1.0F);
		float u = 1.0F - t;
		return 1.0F - u * u * u;
	}

	public static float easeOutQuint(float t) {
		t = MathHelper.clamp(t, 0.0F, 1.0F);
		float u = 1.0F - t;
		return 1.0F - u * u * u * u * u;
	}

	public static float easeInCubic(float t) {
		t = MathHelper.clamp(t, 0.0F, 1.0F);
		return t * t * t;
	}

	public static float easeInOutSine(float t) {
		t = MathHelper.clamp(t, 0.0F, 1.0F);
		return -(MathHelper.cos((float) Math.PI * t) - 1.0F) / 2.0F;
	}

	/** Fast attack, slow decay envelope: rises to 1 at {@code peak}, decays to 0 at 1. */
	public static float envelope(float t, float peak) {
		if (t <= 0.0F || t >= 1.0F) return 0.0F;
		if (t < peak) return easeOutCubic(t / peak);
		return 1.0F - easeInCubic((t - peak) / (1.0F - peak));
	}

	// -------------------------------------------------------------- vertices

	public static void vertex(VertexConsumer vc, MatrixStack.Entry e, float x, float y, float z, float u, float v, float r, float g, float b, float a) {
		vc.vertex(e.getPositionMatrix(), x, y, z)
				.color(r, g, b, a)
				.texture(u, v)
				.overlay(OverlayTexture.DEFAULT_UV)
				.light(FULL_LIGHT)
				.normal(e.getNormalMatrix(), 0.0F, 1.0F, 0.0F)
				.next();
	}

	public static void vertex(VertexConsumer vc, MatrixStack.Entry e, float x, float y, float z, float u, float v, int rgb, float a) {
		vertex(vc, e, x, y, z, u, v, red(rgb), green(rgb), blue(rgb), a);
	}

	/** Vertex for {@link net.minecraft.client.render.RenderLayer#getLightning()} (POSITION_COLOR). */
	public static void colorVertex(VertexConsumer vc, Matrix4f m, float x, float y, float z, int rgb, float a) {
		vc.vertex(m, x, y, z).color(red(rgb), green(rgb), blue(rgb), a).next();
	}

	// ---------------------------------------------------------------- shapes

	/**
	 * Flat square on the XZ plane centered at the origin, full texture mapped.
	 * Rendered double-sided by emitting both windings.
	 */
	public static void flatQuad(VertexConsumer vc, MatrixStack.Entry e, float half, int rgb, float a) {
		vertex(vc, e, -half, 0, -half, 0, 0, rgb, a);
		vertex(vc, e, -half, 0, half, 0, 1, rgb, a);
		vertex(vc, e, half, 0, half, 1, 1, rgb, a);
		vertex(vc, e, half, 0, -half, 1, 0, rgb, a);
	}

	/** Square on the XY plane (facing +Z / -Z). Used after the stack is rotated to face the camera. */
	public static void billboardQuad(VertexConsumer vc, MatrixStack.Entry e, float half, int rgb, float a) {
		vertex(vc, e, -half, -half, 0, 0, 1, rgb, a);
		vertex(vc, e, half, -half, 0, 1, 1, rgb, a);
		vertex(vc, e, half, half, 0, 1, 0, rgb, a);
		vertex(vc, e, -half, half, 0, 0, 0, rgb, a);
	}

	/** Rectangle on the XY plane, width along X, height along Y, bottom edge at y=0. */
	public static void verticalQuad(VertexConsumer vc, MatrixStack.Entry e, float halfWidth, float height, float v0, float v1, int rgb, float a) {
		vertex(vc, e, -halfWidth, 0, 0, 0, v1, rgb, a);
		vertex(vc, e, halfWidth, 0, 0, 1, v1, rgb, a);
		vertex(vc, e, halfWidth, height, 0, 1, v0, rgb, a);
		vertex(vc, e, -halfWidth, height, 0, 0, v0, rgb, a);
	}

	/**
	 * Open cylinder along +Y from y=0 to y=height. Texture U wraps around {@code uRepeats}
	 * times, V runs along height offset by {@code vScroll}. Radius may differ top/bottom to make cones.
	 * Alpha fades from {@code aBottom} to {@code aTop}.
	 */
	public static void cylinder(VertexConsumer vc, MatrixStack.Entry e, float rBottom, float rTop, float height, int segments,
	                            float uRepeats, float vScroll, int rgb, float aBottom, float aTop) {
		cylinder(vc, e, rBottom, rTop, height, segments, uRepeats, 1.0F, vScroll, rgb, aBottom, aTop);
	}

	/** Cylinder variant with explicit number of texture repeats along the height. */
	public static void cylinder(VertexConsumer vc, MatrixStack.Entry e, float rBottom, float rTop, float height, int segments,
	                            float uRepeats, float vRepeats, float vScroll, int rgb, float aBottom, float aTop) {
		float r = red(rgb), g = green(rgb), b = blue(rgb);
		for (int i = 0; i < segments; i++) {
			float a0 = i * TAU / segments;
			float a1 = (i + 1) * TAU / segments;
			float c0 = MathHelper.cos(a0), s0 = MathHelper.sin(a0);
			float c1 = MathHelper.cos(a1), s1 = MathHelper.sin(a1);
			float u0 = uRepeats * i / segments;
			float u1 = uRepeats * (i + 1) / segments;
			vertex(vc, e, c0 * rBottom, 0, s0 * rBottom, u0, vRepeats + vScroll, r, g, b, aBottom);
			vertex(vc, e, c1 * rBottom, 0, s1 * rBottom, u1, vRepeats + vScroll, r, g, b, aBottom);
			vertex(vc, e, c1 * rTop, height, s1 * rTop, u1, vScroll, r, g, b, aTop);
			vertex(vc, e, c0 * rTop, height, s0 * rTop, u0, vScroll, r, g, b, aTop);
		}
	}

	/**
	 * Flat annulus on the XZ plane. U wraps around the circumference, V from inner (0) to outer (1).
	 */
	public static void annulus(VertexConsumer vc, MatrixStack.Entry e, float rInner, float rOuter, int segments, float uRepeats,
	                           float uOffset, int rgb, float aInner, float aOuter) {
		float r = red(rgb), g = green(rgb), b = blue(rgb);
		for (int i = 0; i < segments; i++) {
			float a0 = i * TAU / segments;
			float a1 = (i + 1) * TAU / segments;
			float c0 = MathHelper.cos(a0), s0 = MathHelper.sin(a0);
			float c1 = MathHelper.cos(a1), s1 = MathHelper.sin(a1);
			float u0 = uOffset + uRepeats * i / segments;
			float u1 = uOffset + uRepeats * (i + 1) / segments;
			vertex(vc, e, c0 * rInner, 0, s0 * rInner, u0, 0, r, g, b, aInner);
			vertex(vc, e, c0 * rOuter, 0, s0 * rOuter, u0, 1, r, g, b, aOuter);
			vertex(vc, e, c1 * rOuter, 0, s1 * rOuter, u1, 1, r, g, b, aOuter);
			vertex(vc, e, c1 * rInner, 0, s1 * rInner, u1, 0, r, g, b, aInner);
		}
	}

	/**
	 * Partial arc band on the XZ plane between angles {@code start} and {@code end} (radians),
	 * from {@code rInner} to {@code rOuter}. U spans the arc (0..1), V spans inner..outer.
	 * Alpha is interpolated along the arc from {@code aStart} to {@code aEnd} so a slash can have a fading tail.
	 */
	public static void arcBand(VertexConsumer vc, MatrixStack.Entry e, float rInner, float rOuter, float start, float end, int segments,
	                           int rgb, float aStart, float aEnd) {
		float r = red(rgb), g = green(rgb), b = blue(rgb);
		for (int i = 0; i < segments; i++) {
			float t0 = (float) i / segments;
			float t1 = (float) (i + 1) / segments;
			float a0 = MathHelper.lerp(t0, start, end);
			float a1 = MathHelper.lerp(t1, start, end);
			float al0 = MathHelper.lerp(t0, aStart, aEnd);
			float al1 = MathHelper.lerp(t1, aStart, aEnd);
			float c0 = MathHelper.cos(a0), s0 = MathHelper.sin(a0);
			float c1 = MathHelper.cos(a1), s1 = MathHelper.sin(a1);
			vertex(vc, e, c0 * rInner, 0, s0 * rInner, t0, 1, r, g, b, al0);
			vertex(vc, e, c0 * rOuter, 0, s0 * rOuter, t0, 0, r, g, b, al0);
			vertex(vc, e, c1 * rOuter, 0, s1 * rOuter, t1, 0, r, g, b, al1);
			vertex(vc, e, c1 * rInner, 0, s1 * rInner, t1, 1, r, g, b, al1);
		}
	}

	/**
	 * UV sphere (or hemisphere when {@code hemisphere} is true → only y >= 0) centred at the origin.
	 * Texture wraps {@code uRepeats} around and {@code vRepeats} from pole to pole.
	 */
	public static void sphere(VertexConsumer vc, MatrixStack.Entry e, float radius, int rings, int segments, float uRepeats, float vRepeats,
	                          float uOffset, int rgb, float alpha, boolean hemisphere) {
		float r = red(rgb), g = green(rgb), b = blue(rgb);
		float latStart = hemisphere ? 0.0F : (float) (-Math.PI / 2.0);
		float latEnd = (float) (Math.PI / 2.0);
		for (int j = 0; j < rings; j++) {
			float lat0 = MathHelper.lerp((float) j / rings, latStart, latEnd);
			float lat1 = MathHelper.lerp((float) (j + 1) / rings, latStart, latEnd);
			float y0 = MathHelper.sin(lat0) * radius, y1 = MathHelper.sin(lat1) * radius;
			float rr0 = MathHelper.cos(lat0) * radius, rr1 = MathHelper.cos(lat1) * radius;
			float v0 = vRepeats * j / rings, v1 = vRepeats * (j + 1) / rings;
			for (int i = 0; i < segments; i++) {
				float a0 = i * TAU / segments, a1 = (i + 1) * TAU / segments;
				float c0 = MathHelper.cos(a0), s0 = MathHelper.sin(a0);
				float c1 = MathHelper.cos(a1), s1 = MathHelper.sin(a1);
				float u0 = uOffset + uRepeats * i / segments, u1 = uOffset + uRepeats * (i + 1) / segments;
				vertex(vc, e, c0 * rr0, y0, s0 * rr0, u0, v0, r, g, b, alpha);
				vertex(vc, e, c1 * rr0, y0, s1 * rr0, u1, v0, r, g, b, alpha);
				vertex(vc, e, c1 * rr1, y1, s1 * rr1, u1, v1, r, g, b, alpha);
				vertex(vc, e, c0 * rr1, y1, s0 * rr1, u0, v1, r, g, b, alpha);
			}
		}
	}

	/**
	 * Camera facing ribbon segment between two world-space points (already relative to the
	 * matrix origin). Used for lightning bolts and energy trails. Emits one quad.
	 */
	public static void ribbon(VertexConsumer vc, Matrix4f m, Vec3d p0, Vec3d p1, Vec3d camPos, float width, int rgb, float a0, float a1) {
		Vec3d dir = p1.subtract(p0);
		if (dir.lengthSquared() < 1.0E-6) return;
		Vec3d toCam = camPos.subtract(p0.add(p1).multiply(0.5));
		Vec3d side = dir.crossProduct(toCam);
		if (side.lengthSquared() < 1.0E-6) side = dir.crossProduct(new Vec3d(0, 1, 0));
		side = side.normalize().multiply(width * 0.5);
		float r = red(rgb), g = green(rgb), b = blue(rgb);
		vc.vertex(m, (float) (p0.x - side.x), (float) (p0.y - side.y), (float) (p0.z - side.z)).color(r, g, b, a0).next();
		vc.vertex(m, (float) (p0.x + side.x), (float) (p0.y + side.y), (float) (p0.z + side.z)).color(r, g, b, a0).next();
		vc.vertex(m, (float) (p1.x + side.x), (float) (p1.y + side.y), (float) (p1.z + side.z)).color(r, g, b, a1).next();
		vc.vertex(m, (float) (p1.x - side.x), (float) (p1.y - side.y), (float) (p1.z - side.z)).color(r, g, b, a1).next();
	}

	/**
	 * Textured camera-facing ribbon segment (uses full texture height across the width and
	 * {@code u0..u1} along the length).
	 */
	public static void texturedRibbon(VertexConsumer vc, MatrixStack.Entry e, Vec3d p0, Vec3d p1, Vec3d camPos, float width,
	                                  float u0, float u1, int rgb, float a0, float a1) {
		Vec3d dir = p1.subtract(p0);
		if (dir.lengthSquared() < 1.0E-6) return;
		Vec3d toCam = camPos.subtract(p0.add(p1).multiply(0.5));
		Vec3d side = dir.crossProduct(toCam);
		if (side.lengthSquared() < 1.0E-6) side = dir.crossProduct(new Vec3d(0, 1, 0));
		side = side.normalize().multiply(width * 0.5);
		vertex(vc, e, (float) (p0.x - side.x), (float) (p0.y - side.y), (float) (p0.z - side.z), u0, 0, rgb, a0);
		vertex(vc, e, (float) (p0.x + side.x), (float) (p0.y + side.y), (float) (p0.z + side.z), u0, 1, rgb, a0);
		vertex(vc, e, (float) (p1.x + side.x), (float) (p1.y + side.y), (float) (p1.z + side.z), u1, 1, rgb, a1);
		vertex(vc, e, (float) (p1.x - side.x), (float) (p1.y - side.y), (float) (p1.z - side.z), u1, 0, rgb, a1);
	}

	/**
	 * Axis-aligned "cross" made of two perpendicular vertical quads – cheap volumetric look for
	 * spikes, crystals and sword silhouettes. Height along +Y.
	 */
	public static void crossQuads(VertexConsumer vc, MatrixStack.Entry e, float halfWidth, float height, int rgb, float a) {
		vertex(vc, e, -halfWidth, 0, 0, 0, 1, rgb, a);
		vertex(vc, e, halfWidth, 0, 0, 1, 1, rgb, a);
		vertex(vc, e, halfWidth, height, 0, 1, 0, rgb, a);
		vertex(vc, e, -halfWidth, height, 0, 0, 0, rgb, a);
		vertex(vc, e, 0, 0, -halfWidth, 0, 1, rgb, a);
		vertex(vc, e, 0, 0, halfWidth, 1, 1, rgb, a);
		vertex(vc, e, 0, height, halfWidth, 1, 0, rgb, a);
		vertex(vc, e, 0, height, -halfWidth, 0, 0, rgb, a);
	}

	/** Deterministic pseudo random in [0,1) from an integer seed – used so effects are stable across frames. */
	public static float hash(int seed) {
		int h = seed * 0x27D4EB2D;
		h ^= h >>> 15;
		h *= 0x165667B1;
		h ^= h >>> 13;
		return (h & 0xFFFFFF) / (float) 0x1000000;
	}

	public static float hash(int seed, int salt) {
		return hash(seed * 31 + salt * 7919);
	}
}
