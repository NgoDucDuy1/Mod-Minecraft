package com.ngoducduy.celestialarts.registry;

import com.ngoducduy.celestialarts.CelestialArts;
import com.ngoducduy.celestialarts.cultivation.Realm;
import net.minecraft.advancement.Advancement;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

/**
 * Grants the mod's code-driven advancements (data/celestialarts/advancements/**). Every advancement
 * uses a single {@code minecraft:impossible} criterion named {@value #CRITERION} which is granted here.
 */
public final class ModAdvancements {
	public static final String CRITERION = "granted";

	private ModAdvancements() {
	}

	/** Grants {@code celestialarts:<path>} to the player if the advancement exists. */
	public static void grant(ServerPlayerEntity player, String path) {
		MinecraftServer server = player.getServer();
		if (server == null) return;
		Advancement advancement = server.getAdvancementLoader().get(CelestialArts.id(path));
		if (advancement == null) return;
		player.getAdvancementTracker().grantCriterion(advancement, CRITERION);
	}

	public static void onRealmReached(ServerPlayerEntity player, Realm realm) {
		grant(player, "realm/" + realm.getKey());
	}

	public static void onSkillLearned(ServerPlayerEntity player, Identifier skillId) {
		grant(player, "learn_first_skill");
		grant(player, "skills/" + skillId.getPath());
	}

	public static void onAllSkillsLearned(ServerPlayerEntity player) {
		grant(player, "learn_all_skills");
	}
}
