package com.ngoducduy.celestialarts.client.render.entity.model;

import net.minecraft.client.model.*;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;

/** Rideable flying sword: wide blade with a raised fuller, guard, wrapped handle and pommel. Texture 64x64. */
public class FlyingSwordModel {
	private final ModelPart root;

	public FlyingSwordModel(ModelPart root) {
		this.root = root;
	}

	public static TexturedModelData getTexturedModelData() {
		ModelData data = new ModelData();
		ModelPartData root = data.getRoot();
		root.addChild("blade", ModelPartBuilder.create().uv(0, 0).cuboid(-3.0F, -0.5F, -4.0F, 6.0F, 1.0F, 26.0F), ModelTransform.NONE);
		root.addChild("fuller", ModelPartBuilder.create().uv(0, 27).cuboid(-1.0F, -1.0F, -2.0F, 2.0F, 2.0F, 20.0F), ModelTransform.NONE);
		root.addChild("guard", ModelPartBuilder.create().uv(0, 49).cuboid(-6.0F, -1.5F, -6.0F, 12.0F, 3.0F, 3.0F), ModelTransform.NONE);
		root.addChild("handle", ModelPartBuilder.create().uv(30, 49).cuboid(-1.0F, -1.0F, -12.0F, 2.0F, 2.0F, 6.0F), ModelTransform.NONE);
		root.addChild("pommel", ModelPartBuilder.create().uv(46, 49).cuboid(-1.5F, -1.5F, -14.0F, 3.0F, 3.0F, 2.0F), ModelTransform.NONE);
		return TexturedModelData.of(data, 64, 64);
	}

	public void render(MatrixStack matrices, VertexConsumer vc, int light, int overlay, float r, float g, float b, float a) {
		root.render(matrices, vc, light, overlay, r, g, b, a);
	}
}
