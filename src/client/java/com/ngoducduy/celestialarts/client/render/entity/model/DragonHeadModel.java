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
 * Stylised eastern dragon head shared by the wind and thunder dragons: skull, snout, jaw,
 * two swept-back horns, two side fins and the first neck ring. Faces −Z (vanilla model
 * convention). Texture 64x32.
 */
public class DragonHeadModel {
	private final ModelPart root;
	private final ModelPart jaw;
	private final ModelPart finLeft;
	private final ModelPart finRight;

	public DragonHeadModel(ModelPart root) {
		this.root = root;
		this.jaw = root.getChild("jaw");
		this.finLeft = root.getChild("fin_left");
		this.finRight = root.getChild("fin_right");
	}

	public static TexturedModelData getTexturedModelData() {
		ModelData data = new ModelData();
		ModelPartData root = data.getRoot();
		root.addChild("snout", ModelPartBuilder.create().uv(0, 0).cuboid(-2.0F, -2.0F, -8.0F, 4.0F, 3.0F, 5.0F), ModelTransform.NONE);
		root.addChild("head", ModelPartBuilder.create().uv(0, 8).cuboid(-3.0F, -3.0F, -3.0F, 6.0F, 5.0F, 6.0F), ModelTransform.NONE);
		root.addChild("jaw", ModelPartBuilder.create().uv(24, 8).cuboid(-2.0F, 0.0F, -6.0F, 4.0F, 1.0F, 5.0F), ModelTransform.pivot(0.0F, 1.5F, -1.5F));
		root.addChild("horn_left", ModelPartBuilder.create().uv(18, 0).cuboid(0.0F, -4.0F, 0.0F, 1.0F, 4.0F, 1.0F), ModelTransform.of(1.5F, -3.0F, 1.0F, -0.6F, 0.0F, 0.15F));
		root.addChild("horn_right", ModelPartBuilder.create().uv(22, 0).cuboid(-1.0F, -4.0F, 0.0F, 1.0F, 4.0F, 1.0F), ModelTransform.of(-1.5F, -3.0F, 1.0F, -0.6F, 0.0F, -0.15F));
		root.addChild("fin_left", ModelPartBuilder.create().uv(42, 8).cuboid(0.0F, -1.5F, 0.0F, 1.0F, 3.0F, 4.0F), ModelTransform.of(3.0F, -0.5F, -1.0F, 0.0F, -0.5F, 0.0F));
		root.addChild("fin_right", ModelPartBuilder.create().uv(52, 8).cuboid(-1.0F, -1.5F, 0.0F, 1.0F, 3.0F, 4.0F), ModelTransform.of(-3.0F, -0.5F, -1.0F, 0.0F, 0.5F, 0.0F));
		root.addChild("neck", ModelPartBuilder.create().uv(0, 19).cuboid(-2.5F, -2.5F, 3.0F, 5.0F, 5.0F, 3.0F), ModelTransform.NONE);
		return TexturedModelData.of(data, 64, 32);
	}

	/** Animates the jaw and fins; {@code t} in ticks (with delta), {@code roar} 0..1 opens the jaw. */
	public void animate(float t, float roar) {
		jaw.pitch = 0.15F + roar * 0.55F + (float) Math.sin(t * 0.6F) * 0.06F;
		float flap = (float) Math.sin(t * 0.45F) * 0.25F;
		finLeft.yaw = -0.5F + flap;
		finRight.yaw = 0.5F - flap;
	}

	public void render(MatrixStack matrices, VertexConsumer vc, int light, int overlay, float r, float g, float b, float a) {
		root.render(matrices, vc, light, overlay, r, g, b, a);
	}
}
