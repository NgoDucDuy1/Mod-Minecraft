package com.ngoducduy.celestialarts.cultivation;

import net.minecraft.entity.player.PlayerEntity;

/**
 * Implemented onto {@link PlayerEntity} through a mixin so every player carries
 * a {@link PlayerQi} instance without needing an external component library.
 */
public interface QiHolder {
	PlayerQi celestialarts$getQi();

	static PlayerQi get(PlayerEntity player) {
		return ((QiHolder) player).celestialarts$getQi();
	}
}
