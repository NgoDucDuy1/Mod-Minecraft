package com.ngoducduy.celestialarts.client.render.entity.model;

import net.minecraft.client.model.*;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;

/** Elongated ice crystal with a needle tip and two rotated facet plates. Texture 32x32. */
public class IceShardModel {
	private final ModelPart root;

	public IceShardModel(ModelPart root) {
		this.root = root;
	}

	public static TexturedModelData getTexturedModelData() {
		ModelData data = new ModelData();
		ModelPartData root = data.getRoot();
		root.addChild("body", ModelPartBuilder.create().uv(0, 0).cuboid(-1.0F, -1.0F, -5.0F, 2.0F, 2.0F, 10.0F), ModelTransform.NONE);
		root.addChild("tip", ModelPartBuilder.create().uv(0, 12).cuboid(-0.5F, -0.5F, 5.0F, 1.0F, 1.0F, 3.0F), ModelTransform.NONE);
		root.addChild("facet_a", ModelPartBuilder.create().uv(0, 16).cuboid(-2.0F, -0.5F, -3.0F, 4.0F, 1.0F, 6.0F), ModelTransform.rotation(0.0F, 0.0F, 0.7854F));
		root.addChild("facet_b", ModelPartBuilder.create().uv(0, 23).cuboid(-2.0F, -0.5F, -3.0F, 4.0F, 1.0F, 6.0F), ModelTransform.rotation(0.0F, 0.0F, -0.7854F));
		return TexturedModelData.of(data, 32, 32);
	}

	public void render(MatrixStack matrices, VertexConsumer vc, int light, int overlay, float r, float g, float b, float a) {
		root.render(matrices, vc, light, overlay, r, g, b, a);
	}
}
