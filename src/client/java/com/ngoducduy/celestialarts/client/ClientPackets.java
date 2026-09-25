package com.ngoducduy.celestialarts.client;

import com.ngoducduy.celestialarts.client.render.fx.ClientFxManager;
import com.ngoducduy.celestialarts.cultivation.PlayerQi;
import com.ngoducduy.celestialarts.cultivation.QiHolder;
import com.ngoducduy.celestialarts.network.FxData;
import com.ngoducduy.celestialarts.network.ModPackets;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

/** Client side of the mod's networking: S2C receivers and C2S senders. */
public final class ClientPackets {
	private ClientPackets() {
	}

	public static void register() {
		ClientPlayNetworking.registerGlobalReceiver(ModPackets.SYNC_QI, (client, handler, buf, responseSender) -> {
			NbtCompound nbt = buf.readNbt();
			client.execute(() -> {
				if (client.player == null || nbt == null) return;
				PlayerQi qi = QiHolder.get(client.player);
				qi.readNbt(nbt);
			});
		});
		ClientPlayNetworking.registerGlobalReceiver(ModPackets.SPAWN_FX, (client, handler, buf, responseSender) -> {
			FxData fx = FxData.read(buf);
			client.execute(() -> ClientFxManager.spawn(fx));
		});
		ClientPlayNetworking.registerGlobalReceiver(ModPackets.CAMERA_SHAKE, (client, handler, buf, responseSender) -> {
			float strength = buf.readFloat();
			int ticks = buf.readVarInt();
			client.execute(() -> CameraShake.add(strength, ticks));
		});
	}

	public static void sendCast(int slot) {
		PacketByteBuf buf = PacketByteBufs.create();
		buf.writeVarInt(slot);
		ClientPlayNetworking.send(ModPackets.CAST_SKILL, buf);
	}

	public static void sendRelease(int slot) {
		PacketByteBuf buf = PacketByteBufs.create();
		buf.writeVarInt(slot);
		ClientPlayNetworking.send(ModPackets.RELEASE_SKILL, buf);
	}

	public static void sendSetSlot(int slot, @Nullable Identifier skill) {
		PacketByteBuf buf = PacketByteBufs.create();
		buf.writeVarInt(slot);
		buf.writeBoolean(skill != null);
		if (skill != null) buf.writeIdentifier(skill);
		ClientPlayNetworking.send(ModPackets.SET_SLOT, buf);
	}

	public static void sendAirJump() {
		ClientPlayNetworking.send(ModPackets.AIR_JUMP, PacketByteBufs.empty());
	}

	public static void sendBreakthrough() {
		ClientPlayNetworking.send(ModPackets.BREAKTHROUGH, PacketByteBufs.create());
	}
}
