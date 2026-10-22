package com.quiet.module.impl;

import com.quiet.module.Category;
import com.quiet.module.Module;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public class Bhop extends Module {
	private boolean forced;

	public Bhop() { super("Bhop", Category.MOVEMENT); }

	@Override
	protected void onDisable() { if (forced) { forced = false; KeyMapping.setAll(); } }

	@Override
	protected void onTick() {
		Minecraft mc = mc();
		LocalPlayer p = mc.player;
		boolean go = mc.screen == null && ModUtil.isMoving(mc) && p.onGround()
				&& !p.isShiftKeyDown() && !p.isInWater() && !p.isInLava() && !p.onClimbable();
		if (go) { mc.options.keyJump.setDown(true); forced = true; }
		else if (forced) { forced = false; KeyMapping.setAll(); }
	}
}
