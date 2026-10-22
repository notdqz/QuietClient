package com.quiet.mixin;

import com.quiet.module.impl.CustomSky;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelRenderer.class)
public abstract class CloudRendererMixin {

    @Inject(method = "renderClouds", at = @At("HEAD"), cancellable = true, require = 0)
    private void quiet$hideClouds(CallbackInfoReturnable<Boolean> cir) {
        if (CustomSky.shouldHideClouds()) {
            cir.setReturnValue(false);
        }
    }
}