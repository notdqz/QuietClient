package com.quiet.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.quiet.module.impl.XRay;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import java.util.List;

@Mixin(net.minecraft.client.renderer.block.ModelBlockRenderer.class)
public abstract class ModelBlockRendererMixin {
	/** X-ray: work out this block's alpha before its quads are written; 0 means skip the block entirely. */
	@Inject(method = {"tessellateWithAO", "tessellateWithoutAO"}, at = @At("HEAD"), cancellable = true)
	private void quiet$begin(BlockAndTintGetter level, List<BlockModelPart> parts, BlockState state, BlockPos pos,
	                         PoseStack poseStack, VertexConsumer consumer, boolean cull, int overlay, CallbackInfo ci) {
		int a = XRay.alpha(state);
		XRay.ALPHA.set(a);
		if (a == 0) ci.cancel();
	}

	/** ...and write it into every quad of that block (argument 6 of putBulkData is the alpha). */
	@ModifyArgs(method = "putQuadData", at = @At(value = "INVOKE",
			target = "Lcom/mojang/blaze3d/vertex/VertexConsumer;putBulkData(Lcom/mojang/blaze3d/vertex/PoseStack$Pose;Lnet/minecraft/client/renderer/block/model/BakedQuad;[FFFFF[II)V"))
	private void quiet$alpha(Args args) {
		int a = XRay.ALPHA.get();
		if (a > 0 && a < 255) args.set(6, a / 255f);
	}
}
