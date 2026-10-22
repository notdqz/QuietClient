package com.quiet.module.impl;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;

/**
 * Detects "we just swung at something": the attack cooldown snaps back to ~0
 * while the crosshair is on a living entity. Call tick() once per client tick,
 * before the modules run.
 */
public final class HitTracker {
	public static boolean hit;
	private static float last = 1f;

	public static void tick(Minecraft mc) {
		hit = false;
		if (mc.player == null) { last = 1f; return; }
		float s = mc.player.getAttackStrengthScale(0f);
		if (last > 0.5f && s < 0.2f && mc.crosshairPickEntity instanceof LivingEntity) hit = true;
		last = s;
	}
}
