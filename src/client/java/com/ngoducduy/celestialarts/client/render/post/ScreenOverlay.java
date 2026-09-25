package com.ngoducduy.celestialarts.client.render.post;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.ngoducduy.celestialarts.CelestialArts;
import com.ngoducduy.celestialarts.client.render.RenderUtil;
import com.ngoducduy.celestialarts.network.FxData;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import org.joml.Matrix4f;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Full-screen "cinematic" overlays driven by {@code FxType.SCREEN_FLASH}:
 * <ul>
 *   <li><b>flash</b> (extra 0): a coloured flash that fades out – lightning strikes, big impacts.</li>
 *   <li><b>darken</b> (extra 1): a dark vignette that fades in, holds for the duration and fades out –
 *       the sky "pressing down" during a heavenly tribulation.</li>
 * </ul>
 * Rendered from the HUD callback before the skill HUD, so bars and icons stay readable.
 */
public final class ScreenOverlay {
	private static final Identifier VIGNETTE = CelestialArts.id("textures/fx/vignette.png");
	private static final double RANGE = 24.0;
	private static final List<Entry> ACTIVE = new ArrayList<>();

	private ScreenOverlay() {
	}

	private static final class Entry {
		final int kind;
		final int color;
		final float strength;
		final int duration;
		float age;

		Entry(int kind, int color, float strength, int duration) {
			this.kind = kind;
			this.color = color;
			this.strength = strength;
			this.duration = Math.max(1, duration);
		}
	}

	public static void add(FxData data, MinecraftClient client) {
		float falloff = 1.0F;
		if (client.player != null) {
			double dist = client.player.getEyePos().distanceTo(data.pos());
			falloff = (float) MathHelper.clamp(1.0 - dist / RANGE, 0.0, 1.0);
			// Overlays are never fully lost inside the range: even a distant strike gives a hint.
			falloff = 0.15F + 0.85F * falloff;
		}
		float strength = MathHelper.clamp(data.scale() * falloff, 0.0F, 1.0F);
		if (strength <= 0.01F) return;
		if (ACTIVE.size() > 8) ACTIVE.remove(0);
		int kind = data.extra() == 1 ? 1 : 0;
		// Flashes are short by nature; clamp so a mis-specified one cannot white out the screen for long.
		int duration = kind == 0 ? Math.min(data.duration(), 24) : data.duration();
		ACTIVE.add(new Entry(kind, data.color(), strength, duration));
	}

	public static int count() {
		return ACTIVE.size();
	}

	public static void clear() {
		ACTIVE.clear();
	}

	public static void tick(MinecraftClient client) {
		if (client.isPaused()) return;
		Iterator<Entry> it = ACTIVE.iterator();
		while (it.hasNext()) {
			Entry e = it.next();
			e.age += 1.0F;
			if (e.age >= e.duration) it.remove();
		}
	}

	public static void render(DrawContext ctx, float tickDelta) {
		if (ACTIVE.isEmpty()) return;
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.options.hudHidden) return;
		int w = ctx.getScaledWindowWidth();
		int h = ctx.getScaledWindowHeight();

		float flashR = 0, flashG = 0, flashB = 0, flashA = 0;
		float darkA = 0;
		for (Entry e : ACTIVE) {
			float t = (e.age + tickDelta) / e.duration;
			if (e.kind == 0) {
				// Fast attack, cubic fade.
				float a = e.strength * (1.0F - t) * (1.0F - t);
				if (a > flashA) {
					flashA = a;
					flashR = RenderUtil.red(e.color);
					flashG = RenderUtil.green(e.color);
					flashB = RenderUtil.blue(e.color);
				}
			} else {
				float in = MathHelper.clamp(t * e.duration / 12.0F, 0.0F, 1.0F);
				float out = MathHelper.clamp((1.0F - t) * e.duration / 16.0F, 0.0F, 1.0F);
				darkA = Math.max(darkA, e.strength * Math.min(in, out));
			}
		}

		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		if (darkA > 0.005F) {
			// Uniform dimming plus a heavy vignette.
			int a = (int) (darkA * 105);
			ctx.fill(0, 0, w, h, (a << 24) | 0x0A0614);
			// DrawContext.fill() goes through the GUI render layer, whose end action disables blending
			// again - re-enable it or the vignette is drawn opaque (position_tex only discards alpha 0).
			RenderSystem.enableBlend();
			RenderSystem.defaultBlendFunc();
			RenderSystem.setShader(GameRenderer::getPositionTexProgram);
			RenderSystem.setShaderColor(0.16F, 0.09F, 0.30F, Math.min(1.0F, darkA * 0.75F));
			ctx.drawTexture(VIGNETTE, 0, 0, 0, 0, w, h, w, h);
			RenderSystem.setShaderColor(1, 1, 1, 1);
		}
		if (flashA > 0.005F) {
			// Additive flash keeps the world visible underneath (DrawContext.fill would force normal blending).
			Matrix4f m = ctx.getMatrices().peek().getPositionMatrix();
			RenderSystem.enableBlend();
			RenderSystem.setShader(GameRenderer::getPositionColorProgram);
			RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
			float a = Math.min(1.0F, flashA);
			BufferBuilder bb = Tessellator.getInstance().getBuffer();
			bb.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
			bb.vertex(m, 0, h, 0).color(flashR, flashG, flashB, a).next();
			bb.vertex(m, w, h, 0).color(flashR, flashG, flashB, a).next();
			bb.vertex(m, w, 0, 0).color(flashR, flashG, flashB, a).next();
			bb.vertex(m, 0, 0, 0).color(flashR, flashG, flashB, a).next();
			BufferRenderer.drawWithGlobalProgram(bb.end());
			RenderSystem.defaultBlendFunc();
		}
		RenderSystem.disableBlend();
	}
}
