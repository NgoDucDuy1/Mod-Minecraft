package com.ngoducduy.celestialarts.client.render.fx;

import com.ngoducduy.celestialarts.client.render.RenderUtil;
import com.ngoducduy.celestialarts.network.FxData;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

/**
 * A single procedural world-space visual effect living on the client.
 *
 * <p>Sub-classes draw geometry in {@link #render}. The matrix stack handed to them is already
 * translated so that world coordinates can be used directly (camera offset applied by the manager).</p>
 */
public abstract class ClientFx {
	protected final FxData data;
	protected final ClientWorld world;
	protected final int seed;
	protected final int color;
	protected final float scale;
	protected final int duration;
	protected int age;
	/** Spawn yaw/pitch of the followed entity (degrees), captured on creation. */
	protected float yaw;
	protected float pitch;
	private Vec3d lastPos;
	private Vec3d pos;
	private boolean removed;

	protected ClientFx(FxData data, ClientWorld world) {
		this.data = data;
		this.world = world;
		this.seed = world.random.nextInt();
		this.color = data.color();
		this.scale = data.scale();
		this.duration = Math.max(1, data.duration());
		this.pos = data.pos();
		this.lastPos = this.pos;
		Entity e = entity();
		if (e != null) {
			this.yaw = e.getYaw();
			this.pitch = e.getPitch();
			this.pos = anchor(e);
			this.lastPos = this.pos;
		}
	}

	/** World-space anchor used when following an entity. Defaults to the entity's feet. */
	protected Vec3d anchor(Entity e) {
		return e.getPos();
	}

	@Nullable
	protected Entity entity() {
		if (data.entityId() < 0) return null;
		return world.getEntityById(data.entityId());
	}

	@Nullable
	protected LivingEntity living() {
		return entity() instanceof LivingEntity l ? l : null;
	}

	public boolean follows() {
		return data.entityId() >= 0;
	}

	/**
	 * Whether this effect should be skipped while the camera sits inside the followed entity
	 * (first person). Auras, pillars and rune orbits wrap around the body; seen from the inside
	 * they are a wall of light across the whole screen that hides the actual skill. Other viewers
	 * (and the third-person camera) still see them. Effects that project away from the body
	 * (beams, slashes, wind blades, ground circles) stay visible.
	 */
	public boolean hiddenInFirstPerson() {
		if (!follows()) return false;
		return switch (data.type()) {
			case QI_AURA, HEAVEN_PILLAR, RUNE_ORBIT -> true;
			default -> false;
		};
	}

	public void tick() {
		age++;
		Entity e = entity();
		if (e != null) {
			lastPos = pos;
			pos = anchor(e);
			if (e.isRemoved() && stopsWithEntity()) removed = true;
		}
		onTick();
	}

	protected void onTick() {
	}

	/** Whether the effect should vanish when the followed entity disappears. */
	protected boolean stopsWithEntity() {
		return true;
	}

	public boolean isDead() {
		return removed || age >= duration;
	}

	public void remove() {
		removed = true;
	}

	/** Interpolated origin for this frame. */
	protected Vec3d origin(float tickDelta) {
		Entity e = entity();
		if (e != null) {
			// Interpolate the entity's own position for perfect tracking.
			double x = MathHelper.lerp(tickDelta, e.prevX, e.getX());
			double y = MathHelper.lerp(tickDelta, e.prevY, e.getY());
			double z = MathHelper.lerp(tickDelta, e.prevZ, e.getZ());
			return anchor(e).subtract(e.getPos()).add(x, y, z);
		}
		return lastPos.lerp(pos, tickDelta);
	}

	/** Life progress in [0,1]. */
	protected float progress(float tickDelta) {
		return MathHelper.clamp((age + tickDelta) / duration, 0.0F, 1.0F);
	}

	/** Age in ticks including partial tick. */
	protected float time(float tickDelta) {
		return age + tickDelta;
	}

	/** Fade-in for the first {@code in} ticks and fade-out for the last {@code out} ticks. */
	protected float fade(float tickDelta, float in, float out) {
		float t = time(tickDelta);
		float a = 1.0F;
		if (in > 0) a = Math.min(a, t / in);
		if (out > 0) a = Math.min(a, (duration - t) / out);
		return MathHelper.clamp(a, 0.0F, 1.0F);
	}

	/**
	 * 0 while the camera is within {@code inner} blocks of {@code p}, 1 beyond {@code outer}. Used to
	 * fade out camera-facing sprites and wide shells that would otherwise fill the whole screen when the
	 * camera stands inside them (first person on the caster, third-person camera clipped into the ground).
	 */
	protected static float nearFade(Camera camera, Vec3d p, float inner, float outer) {
		float d = (float) camera.getPos().distanceTo(p);
		return MathHelper.clamp((d - inner) / Math.max(0.01F, outer - inner), 0.0F, 1.0F);
	}

	protected float rand(int salt) {
		return RenderUtil.hash(seed, salt);
	}

	/** The world-space unit direction of the followed entity's look, or the packet direction. */
	protected Vec3d lookDir(float tickDelta) {
		Entity e = entity();
		if (e != null) return e.getRotationVec(tickDelta);
		Vec3d d = data.target().subtract(data.pos());
		return d.lengthSquared() < 1.0E-6 ? new Vec3d(0, 0, 1) : d.normalize();
	}

	public FxData data() {
		return data;
	}

	/**
	 * Draw the effect.
	 *
	 * @param matrices  stack already translated by -cameraPos; use world coordinates.
	 * @param consumers provider yielding custom layers
	 * @param camera    active camera
	 * @param tickDelta partial tick
	 */
	public abstract void render(MatrixStack matrices, VertexConsumerProvider consumers, Camera camera, float tickDelta);

	/** Rotates the stack so +Y points along {@code dir}. */
	protected static void alignY(MatrixStack matrices, Vec3d dir) {
		double len = dir.length();
		if (len < 1.0E-6) return;
		Vec3d d = dir.multiply(1.0 / len);
		// RotationAxis.POSITIVE_Y is right-handed: R_y(a)·(0,0,1) = (sin a, 0, cos a), so the angle is
		// atan2(x, z) - NOT Minecraft's entity yaw atan2(-x, z), which would mirror the effect in X
		// (visible as arrays and beams skewing whenever the caster does not face ±Z).
		float yawDeg = (float) Math.toDegrees(Math.atan2(d.x, d.z));
		float pitchDeg = (float) Math.toDegrees(Math.acos(MathHelper.clamp(d.y, -1.0, 1.0)));
		matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Y.rotationDegrees(yawDeg));
		matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_X.rotationDegrees(pitchDeg));
	}

	/** Rotates the stack so +Z points along {@code dir} (for billboards / vertical quads). */
	protected static void alignZ(MatrixStack matrices, Vec3d dir) {
		double len = dir.length();
		if (len < 1.0E-6) return;
		Vec3d d = dir.multiply(1.0 / len);
		float yawDeg = (float) Math.toDegrees(Math.atan2(d.x, d.z)); // see alignY
		float pitchDeg = (float) -Math.toDegrees(Math.asin(MathHelper.clamp(d.y, -1.0, 1.0)));
		matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Y.rotationDegrees(yawDeg));
		matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_X.rotationDegrees(pitchDeg));
	}

	/** Rotates the stack so the XY plane faces the camera (billboard). */
	protected static void faceCamera(MatrixStack matrices, Camera camera) {
		matrices.multiply(camera.getRotation());
	}
}
