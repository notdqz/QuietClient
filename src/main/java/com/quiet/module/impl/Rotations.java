package com.quiet.module.impl;

import net.minecraft.util.Mth;

/**
 * Server-side rotation: what the movement packets say you are looking at. It eases toward the target over a few
 * ticks, holds, then eases back to your real view (no instant snap). Your own camera is never touched.
 * LocalPlayerMixin swaps these values in only while the movement packet is being built.
 */
public final class Rotations {
	private static float tgtYaw, tgtPitch, curYaw, curPitch;
	private static int hold;
	private static boolean engaged, first;

	/** Ask the server to see this rotation for the next `ticks` ticks. */
	public static void set(float yaw, float pitch, int ticks) {
		tgtYaw = yaw;
		tgtPitch = Mth.clamp(pitch, -90f, 90f);
		hold = Math.max(1, ticks);
		if (!engaged) { engaged = true; first = true; }
	}

	/** Called by LocalPlayerMixin once per tick. Returns {yaw, pitch} to send, or null to send the real ones. */
	public static float[] serverRotation(float realYaw, float realPitch) {
		if (!engaged) return null;
		if (first) { curYaw = realYaw; curPitch = realPitch; first = false; }
		float ty, tp, k;
		if (hold > 0) { ty = tgtYaw; tp = tgtPitch; hold--; k = 0.55f; }
		else { ty = realYaw; tp = realPitch; k = 0.4f; }
		float dy = Mth.wrapDegrees(ty - curYaw), dp = tp - curPitch;
		curYaw += Mth.clamp(dy * k, -55f, 55f);
		curPitch = Mth.clamp(curPitch + Mth.clamp(dp * k, -35f, 35f), -90f, 90f);
		if (hold <= 0 && Math.abs(Mth.wrapDegrees(realYaw - curYaw)) < 1.2f && Math.abs(realPitch - curPitch) < 1.2f) {
			engaged = false;
			return null;
		}
		return new float[]{curYaw, curPitch};
	}

	/** True once the rotation the server has seen is within tol degrees of the target. */
	public static boolean closeTo(float yaw, float pitch, float tol) {
		return engaged && !first && Math.abs(Mth.wrapDegrees(yaw - curYaw)) < tol && Math.abs(pitch - curPitch) < tol;
	}
}
