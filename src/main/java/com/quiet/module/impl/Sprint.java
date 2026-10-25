package com.quiet.module.impl;

import com.quiet.module.Category;
import com.quiet.module.Module;
import com.quiet.module.Util;
import net.minecraft.client.Minecraft;

/**
 * Sprints whenever you walk forward by holding the real sprint key for you. Since 1.21.2 the client reports its key
 * state to the server every tick (forward, back, left, right, jump, sneak and SPRINT), so the sprint flag has to be
 * true there too; setting the sprint state directly would leave that flag false while you move at sprint speed.
 * The game itself still decides whether sprinting actually starts (food, walls, sneaking, eating...).
 */
public class Sprint extends Module {
	private boolean held;

	public Sprint() { super("Sprint", Category.MOVEMENT); }

	@Override
	protected void onDisable() {
		if (held) { held = false; Util.releaseKey(mc().options.keySprint); }
	}

	@Override
	protected void onTick() {
		Minecraft mc = mc();
		if (mc.screen != null) return;
		boolean forward = mc.options.keyUp.isDown() && !mc.options.keyDown.isDown();
		if (forward) {
			mc.options.keySprint.setDown(true);
			held = true;
		} else if (held) {
			held = false;
			Util.releaseKey(mc.options.keySprint);
		}
	}
}
