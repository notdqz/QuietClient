package com.quiet.mixin;

import com.quiet.module.impl.BackTrack;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class BackTrackMixin {
	@Inject(method = "handleMoveEntity", at = @At("HEAD"), cancellable = true, require = 0)
	private void quiet$move(ClientboundMoveEntityPacket pkt, CallbackInfo ci) {
		if (BackTrack.onMove(pkt)) ci.cancel();
	}

	@Inject(method = "handleTeleportEntity", at = @At("HEAD"), require = 0)
	private void quiet$teleport(ClientboundTeleportEntityPacket pkt, CallbackInfo ci) {
		BackTrack.onSync(pkt.id());
	}
}
