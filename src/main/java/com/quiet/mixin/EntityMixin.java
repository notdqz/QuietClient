package com.quiet.mixin;

import com.quiet.module.impl.FreeLook;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityMixin {
	/** Free Look: mouse input goes to the camera instead of the player. */
	@Inject(method = "turn", at = @At("HEAD"), cancellable = true)
	private void quiet$turn(double yRot, double xRot, CallbackInfo ci) {
		if (FreeLook.active && (Object) this == Minecraft.getInstance().player) {
			FreeLook.turn(yRot, xRot);
			ci.cancel();
		}
	}
}
