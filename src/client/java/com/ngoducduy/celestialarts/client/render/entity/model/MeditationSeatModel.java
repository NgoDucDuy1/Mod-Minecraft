package com.ngoducduy.celestialarts.client.render.entity.model;

import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;

/**
 * Bồ đoàn – a round meditation cushion, approximated by a square core with two flattened wings
 * (a plus shape that reads as an octagon). Ground level is y=0, the cushion is 2 px thick.
 * Texture 64x64 (drawn at 4x density = 256x256).
 */
public class MeditationSeatModel {
	private final ModelPart root;

	public MeditationSeatModel(ModelPart root) {
		this.root = root;
	}

	public static TexturedModelData getTexturedModelData() {
		ModelData data = new ModelData();
		ModelPartData root = data.getRoot();
		root.addChild("core", ModelPartBuilder.create().uv(0, 0).cuboid(-6.0F, -2.0F, -6.0F, 12.0F, 2.0F, 12.0F), ModelTransform.NONE);
		root.addChild("wing_x", ModelPartBuilder.create().uv(0, 14).cuboid(-8.0F, -1.5F, -4.0F, 16.0F, 1.5F, 8.0F), ModelTransform.NONE);
		root.addChild("wing_z", ModelPartBuilder.create().uv(0, 24).cuboid(-4.0F, -1.5F, -8.0F, 8.0F, 1.5F, 16.0F), ModelTransform.NONE);
		return TexturedModelData.of(data, 64, 64);
	}

	public void render(MatrixStack matrices, VertexConsumer vc, int light, int overlay, float r, float g, float b, float a) {
		root.render(matrices, vc, light, overlay, r, g, b, a);
	}
}
