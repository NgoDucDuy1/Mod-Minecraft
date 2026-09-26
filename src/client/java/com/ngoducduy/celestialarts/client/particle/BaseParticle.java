package com.ngoducduy.celestialarts.client.particle;

import net.minecraft.client.particle.ParticleTextureSheet;
import net.minecraft.client.particle.SpriteBillboardParticle;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

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

	/** Particles closer than this to the camera fade out instead of filling the screen as a blob. */
	protected static final double NEAR_FADE = 1.8;

	/**
	 * Own-body particles (the golden light around a channelling caster, sword glints, embers) end up
	 * centimetres from a first-person camera, where a 0.3-block sprite covers a quarter of the
	 * screen. They fade to nothing inside {@link #NEAR_FADE} blocks; the remaining alpha handling
	 * is untouched.
	 */
	@Override
	public void buildGeometry(VertexConsumer vertexConsumer, Camera camera, float tickDelta) {
		Vec3d cam = camera.getPos();
		double dx = MathHelper.lerp(tickDelta, this.prevPosX, this.x) - cam.x;
		double dy = MathHelper.lerp(tickDelta, this.prevPosY, this.y) - cam.y;
		double dz = MathHelper.lerp(tickDelta, this.prevPosZ, this.z) - cam.z;
		double d2 = dx * dx + dy * dy + dz * dz;
		if (d2 >= NEAR_FADE * NEAR_FADE) {
			super.buildGeometry(vertexConsumer, camera, tickDelta);
			return;
		}
		float near = (float) MathHelper.clamp((Math.sqrt(d2) - 0.5) / (NEAR_FADE - 0.5), 0.0, 1.0);
		if (near <= 0.01F) return;
		float saved = this.alpha;
		this.alpha = saved * near;
		super.buildGeometry(vertexConsumer, camera, tickDelta);
		this.alpha = saved;
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
