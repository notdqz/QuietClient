package com.quiet.mixin;

import com.quiet.render.RenderCapture;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LevelRenderer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Peeks at renderLevel's arguments (by type, so the exact signature doesn't matter). */
@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
	@ModifyVariable(method = "renderLevel", at = @At("HEAD"), argsOnly = true, ordinal = 0, require = 0)
	private Matrix4f quiet$m0(Matrix4f m) { RenderCapture.first(m); return m; }

	@ModifyVariable(method = "renderLevel", at = @At("HEAD"), argsOnly = true, ordinal = 1, require = 0)
	private Matrix4f quiet$m1(Matrix4f m) { RenderCapture.second(m); return m; }

	@ModifyVariable(method = "renderLevel", at = @At("HEAD"), argsOnly = true, ordinal = 0, require = 0)
	private Camera quiet$cam(Camera c) { RenderCapture.camera(c); return c; }
}
