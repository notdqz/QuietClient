package com.quiet.mixin;

import com.quiet.module.impl.XRay;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Block.class)
public abstract class BlockFaceMixin {
	/** X-ray: ores always draw every face, even the ones touching stone. */
	@Inject(method = "shouldRenderFace", at = @At("HEAD"), cancellable = true, require = 0)
	private static void quiet$face(BlockState state, BlockState neighbor, Direction face, CallbackInfoReturnable<Boolean> cir) {
		if (XRay.active() && XRay.isOre(state)) cir.setReturnValue(true);
	}
}
