package com.quiet.module;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/** Small helpers shared by the modules. */
public final class Util {
	/** In a world with no screen open. */
	public static boolean canAct(Minecraft mc) {
		return mc.player != null && mc.level != null && mc.screen == null;
	}

	public static boolean validTarget(Minecraft mc, Entity e) {
		return e instanceof Player p && p != mc.player && p.isAlive() && !p.isSpectator();
	}

	public static void attack(Minecraft mc, Entity e) {
		mc.gameMode.attack(mc.player, e);
		mc.player.swing(InteractionHand.MAIN_HAND);
	}

	public static float cooldown(Minecraft mc) { return mc.player.getAttackStrengthScale(0f); }

	public static boolean isMoving(Minecraft mc) {
		return mc.options.keyUp.isDown() || mc.options.keyDown.isDown()
				|| mc.options.keyLeft.isDown() || mc.options.keyRight.isDown();
	}

	public static Vec3 aimPoint(Player p, String mode) {
		double f = mode.equals("Head") ? 0.92 : mode.equals("Body") ? 0.55 : 0.15;
		return new Vec3(p.getX(), p.getY() + p.getBbHeight() * f, p.getZ());
	}

	/**
	 * Gives a key back to the keyboard: sets it to whatever your finger is really doing. Used after a module pressed or
	 * released a key for you, so it never leaves a key stuck and never disturbs the other keys.
	 */
	public static void releaseKey(KeyMapping km) {
		InputConstants.Key k = InputConstants.getKey(km.saveString());
		if (k.getType() == InputConstants.Type.KEYSYM) km.setDown(Keys.isDown(k.getValue()));
		else KeyMapping.setAll();
	}

	private static float resYaw, resPitch;

	/**
	 * Turns the player by about this many degrees the way a real mouse does: in whole mouse "counts". One count is
	 * a fixed angle that depends on your sensitivity setting, and a real mouse can only ever turn by whole multiples
	 * of it. Turning by arbitrary float angles is one of the easiest things for an anti-cheat to spot, because the
	 * rotation steps stop being multiples of that angle. Leftovers smaller than one count carry over to the next call.
	 */
	public static void mouseTurn(Minecraft mc, float yawDeg, float pitchDeg) {
		double f = mc.options.sensitivity().get() * 0.6 + 0.2;
		double raw = f * f * f * 8.0;          // what one count passes to Entity.turn
		double step = raw * 0.15;              // ...which is this many degrees
		resYaw += yawDeg;
		resPitch += pitchDeg;
		int ny = (int) (resYaw / step), np = (int) (resPitch / step);
		resYaw -= ny * step;
		resPitch -= np * step;
		if (ny != 0 || np != 0) mc.player.turn(ny * raw, np * raw);
	}

	/** Forget leftover sub-count turning (call when nothing is being aimed at). */
	public static void resetTurn() { resYaw = 0; resPitch = 0; }

	/** {yaw, pitch} needed to look from one point to another. */
	public static float[] rotTo(Vec3 from, Vec3 to) {
		double dx = to.x - from.x, dy = to.y - from.y, dz = to.z - from.z;
		float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
		float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.hypot(dx, dz)));
		return new float[]{Mth.wrapDegrees(yaw), Mth.clamp(pitch, -90f, 90f)};
	}
}
