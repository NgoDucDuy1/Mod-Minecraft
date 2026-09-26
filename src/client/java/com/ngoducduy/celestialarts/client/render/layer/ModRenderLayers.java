package com.ngoducduy.celestialarts.client.render.layer;

import com.ngoducduy.celestialarts.CelestialArts;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderPhase;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import com.ngoducduy.celestialarts.client.render.FxTextures;
import com.ngoducduy.celestialarts.client.render.post.GlowPass;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.util.Identifier;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Map;
import net.minecraft.util.Util;

import java.util.function.Function;

/**
 * Custom render layers for skill effects.
 *
 * <p>Extends {@link RenderLayer} purely to gain access to the protected
 * {@link RenderPhase} constants and {@code MultiPhaseParameters}. Never instantiated.</p>
 *
 * <ul>
 *   <li>{@link #additive(Identifier)} – textured, additive (src alpha, one), fullbright, no depth
 *       write, no culling. Used for glows, beams, rings, magic circles.</li>
 *   <li>{@link #translucentGlow(Identifier)} – textured, normal alpha blending, fullbright,
 *       no depth write, no culling. Used for petals, cloud, crack decals.</li>
 *   <li>{@link #solidGlow(Identifier)} – like translucentGlow but with depth write, for
 *       ice spikes / domes that should occlude what's behind them.</li>
 *   <li>{@link #lightning()} – untextured additive layer for bolts (routed to the glow pass).</li>
 * </ul>
 */
public abstract class ModRenderLayers extends RenderLayer {
	private ModRenderLayers(String name, VertexFormat vertexFormat, VertexFormat.DrawMode drawMode, int expectedBufferSize, boolean hasCrumbling, boolean translucent, Runnable startAction, Runnable endAction) {
		super(name, vertexFormat, drawMode, expectedBufferSize, hasCrumbling, translucent, startAction, endAction);
	}

	/**
	 * Global gain applied to every additive draw. Additive light stacks without bound, so dense
	 * scenes (formation + swords + aura) used to blow out to pure white; 0.7 keeps highlights while
	 * leaving headroom for several overlapping layers.
	 */
	public static final float ADDITIVE_GAIN = 0.62F;

	/**
	 * Render target of every additive layer: the {@link GlowPass} buffer while the glow pass is active
	 * (so it gets bloomed), the main framebuffer otherwise.
	 */
	private static final Target GLOW_TARGET = new Target(CelestialArts.MOD_ID + "_glow_target", GlowPass::bindGlow, GlowPass::bindMain);

	private static final Function<Identifier, RenderLayer> ADDITIVE = Util.memoize(texture -> {
		MultiPhaseParameters params = MultiPhaseParameters.builder()
				.program(BEACON_BEAM_PROGRAM)
				.texture(new RenderPhase.Texture(texture, false, false))
				.transparency(LIGHTNING_TRANSPARENCY)
				.cull(DISABLE_CULLING)
				.lightmap(DISABLE_LIGHTMAP)
				.overlay(DISABLE_OVERLAY_COLOR)
				.writeMaskState(COLOR_MASK)
				.target(GLOW_TARGET)
				.build(false);
		RenderLayer inner = RenderLayer.of(CelestialArts.MOD_ID + "_additive", VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL,
				VertexFormat.DrawMode.QUADS, 256, false, true, params);
		return new GainLayer(CelestialArts.MOD_ID + "_additive_soft", inner, ADDITIVE_GAIN);
	});

	/**
	 * Additive layer drawn straight onto the main framebuffer, bypassing the bloom pass. For dense
	 * line-art (the sky formation of Thiên Đạo Chi Thủ seen from below) bloom fills the gaps between
	 * the lines with a milky haze; crisp geometry stays legible and only chosen accents glow.
	 */
	private static final Function<Identifier, RenderLayer> ADDITIVE_CRISP = Util.memoize(texture -> {
		MultiPhaseParameters params = MultiPhaseParameters.builder()
				.program(BEACON_BEAM_PROGRAM)
				.texture(new RenderPhase.Texture(texture, false, false))
				.transparency(LIGHTNING_TRANSPARENCY)
				.cull(DISABLE_CULLING)
				.lightmap(DISABLE_LIGHTMAP)
				.overlay(DISABLE_OVERLAY_COLOR)
				.writeMaskState(COLOR_MASK)
				.target(MAIN_TARGET)
				.build(false);
		RenderLayer inner = RenderLayer.of(CelestialArts.MOD_ID + "_additive_crisp", VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL,
				VertexFormat.DrawMode.QUADS, 256, false, true, params);
		return new GainLayer(CelestialArts.MOD_ID + "_additive_crisp_soft", inner, ADDITIVE_GAIN);
	});

