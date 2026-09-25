package com.ngoducduy.celestialarts.client.render.post;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.ngoducduy.celestialarts.CelestialArts;
import net.fabricmc.fabric.api.client.rendering.v1.CoreShaderRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.GlUniform;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import org.lwjgl.opengl.GL11;

/**
 * Glow / bloom pass for every additive effect in the mod.
 *
 * <p>Additive layers (beams, rings, arrays, lightning, glow particles) do not draw straight into the
 * main framebuffer. They are drawn into a separate <em>glow buffer</em> that shares the world's depth
 * (copied right after the terrain), so occlusion still works. At the end of the world render the glow
 * buffer is blurred into three progressively smaller mip levels and everything is added back onto the
 * main image: the sharp glow plus wide, soft halos around it – real bloom, without a shader pack.</p>
 *
 * <p>Timeline per frame:</p>
 * <ol>
 *   <li>{@code BEFORE_ENTITIES}: clear glow buffer, copy depth from the main buffer.</li>
 *   <li>Entities / FX / particles: additive layers bind the glow buffer via {@link #bindGlow()}.</li>
 *   <li>{@code LAST}: blur, composite additively onto the main buffer.</li>
 * </ol>
 *
 * <p>Disabled automatically with Fabulous graphics (the world is rendered through vanilla's own
 * transparency post-processor there) or when the blur shader failed to load – in both cases the
 * additive layers simply draw into the main buffer as before.</p>
 */
public final class GlowPass {
	private static final int LEVELS = 3;
	/** Intensity of the sharp (un-blurred) glow when added back. */
	private static final float SHARP_GAIN = 0.85F;
	/** Intensity of each blurred level, widest last. */
	private static final float[] LEVEL_GAIN = {0.30F, 0.26F, 0.22F};

	private static ShaderProgram blurProgram;
	private static Framebuffer glow;
	private static final Framebuffer[] ping = new Framebuffer[LEVELS];
	private static final Framebuffer[] pong = new Framebuffer[LEVELS];
	private static boolean active;
	/** Global switch (autotest / debugging). */
	public static boolean enabled = true;

	private GlowPass() {
	}

	public static void init() {
		CoreShaderRegistrationCallback.EVENT.register(context ->
				context.register(CelestialArts.id("glow_blur"), VertexFormats.POSITION_TEXTURE, program -> blurProgram = program));
		WorldRenderEvents.BEFORE_ENTITIES.register(GlowPass::begin);
		WorldRenderEvents.LAST.register(GlowPass::composite);
	}

	/** True while the glow buffer is the intended target for additive geometry. */
	public static boolean isActive() {
		return active;
	}

	/** Render-target hook for additive layers: glow buffer while the pass is active, main otherwise. */
	public static void bindGlow() {
		if (active) {
			glow.beginWrite(false);
		} else {
			MinecraftClient.getInstance().getFramebuffer().beginWrite(false);
		}
	}

	public static void bindMain() {
		MinecraftClient.getInstance().getFramebuffer().beginWrite(false);
	}

	private static void begin(WorldRenderContext context) {
		active = false;
		MinecraftClient client = MinecraftClient.getInstance();
		if (!enabled || blurProgram == null || MinecraftClient.isFabulousGraphicsOrBetter()) return;
		Framebuffer main = client.getFramebuffer();
		if (main.textureWidth <= 0 || main.textureHeight <= 0) return;
		ensureBuffers(main.textureWidth, main.textureHeight);

		glow.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
		glow.clear(MinecraftClient.IS_SYSTEM_MAC);
		glow.copyDepthFrom(main);
		main.beginWrite(false);
		active = true;
	}

