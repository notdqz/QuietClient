package com.quiet.mixin;

import com.quiet.module.impl.FreeLook;
import com.quiet.module.impl.NoSlow;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityMixin {
	/** Free Look: mouse input goes to the camera instead of the player. */
	@Inject(method = "turn", at = @At("HEAD"), cancellable = true, require = 0)
	private void quiet$turn(double yRot, double xRot, CallbackInfo ci) {
		if (FreeLook.active && (Object) this == Minecraft.getInstance().player) {
			FreeLook.turn(yRot, xRot);
			ci.cancel();
		}
	}

	/** No Slow: cobwebs / berry bushes. */
	@Inject(method = "makeStuckInBlock", at = @At("HEAD"), cancellable = true, require = 0)
	private void quiet$stuck(BlockState state, Vec3 multiplier, CallbackInfo ci) {
		if ((Object) this == Minecraft.getInstance().player && NoSlow.blocksStuck()) ci.cancel();
	}
}