	/** Wraps a layer and multiplies the shader colour while it is active (the core shaders honour ColorModulator). */
	private static final class GainLayer extends RenderLayer {
		GainLayer(String name, RenderLayer inner, float gain) {
			super(name, inner.getVertexFormat(), inner.getDrawMode(), inner.getExpectedBufferSize(), inner.hasCrumbling(), true,
					() -> {
						inner.startDrawing();
						RenderSystem.setShaderColor(gain, gain, gain, 1.0F);
					},
					() -> {
						RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
						inner.endDrawing();
					});
		}
	}

	private static final Function<Identifier, RenderLayer> TRANSLUCENT_GLOW = Util.memoize(texture -> {
		MultiPhaseParameters params = MultiPhaseParameters.builder()
				.program(BEACON_BEAM_PROGRAM)
				.texture(new RenderPhase.Texture(texture, false, false))
				.transparency(TRANSLUCENT_TRANSPARENCY)
				.cull(DISABLE_CULLING)
				.lightmap(DISABLE_LIGHTMAP)
				.overlay(DISABLE_OVERLAY_COLOR)
				.writeMaskState(COLOR_MASK)
				.build(false);
		return RenderLayer.of(CelestialArts.MOD_ID + "_translucent_glow", VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL,
				VertexFormat.DrawMode.QUADS, 256, false, true, params);
	});

	private static final Function<Identifier, RenderLayer> SOLID_GLOW = Util.memoize(texture -> {
		MultiPhaseParameters params = MultiPhaseParameters.builder()
				.program(BEACON_BEAM_PROGRAM)
				.texture(new RenderPhase.Texture(texture, false, false))
				.transparency(TRANSLUCENT_TRANSPARENCY)
				.cull(DISABLE_CULLING)
				.lightmap(DISABLE_LIGHTMAP)
				.overlay(DISABLE_OVERLAY_COLOR)
				.writeMaskState(ALL_MASK)
				.build(false);
		return RenderLayer.of(CelestialArts.MOD_ID + "_solid_glow", VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL,
				VertexFormat.DrawMode.QUADS, 256, false, true, params);
	});

	public static RenderLayer additive(Identifier texture) {
		return ADDITIVE.apply(texture);
	}

	/**
	 * Wraps a layer and pushes the fog planes out to infinity while it draws. The core shaders read
	 * {@code FogStart/FogEnd} from {@link RenderSystem} when the program is bound (i.e. at draw time,
	 * inside this layer's start/end actions), so world-scale effects such as the sky formation and the
	 * 110-block hand of Thiên Đạo Chi Thủ stay visible instead of dissolving into the distance fog
	 * 150-250 blocks away. Depth testing against terrain is untouched.
	 */
	private static final class FarLayer extends RenderLayer {
		private static float savedStart, savedEnd;

		FarLayer(String name, RenderLayer inner) {
			super(name, inner.getVertexFormat(), inner.getDrawMode(), inner.getExpectedBufferSize(), inner.hasCrumbling(), true,
					() -> {
						inner.startDrawing();
						savedStart = RenderSystem.getShaderFogStart();
						savedEnd = RenderSystem.getShaderFogEnd();
						RenderSystem.setShaderFogStart(1.0E6F);
						RenderSystem.setShaderFogEnd(1.0E7F);
					},
					() -> {
						RenderSystem.setShaderFogStart(savedStart);
						RenderSystem.setShaderFogEnd(savedEnd);
						inner.endDrawing();
					});
		}
	}

	private static final Function<Identifier, RenderLayer> ADDITIVE_FAR = Util.memoize(texture -> new FarLayer(CelestialArts.MOD_ID + "_additive_far", additive(texture)));
	private static final Function<Identifier, RenderLayer> TRANSLUCENT_GLOW_FAR = Util.memoize(texture -> new FarLayer(CelestialArts.MOD_ID + "_translucent_glow_far", translucentGlow(texture)));
	private static final Function<Identifier, RenderLayer> SOLID_GLOW_FAR = Util.memoize(texture -> new FarLayer(CelestialArts.MOD_ID + "_solid_glow_far", solidGlow(texture)));

	/** {@link #additive(Identifier)} without distance fog (world-scale effects). */
	public static RenderLayer additiveFar(Identifier texture) {
		return ADDITIVE_FAR.apply(texture);
	}

	/** {@link #translucentGlow(Identifier)} without distance fog. */
	public static RenderLayer translucentGlowFar(Identifier texture) {
		return TRANSLUCENT_GLOW_FAR.apply(texture);
	}

	/** {@link #solidGlow(Identifier)} without distance fog. */
	public static RenderLayer solidGlowFar(Identifier texture) {
		return SOLID_GLOW_FAR.apply(texture);
	}

	private static final Function<Identifier, RenderLayer> ADDITIVE_CRISP_FAR = Util.memoize(texture -> new FarLayer(CelestialArts.MOD_ID + "_additive_crisp_far", ADDITIVE_CRISP.apply(texture)));

