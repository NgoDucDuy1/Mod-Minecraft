package com.ngoducduy.celestialarts.client.render.fx;

import com.ngoducduy.celestialarts.client.render.fx.types.*;
import com.ngoducduy.celestialarts.client.render.layer.ModRenderLayers;
import com.ngoducduy.celestialarts.client.render.post.ScreenOverlay;
import com.ngoducduy.celestialarts.network.FxData;
import com.ngoducduy.celestialarts.network.FxType;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Owns every live {@link ClientFx}. Ticked once per client tick and rendered from
 * {@code WorldRenderEvents.AFTER_TRANSLUCENT} so effects blend correctly over water and particles.
 */
public final class ClientFxManager {
	private static final int MAX_FX = 512;
	private static final List<ClientFx> ACTIVE = new ArrayList<>();
	// Dedicated buffer per effect layer (see ModRenderLayers.createFxBuffers) + a fallback for anything else.
	private static final VertexConsumerProvider.Immediate IMMEDIATE = VertexConsumerProvider.immediate(ModRenderLayers.createFxBuffers(), new BufferBuilder(1 << 16));
	private static ClientWorld lastWorld;

	private ClientFxManager() {
	}

	public static void spawn(FxData data) {
		MinecraftClient client = MinecraftClient.getInstance();
		ClientWorld world = client.world;
		if (world == null) return;

		if (data.type() == FxType.SCREEN_FLASH) {
			ScreenOverlay.add(data, client);
			return;
		}
		// "Stop" packet: same type + entity with extra == -1 and no duration.
		if (data.extra() == -1 && data.duration() <= 0 && data.entityId() >= 0) {
			for (ClientFx fx : ACTIVE) {
				if (fx.data().type() == data.type() && fx.data().entityId() == data.entityId()) fx.remove();
			}
			return;
		}
		// Refreshing a follow effect replaces the old one instead of stacking.
		if (data.entityId() >= 0 && isExclusive(data.type())) {
			for (ClientFx fx : ACTIVE) {
				if (fx.data().type() == data.type() && fx.data().entityId() == data.entityId()) fx.remove();
			}
		}
		ClientFx fx = create(data, world);
		if (fx == null) return;
		if (ACTIVE.size() >= MAX_FX) ACTIVE.remove(0);
		ACTIVE.add(fx);
	}

	private static boolean isExclusive(FxType type) {
		return switch (type) {
			case BEAM, VORTEX, TORTOISE_SHIELD, WIND_BLADES, QI_AURA -> true;
			default -> false;
		};
	}

	private static ClientFx create(FxData d, ClientWorld w) {
		return switch (d.type()) {
			case SHOCKWAVE_RING -> new ShockwaveRingFx(d, w);
			case MAGIC_CIRCLE -> new MagicCircleFx(d, w);
			case LIGHTNING_BOLT -> new LightningBoltFx(d, w);
			case SLASH_ARC -> new SlashArcFx(d, w);
			case BEAM -> new BeamFx(d, w);
			case FLAME_PILLAR -> new FlamePillarFx(d, w);
			case FROST_DOME -> new FrostDomeFx(d, w);
			case WIND_BLADES -> new WindBladesFx(d, w);
			case TORTOISE_SHIELD -> new TortoiseShieldFx(d, w);
			case TRIBULATION_CLOUD -> new TribulationCloudFx(d, w);
			case GROUND_CRACK -> new GroundCrackFx(d, w);
			case VORTEX -> new VortexFx(d, w);
			case ENERGY_BURST -> new EnergyBurstFx(d, w);
			case QI_AURA -> new QiAuraFx(d, w);
			case ICE_SPIKES -> new IceSpikesFx(d, w);
			case HEAVEN_PILLAR -> new HeavenPillarFx(d, w);
			case LOTUS_BLOOM -> new LotusBloomFx(d, w);
			case RUNE_ORBIT -> new RuneOrbitFx(d, w);
			case SCREEN_FLASH -> null; // handled by ScreenOverlay in spawn()
			case AFTERIMAGE -> new AfterimageFx(d, w);
			case GROUND_DECAL -> new GroundDecalFx(d, w);
			case HEAVEN_HAND -> new HeavenHandFx(d, w);
		};
	}

	public static void tick(MinecraftClient client) {
		if (client.world == null) {
			ACTIVE.clear();
			lastWorld = null;
			return;
		}
		if (client.world != lastWorld) {
			ACTIVE.clear();
			lastWorld = client.world;
		}
		if (client.isPaused()) return;
		Iterator<ClientFx> it = ACTIVE.iterator();
		while (it.hasNext()) {
			ClientFx fx = it.next();
			fx.tick();
			if (fx.isDead()) it.remove();
		}
	}

	public static void clear() {
		ACTIVE.clear();
	}

	public static int count() {
		return ACTIVE.size();
	}

	public static void render(WorldRenderContext context) {
		if (ACTIVE.isEmpty()) return;
		MatrixStack matrices = context.matrixStack();
		Camera camera = context.camera();
		float tickDelta = context.tickDelta();
		Vec3d cam = camera.getPos();

		matrices.push();
		matrices.translate(-cam.x, -cam.y, -cam.z);
		for (ClientFx fx : ACTIVE) {
			if (fx.isDead()) continue;
			matrices.push();
			try {
				fx.render(matrices, IMMEDIATE, camera, tickDelta);
			} finally {
				matrices.pop();
			}
		}
		matrices.pop();
		IMMEDIATE.draw();
	}
}
