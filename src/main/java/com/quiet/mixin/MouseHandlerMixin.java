package com.quiet.mixin;

import com.quiet.module.impl.CameraClip;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {
	/** Camera Clip: zoom key + scroll changes the camera distance instead of the hotbar. */
	@Inject(method = "onScroll", at = @At("HEAD"), cancellable = true, require = 0)
	private void quiet$scroll(long window, double xOffset, double yOffset, CallbackInfo ci) {
		if (CameraClip.handleScroll(yOffset)) ci.cancel();
	}
}
