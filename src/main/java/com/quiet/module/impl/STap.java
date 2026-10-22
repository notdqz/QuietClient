package com.quiet.module.impl;

import com.quiet.module.Category;
import com.quiet.module.Module;
import com.quiet.module.setting.NumberSetting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

/** Needs HitTracker.tick(mc) to run first each tick. */
public class STap extends Module {
	private final NumberSetting ticks = num("Tap ticks", 1, 1, 4, 1);
	private final NumberSetting chance = num("Chance %", 100, 0, 100, 5);
	private int left;

	public STap() { super("STap", Category.COMBAT); }

	@Override
	protected void onDisable() { if (left > 0) { left = 0; KeyMapping.setAll(); } }

	@Override
	protected void onTick() {
		Minecraft mc = mc();
		if (left > 0) {
			mc.options.keyDown.setDown(true);
			if (--left == 0) KeyMapping.setAll();
			return;
		}
		if (HitTracker.hit && mc.options.keyUp.isDown() && Math.random() * 100 < chance.get()) {
			left = ticks.asInt();
			mc.options.keyDown.setDown(true);
		}
	}
}
