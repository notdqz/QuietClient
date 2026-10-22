package com.quiet.mixin;

import com.quiet.module.impl.BackTrack;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundEntityPositionSyncPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Separate class so a mismatch here can't take the other BackTrack hooks down with it. */
@Mixin(ClientPacketListener.class)
public abstract class BackTrackSyncMixin {
	@Inject(method = "handleEntityPositionSync", at = @At("HEAD"), require = 0)
	private void quiet$sync(ClientboundEntityPositionSyncPacket pkt, CallbackInfo ci) {
		BackTrack.onSync(pkt.id());
	}
}
