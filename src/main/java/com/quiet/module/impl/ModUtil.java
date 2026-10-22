package com.quiet.module.impl;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/** Small helpers shared by the modules. */
public final class ModUtil {
	/** True when we're in a world with no screen open. */
	public static boolean canAct(Minecraft mc) {
		return mc.player != null && mc.level != null && mc.screen == null;
	}

	public static boolean validTarget(Minecraft mc, Entity e) {
		return e instanceof Player p && p != mc.player && p.isAlive() && !p.isSpectator();
	}

	public static void attack(Minecraft mc, Entity e) {
		mc.gameMode.attack(mc.player, e);     // MultiPlayerGameModeMixin catches hits on the Test Dummy
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

	/** {yaw, pitch} needed to look from one point to another. */
	public static float[] rotTo(Vec3 from, Vec3 to) {
		double dx = to.x - from.x, dy = to.y - from.y, dz = to.z - from.z;
		float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
		float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.hypot(dx, dz)));
		return new float[]{Mth.wrapDegrees(yaw), Mth.clamp(pitch, -90f, 90f)};
	}
}
