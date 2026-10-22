package com.quiet.module.impl;

import com.quiet.module.Category;
import com.quiet.module.Module;
import com.quiet.module.setting.BooleanSetting;
import com.quiet.module.setting.ModeSetting;
import com.quiet.module.setting.NumberSetting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Plays a fight for you using your real inputs: turns your camera with eased, mouse-step-sized movements,
 * holds W / A / D / jump / sprint, circle-strafes, and swings only when the crosshair is truly on the target.
 * No pathfinding: it walks straight at the target and jumps small obstacles.
 */
public class KillBot extends Module {
	private final NumberSetting searchRange = num("Search range", 24, 6, 64, 1);
	private final ModeSetting priority = mode("Target", "Nearest", "Nearest", "Lowest health");
	private final NumberSetting circleDist = num("Circle distance", 2.8, 2.0, 3.5, 0.1);
	private final BooleanSetting strafe = bool("Circle strafe", true);
	private final BooleanSetting bhop = bool("Bhop", true);
	private final BooleanSetting crits = bool("Crits", true);
	private final BooleanSetting avoidEdges = bool("Avoid edges", true);
	private final NumberSetting aimSpeed = num("Aim speed", 30, 5, 80, 1);

	private Player target;
	private int strafeDir = 1, strafeT, noiseT, critWait;
	private Vec3 noise = Vec3.ZERO;
	private boolean keysHeld;

	public KillBot() { super("KillBot", Category.COMBAT); }

	@Override protected void onDisable() { target = null; release(); }

	private void release() {
		if (keysHeld) { keysHeld = false; KeyMapping.setAll(); }
	}

	private Player pick(Minecraft mc, LocalPlayer p) {
		Player best = null;
		double bv = Double.MAX_VALUE;
		for (Player o : mc.level.players()) {
			if (!ModUtil.validTarget(mc, o)) continue;
			double d = p.distanceTo(o);
			if (d > searchRange.get()) continue;
			double v = priority.is("Lowest health") ? o.getHealth() + o.getAbsorptionAmount() + d * 0.01 : d;
			if (v < bv) { bv = v; best = o; }
		}
		return best;
	}

	@Override
	protected void onTick() {
		Minecraft mc = mc();
		LocalPlayer p = mc.player;
		if (!ModUtil.canAct(mc)) { release(); return; }
		if (target == null || !ModUtil.validTarget(mc, target) || p.distanceTo(target) > searchRange.get() + 4)
			target = pick(mc, p);
		if (target == null) { release(); return; }

		ThreadLocalRandom rnd = ThreadLocalRandom.current();
		double hd = Math.hypot(target.getX() - p.getX(), target.getZ() - p.getZ());
		double cd = circleDist.get();

		aim(mc, p, rnd);

		// ---- movement
		boolean fwd = false, back = false, left = false, right = false, jump = false, sprint = false;
		if (hd > cd + 0.5) { fwd = true; sprint = true; }
		else if (hd < 1.7) back = true;
		else if (hd > cd) { fwd = true; sprint = true; }
		if (strafe.get() && hd < cd + 2.5) {
			if (--strafeT <= 0) { strafeDir = rnd.nextBoolean() ? 1 : -1; strafeT = 15 + rnd.nextInt(30); }
			if (strafeDir > 0) right = true; else left = true;
		}
		if (bhop.get() && p.onGround() && (fwd || left || right) && hd > 1.5) jump = true;

		boolean critReady = !crits.get() || CritHelper.ready(mc, hd < 3.6);
		if (CritHelper.noSprint) { sprint = false; CritHelper.noSprint = false; }   // let the crit line up

		if (avoidEdges.get() && p.onGround() && danger(mc, p, fwd, left, right)) {
			fwd = left = right = jump = sprint = false;
		}

		mc.options.keyUp.setDown(fwd);
		mc.options.keyDown.setDown(back);
		mc.options.keyLeft.setDown(left);
		mc.options.keyRight.setDown(right);
		mc.options.keyJump.setDown(jump);
		mc.options.keySprint.setDown(sprint);
		keysHeld = true;

		// ---- attack: only when the crosshair is genuinely on the target
		Entity e = mc.crosshairPickEntity;
		if (e == target && !p.isUsingItem() && ModUtil.cooldown(mc) >= 0.92f) {
			boolean ok = critReady;
			if (!ok && ++critWait > 16) ok = true;      // can't crit here (no room to jump?), hit anyway
			if (ok) { ModUtil.attack(mc, target); critWait = 0; }
		}
	}

	private void aim(Minecraft mc, LocalPlayer p, ThreadLocalRandom rnd) {
		if (--noiseT <= 0) {
			noise = new Vec3(rnd.nextDouble(-0.15, 0.15), rnd.nextDouble(-0.15, 0.2), rnd.nextDouble(-0.15, 0.15));
			noiseT = 4 + rnd.nextInt(7);
		}
		Vec3 vel = new Vec3(target.getX() - target.xo, 0, target.getZ() - target.zo);
		Vec3 pt = new Vec3(target.getX(), target.getY() + target.getBbHeight() * 0.62, target.getZ())
				.add(noise).add(vel.scale(1.2));
		float[] r = ModUtil.rotTo(p.getEyePosition(), pt);
		float dy = Mth.wrapDegrees(r[0] - p.getYRot()), dp = r[1] - p.getXRot();
		float k = (float) (aimSpeed.get() / 100.0);
		float sy = Mth.clamp(dy * k * (0.85f + rnd.nextFloat() * 0.3f), -40f, 40f);
		float sp = Mth.clamp(dp * k * (0.85f + rnd.nextFloat() * 0.3f), -25f, 25f);

		// real mouse movement comes in whole "counts" of this many degrees, so round to that
		double f = mc.options.sensitivity().get() * 0.6 + 0.2;
		double step = f * f * f * 8.0 * 0.15;
		if (step > 0) {
			sy = (float) (Math.round(sy / step) * step);
			sp = (float) (Math.round(sp / step) * step);
		}
		p.setYRot(p.getYRot() + sy);
		p.setXRot(Mth.clamp(p.getXRot() + sp, -90f, 90f));
	}

	/** Looks ahead along the way we're about to move for a drop of more than 3 blocks, or lava. */
	private boolean danger(Minecraft mc, LocalPlayer p, boolean fwd, boolean left, boolean right) {
		if (!fwd && !left && !right) return false;
		double rad = Math.toRadians(p.getYRot());
		double fx = -Math.sin(rad), fz = Math.cos(rad), rx = -Math.cos(rad), rz = -Math.sin(rad);
		double mx = (fwd ? fx : 0) + (right ? rx : 0) - (left ? rx : 0);
		double mz = (fwd ? fz : 0) + (right ? rz : 0) - (left ? rz : 0);
		double len = Math.hypot(mx, mz);
		if (len < 1e-3) return false;
		mx /= len; mz /= len;
		for (double dist : new double[]{1.0, 2.0}) {
			BlockPos base = BlockPos.containing(p.getX() + mx * dist, p.getY(), p.getZ() + mz * dist);
			boolean ground = false;
			for (int dy = 0; dy <= 3; dy++) {
				BlockPos pos = base.below(dy);
				BlockState s = mc.level.getBlockState(pos);
				if (s.getFluidState().is(FluidTags.LAVA)) return true;
				if (!s.getCollisionShape(mc.level, pos).isEmpty()) { ground = true; break; }
			}
			if (!ground) return true;
		}
		return false;
	}
}
