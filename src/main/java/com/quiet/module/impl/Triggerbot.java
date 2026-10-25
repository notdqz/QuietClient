package com.quiet.module.impl;

import com.quiet.module.Category;
import com.quiet.module.Module;
import com.quiet.module.Setting;
import com.quiet.module.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Hits a player the moment your crosshair is on them. It does not aim for you: you put the crosshair on them, it
 * clicks. It waits a random reaction time after the crosshair lands on someone, and only swings once your weapon
 * has recharged to a slightly different level each time, so it does not fire the instant a target appears.
 */
public class Triggerbot extends Module {
	private final Setting.Num minReact = num("Min reaction (ms)", 60, 0, 400, 10);
	private final Setting.Num maxReact = num("Max reaction (ms)", 140, 0, 400, 10);
	private final Setting.Num cooldown = num("Cooldown", 0.92, 0.5, 1.0, 0.01);
	private final Setting.Num maxCps = num("Max CPS", 10, 1, 20, 1);

	private Entity last;
	private long acquired, reaction;
	private double cdTarget = -1;
	private long nextHit;

	public Triggerbot() { super("Triggerbot", Category.COMBAT); }

	@Override protected void onDisable() { last = null; cdTarget = -1; }

	@Override
	protected void onTick() {
		Minecraft mc = mc();
		Entity e = mc.crosshairPickEntity;
		if (!Util.canAct(mc) || e == null || !Util.validTarget(mc, e)) { last = null; return; }

		ThreadLocalRandom rnd = ThreadLocalRandom.current();
		long now = System.currentTimeMillis();
		if (e != last) {                                   // crosshair just landed on someone new
			last = e;
			acquired = now;
			double lo = Math.min(minReact.get(), maxReact.get()), hi = Math.max(minReact.get(), maxReact.get());
			reaction = (long) (lo + rnd.nextDouble() * (hi - lo));
			cdTarget = -1;
		}
		if (mc.player.isUsingItem()) return;
		if (now - acquired < reaction) return;

		if (cdTarget < 0) cdTarget = Math.max(0.5, Math.min(1.0, cooldown.get() + (rnd.nextDouble() - 0.5) * 0.06));
		if (Util.cooldown(mc) < cdTarget) return;

		if (now < nextHit) return;   // on 1.8-style servers there is no recharge to wait for, so cap the rate in real time
		Util.attack(mc, e);
		cdTarget = -1;
		nextHit = now + (long) (1000.0 / Math.max(1.0, maxCps.get()) * (0.85 + rnd.nextDouble() * 0.3));
	}
}
