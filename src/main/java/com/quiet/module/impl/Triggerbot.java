package com.quiet.module.impl;

import com.quiet.module.Category;
import com.quiet.module.Module;
import com.quiet.module.setting.BooleanSetting;
import com.quiet.module.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

public class Triggerbot extends Module {
	private final NumberSetting cooldown = num("Cooldown", 0.92, 0.5, 1.0, 0.01);
	private final NumberSetting delay = num("Reaction (ticks)", 1, 0, 6, 1);
	private final BooleanSetting crits = bool("Crits only", false);
	private int onTarget;

	public Triggerbot() { super("Triggerbot", Category.COMBAT); }

	@Override
	protected void onDisable() { onTarget = 0; }

	@Override
	protected void onTick() {
		Minecraft mc = mc();
		Entity e = mc.crosshairPickEntity;
		if (!ModUtil.canAct(mc) || e == null || !ModUtil.validTarget(mc, e)) { onTarget = 0; return; }
		onTarget++;
		if (mc.player.isUsingItem()) return;
		if (onTarget <= delay.asInt()) return;
		if (ModUtil.cooldown(mc) < cooldown.get()) return;
		if (crits.get() && !CritHelper.ready(mc, true)) return;
		ModUtil.attack(mc, e);
	}
}
