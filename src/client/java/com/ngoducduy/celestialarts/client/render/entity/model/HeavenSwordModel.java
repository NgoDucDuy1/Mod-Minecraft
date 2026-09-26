package com.ngoducduy.celestialarts.client.render.entity.model;

import net.minecraft.client.model.*;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;

/**
 * Giant heaven-sword (rendered at 4x scale → 12 blocks long). Blade along +Z. Texture 128x128.
 */
public class HeavenSwordModel {
	public static final float RENDER_SCALE = 4.0F;
	private final ModelPart root;

	public HeavenSwordModel(ModelPart root) {
		this.root = root;
	}

	public static TexturedModelData getTexturedModelData() {
		ModelData data = new ModelData();
		ModelPartData root = data.getRoot();
		root.addChild("blade", ModelPartBuilder.create().uv(0, 0).cuboid(-3.0F, -0.5F, 0.0F, 6.0F, 1.0F, 48.0F), ModelTransform.NONE);
		root.addChild("ridge", ModelPartBuilder.create().uv(0, 49).cuboid(-1.0F, -1.0F, 2.0F, 2.0F, 2.0F, 40.0F), ModelTransform.NONE);
		root.addChild("guard", ModelPartBuilder.create().uv(0, 91).cuboid(-8.0F, -1.5F, -3.0F, 16.0F, 3.0F, 3.0F), ModelTransform.NONE);
		root.addChild("handle", ModelPartBuilder.create().uv(38, 91).cuboid(-1.0F, -1.0F, -12.0F, 2.0F, 2.0F, 9.0F), ModelTransform.NONE);
		root.addChild("pommel", ModelPartBuilder.create().uv(60, 91).cuboid(-2.0F, -2.0F, -15.0F, 4.0F, 4.0F, 3.0F), ModelTransform.NONE);
		return TexturedModelData.of(data, 128, 128);
	}

	public void render(MatrixStack matrices, VertexConsumer vc, int light, int overlay, float r, float g, float b, float a) {
		root.render(matrices, vc, light, overlay, r, g, b, a);
	}
}
