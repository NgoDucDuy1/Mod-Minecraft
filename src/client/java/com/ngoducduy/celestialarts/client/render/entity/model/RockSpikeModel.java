package com.ngoducduy.celestialarts.client.render.entity.model;

import net.minecraft.client.model.*;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;

/**
 * Jagged stone spike made of four stacked, twisted blocks. Ground level is y=0; the spike extends
 * upward (negative model Y). Total height 36 px = 2.25 blocks. Texture 64x64.
 */
public class RockSpikeModel {
	public static final float HEIGHT_BLOCKS = 2.25F;
	private final ModelPart root;
	private final ModelPart mid;
	private final ModelPart upper;
	private final ModelPart tip;

	public RockSpikeModel(ModelPart root) {
		this.root = root;
		this.mid = root.getChild("mid");
		this.upper = root.getChild("upper");
		this.tip = root.getChild("tip");
	}

	public static TexturedModelData getTexturedModelData() {
		ModelData data = new ModelData();
		ModelPartData root = data.getRoot();
		root.addChild("base", ModelPartBuilder.create().uv(0, 0).cuboid(-6.0F, -8.0F, -6.0F, 12.0F, 8.0F, 12.0F), ModelTransform.NONE);
		root.addChild("mid", ModelPartBuilder.create().uv(0, 20).cuboid(-4.0F, -18.0F, -4.0F, 8.0F, 10.0F, 8.0F), ModelTransform.rotation(0.0F, 0.5F, 0.0F));
		root.addChild("upper", ModelPartBuilder.create().uv(32, 20).cuboid(-2.5F, -28.0F, -2.5F, 5.0F, 10.0F, 5.0F), ModelTransform.rotation(0.0F, 1.0F, 0.0F));
		root.addChild("tip", ModelPartBuilder.create().uv(0, 38).cuboid(-1.0F, -36.0F, -1.0F, 2.0F, 8.0F, 2.0F), ModelTransform.rotation(0.0F, 1.5F, 0.0F));
		return TexturedModelData.of(data, 64, 64);
	}

	/** Twists the segments by a per-entity seed so no two spikes look identical. */
	public void setAngles(float seedRad) {
		mid.yaw = 0.5F + seedRad;
		upper.yaw = 1.0F + seedRad * 1.7F;
		tip.yaw = 1.5F + seedRad * 2.3F;
		mid.roll = 0.08F;
		upper.roll = -0.1F;
		tip.roll = 0.12F;
	}

	public void render(MatrixStack matrices, VertexConsumer vc, int light, int overlay, float r, float g, float b, float a) {
		root.render(matrices, vc, light, overlay, r, g, b, a);
	}
}
