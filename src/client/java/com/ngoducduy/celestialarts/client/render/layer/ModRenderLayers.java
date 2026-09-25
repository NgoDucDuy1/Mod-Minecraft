package com.ngoducduy.celestialarts.client.render.layer;

import com.ngoducduy.celestialarts.CelestialArts;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderPhase;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
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
 *   <li>{@link #lightning()} – vanilla untextured additive layer for bolts.</li>
 * </ul>
 */
public abstract class ModRenderLayers extends RenderLayer {
	private ModRenderLayers(String name, VertexFormat vertexFormat, VertexFormat.DrawMode drawMode, int expectedBufferSize, boolean hasCrumbling, boolean translucent, Runnable startAction, Runnable endAction) {
		super(name, vertexFormat, drawMode, expectedBufferSize, hasCrumbling, translucent, startAction, endAction);
	}

	private static final Function<Identifier, RenderLayer> ADDITIVE = Util.memoize(texture -> {
		MultiPhaseParameters params = MultiPhaseParameters.builder()
				.program(BEACON_BEAM_PROGRAM)
				.texture(new RenderPhase.Texture(texture, false, false))
				.transparency(LIGHTNING_TRANSPARENCY)
				.cull(DISABLE_CULLING)
				.lightmap(DISABLE_LIGHTMAP)
				.overlay(DISABLE_OVERLAY_COLOR)
				.writeMaskState(COLOR_MASK)
				.build(false);
		return RenderLayer.of(CelestialArts.MOD_ID + "_additive", VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL,
				VertexFormat.DrawMode.QUADS, 256, false, true, params);
	});

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

	public static RenderLayer translucentGlow(Identifier texture) {
		return TRANSLUCENT_GLOW.apply(texture);
	}

	public static RenderLayer solidGlow(Identifier texture) {
		return SOLID_GLOW.apply(texture);
	}

	public static RenderLayer lightning() {
		return RenderLayer.getLightning();
	}
}
