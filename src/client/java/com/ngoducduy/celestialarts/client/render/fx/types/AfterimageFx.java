package com.ngoducduy.celestialarts.client.render.fx.types;

import com.ngoducduy.celestialarts.client.render.RenderUtil;
import com.ngoducduy.celestialarts.client.render.fx.ClientFx;
import com.ngoducduy.celestialarts.client.render.layer.ModRenderLayers;
import com.ngoducduy.celestialarts.network.FxData;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/**
 * Afterimages ("tàn ảnh") of an entity along a path – the classic donghua flash-step look.
 * <p>
 * {@code pos} → {@code target} is the path, {@code entityId} the entity whose model is ghosted,
 * {@code scale} the number of ghosts, {@code color} the tint. The entity's <em>current</em> pose is
 * redrawn at evenly spaced points, through the additive layer using its own texture (so a player's
 * skin, armour and held item all appear as translucent light). Every ghost fades over the duration,
 * the ones nearer the destination lingering longest.
 */
public class AfterimageFx extends ClientFx {
	private static final int FULL_LIGHT = 0xF000F0;

	public AfterimageFx(FxData data, ClientWorld world) {
		super(data, world);
	}

	@Override
	protected boolean stopsWithEntity() {
		return false;
	}

	@Override
	public void render(MatrixStack matrices, VertexConsumerProvider consumers, Camera camera, float tickDelta) {
		Entity e = entity();
		if (e == null) return;
		MinecraftClient client = MinecraftClient.getInstance();
		EntityRenderDispatcher dispatcher = client.getEntityRenderDispatcher();
		EntityRenderer<? super Entity> renderer = dispatcher.getRenderer(e);
		if (renderer == null) return;
		Identifier texture = renderer.getTexture(e);
		RenderLayer layer = ModRenderLayers.additive(texture);

		float t = progress(tickDelta);
		int ghosts = Math.max(1, Math.min(12, Math.round(scale)));
		Vec3d from = data.pos();
		Vec3d to = data.target();
		float yaw = MathHelper.lerp(tickDelta, e.prevYaw, e.getYaw());
		float r = RenderUtil.red(color), g = RenderUtil.green(color), b = RenderUtil.blue(color);

		boolean shadows = dispatcher.gameOptions.getEntityShadows().getValue();
		dispatcher.setRenderShadows(false);
		try {
			for (int i = 0; i < ghosts; i++) {
				float f = ghosts == 1 ? 1.0F : (float) i / (ghosts - 1);
				// Ghosts near the start die first.
				float life = MathHelper.clamp(1.0F - t * (1.6F - 0.6F * f), 0.0F, 1.0F);
				float alpha = (0.25F + 0.45F * f) * life * life;
				if (alpha <= 0.01F) continue;
				Vec3d p = from.lerp(to, f);
				TintedProvider provider = new TintedProvider(consumers, layer, r, g, b, alpha);
				matrices.push();
				dispatcher.render(e, p.x, p.y, p.z, yaw, tickDelta, matrices, provider, FULL_LIGHT);
				matrices.pop();
			}
		} finally {
			dispatcher.setRenderShadows(shadows);
		}
	}

	/** Routes every entity-format layer of a model to one additive layer with a constant tint. */
	private static final class TintedProvider implements VertexConsumerProvider {
		private final VertexConsumerProvider parent;
		private final RenderLayer layer;
		private final float r, g, b, a;

		TintedProvider(VertexConsumerProvider parent, RenderLayer layer, float r, float g, float b, float a) {
			this.parent = parent;
			this.layer = layer;
			this.r = r;
			this.g = g;
			this.b = b;
			this.a = a;
		}

		@Override
		public VertexConsumer getBuffer(RenderLayer requested) {
			if (requested.getVertexFormat() != VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL) {
				// Name tags, shadows, particles: not part of a ghost.
				return DiscardingConsumer.INSTANCE;
			}
			return new TintingConsumer(parent.getBuffer(layer), r, g, b, a);
		}
	}

	/** Delegates everything but forces the vertex colour. */
	private record TintingConsumer(VertexConsumer delegate, float r, float g, float b, float a) implements VertexConsumer {
		@Override
		public VertexConsumer vertex(double x, double y, double z) {
			delegate.vertex(x, y, z);
			return this;
		}

		@Override
		public VertexConsumer color(int red, int green, int blue, int alpha) {
			delegate.color(r, g, b, a);
			return this;
		}

		@Override
		public VertexConsumer texture(float u, float v) {
			delegate.texture(u, v);
			return this;
		}

		@Override
		public VertexConsumer overlay(int u, int v) {
			delegate.overlay(u, v);
			return this;
		}

		@Override
		public VertexConsumer light(int u, int v) {
			delegate.light(u, v);
			return this;
		}

		@Override
		public VertexConsumer normal(float x, float y, float z) {
			delegate.normal(x, y, z);
			return this;
		}

		@Override
		public void next() {
			delegate.next();
		}

		@Override
		public void fixedColor(int red, int green, int blue, int alpha) {
			delegate.fixedColor((int) (r * 255), (int) (g * 255), (int) (b * 255), (int) (a * 255));
		}

		@Override
		public void unfixColor() {
			delegate.unfixColor();
		}
	}

	private static final class DiscardingConsumer implements VertexConsumer {
		static final DiscardingConsumer INSTANCE = new DiscardingConsumer();

		@Override
		public VertexConsumer vertex(double x, double y, double z) {
			return this;
		}

		@Override
		public VertexConsumer color(int red, int green, int blue, int alpha) {
			return this;
		}

		@Override
		public VertexConsumer texture(float u, float v) {
			return this;
		}

		@Override
		public VertexConsumer overlay(int u, int v) {
			return this;
		}

		@Override
		public VertexConsumer light(int u, int v) {
			return this;
		}

		@Override
		public VertexConsumer normal(float x, float y, float z) {
			return this;
		}

		@Override
		public void next() {
		}

		@Override
		public void fixedColor(int red, int green, int blue, int alpha) {
		}

		@Override
		public void unfixColor() {
		}
	}
}
