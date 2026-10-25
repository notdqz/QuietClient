package com.quiet.mixin;

import com.quiet.module.impl.BackTrack;
import com.quiet.module.impl.Velocity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundEntityPositionSyncPacket;
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {
	/** Velocity: after the knockback packet for YOU was applied on the main thread, scale the new motion. */
	@Inject(method = "handleSetEntityMotion", at = @At("RETURN"))
	private void quiet$kb(ClientboundSetEntityMotionPacket pkt, CallbackInfo ci) {
		Minecraft mc = Minecraft.getInstance();
		if (!mc.isSameThread() || mc.player == null || pkt.getId() != mc.player.getId()) return;
		Velocity.afterPacket();
	}

	/** BackTrack: hold a nearby player's small movement updates for a moment (replayed later). */
	@Inject(method = "handleMoveEntity", at = @At("HEAD"), cancellable = true)
	private void quiet$move(ClientboundMoveEntityPacket pkt, CallbackInfo ci) {
		if (BackTrack.onMove(pkt)) ci.cancel();
	}

	/** BackTrack: a teleport / position sync must play back anything held for that entity first. */
	@Inject(method = "handleTeleportEntity", at = @At("HEAD"))
	private void quiet$teleport(ClientboundTeleportEntityPacket pkt, CallbackInfo ci) {
		BackTrack.onSync(pkt.id());
	}

	@Inject(method = "handleEntityPositionSync", at = @At("HEAD"))
	private void quiet$sync(ClientboundEntityPositionSyncPacket pkt, CallbackInfo ci) {
		BackTrack.onSync(pkt.id());
	}
}
