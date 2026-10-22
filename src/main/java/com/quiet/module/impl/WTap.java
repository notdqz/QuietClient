package com.quiet.module.impl;

import com.quiet.module.Category;
import com.quiet.module.Module;
import com.quiet.module.setting.NumberSetting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

/** Needs HitTracker.tick(mc) to run first each tick. Works best with Auto Sprint on. */
public class WTap extends Module {
	private final NumberSetting ticks = num("Release ticks", 1, 1, 4, 1);
	private final NumberSetting chance = num("Chance %", 100, 0, 100, 5);
	private int left;

	public WTap() { super("WTap", Category.COMBAT); }

	@Override
	protected void onDisable() { if (left > 0) { left = 0; KeyMapping.setAll(); } }

	@Override
	protected void onTick() {
		Minecraft mc = mc();
		if (left > 0) {
			mc.options.keyUp.setDown(false);
			mc.player.setSprinting(false);
			if (--left == 0) KeyMapping.setAll();   // restore real key state
			return;
		}
		if (HitTracker.hit && mc.options.keyUp.isDown() && Math.random() * 100 < chance.get()) {
			left = ticks.asInt();
			mc.options.keyUp.setDown(false);
			mc.player.setSprinting(false);
		}
	}
}
