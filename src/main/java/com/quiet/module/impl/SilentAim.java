package com.quiet.module.impl;

import com.quiet.module.Category;
import com.quiet.module.Module;
import com.quiet.module.setting.BooleanSetting;
import com.quiet.module.setting.ModeSetting;
import com.quiet.module.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Click assist. When YOU left-click and your crosshair isn't on anyone, but a player is in range and roughly
 * in front of you, the hit goes to them instead of swinging at air. Your camera never moves; the server (and
 * other players) just see you turn toward the target, eased in, then back out. Hooked from MinecraftMixin.
 */
public class SilentAim extends Module {
	private static SilentAim inst;

	private final NumberSetting range = num("Range", 3.2, 2.0, 6.0, 0.1);
	private final NumberSetting fov = num("FOV", 60, 10, 180, 5);
	private final ModeSetting aim = mode("Aim at", "Body", "Head", "Body", "Legs");
	private final BooleanSetting visible = bool("Visible only", true);
	private final BooleanSetting rotateFirst = bool("Turn before hit", true);
	private final BooleanSetting jitter = bool("Aim jitter", true);

	private Player pending;
	private int wait, noiseT;
	private float[] aimRot = new float[]{0, 0};
	private Vec3 noise = Vec3.ZERO;

	public SilentAim() { super("Silent Aim", Category.COMBAT); inst = this; }

	/** Called from MinecraftMixin when you click. Returns true if the click was taken over. */
	public static boolean onClick() { return inst != null && inst.isEnabled() && inst.click(); }

	private boolean click() {
		Minecraft mc = mc();
		if (pending != null) return true;
		if (!ModUtil.canAct(mc)) return false;
		LocalPlayer me = mc.player;
		if (me.isUsingItem()) return false;

		HitResult hr = mc.hitResult;
		if (hr != null && hr.getType() == HitResult.Type.ENTITY) return false;   // already aimed at someone

		Vec3 eye = me.getEyePosition();
		Player best = null;
		double bestAng = fov.get();
		for (Player p : mc.level.players()) {
			if (!ModUtil.validTarget(mc, p) || me.distanceTo(p) > range.get()) continue;
			if (visible.get() && !me.hasLineOfSight(p)) continue;
			float[] r = ModUtil.rotTo(eye, ModUtil.aimPoint(p, aim.mode()));
			double ang = Math.hypot(Mth.wrapDegrees(r[0] - me.getYRot()), r[1] - me.getXRot());
			if (ang < bestAng) { bestAng = ang; best = p; }
		}
		if (best == null) return false;
		if (hr != null && hr.getType() == HitResult.Type.BLOCK && hr.getLocation().distanceTo(eye) < me.distanceTo(best))
			return false;   // you're clicking a closer block, leave it alone

		pending = best;
		wait = 3;
		updateAim(me);
		return true;
	}

	private void updateAim(LocalPlayer me) {
		if (jitter.get() && --noiseT <= 0) {
			ThreadLocalRandom r = ThreadLocalRandom.current();
			noise = new Vec3(r.nextDouble(-0.15, 0.15), r.nextDouble(-0.12, 0.15), r.nextDouble(-0.15, 0.15));
			noiseT = 4 + r.nextInt(6);
		}
		Vec3 pt = ModUtil.aimPoint(pending, aim.mode()).add(jitter.get() ? noise : Vec3.ZERO);
		aimRot = ModUtil.rotTo(me.getEyePosition(), pt);
		Rotations.set(aimRot[0], aimRot[1], 2);
	}

	@Override protected void onDisable() { pending = null; }

	@Override
	protected void onTick() {
		if (pending == null) return;
		Minecraft mc = mc();
		LocalPlayer me = mc.player;
		if (!ModUtil.canAct(mc) || !pending.isAlive() || me.distanceTo(pending) > range.get() + 0.6 || me.isUsingItem()) {
			pending = null;
			return;
		}
		updateAim(me);
		if (!rotateFirst.get() || Rotations.closeTo(aimRot[0], aimRot[1], 9f) || --wait <= 0) {
			ModUtil.attack(mc, pending);
			pending = null;
		}
	}
}
