package com.ngoducduy.celestialarts.client.render.entity.model;

import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;

/**
 * Hỏa Linh: a flickering core with 6 flame tongues fanning out and licking up and down out of
 * phase. Texture 32x32.
 */
public class FireSpiritModel {
	public static final int TONGUES = 6;
	private final ModelPart root;
	private final ModelPart core;
	private final ModelPart[] tongues = new ModelPart[TONGUES];

	public FireSpiritModel(ModelPart root) {
		this.root = root;
		this.core = root.getChild("core");
		for (int i = 0; i < TONGUES; i++) tongues[i] = root.getChild("tongue_" + i);
	}

	public static TexturedModelData getTexturedModelData() {
		ModelData data = new ModelData();
		ModelPartData root = data.getRoot();
		root.addChild("core", ModelPartBuilder.create().uv(0, 0).cuboid(-3.5F, -3.5F, -3.5F, 7.0F, 7.0F, 7.0F), ModelTransform.NONE);
		for (int i = 0; i < TONGUES; i++) {
			float yaw = (float) (i * Math.PI * 2.0 / TONGUES);
			root.addChild("tongue_" + i, ModelPartBuilder.create().uv(0, 14).cuboid(-1.5F, -6.0F, -1.5F, 3.0F, 6.0F, 3.0F),
					ModelTransform.of(0.0F, 0.0F, 0.0F, 0.0F, yaw, 0.0F));
		}
		return TexturedModelData.of(data, 32, 32);
	}

	/**
	 * @param age     ticks alive (entity age + partial tick), drives the flicker
	 * @param enraged whether it just failed to be bound – tongues whip faster and reach higher
	 */
	public void setAngles(float age, boolean enraged) {
		float speed = enraged ? 0.24F : 0.11F;
		for (int i = 0; i < TONGUES; i++) {
			float phase = age * speed + i * 1.05F;
			tongues[i].pivotY = -0.5F - 1.1F * (0.5F + 0.5F * MathHelper.sin(phase));
			tongues[i].pitch = -0.25F + 0.2F * MathHelper.sin(phase * 1.7F);
			tongues[i].yaw = (float) (i * Math.PI * 2.0 / TONGUES) + age * 0.02F;
		}
		core.pivotY = -0.3F * MathHelper.sin(age * 0.15F);
	}

	public void render(MatrixStack matrices, VertexConsumer vc, int light, int overlay, float r, float g, float b, float a) {
		root.render(matrices, vc, light, overlay, r, g, b, a);
	}
}
