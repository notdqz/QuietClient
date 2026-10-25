package com.quiet.module.impl;

import com.quiet.module.Category;
import com.quiet.module.Module;
import com.quiet.module.Setting;
import com.quiet.module.Util;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Pulls your aim toward the closest player inside your FOV while you hold attack.
 * Runs every rendered frame, turns by whole mouse counts (Util.mouseTurn) so every rotation step is something a real
 * mouse could have produced, aims at a wandering point near the target instead of one exact spot, varies its pull
 * speed slowly, and leaves tiny errors alone like a hand would. Strength = how hard it pulls.
 */
public class AimAssist extends Module {
	private static final float MAX_DEG_PER_SEC = 400f;
	private static final float DEADZONE_DEG = 0.4f;

	private static AimAssist inst;
	private final Setting.Num range = num("Range", 4.5, 2, 8, 0.1);
	private final Setting.Num fov = num("FOV", 60, 10, 180, 5);
	private final Setting.Num strength = num("Strength", 40, 1, 100, 1);
	private final Setting.Mode aim = mode("Aim at", "Head", "Head", "Body", "Legs");
	private final Setting.Bool clickOnly = bool("Only while attacking", true);

	private long lastNs = System.nanoTime(), noiseUntil;
	private Vec3 noise = Vec3.ZERO;
	private float rateMul = 1f;

	public AimAssist() {
		super("Aim Assist", Category.COMBAT);
		inst = this;
		WorldRenderEvents.BEFORE_TRANSLUCENT.register(ctx -> { if (inst.isEnabled()) inst.frame(); });
	}

	@Override protected void onEnable() { lastNs = System.nanoTime(); Util.resetTurn(); }
	@Override protected void onDisable() { Util.resetTurn(); }

	private void frame() {
		long nowNs = System.nanoTime();
		float dt = Mth.clamp((nowNs - lastNs) / 1.0e9f, 0f, 0.1f);
		lastNs = nowNs;

		Minecraft mc = mc();
		if (!Util.canAct(mc) || (clickOnly.get() && !mc.options.keyAttack.isDown())) { Util.resetTurn(); return; }
		LocalPlayer me = mc.player;

		// the aim point drifts a little every 80-250 ms, and so does the pull speed
		ThreadLocalRandom rnd = ThreadLocalRandom.current();
		long now = System.currentTimeMillis();
		if (now > noiseUntil) {
			noise = new Vec3(rnd.nextDouble(-0.12, 0.12), rnd.nextDouble(-0.10, 0.12), rnd.nextDouble(-0.12, 0.12));
			rateMul = 0.85f + rnd.nextFloat() * 0.3f;
			noiseUntil = now + 80 + rnd.nextInt(170);
		}

		Player best = null;
		float bestYaw = 0, bestPitch = 0;
		double bestAng = fov.get();
		Vec3 eye = me.getEyePosition();
		for (Player p : mc.level.players()) {
			if (!Util.validTarget(mc, p) || me.distanceTo(p) > range.get()) continue;
			float[] r = Util.rotTo(eye, Util.aimPoint(p, aim.mode()).add(noise));
			float dy = Mth.wrapDegrees(r[0] - me.getYRot());
			float dp = r[1] - me.getXRot();
			double ang = Math.hypot(dy, dp);
			if (ang < bestAng) { bestAng = ang; best = p; bestYaw = dy; bestPitch = dp; }
		}
		if (best == null || dt <= 0f) { Util.resetTurn(); return; }
		if (Math.hypot(bestYaw, bestPitch) < DEADZONE_DEG) return;   // close enough: a hand doesn't micro-correct

		float rate = (0.5f + strength.asFloat() * 0.12f) * rateMul;
		float k = 1f - (float) Math.exp(-rate * dt);
		float cap = MAX_DEG_PER_SEC * dt;
		Util.mouseTurn(mc, Mth.clamp(bestYaw * k, -cap, cap), Mth.clamp(bestPitch * k, -cap, cap));
	}
}
