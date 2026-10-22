package com.quiet.mixin;

import com.quiet.module.impl.XRay;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemBlockRenderTypes.class)
public abstract class ItemBlockRenderTypesMixin {
	/** X-ray: see-through blocks have to be drawn in the translucent layer. */
	@Inject(method = "getChunkRenderType", at = @At("HEAD"), cancellable = true, require = 0)
	private static void quiet$layer(BlockState state, CallbackInfoReturnable<ChunkSectionLayer> cir) {
		int a = XRay.alpha(state);
		if (a > 0 && a < 255) cir.setReturnValue(ChunkSectionLayer.TRANSLUCENT);
	}
}
