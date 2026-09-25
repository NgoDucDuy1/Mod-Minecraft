package com.ngoducduy.celestialarts.client.particle;

import net.minecraft.client.particle.ParticleTextureSheet;
import net.minecraft.client.particle.SpriteBillboardParticle;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.MathHelper;

/**
 * Shared behaviour for all Celestial Arts particles: explicit velocity (vanilla's 6-arg constructor
 * randomises it), full-bright by default, and a smooth alpha envelope.
 */
public abstract class BaseParticle extends SpriteBillboardParticle {
	protected float fadeIn = 0.0F;
	protected float fadeOut = 0.4F;
	protected float baseScale;
	protected float baseAlpha = 1.0F;
	protected boolean fullBright = true;

	protected BaseParticle(ClientWorld world, double x, double y, double z, double vx, double vy, double vz) {
		super(world, x, y, z);
		this.velocityX = vx;
		this.velocityY = vy;
		this.velocityZ = vz;
		this.collidesWithWorld = false;
		this.gravityStrength = 0.0F;
		this.velocityMultiplier = 0.98F;
	}

	protected void setBaseScale(float scale) {
		this.baseScale = scale;
		this.scale = scale;
	}

	protected void setRGB(int rgb) {
		this.red = ((rgb >> 16) & 0xFF) / 255.0F;
		this.green = ((rgb >> 8) & 0xFF) / 255.0F;
		this.blue = (rgb & 0xFF) / 255.0F;
	}

	/** Life progress in [0,1]. */
	protected float life() {
		return MathHelper.clamp((float) this.age / (float) this.maxAge, 0.0F, 1.0F);
	}

	/** Alpha envelope: linear fade-in over {@code fadeIn} and fade-out over the last {@code fadeOut}. */
	protected float envelope() {
		float t = life();
		float a = 1.0F;
		if (fadeIn > 0.0F) a = Math.min(a, t / fadeIn);
		if (fadeOut > 0.0F) a = Math.min(a, (1.0F - t) / fadeOut);
		return MathHelper.clamp(a, 0.0F, 1.0F);
	}

	@Override
	public void tick() {
		super.tick();
		this.alpha = baseAlpha * envelope();
	}

	@Override
	public int getBrightness(float tint) {
		return fullBright ? LightmapTextureManager.MAX_LIGHT_COORDINATE : super.getBrightness(tint);
	}

	@Override
	public ParticleTextureSheet getType() {
		return ModParticleSheets.ADDITIVE;
	}
}
