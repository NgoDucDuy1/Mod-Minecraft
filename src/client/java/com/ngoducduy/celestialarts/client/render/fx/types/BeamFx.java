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
		// Hand offset so the beam does not start inside the camera in first person.
		Vec3d right = dir.crossProduct(new Vec3d(0, 1, 0)).normalize();
		Vec3d from = eye.add(dir.multiply(0.8)).add(right.multiply(0.15));
		Vec3d far = from.add(dir.multiply(range));
		HitResult hit = world.raycast(new RaycastContext(from, far, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, e));
		Vec3d end = hit.getType() == HitResult.Type.MISS ? far : hit.getPos();
		float length = (float) end.distanceTo(from);
		// Beam extends quickly on cast.
		length *= RenderUtil.easeOutQuint(Math.min(1.0F, t / 3.0F));
		if (length < 0.1F) return;

		float r = scale * env * (1.0F + 0.08F * MathHelper.sin(t * 1.7F));
		int white = RenderUtil.whiten(color, 0.9F);

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
		RenderUtil.billboardQuad(glow, matrices.peek(), r * 3.2F, white, env * 0.6F);
		matrices.pop();
		Vec3d tip = from.add(dir.multiply(length));
		matrices.push();
		matrices.translate(tip.x, tip.y, tip.z);
		faceCamera(matrices, camera);
		float pulse = 1.0F + 0.15F * MathHelper.sin(t * 2.3F);
		RenderUtil.billboardQuad(glow, matrices.peek(), r * 5.0F * pulse, white, env * 0.8F);
		RenderUtil.billboardQuad(glow, matrices.peek(), r * 8.0F * pulse, color, env * 0.35F);
		matrices.pop();

		matrices.translate(from.x, from.y, from.z);
		alignY(matrices, dir);
		MatrixStack.Entry en = matrices.peek();

		VertexConsumer beam = consumers.getBuffer(ModRenderLayers.additive(FxTextures.BEAM));
		float vRep = Math.max(1.0F, length / 3.0F);
		// Outer energy sheath, scrolling.
		RenderUtil.cylinder(beam, en, r * 1.05F, r * 1.05F, length, 16, 2.0F, vRep, -t * 0.25F, color, env * 0.85F, env * 0.85F);
		// Second sheath, offset & scrolling faster, rotating around the axis.
		matrices.push();
		matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(t * 9.0F));
		RenderUtil.cylinder(beam, matrices.peek(), r * 1.35F, r * 1.35F, length, 16, 3.0F, vRep * 0.7F, -t * 0.45F, color, env * 0.35F, env * 0.35F);
		matrices.pop();
		// Wide soft haze.
		RenderUtil.cylinder(glow, en, r * 2.4F, r * 2.4F, length, 12, 1.0F, 0.0F, color, env * 0.22F, env * 0.22F);
		// White-hot core.
		VertexConsumer core = consumers.getBuffer(ModRenderLayers.additive(FxTextures.BEAM_CORE));
		RenderUtil.cylinder(core, en, r * 0.45F, r * 0.45F, length, 10, 1.0F, vRep, -t * 0.6F, white, env, env);

	}
}
