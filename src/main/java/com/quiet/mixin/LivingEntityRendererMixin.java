package com.quiet.mixin;

import com.quiet.module.impl.Rotations;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Draws your own model turning like the server sees it: head first, then the body follows. Rendering only. */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {
	@Inject(method = "extractRenderState", at = @At("RETURN"), require = 0)
	private void quiet$visual(LivingEntity entity, LivingEntityRenderState state, float partialTick, CallbackInfo ci) {
		if (entity != Minecraft.getInstance().player) return;
		float[] v = Rotations.visual(state.bodyRot + state.yRot, state.xRot);
		if (v == null) return;

		float body = state.bodyRot;
		float diff = Mth.wrapDegrees(v[0] - body);
		float limit = 35f;                                  // the head may turn this far before the body follows
		if (Math.abs(diff) > limit) body += diff - Math.signum(diff) * limit;
		state.bodyRot = body;
		state.yRot = Mth.clamp(Mth.wrapDegrees(v[0] - body), -75f, 75f);
		state.xRot = v[1];
	}
}
