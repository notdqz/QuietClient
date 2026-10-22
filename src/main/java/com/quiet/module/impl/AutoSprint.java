package com.quiet.module.impl;

import com.quiet.module.Category;
import com.quiet.module.Module;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

public class AutoSprint extends Module {
	public AutoSprint() { super("Auto Sprint", Category.MOVEMENT); }

	@Override
	protected void onDisable() { KeyMapping.setAll(); }

	@Override
	protected void onTick() {
		Minecraft mc = mc();
		if (CritHelper.noSprint) { CritHelper.noSprint = false; return; }   // a crit is lining up
		if (mc.screen != null) return;
		boolean forward = mc.options.keyUp.isDown() && !mc.options.keyDown.isDown();
		if (forward && !mc.player.isShiftKeyDown() && !mc.player.isUsingItem())
			mc.options.keySprint.setDown(true);
	}
}
