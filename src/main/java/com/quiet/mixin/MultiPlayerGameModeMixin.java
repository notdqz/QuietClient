package com.quiet.mixin;

import com.quiet.module.impl.HitEffect;
import com.quiet.module.impl.KeyTap;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MultiPlayerGameMode.class)
public abstract class MultiPlayerGameModeMixin {
	/** Every attack you send, from clicks and from modules: tells Hit Effect and Key Tap. */
	@Inject(method = "attack", at = @At("HEAD"))
	private void quiet$attack(Player player, Entity target, CallbackInfo ci) {
		HitEffect.onAttack(player, target);
		KeyTap.onAttack(target);
	}
}
