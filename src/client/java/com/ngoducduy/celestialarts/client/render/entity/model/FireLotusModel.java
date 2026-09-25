package com.ngoducduy.celestialarts.client.render.entity.model;

import net.minecraft.client.model.*;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;

/**
 * Fire lotus: a glowing core with an outer ring of 8 petals and an inner ring of 6.
 * Petals pivot at their base so they can open as the lotus charges. Texture 64x64.
 */
public class FireLotusModel {
	public static final int OUTER = 8;
	public static final int INNER = 6;
	private final ModelPart root;
	private final ModelPart[] outer = new ModelPart[OUTER];
	private final ModelPart[] inner = new ModelPart[INNER];

	public FireLotusModel(ModelPart root) {
		this.root = root;
		for (int i = 0; i < OUTER; i++) outer[i] = root.getChild("outer_" + i);
		for (int i = 0; i < INNER; i++) inner[i] = root.getChild("inner_" + i);
	}

	public static TexturedModelData getTexturedModelData() {
		ModelData data = new ModelData();
		ModelPartData root = data.getRoot();
		root.addChild("core", ModelPartBuilder.create().uv(0, 0).cuboid(-3.0F, -3.0F, -3.0F, 6.0F, 6.0F, 6.0F), ModelTransform.NONE);
		for (int i = 0; i < OUTER; i++) {
			float yaw = (float) (i * Math.PI * 2.0 / OUTER);
			root.addChild("outer_" + i, ModelPartBuilder.create().uv(0, 12).cuboid(-3.0F, -1.0F, 0.0F, 6.0F, 1.0F, 9.0F), ModelTransform.of(0.0F, 2.0F, 0.0F, -1.2F, yaw, 0.0F));
		}
		for (int i = 0; i < INNER; i++) {
			float yaw = (float) (i * Math.PI * 2.0 / INNER + Math.PI / INNER);
			root.addChild("inner_" + i, ModelPartBuilder.create().uv(0, 22).cuboid(-2.0F, -1.0F, 0.0F, 4.0F, 1.0F, 6.0F), ModelTransform.of(0.0F, 1.0F, 0.0F, -1.4F, yaw, 0.0F));
		}
		return TexturedModelData.of(data, 64, 64);
	}

	/**
	 * @param growth 0 = closed bud, 1 = fully open
	 * @param spin   rotation of the whole flower in radians
	 */
	public void setAngles(float growth, float spin) {
		for (int i = 0; i < OUTER; i++) {
			outer[i].yaw = (float) (i * Math.PI * 2.0 / OUTER) + spin;
			outer[i].pitch = -1.35F + 1.05F * growth;
		}
		for (int i = 0; i < INNER; i++) {
			inner[i].yaw = (float) (i * Math.PI * 2.0 / INNER + Math.PI / INNER) - spin * 0.6F;
			inner[i].pitch = -1.45F + 0.85F * growth;
		}
	}

	public void render(MatrixStack matrices, VertexConsumer vc, int light, int overlay, float r, float g, float b, float a) {
		root.render(matrices, vc, light, overlay, r, g, b, a);
	}
}
