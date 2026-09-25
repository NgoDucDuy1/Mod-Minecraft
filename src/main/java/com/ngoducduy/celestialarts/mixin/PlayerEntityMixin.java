package com.ngoducduy.celestialarts.mixin;

import com.ngoducduy.celestialarts.cultivation.PlayerQi;
import com.ngoducduy.celestialarts.cultivation.QiHolder;
import com.ngoducduy.celestialarts.cultivation.RealmPassives;
import net.minecraft.entity.damage.DamageSource;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Attaches the cultivation data to every player and persists it in the player NBT.
 */
@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin implements QiHolder {
	@Unique
	private final PlayerQi celestialarts$qi = new PlayerQi();

	@Override
	public PlayerQi celestialarts$getQi() {
		return celestialarts$qi;
	}

	/** The cultivator's body shrugs off falls, drowning and fire depending on realm (see RealmPassives). */
	@Inject(method = "damage(Lnet/minecraft/entity/damage/DamageSource;F)Z", at = @At("HEAD"), cancellable = true)
	private void celestialarts$bodyImmunity(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
		PlayerEntity self = (PlayerEntity) (Object) this;
		if (!self.getWorld().isClient && RealmPassives.ignoresDamage(self, source)) {
			cir.setReturnValue(false);
		}
	}

	@Inject(method = "writeCustomDataToNbt", at = @At("TAIL"))
	private void celestialarts$write(NbtCompound nbt, CallbackInfo ci) {
		nbt.put("CelestialArts", celestialarts$qi.writeNbt(new NbtCompound()));
	}

	@Inject(method = "readCustomDataFromNbt", at = @At("TAIL"))
	private void celestialarts$read(NbtCompound nbt, CallbackInfo ci) {
		if (nbt.contains("CelestialArts", NbtCompound.COMPOUND_TYPE)) {
			celestialarts$qi.readNbt(nbt.getCompound("CelestialArts"));
		}
	}
}
