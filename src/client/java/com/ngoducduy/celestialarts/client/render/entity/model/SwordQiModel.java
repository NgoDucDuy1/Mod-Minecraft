package com.ngoducduy.celestialarts.client.render.entity.model;

import net.minecraft.client.model.*;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;

/**
 * Crescent sword-qi blade: a flat centre plate with two swept-back wings and a bright leading edge.
 * Model space: +Z = direction of travel, Y down (rendered with the usual (-1,-1,1) flip). Texture 64x32.
 */
public class SwordQiModel {
	private final ModelPart root;
	private final ModelPart wingLeft;
	private final ModelPart wingRight;

	public SwordQiModel(ModelPart root) {
		this.root = root;
		this.wingLeft = root.getChild("wing_left");
		this.wingRight = root.getChild("wing_right");
	}

	public static TexturedModelData getTexturedModelData() {
		ModelData data = new ModelData();
		ModelPartData root = data.getRoot();
		root.addChild("core", ModelPartBuilder.create().uv(0, 0).cuboid(-12.0F, -1.0F, -3.0F, 24.0F, 2.0F, 6.0F), ModelTransform.NONE);
		root.addChild("wing_left", ModelPartBuilder.create().uv(0, 8).cuboid(-12.0F, -1.0F, -2.0F, 12.0F, 2.0F, 4.0F), ModelTransform.of(-12.0F, 0.0F, 0.0F, 0.0F, -0.5F, 0.0F));
		root.addChild("wing_right", ModelPartBuilder.create().uv(32, 8).cuboid(0.0F, -1.0F, -2.0F, 12.0F, 2.0F, 4.0F), ModelTransform.of(12.0F, 0.0F, 0.0F, 0.0F, 0.5F, 0.0F));
		root.addChild("edge", ModelPartBuilder.create().uv(0, 14).cuboid(-13.0F, -0.5F, 3.0F, 26.0F, 1.0F, 1.0F), ModelTransform.NONE);
		return TexturedModelData.of(data, 64, 32);
	}

	/** Opens the wings slightly with age so the crescent "unfolds" after launch. */
	public void setAngles(float age) {
		float open = Math.min(1.0F, age / 6.0F);
		float spread = 0.9F - 0.4F * open;
		wingLeft.yaw = -spread;
		wingRight.yaw = spread;
	}

	public void render(MatrixStack matrices, VertexConsumer vc, int light, int overlay, float r, float g, float b, float a) {
		root.render(matrices, vc, light, overlay, r, g, b, a);
	}
}
