package com.quiet.mixin;

import com.quiet.module.impl.Rotations;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Swaps in the eased "server" rotation only while the movement packet is being built. */
@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin {
	@Unique private float quiet$y, quiet$x;
	@Unique private boolean quiet$swapped;

	@Inject(method = "sendPosition", at = @At("HEAD"), require = 0)
	private void quiet$pre(CallbackInfo ci) {
		LocalPlayer self = (LocalPlayer) (Object) this;
		float[] r = Rotations.serverRotation(self.getYRot(), self.getXRot());
		if (r == null) return;
		quiet$y = self.getYRot();
		quiet$x = self.getXRot();
		self.setYRot(r[0]);
		self.setXRot(r[1]);
		quiet$swapped = true;
	}

	@Inject(method = "sendPosition", at = @At("RETURN"), require = 0)
	private void quiet$post(CallbackInfo ci) {
		if (!quiet$swapped) return;
		LocalPlayer self = (LocalPlayer) (Object) this;
		self.setYRot(quiet$y);
		self.setXRot(quiet$x);
		quiet$swapped = false;
	}
}
