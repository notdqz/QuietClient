package com.quiet.mixin;

import com.quiet.module.impl.Velocity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {
	@Inject(method = "handleSetEntityMotion", at = @At("HEAD"), cancellable = true, require = 0)
	private void quiet$kbHead(ClientboundSetEntityMotionPacket pkt, CallbackInfo ci) {
		Minecraft mc = Minecraft.getInstance();
		if (!mc.isSameThread() || mc.player == null || pkt.getId() != mc.player.getId()) return;
		if (Velocity.onPacket(pkt)) ci.cancel();
	}

	@Inject(method = "handleSetEntityMotion", at = @At("RETURN"), require = 0)
	private void quiet$kbTail(ClientboundSetEntityMotionPacket pkt, CallbackInfo ci) {
		Minecraft mc = Minecraft.getInstance();
		if (!mc.isSameThread() || mc.player == null || pkt.getId() != mc.player.getId()) return;
		Velocity.afterPacket();
	}
}
