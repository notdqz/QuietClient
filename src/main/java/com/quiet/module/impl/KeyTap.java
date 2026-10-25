package com.quiet.module.impl;

import com.quiet.module.Category;
import com.quiet.module.Module;
import com.quiet.module.Setting;
import com.quiet.module.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/**
 * Resets your sprint right after you land a hit so the next hit knocks back harder.
 * W-Tap: releases W for a moment.  S-Tap: taps S for a moment.
 * It only presses and releases real keys, so the key state the client reports to the server matches what you move
 * like (the game itself stops your sprint when forward is released). Hits arrive from MultiPlayerGameModeMixin.
 * Works best with Sprint.
 */
public class KeyTap extends Module {
	private static KeyTap inst;
	private final Setting.Mode mode = mode("Mode", "W-Tap", "W-Tap", "S-Tap");
	private final Setting.Num ticks = num("Tap ticks", 1, 1, 4, 1);
	private final Setting.Num chance = num("Chance %", 100, 0, 100, 5);
	private int left;
	private boolean hitPending;

	public KeyTap() { super("Key Tap", Category.COMBAT); inst = this; }

	/** Called from MultiPlayerGameModeMixin for every attack you send. */
	public static void onAttack(Entity target) {
		if (inst != null && inst.isEnabled() && target instanceof LivingEntity) inst.hitPending = true;
	}

	private void release(Minecraft mc) {
		Util.releaseKey(mc.options.keyUp);
		Util.releaseKey(mc.options.keyDown);
	}

	@Override
	protected void onDisable() {
		hitPending = false;
		if (left > 0) { left = 0; release(mc()); }
	}

	private void press(Minecraft mc) {
		if (mode.is("S-Tap")) mc.options.keyDown.setDown(true);
		else mc.options.keyUp.setDown(false);
	}

	@Override
	protected void onTick() {
		Minecraft mc = mc();
		boolean hit = hitPending;
		hitPending = false;

		if (left > 0) {
			press(mc);
			if (--left == 0) release(mc);
			return;
		}
		if (hit && mc.options.keyUp.isDown() && Math.random() * 100 < chance.get()) {
			left = ticks.asInt();
			press(mc);
		}
	}
}
