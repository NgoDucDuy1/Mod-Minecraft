package com.ngoducduy.celestialarts.client.mixin;

import com.ngoducduy.celestialarts.entity.FlyingSwordEntity;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Players standing on a flying sword should stand, not sit: clears the model's {@code riding}
 * flag before the pose is computed so legs stay straight.
 */
@Mixin(BipedEntityModel.class)
public abstract class BipedEntityModelMixin<T extends LivingEntity> {
	@Inject(method = "setAngles(Lnet/minecraft/entity/LivingEntity;FFFFF)V", at = @At("HEAD"))
	private void celestialarts$standOnSword(T entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch, CallbackInfo ci) {
		if (entity.getVehicle() instanceof FlyingSwordEntity) {
			((BipedEntityModel<?>) (Object) this).riding = false;
		}
	}
}
