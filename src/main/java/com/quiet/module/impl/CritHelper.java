package com.quiet.module.impl;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.effect.MobEffects;

/**
 * Vanilla crit rules: falling, not on ground/ladder/water, not blind, not riding and NOT SPRINTING.
 * ready() drops sprint while you're falling at a target, and only reports true on a LATER tick, once the
 * server has actually received the "stop sprinting" packet (otherwise the server still counts you as sprinting).
 */
public final class CritHelper {
	public static boolean noSprint;

	public static boolean ready(Minecraft mc, boolean hasTarget) {
		LocalPlayer p = mc.player;
		boolean air = !p.onGround() && !p.onClimbable() && !p.isInWater() && !p.isInLava()
				&& !p.hasEffect(MobEffects.BLINDNESS) && !p.isPassenger();
		boolean falling = air && p.fallDistance > 0 && p.getDeltaMovement().y < 0;
		boolean wasSprinting = p.isSprinting();
		if (falling && hasTarget) {
			noSprint = true;
			if (wasSprinting) {
				p.setSprinting(false);
				mc.options.keySprint.setDown(false);
			}
		}
		return falling && !wasSprinting;
	}
}
