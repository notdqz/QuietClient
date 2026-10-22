package com.quiet.mixin;

import com.quiet.module.impl.HitParticles;
import com.quiet.module.impl.KnockbackFlick;
import com.quiet.module.impl.TestDummy;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MultiPlayerGameMode.class)
public abstract class MultiPlayerGameModeMixin {
	@Inject(method = "attack", at = @At("HEAD"), cancellable = true, require = 0)
	private void quiet$attack(Player player, Entity target, CallbackInfo ci) {
		HitParticles.onAttack(player, target);
		float flick = KnockbackFlick.plan(player, target);
		if (TestDummy.onAttack(player, target, flick)) { ci.cancel(); return; }   // client-only dummy: no packet
		if (!Float.isNaN(flick)) KnockbackFlick.before(player, flick);             // turn, then the attack packet follows
	}

	@Inject(method = "attack", at = @At("RETURN"), require = 0)
	private void quiet$attackDone(Player player, Entity target, CallbackInfo ci) {
		KnockbackFlick.after(player);                                              // back to your real rotation
	}
}
