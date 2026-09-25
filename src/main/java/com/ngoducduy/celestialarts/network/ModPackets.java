package com.ngoducduy.celestialarts.network;

import com.ngoducduy.celestialarts.CelestialArts;
import com.ngoducduy.celestialarts.cultivation.PlayerQi;
import com.ngoducduy.celestialarts.cultivation.QiHolder;
import com.ngoducduy.celestialarts.skill.SkillManager;
import com.ngoducduy.celestialarts.skill.SkillRegistry;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

/**
 * Packet identifiers and the server-side half of the networking.
 * Client receivers live in {@code com.ngoducduy.celestialarts.client.ClientPackets}.
 */
public final class ModPackets {
	/** C2S: player pressed a skill slot key. [varint slot] */
	public static final Identifier CAST_SKILL = CelestialArts.id("cast_skill");
	/** C2S: player released a skill slot key. [varint slot] */
	public static final Identifier RELEASE_SKILL = CelestialArts.id("release_skill");
	/** C2S: bind a skill to a slot from the skill book. [varint slot, boolean hasSkill, identifier skill] */
	public static final Identifier SET_SLOT = CelestialArts.id("set_slot");
	/** C2S: player asks to break through to the next realm. */
	public static final Identifier BREAKTHROUGH = CelestialArts.id("breakthrough");

	/** S2C: full cultivation state snapshot. [nbt] */
	public static final Identifier SYNC_QI = CelestialArts.id("sync_qi");
	/** S2C: spawn a client visual effect. [FxData] */
	public static final Identifier SPAWN_FX = CelestialArts.id("spawn_fx");
	/** S2C: short camera shake. [float strength, varint ticks] */
	public static final Identifier CAMERA_SHAKE = CelestialArts.id("camera_shake");

	/** Effect broadcast radius in blocks. */
	private static final double FX_RANGE = 128.0;

	private ModPackets() {
	}

	public static void registerServerReceivers() {
		ServerPlayNetworking.registerGlobalReceiver(CAST_SKILL, (server, player, handler, buf, responseSender) -> {
			int slot = buf.readVarInt();
			server.execute(() -> SkillManager.castSlot(player, slot));
		});
		ServerPlayNetworking.registerGlobalReceiver(RELEASE_SKILL, (server, player, handler, buf, responseSender) -> {
			int slot = buf.readVarInt();
			server.execute(() -> SkillManager.releaseSlot(player, slot));
		});
		ServerPlayNetworking.registerGlobalReceiver(SET_SLOT, (server, player, handler, buf, responseSender) -> {
			int slot = buf.readVarInt();
			boolean has = buf.readBoolean();
			Identifier skill = has ? buf.readIdentifier() : null;
			server.execute(() -> {
				PlayerQi qi = QiHolder.get(player);
				if (skill == null || SkillRegistry.exists(skill)) {
					qi.setSlot(slot, skill);
					sendSync(player, qi);
				}
			});
		});
		ServerPlayNetworking.registerGlobalReceiver(BREAKTHROUGH, (server, player, handler, buf, responseSender) ->
				server.execute(() -> com.ngoducduy.celestialarts.cultivation.Breakthrough.tryBreakthrough(player)));
	}

	// ------------------------------------------------------------- senders

	public static void sendSync(ServerPlayerEntity player, PlayerQi qi) {
		if (player.networkHandler == null) return;
		PacketByteBuf buf = PacketByteBufs.create();
		buf.writeNbt(qi.writeNbt(new NbtCompound()));
		ServerPlayNetworking.send(player, SYNC_QI, buf);
	}

	/** Broadcasts an effect to every player near its origin. */
	public static void sendFx(ServerWorld world, FxData fx) {
		PacketByteBuf buf = PacketByteBufs.create();
		fx.write(buf);
		for (ServerPlayerEntity p : PlayerLookup.around(world, fx.pos(), FX_RANGE)) {
			ServerPlayNetworking.send(p, SPAWN_FX, buf);
		}
	}

	/** Broadcasts an effect to every player tracking an entity (plus the entity itself if a player). */
	public static void sendFx(Entity entity, FxData fx) {
		PacketByteBuf buf = PacketByteBufs.create();
		fx.write(buf);
		for (ServerPlayerEntity p : PlayerLookup.tracking(entity)) {
			ServerPlayNetworking.send(p, SPAWN_FX, buf);
		}
		if (entity instanceof ServerPlayerEntity self) {
			ServerPlayNetworking.send(self, SPAWN_FX, buf);
		}
	}

	public static void sendCameraShake(ServerWorld world, Vec3d pos, double range, float strength, int ticks) {
		for (ServerPlayerEntity p : PlayerLookup.around(world, pos, range)) {
			double dist = p.getPos().distanceTo(pos);
			float falloff = (float) Math.max(0.0, 1.0 - dist / range);
			if (falloff <= 0.01f) continue;
			PacketByteBuf buf = PacketByteBufs.create();
			buf.writeFloat(strength * falloff);
			buf.writeVarInt(ticks);
			ServerPlayNetworking.send(p, CAMERA_SHAKE, buf);
		}
	}
}
