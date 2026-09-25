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

import net.minecraft.entity.Entity;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.RaycastContext;

/**
 * Continuous energy beam fired from the followed entity's eyes along its look direction.
 * {@code scale} = core radius, {@code extra} = max range in blocks. Ends where it hits a block.
 */
public class BeamFx extends ClientFx {
	public BeamFx(FxData data, ClientWorld world) {
		super(data, world);
	}

	@Override
	protected boolean stopsWithEntity() {
		return true;
	}

	@Override
	public void render(MatrixStack matrices, VertexConsumerProvider consumers, Camera camera, float tickDelta) {
		Entity e = entity();
		if (e == null) return;
		float t = time(tickDelta);
		float in = RenderUtil.easeOutCubic(Math.min(1.0F, t / 5.0F));
		float out = MathHelper.clamp((duration - t) / 4.0F, 0.0F, 1.0F);
		float env = Math.min(in, out);
		if (env <= 0.0F) return;

		double range = data.extra() > 0 ? data.extra() : 24.0;
		Vec3d eye = e.getCameraPosVec(tickDelta).add(0, -0.2, 0);
		Vec3d dir = e.getRotationVec(tickDelta);
		// Hand offset so the beam does not start inside the camera. In first person the sheath radius is
		// larger than the camera distance, so push the origin out to the hand and taper the first blocks
		// instead of flooding the whole viewport with additive light.
		boolean firstPerson = camera.getFocusedEntity() == e && !camera.isThirdPerson();
		Vec3d right = dir.crossProduct(new Vec3d(0, 1, 0)).normalize();
		// The aim line always starts at the eyes so the beam lands where the crosshair points.
		Vec3d aimFrom = eye.add(dir.multiply(0.8)).add(right.multiply(0.15));
		Vec3d far = aimFrom.add(dir.multiply(range));
		HitResult hit = world.raycast(new RaycastContext(aimFrom, far, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, e));
		Vec3d end = hit.getType() == HitResult.Type.MISS ? far : hit.getPos();
		// In first person the beam is fired from the hand (bottom right of the view) and converges on the
		// aim point, like a hand-cast beam; looking straight down a parallel tube would flood the viewport.
		Vec3d from = firstPerson ? eye.add(dir.multiply(0.9)).add(right.multiply(0.45)).add(0, -0.42, 0) : aimFrom;
		if (firstPerson) dir = end.subtract(from).normalize();
		float length = (float) end.distanceTo(from);
		// Beam extends quickly on cast.
		length *= RenderUtil.easeOutQuint(Math.min(1.0F, t / 3.0F));
		if (length < 0.1F) return;

		float r = scale * env * (1.0F + 0.08F * MathHelper.sin(t * 1.7F));
		int white = RenderUtil.whiten(color, 0.9F);
		// A tube viewed nearly along its axis (first person, or a third-person camera behind the caster)
		// stacks all its additive layers into one saturated disc: shrink the muzzle flash, taper the
		// beam over a longer lead and use a narrower haze in that case.
		Vec3d toCam = camera.getPos().subtract(from);
		double along = toCam.lengthSquared() > 1.0e-4 ? Math.abs(toCam.normalize().dotProduct(dir)) : 1.0;
		boolean axial = firstPerson || along > 0.8;

		// Impact ring flush against the block face (drawn in world space before the beam transforms).
		if (hit instanceof net.minecraft.util.hit.BlockHitResult bhr && hit.getType() != HitResult.Type.MISS) {
			Vec3d n = Vec3d.of(bhr.getSide().getVector());
			VertexConsumer ringVc = consumers.getBuffer(ModRenderLayers.additive(FxTextures.RING));
			matrices.push();
			matrices.translate(end.x + n.x * 0.03, end.y + n.y * 0.03, end.z + n.z * 0.03);
			alignY(matrices, n);
			float ringR = r * (4.0F + 1.5F * MathHelper.sin(t * 1.1F));
			RenderUtil.flatQuad(ringVc, matrices.peek(), ringR, white, env * 0.5F);
			matrices.pop();
		}

		// Muzzle flash and impact flare (camera-facing billboards in world space).
		VertexConsumer glow = consumers.getBuffer(ModRenderLayers.additive(FxTextures.GLOW));
		matrices.push();
		matrices.translate(from.x, from.y, from.z);
		faceCamera(matrices, camera);
		RenderUtil.billboardQuad(glow, matrices.peek(), r * (axial ? 1.2F : 3.2F), white, env * (axial ? 0.25F : 0.6F));
		matrices.pop();
		Vec3d tip = from.add(dir.multiply(length));
		matrices.push();
		matrices.translate(tip.x, tip.y, tip.z);
		faceCamera(matrices, camera);
		float pulse = 1.0F + 0.15F * MathHelper.sin(t * 2.3F);
		// A beam that hits nothing just dissipates: only a faint tip, no impact flare (a full flare at max
		// range sits exactly on the crosshair in first person and blinds the caster).
		boolean missed = hit.getType() == HitResult.Type.MISS;
		float tipK = missed ? 0.35F : 1.0F;
		RenderUtil.billboardQuad(glow, matrices.peek(), r * 5.0F * pulse * tipK, white, env * (missed ? 0.3F : 0.8F));
		RenderUtil.billboardQuad(glow, matrices.peek(), r * 8.0F * pulse * tipK, color, env * (missed ? 0.15F : 0.35F));
		matrices.pop();

		matrices.translate(from.x, from.y, from.z);
		alignY(matrices, dir);
		MatrixStack.Entry en = matrices.peek();

		VertexConsumer beam = consumers.getBuffer(ModRenderLayers.additive(FxTextures.BEAM));
		VertexConsumer core = consumers.getBuffer(ModRenderLayers.additive(FxTextures.BEAM_CORE));
		// Lead segment: the beam gathers from a thin point at the hand to full width over the first blocks.
		float lead = axial ? Math.min(4.0F, length) : Math.min(1.0F, length);
		float startK = axial ? 0.12F : 0.6F;
		float startA = axial ? 0.2F : 0.8F;
		float hazeK = axial ? 1.4F : 2.4F;
		if (lead > 0.05F) {
			RenderUtil.cylinder(beam, en, r * 1.05F * startK, r * 1.05F, lead, 16, 2.0F, lead / 3.0F, -t * 0.25F, color, env * 0.85F * startA, env * 0.85F);
			RenderUtil.cylinder(glow, en, r * hazeK * startK, r * hazeK, lead, 12, 1.0F, 0.0F, color, env * 0.22F * startA, env * 0.22F);
			RenderUtil.cylinder(core, en, r * 0.45F * startK, r * 0.45F, lead, 10, 1.0F, lead / 3.0F, -t * 0.6F, white, env * startA, env);
		}
		float rest = length - lead;
		if (rest > 0.05F) {
			matrices.translate(0.0, lead, 0.0);
			en = matrices.peek();
			float vRep = Math.max(1.0F, rest / 3.0F);
			// Outer energy sheath, scrolling.
			RenderUtil.cylinder(beam, en, r * 1.05F, r * 1.05F, rest, 16, 2.0F, vRep, -t * 0.25F, color, env * 0.85F, env * 0.85F);
			// Second sheath, offset & scrolling faster, rotating around the axis.
			matrices.push();
			matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(t * 9.0F));
			RenderUtil.cylinder(beam, matrices.peek(), r * 1.35F, r * 1.35F, rest, 16, 3.0F, vRep * 0.7F, -t * 0.45F, color, env * 0.35F, env * 0.35F);
			matrices.pop();
			// Wide soft haze.
			RenderUtil.cylinder(glow, en, r * hazeK, r * hazeK, rest, 12, 1.0F, 0.0F, color, env * 0.22F, env * 0.22F);
			// White-hot core.
			RenderUtil.cylinder(core, en, r * 0.45F, r * 0.45F, rest, 10, 1.0F, vRep, -t * 0.6F, white, env, env);
		}
	}
}
