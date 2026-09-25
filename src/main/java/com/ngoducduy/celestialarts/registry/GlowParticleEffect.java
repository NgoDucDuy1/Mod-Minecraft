package com.ngoducduy.celestialarts.registry;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleType;
import net.minecraft.registry.Registries;
import net.minecraft.util.math.MathHelper;

import java.util.Locale;

/**
 * Particle effect carrying a colour, a size and a lifetime, so a single particle type
 * can be reused for every element (sword qi cyan, fire orange, void purple...).
 */
public class GlowParticleEffect implements ParticleEffect {
	public static final Codec<GlowParticleEffect> CODEC_GLOW = codec(() -> ModParticles.GLOW);
	public static final Codec<GlowParticleEffect> CODEC_SPARK = codec(() -> ModParticles.SPARK);

	public static final ParticleEffect.Factory<GlowParticleEffect> FACTORY = new ParticleEffect.Factory<>() {
		@Override
		public GlowParticleEffect read(ParticleType<GlowParticleEffect> type, StringReader reader) throws CommandSyntaxException {
			reader.expect(' ');
			float r = reader.readFloat();
			reader.expect(' ');
			float g = reader.readFloat();
			reader.expect(' ');
			float b = reader.readFloat();
			reader.expect(' ');
			float scale = reader.readFloat();
			reader.expect(' ');
			int lifetime = reader.readInt();
			return new GlowParticleEffect(type, r, g, b, scale, lifetime);
		}

		@Override
		public GlowParticleEffect read(ParticleType<GlowParticleEffect> type, PacketByteBuf buf) {
			return new GlowParticleEffect(type, buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readVarInt());
		}
	};

	private final ParticleType<GlowParticleEffect> type;
	private final float red;
	private final float green;
	private final float blue;
	private final float scale;
	private final int lifetime;

	public GlowParticleEffect(ParticleType<GlowParticleEffect> type, float red, float green, float blue, float scale, int lifetime) {
		this.type = type;
		this.red = MathHelper.clamp(red, 0f, 1f);
		this.green = MathHelper.clamp(green, 0f, 1f);
		this.blue = MathHelper.clamp(blue, 0f, 1f);
		this.scale = MathHelper.clamp(scale, 0.01f, 8f);
		this.lifetime = MathHelper.clamp(lifetime, 1, 400);
	}

	/** Convenience: build from an 0xRRGGBB colour. */
	public static GlowParticleEffect of(ParticleType<GlowParticleEffect> type, int rgb, float scale, int lifetime) {
		float r = ((rgb >> 16) & 0xFF) / 255f;
		float g = ((rgb >> 8) & 0xFF) / 255f;
		float b = (rgb & 0xFF) / 255f;
		return new GlowParticleEffect(type, r, g, b, scale, lifetime);
	}

	public static GlowParticleEffect glow(int rgb, float scale, int lifetime) {
		return of(ModParticles.GLOW, rgb, scale, lifetime);
	}

	public static GlowParticleEffect spark(int rgb, float scale, int lifetime) {
		return of(ModParticles.SPARK, rgb, scale, lifetime);
	}

	private static Codec<GlowParticleEffect> codec(java.util.function.Supplier<ParticleType<GlowParticleEffect>> type) {
		return RecordCodecBuilder.create(instance -> instance.group(
				Codec.FLOAT.fieldOf("r").forGetter(e -> e.red),
				Codec.FLOAT.fieldOf("g").forGetter(e -> e.green),
				Codec.FLOAT.fieldOf("b").forGetter(e -> e.blue),
				Codec.FLOAT.fieldOf("scale").forGetter(e -> e.scale),
				Codec.INT.fieldOf("lifetime").forGetter(e -> e.lifetime)
		).apply(instance, (r, g, b, s, l) -> new GlowParticleEffect(type.get(), r, g, b, s, l)));
	}

	@Override
	public ParticleType<?> getType() {
		return type;
	}

	@Override
	public void write(PacketByteBuf buf) {
		buf.writeFloat(red);
		buf.writeFloat(green);
		buf.writeFloat(blue);
		buf.writeFloat(scale);
		buf.writeVarInt(lifetime);
	}

	@Override
	public String asString() {
		return String.format(Locale.ROOT, "%s %.2f %.2f %.2f %.2f %d",
				Registries.PARTICLE_TYPE.getId(type), red, green, blue, scale, lifetime);
	}

	public float getRed() {
		return red;
	}

	public float getGreen() {
		return green;
	}

	public float getBlue() {
		return blue;
	}

	public float getScale() {
		return scale;
	}

	public int getLifetime() {
		return lifetime;
	}
}
