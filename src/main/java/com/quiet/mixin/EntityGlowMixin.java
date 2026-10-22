package com.quiet.mixin;

import com.quiet.module.impl.Chams;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityGlowMixin {
	@Inject(method = "isCurrentlyGlowing", at = @At("HEAD"), cancellable = true, require = 0)
	private void quiet$glow(CallbackInfoReturnable<Boolean> cir) {
		if (Chams.glows((Entity) (Object) this)) cir.setReturnValue(true);
	}

	@Inject(method = "getTeamColor", at = @At("HEAD"), cancellable = true, require = 0)
	private void quiet$color(CallbackInfoReturnable<Integer> cir) {
		if (Chams.glows((Entity) (Object) this)) cir.setReturnValue(Chams.rgb());
	}
}
