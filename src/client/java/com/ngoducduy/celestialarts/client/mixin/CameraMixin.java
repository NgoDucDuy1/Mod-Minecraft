package com.ngoducduy.celestialarts.client.mixin;

import com.ngoducduy.celestialarts.client.CameraShake;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Applies {@link CameraShake} as a small positional jitter after the camera has been positioned. */
@Mixin(Camera.class)
public abstract class CameraMixin {
	@Shadow
	protected abstract void moveBy(double x, double y, double z);

	@Inject(method = "update", at = @At("TAIL"))
	private void celestialarts$shake(BlockView area, Entity focusedEntity, boolean thirdPerson, boolean inverseView, float tickDelta, CallbackInfo ci) {
		if (!CameraShake.isActive()) return;
		// moveBy(x = forward, y = up, z = right) in camera space.
		this.moveBy(0.0, CameraShake.offsetY(tickDelta), CameraShake.offsetX(tickDelta));
	}
}
