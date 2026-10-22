package com.quiet.mixin;

import com.quiet.module.impl.CustomSky;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SkyRenderer;
import net.minecraft.client.renderer.state.SkyRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SkyRenderer.class)
public abstract class SkyRendererMixin {

    @Inject(method = "extractRenderState", at = @At("TAIL"), require = 0)
    private void quiet$sky(ClientLevel level, float partialTick, Camera camera, SkyRenderState state, CallbackInfo ci) {
        CustomSky.applySky(state);
    }

    // Suppress sun and moon rendering if enabled
    @Inject(method = "renderSunMoon", at = @At("HEAD"), cancellable = true, require = 0)
    private void quiet$hideSunMoon(CallbackInfo ci) {
        if (CustomSky.shouldHideCelestials()) {
            ci.cancel();
        }
    }
}