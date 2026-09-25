package com.ngoducduy.celestialarts.client.render.entity.model;

import net.minecraft.client.model.*;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;

/** Small spirit sword (blade along +Z). Texture 64x32. */
public class SpiritSwordModel {
	private final ModelPart root;

	public SpiritSwordModel(ModelPart root) {
		this.root = root;
	}

	public static TexturedModelData getTexturedModelData() {
		ModelData data = new ModelData();
		ModelPartData root = data.getRoot();
		root.addChild("blade", ModelPartBuilder.create().uv(0, 0).cuboid(-1.5F, -0.5F, 0.0F, 3.0F, 1.0F, 14.0F), ModelTransform.NONE);
		root.addChild("guard", ModelPartBuilder.create().uv(0, 16).cuboid(-3.0F, -1.0F, -1.0F, 6.0F, 2.0F, 2.0F), ModelTransform.NONE);
		root.addChild("handle", ModelPartBuilder.create().uv(16, 16).cuboid(-0.5F, -0.5F, -6.0F, 1.0F, 1.0F, 5.0F), ModelTransform.NONE);
		root.addChild("pommel", ModelPartBuilder.create().uv(34, 0).cuboid(-1.0F, -1.0F, -8.0F, 2.0F, 2.0F, 2.0F), ModelTransform.NONE);
		return TexturedModelData.of(data, 64, 32);
	}

	public void render(MatrixStack matrices, VertexConsumer vc, int light, int overlay, float r, float g, float b, float a) {
		root.render(matrices, vc, light, overlay, r, g, b, a);
	}
}
