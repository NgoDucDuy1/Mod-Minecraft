package com.ngoducduy.celestialarts.network;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.math.Vec3d;

/**
 * Serialisable description of a client visual effect.
 *
 * @param type      effect kind
 * @param pos       origin
 * @param target    secondary position (bolt end, beam target, direction...)
 * @param entityId  entity to follow, or -1
 * @param color     0xRRGGBB tint
 * @param scale     size multiplier / radius
 * @param duration  lifetime in ticks
 * @param extra     effect-specific parameter
 */
public record FxData(FxType type, Vec3d pos, Vec3d target, int entityId, int color, float scale, int duration, int extra) {

	public static FxData at(FxType type, Vec3d pos, int color, float scale, int duration) {
		return new FxData(type, pos, pos, -1, color, scale, duration, 0);
	}

	public static FxData line(FxType type, Vec3d from, Vec3d to, int color, float scale, int duration) {
		return new FxData(type, from, to, -1, color, scale, duration, 0);
	}

	public static FxData follow(FxType type, int entityId, Vec3d pos, int color, float scale, int duration) {
		return new FxData(type, pos, pos, entityId, color, scale, duration, 0);
	}

	public FxData withExtra(int extra) {
		return new FxData(type, pos, target, entityId, color, scale, duration, extra);
	}

	public FxData withTarget(Vec3d target) {
		return new FxData(type, pos, target, entityId, color, scale, duration, extra);
	}

	public FxData withEntity(int entityId) {
		return new FxData(type, pos, target, entityId, color, scale, duration, extra);
	}

	public void write(PacketByteBuf buf) {
		buf.writeVarInt(type.ordinal());
		buf.writeDouble(pos.x);
		buf.writeDouble(pos.y);
		buf.writeDouble(pos.z);
		buf.writeDouble(target.x);
		buf.writeDouble(target.y);
		buf.writeDouble(target.z);
		buf.writeInt(entityId);
		buf.writeInt(color);
		buf.writeFloat(scale);
		buf.writeVarInt(duration);
		buf.writeVarInt(extra);
	}

	public static FxData read(PacketByteBuf buf) {
		FxType type = FxType.byOrdinal(buf.readVarInt());
		Vec3d pos = new Vec3d(buf.readDouble(), buf.readDouble(), buf.readDouble());
		Vec3d target = new Vec3d(buf.readDouble(), buf.readDouble(), buf.readDouble());
		int entityId = buf.readInt();
		int color = buf.readInt();
		float scale = buf.readFloat();
		int duration = buf.readVarInt();
		int extra = buf.readVarInt();
		return new FxData(type, pos, target, entityId, color, scale, duration, extra);
	}
}
