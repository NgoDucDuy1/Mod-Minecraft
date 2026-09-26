package com.ngoducduy.celestialarts.cultivation;

import com.ngoducduy.celestialarts.network.FxData;
import com.ngoducduy.celestialarts.network.FxType;
import com.ngoducduy.celestialarts.network.ModPackets;
import com.ngoducduy.celestialarts.registry.ModSounds;
import com.ngoducduy.celestialarts.util.SkillFx;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;

/**
 * Linh căn thức tỉnh – the one-time roll of spirit root and talent when a player first joins a
 * world (or when an operator re-rolls them). The roll is seeded from the world seed and the
 * player's UUID, so the same player gets the same fate in the same world even if the data is
 * wiped, and a different one in another world.
 */
public final class Awakening {
	private Awakening() {
	}

	/** Rolls root and talent if the player has none yet. Returns true when an awakening happened. */
	public static boolean ensure(ServerPlayerEntity player) {
		PlayerQi qi = QiHolder.get(player);
		if (qi.hasAwakened()) return false;
		long seed = player.getServerWorld().getSeed() ^ player.getUuid().getMostSignificantBits() ^ (player.getUuid().getLeastSignificantBits() * 31L);
		roll(player, Random.create(seed), true);
		return true;
	}

	/** (Re)rolls root and talent; {@code ceremony} shows the awakening to the player. */
	public static void roll(ServerPlayerEntity player, Random random, boolean ceremony) {
		PlayerQi qi = QiHolder.get(player);
		SpiritRoot root = SpiritRoot.roll(random);
		Talent talent = Talent.roll(random);
		qi.setRoot(root);
		qi.setTalent(talent);
		qi.markDirty();
		RealmPassives.apply(player);
		ModPackets.sendSync(player, qi);
		if (ceremony) ceremony(player, qi);
	}

	public static void ceremony(ServerPlayerEntity player, PlayerQi qi) {
		SpiritRoot root = qi.getRoot();
		if (root == null) return;
		ServerWorld world = player.getServerWorld();
		Vec3d pos = player.getPos();
		int color = root.getRgb();
		ModPackets.sendFx(world, FxData.at(FxType.MAGIC_CIRCLE, pos.add(0, 0.05, 0), color, 3.0F, 80).withExtra(1));
		ModPackets.sendFx(world, FxData.follow(FxType.QI_AURA, player.getId(), pos, color, 1.1F, 80));
		ModPackets.sendFx(world, FxData.at(FxType.HEAVEN_PILLAR, pos, color, 1.2F, 50));
		ModPackets.sendFx(world, FxData.at(FxType.SHOCKWAVE_RING, pos.add(0, 0.1, 0), color, 3.0F, 16));
		SkillFx.glowBurst(world, pos.add(0, 1.0, 0), color, 50, 0.9F, 0.25);
		SkillFx.runes(world, pos.add(0, 0.3, 0), 16, 2.5);
		world.playSound(null, player.getBlockPos(), ModSounds.QI_GATHER, SoundCategory.PLAYERS, 1.0F, 1.1F);
		world.playSound(null, player.getBlockPos(), ModSounds.BREAKTHROUGH, SoundCategory.PLAYERS, 1.0F, 1.2F);

		if (player.networkHandler != null) {
			player.networkHandler.sendPacket(new TitleFadeS2CPacket(10, 70, 20));
			player.networkHandler.sendPacket(new SubtitleS2CPacket(Text.empty().append(root.getTypeName()).append(" · ").append(root.getGradeName())));
			player.networkHandler.sendPacket(new TitleS2CPacket(Text.translatable("title.celestialarts.awakening").formatted(Formatting.GOLD)));
		}
		player.sendMessage(summary(qi), false);
	}

	/** Multi-line description of root, talent and aptitude for chat / the command. */
	public static Text summary(PlayerQi qi) {
		SpiritRoot root = qi.getRoot();
		int apt = CultivationStats.aptitude(qi);
		MutableText t = Text.translatable("message.celestialarts.awakening_header").formatted(Formatting.GOLD);
		t.append("\n").append(Text.translatable("message.celestialarts.awakening_root",
				root == null ? Text.literal("-") : root.getTypeName(),
				root == null ? Text.literal("-") : root.getKindsText(),
				root == null ? Text.literal("-") : root.getGradeName()).formatted(Formatting.WHITE));
		t.append("\n").append(Text.translatable("message.celestialarts.awakening_talent", qi.getTalent().getName(), qi.getTalent().getRarity().getName()).formatted(Formatting.WHITE));
		t.append("\n  ").append(qi.getTalent().getDescription());
		t.append("\n").append(Text.translatable("message.celestialarts.awakening_aptitude", apt,
				Text.translatable("aptitude.celestialarts." + CultivationStats.aptitudeTier(apt)),
				Math.round(CultivationStats.powerMultiplier(qi) * 100)).formatted(Formatting.WHITE));
		return t;
	}
}
