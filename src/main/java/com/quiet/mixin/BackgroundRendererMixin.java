package com.quiet.mixin;

import com.quiet.module.impl.CustomSky;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogRenderer;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FogRenderer.class)
public abstract class BackgroundRendererMixin {

    /** Overrides the computed fog color to match your custom presets seamlessly. */
    @Inject(method = "computeFogColor", at = @At("RETURN"), cancellable = true)
    private void quiet$computeFogColor(Camera camera, float partialTick, ClientLevel level, int renderDistance, float darkenWorldAmount, CallbackInfoReturnable<Vector4f> cir) {
        Vector4f original = cir.getReturnValue();
        if (original != null) {
            Vector4f customColor = CustomSky.fogColor(new Vector4f(original));
            if (customColor != null) {
                cir.setReturnValue(customColor);
            }
        }
    }
}