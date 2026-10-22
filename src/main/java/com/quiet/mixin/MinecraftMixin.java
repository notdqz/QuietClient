package com.quiet.mixin;

import com.quiet.module.impl.SilentAim;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
	/** Silent Aim: a click that would swing at air can be taken over and aimed at a nearby player. */
	@Inject(method = "startAttack", at = @At("HEAD"), cancellable = true, require = 0)
	private void quiet$click(CallbackInfoReturnable<Boolean> cir) {
		if (SilentAim.onClick()) cir.setReturnValue(true);
	}
}
