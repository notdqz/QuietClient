package com.quiet.mixin;

import com.quiet.module.impl.FreeLook;
import com.quiet.module.impl.Fullbright;
import net.minecraft.client.Camera;
import net.minecraft.world.level.material.FogType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Camera.class)
public abstract class CameraMixin {
	@ModifyVariable(method = "setRotation", at = @At("HEAD"), argsOnly = true, ordinal = 0)
	private float quiet$yaw(float yaw) { return FreeLook.active ? FreeLook.yaw : yaw; }

	@ModifyVariable(method = "setRotation", at = @At("HEAD"), argsOnly = true, ordinal = 1)
	private float quiet$pitch(float pitch) { return FreeLook.active ? FreeLook.pitch : pitch; }

	/** Fullbright: pretend we're never inside water/lava, so no liquid fog or tint. */
	@Inject(method = "getFluidInCamera", at = @At("RETURN"), cancellable = true)
	private void quiet$fluid(CallbackInfoReturnable<FogType> cir) {
		FogType t = cir.getReturnValue();
		if ((t == FogType.WATER || t == FogType.LAVA) && Fullbright.clearFog())
			cir.setReturnValue(FogType.ATMOSPHERIC);
	}
}