	/** Additive without bloom (see {@link #ADDITIVE_CRISP}). */
	public static RenderLayer additiveCrisp(Identifier texture) {
		return ADDITIVE_CRISP.apply(texture);
	}

	/** {@link #additiveCrisp(Identifier)} without distance fog. */
	public static RenderLayer additiveCrispFar(Identifier texture) {
		return ADDITIVE_CRISP_FAR.apply(texture);
	}

	private static final Function<Identifier, RenderLayer> ENTITY_FAR = Util.memoize(texture -> new FarLayer(CelestialArts.MOD_ID + "_entity_far", RenderLayer.getEntityTranslucentCull(texture)));

	/**
	 * Vanilla lit entity layer (directional face shading, depth-writing, alpha-blended) without
	 * distance fog – for world-scale <em>solid</em> props such as the 110-block hand of heaven, which
	 * must read as a shaded object rather than a flat sheet of light.
	 */
	public static RenderLayer entityFar(Identifier texture) {
		return ENTITY_FAR.apply(texture);
	}

	public static RenderLayer translucentGlow(Identifier texture) {
		return TRANSLUCENT_GLOW.apply(texture);
	}

	public static RenderLayer solidGlow(Identifier texture) {
		return SOLID_GLOW.apply(texture);
	}

	/** Untextured additive quads (bolts, ribbons). Like vanilla's lightning layer but routed to the glow pass. */
	private static final RenderLayer LIGHTNING = RenderLayer.of(CelestialArts.MOD_ID + "_lightning", VertexFormats.POSITION_COLOR,
			VertexFormat.DrawMode.QUADS, 256, false, true, MultiPhaseParameters.builder()
					.program(LIGHTNING_PROGRAM)
					.writeMaskState(COLOR_MASK)
					.transparency(LIGHTNING_TRANSPARENCY)
					.cull(DISABLE_CULLING)
					.target(GLOW_TARGET)
					.build(false));

	public static RenderLayer lightning() {
		return LIGHTNING;
	}

	/**
	 * One {@link BufferBuilder} per effect layer, for a {@link VertexConsumerProvider.Immediate}.
	 * <p>
	 * An immediate provider with only a fallback buffer re-begins that single buffer every time a
	 * different layer is requested, so an effect that fetches two consumers and then draws with the
	 * first one silently pushes those vertices into the second layer (wrong texture / blend). With a
	 * dedicated buffer per layer the consumers stay valid until {@code draw()}.
	 */
	public static Map<RenderLayer, BufferBuilder> createFxBuffers() {
		Map<RenderLayer, BufferBuilder> map = new Object2ObjectLinkedOpenHashMap<>();
		// Lit solid props first: they write depth and everything luminous blends over them.
		map.put(entityFar(FxTextures.HEAVEN_HAND), new BufferBuilder(256));
		for (Field field : FxTextures.class.getFields()) {
			if (!Modifier.isStatic(field.getModifiers()) || field.getType() != Identifier.class) continue;
			Identifier texture;
			try {
				texture = (Identifier) field.get(null);
			} catch (IllegalAccessException e) {
				continue;
			}
			// Solid (depth-writing) layers first so translucent light blends over them, additive last.
			map.put(solidGlow(texture), new BufferBuilder(256));
		}
		for (Field field : FxTextures.class.getFields()) {
			if (!Modifier.isStatic(field.getModifiers()) || field.getType() != Identifier.class) continue;
			try {
				Identifier texture = (Identifier) field.get(null);
				map.put(translucentGlow(texture), new BufferBuilder(256));
			} catch (IllegalAccessException ignored) {
			}
		}
		for (Field field : FxTextures.class.getFields()) {
			if (!Modifier.isStatic(field.getModifiers()) || field.getType() != Identifier.class) continue;
			try {
				Identifier texture = (Identifier) field.get(null);
				map.put(additive(texture), new BufferBuilder(256));
			} catch (IllegalAccessException ignored) {
			}
		}
		// Fog-less variants, same ordering: solid, translucent, additive.
		for (Function<Identifier, RenderLayer> kind : java.util.List.of(ADDITIVE_CRISP, SOLID_GLOW_FAR, TRANSLUCENT_GLOW_FAR, ADDITIVE_FAR, ADDITIVE_CRISP_FAR)) {
			for (Field field : FxTextures.class.getFields()) {
				if (!Modifier.isStatic(field.getModifiers()) || field.getType() != Identifier.class) continue;
				try {
					map.put(kind.apply((Identifier) field.get(null)), new BufferBuilder(256));
				} catch (IllegalAccessException ignored) {
				}
			}
		}
		map.put(lightning(), new BufferBuilder(256));
		return map;
	}
}