	private static void composite(WorldRenderContext context) {
		if (!active) return;
		active = false;
		MinecraftClient client = MinecraftClient.getInstance();
		Framebuffer main = client.getFramebuffer();

		RenderSystem.disableDepthTest();
		RenderSystem.depthMask(false);
		RenderSystem.disableCull();
		RenderSystem.disableBlend();

		// Separable blur down the mip chain: each level blurs the previous (smaller = wider halo).
		Framebuffer src = glow;
		for (int i = 0; i < LEVELS; i++) {
			blit(src, ping[i], 1.0F / src.textureWidth * 1.5F, 0.0F, 1.0F);
			blit(ping[i], pong[i], 0.0F, 1.0F / ping[i].textureHeight * 1.5F, 1.0F);
			src = pong[i];
		}

		// Add everything back onto the world image.
		main.beginWrite(true);
		RenderSystem.enableBlend();
		RenderSystem.blendFunc(GlStateManager.SrcFactor.ONE, GlStateManager.DstFactor.ONE);
		blitOnto(glow, SHARP_GAIN);
		for (int i = 0; i < LEVELS; i++) blitOnto(pong[i], LEVEL_GAIN[i]);

		RenderSystem.defaultBlendFunc();
		RenderSystem.disableBlend();
		RenderSystem.enableCull();
		RenderSystem.depthMask(true);
		RenderSystem.enableDepthTest();
		main.beginWrite(true);
	}

	/** Draws {@code src} through the blur shader into {@code dst} (binds {@code dst}). */
	private static void blit(Framebuffer src, Framebuffer dst, float dirX, float dirY, float gain) {
		dst.beginWrite(true);
		drawFullscreen(src, dirX, dirY, gain);
	}

	/** Draws {@code src} (no blur) onto whatever framebuffer is bound, with the current blend state. */
	private static void blitOnto(Framebuffer src, float gain) {
		drawFullscreen(src, 0.0F, 0.0F, gain);
	}

	private static void drawFullscreen(Framebuffer src, float dirX, float dirY, float gain) {
		ShaderProgram program = blurProgram;
		RenderSystem.setShader(() -> program);
		program.addSampler("DiffuseSampler", src.getColorAttachment());
		GlUniform dir = program.getUniform("BlurDir");
		if (dir != null) dir.set(dirX, dirY);
		GlUniform g = program.getUniform("Gain");
		if (g != null) g.set(gain);
		BufferBuilder bb = Tessellator.getInstance().getBuffer();
		bb.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
		bb.vertex(-1.0, -1.0, 0.0).texture(0.0F, 0.0F).next();
		bb.vertex(1.0, -1.0, 0.0).texture(1.0F, 0.0F).next();
		bb.vertex(1.0, 1.0, 0.0).texture(1.0F, 1.0F).next();
		bb.vertex(-1.0, 1.0, 0.0).texture(0.0F, 1.0F).next();
		BufferRenderer.drawWithGlobalProgram(bb.end());
	}

	private static void ensureBuffers(int width, int height) {
		if (glow != null && glow.textureWidth == width && glow.textureHeight == height) return;
		destroy();
		glow = new SimpleFramebuffer(width, height, true, MinecraftClient.IS_SYSTEM_MAC);
		glow.setTexFilter(GL11.GL_LINEAR);
		int w = width, h = height;
		for (int i = 0; i < LEVELS; i++) {
			w = Math.max(1, w / 2);
			h = Math.max(1, h / 2);
			ping[i] = new SimpleFramebuffer(w, h, false, MinecraftClient.IS_SYSTEM_MAC);
			pong[i] = new SimpleFramebuffer(w, h, false, MinecraftClient.IS_SYSTEM_MAC);
			ping[i].setTexFilter(GL11.GL_LINEAR);
			pong[i].setTexFilter(GL11.GL_LINEAR);
			ping[i].setClearColor(0, 0, 0, 0);
			pong[i].setClearColor(0, 0, 0, 0);
		}
	}

	private static void destroy() {
		if (glow != null) glow.delete();
		glow = null;
		for (int i = 0; i < LEVELS; i++) {
			if (ping[i] != null) ping[i].delete();
			if (pong[i] != null) pong[i].delete();
			ping[i] = null;
			pong[i] = null;
		}
	}
}
