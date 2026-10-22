package com.quiet.module.impl;

import com.quiet.module.Category;
import com.quiet.module.Module;
import com.quiet.module.setting.BooleanSetting;
import com.quiet.module.setting.ModeSetting;
import com.quiet.module.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.HitResult;

import java.util.concurrent.ThreadLocalRandom;

public class AutoClicker extends Module {
	private final ModeSetting mode = mode("Mode", "CPS", "CPS", "Cooldown");
	private final NumberSetting minCps = num("Min CPS", 8, 1, 20, 1);
	private final NumberSetting maxCps = num("Max CPS", 12, 1, 20, 1);
	private final NumberSetting cooldown = num("Cooldown", 0.92, 0.5, 1.0, 0.01);
	private final BooleanSetting crits = bool("Crits only", false);
	private double acc;

	public AutoClicker() {
		super("Auto Clicker", Category.COMBAT);
		minCps.visibleWhen(() -> mode.is("CPS"));
		maxCps.visibleWhen(() -> mode.is("CPS"));
		cooldown.visibleWhen(() -> mode.is("Cooldown"));
	}

	@Override
	protected void onTick() {
		Minecraft mc = mc();
		if (!ModUtil.canAct(mc) || !mc.options.keyAttack.isDown()) { acc = 0; return; }
		HitResult hr = mc.hitResult;
		if (hr == null || hr.getType() == HitResult.Type.BLOCK) return;   // vanilla handles mining
		if (mc.player.isUsingItem()) return;

		Entity e = mc.crosshairPickEntity;
		boolean click;
		if (mode.is("Cooldown")) {
			click = ModUtil.cooldown(mc) >= cooldown.get();
		} else {
			double lo = Math.min(minCps.get(), maxCps.get()), hi = Math.max(minCps.get(), maxCps.get());
			acc += (lo + ThreadLocalRandom.current().nextDouble() * (hi - lo)) / 20.0;
			click = acc >= 1;
		}
		if (!click) return;

		// with Crits only, wait for a falling, non-sprinting moment (only matters when aimed at something)
		if (crits.get() && e != null && !CritHelper.ready(mc, true)) { acc = Math.min(acc, 1.0); return; }
		if (acc >= 1) acc -= 1;

		if (e != null) ModUtil.attack(mc, e);
		else mc.player.swing(InteractionHand.MAIN_HAND);
	}
}
