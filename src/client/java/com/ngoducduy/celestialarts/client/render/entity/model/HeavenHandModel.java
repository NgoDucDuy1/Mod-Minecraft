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
 * Thiên Đạo Chi Thủ – the colossal palm of the Heavenly Dao (rendered by {@code HeavenHandFx} at
 * ~37x scale → 110 blocks across).
 *
 * <p>Built in world orientation (no entity y-flip): the palm faces {@code -Y} (down), the fingers
 * point along {@code +Z}, the thumb sits on the {@code -X} side. Every finger has three jointed
 * segments so the hand can be posed pressing / relaxing over time. Texture 256x128 – the palm alone
 * needs a 136-pixel UV footprint.</p>
 */
public class HeavenHandModel {
	/** Width of the whole hand in model units (16 units = 1 block), used to derive the render scale. */
	public static final float WIDTH_UNITS = 48.0F;
	/** Vertical extent from the finger tips (curled down) to the back of the palm, in model units. */
	public static final float THICKNESS_UNITS = 16.0F;

	private final ModelPart root;
	private final ModelPart[] fingers = new ModelPart[5];
	private final ModelPart[] joints2 = new ModelPart[5];
	private final ModelPart[] joints3 = new ModelPart[4];

	public HeavenHandModel(ModelPart root) {
		this.root = root;
		String[] names = {"index", "middle", "ring", "pinky", "thumb"};
		for (int i = 0; i < 5; i++) {
			fingers[i] = root.getChild(names[i]);
			joints2[i] = fingers[i].getChild(names[i] + "2");
			if (i < 4) joints3[i] = joints2[i].getChild(names[i] + "3");
		}
	}

	public static TexturedModelData getTexturedModelData() {
		ModelData data = new ModelData();
		ModelPartData root = data.getRoot();
		// Palm: 32 wide, 6 thick, 36 long, centred on the origin.
		root.addChild("palm", ModelPartBuilder.create().uv(0, 0).cuboid(-16.0F, -3.0F, -18.0F, 32.0F, 6.0F, 36.0F), ModelTransform.NONE);

		// Fingers: pivot on the front edge of the palm, segments chained at their tips.
		finger(root, "index", -12.0F, 7.0F, 14.0F, 12.0F, 8.0F, 0, 44, 0, 66, 0, 86, 0.0F);
		finger(root, "middle", -4.0F, 7.0F, 15.0F, 13.0F, 9.0F, 44, 44, 40, 66, 32, 86, 0.0F);
		finger(root, "ring", 4.0F, 7.0F, 14.0F, 12.0F, 8.0F, 90, 44, 82, 66, 66, 86, 0.0F);
		finger(root, "pinky", 12.0F, 6.0F, 11.0F, 9.0F, 7.0F, 134, 44, 122, 66, 98, 86, 0.06F);

		// Thumb: two segments on the -X side, splayed forward.
		ModelPartData thumb = root.addChild("thumb", ModelPartBuilder.create().uv(170, 44).cuboid(-3.5F, -3.0F, 0.0F, 7.0F, 6.0F, 12.0F),
				ModelTransform.of(-16.0F, 0.0F, 4.0F, 0.20F, 0.62F, 0.0F));
		thumb.addChild("thumb2", ModelPartBuilder.create().uv(154, 66).cuboid(-3.5F, -3.0F, 0.0F, 7.0F, 6.0F, 9.0F),
				ModelTransform.of(0.0F, 0.0F, 12.0F, 0.30F, 0.0F, 0.0F));
		return TexturedModelData.of(data, 256, 128);
	}

	private static void finger(ModelPartData root, String name, float x, float width, float l1, float l2, float l3,
	                           int u1, int v1, int u2, int v2, int u3, int v3, float splay) {
		float hw = width / 2.0F;
		ModelPartData s1 = root.addChild(name, ModelPartBuilder.create().uv(u1, v1).cuboid(-hw, -2.5F, 0.0F, width, 5.0F, l1),
				ModelTransform.of(x, 0.0F, 18.0F, 0.18F, -splay * Math.signum(x), 0.0F));
		ModelPartData s2 = s1.addChild(name + "2", ModelPartBuilder.create().uv(u2, v2).cuboid(-hw, -2.5F, 0.0F, width, 5.0F, l2),
				ModelTransform.of(0.0F, 0.0F, l1, 0.28F, 0.0F, 0.0F));
		s2.addChild(name + "3", ModelPartBuilder.create().uv(u3, v3).cuboid(-hw, -2.5F, 0.0F, width, 5.0F, l3),
				ModelTransform.of(0.0F, 0.0F, l2, 0.34F, 0.0F, 0.0F));
	}

	/**
	 * Poses the fingers. {@code curl} 0 = open, flat palm; 1 = fingers pressed down hard (the slam).
	 * {@code tremble} adds a small per-finger shiver (the hand straining against the heavens).
	 */
	public void pose(float curl, float tremble, float time) {
		for (int i = 0; i < 4; i++) {
			float phase = time * 0.35F + i * 1.7F;
			float shiver = tremble * 0.04F * (float) Math.sin(phase);
			fingers[i].pitch = 0.10F + 0.35F * curl + shiver;
			joints2[i].pitch = 0.16F + 0.45F * curl + shiver * 0.6F;
			joints3[i].pitch = 0.20F + 0.50F * curl;
		}
		fingers[4].pitch = 0.12F + 0.30F * curl;
		joints2[4].pitch = 0.20F + 0.40F * curl;
	}

	public void render(MatrixStack matrices, VertexConsumer vc, int light, int overlay, float r, float g, float b, float a) {
		root.render(matrices, vc, light, overlay, r, g, b, a);
	}
}
