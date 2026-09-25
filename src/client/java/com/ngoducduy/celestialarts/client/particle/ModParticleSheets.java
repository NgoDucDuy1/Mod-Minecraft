package com.ngoducduy.celestialarts.client.particle;

import com.mojang.blaze3d.platform.GlStateManager;
import com.ngoducduy.celestialarts.client.render.post.GlowPass;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.particle.ParticleTextureSheet;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.texture.TextureManager;

/**
 * Extra particle texture sheets. {@link #ADDITIVE} renders particle-atlas sprites with additive
 * blending and no depth write – the standard look for energy / magic particles. It is appended to
 * {@code ParticleManager.PARTICLE_TEXTURE_SHEETS} by {@code ParticleManagerMixin}.
 */
public final class ModParticleSheets {
	public static final ParticleTextureSheet ADDITIVE = new ParticleTextureSheet() {
		@Override
		public void begin(BufferBuilder builder, TextureManager textureManager) {
			// Glow particles take part in the bloom pass like every other additive layer.
			GlowPass.bindGlow();
			RenderSystem.depthMask(false);
			RenderSystem.setShader(GameRenderer::getParticleProgram);
			RenderSystem.setShaderTexture(0, SpriteAtlasTexture.PARTICLE_ATLAS_TEXTURE);
			RenderSystem.enableBlend();
			RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
			builder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR_LIGHT);
		}

		@Override
		public void draw(Tessellator tessellator) {
			tessellator.draw();
			RenderSystem.defaultBlendFunc();
			RenderSystem.depthMask(true);
			GlowPass.bindMain();
		}

		@Override
		public String toString() {
			return "CELESTIALARTS_ADDITIVE";
		}
	};

	private ModParticleSheets() {
	}
}
