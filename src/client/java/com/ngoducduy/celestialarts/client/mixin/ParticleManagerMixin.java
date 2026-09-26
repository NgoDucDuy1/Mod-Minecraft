package com.ngoducduy.celestialarts.client.mixin;

import com.google.common.collect.ImmutableList;
import com.ngoducduy.celestialarts.client.particle.ModParticleSheets;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.client.particle.ParticleTextureSheet;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Registers the mod's additive particle texture sheet so {@code renderParticles} draws it.
 */
@Mixin(ParticleManager.class)
public abstract class ParticleManagerMixin {
	@Mutable
	@Shadow
	@Final
	private static List<ParticleTextureSheet> PARTICLE_TEXTURE_SHEETS;

	@Inject(method = "<clinit>", at = @At("TAIL"))
	private static void celestialarts$addSheets(CallbackInfo ci) {
		PARTICLE_TEXTURE_SHEETS = ImmutableList.<ParticleTextureSheet>builder()
				.addAll(PARTICLE_TEXTURE_SHEETS)
				.add(ModParticleSheets.ADDITIVE)
				.build();
	}
}
