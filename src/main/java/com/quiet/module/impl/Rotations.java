package com.quiet.module.impl;

import net.minecraft.util.Mth;

/**
 * Server rotation: what the movement packets say you're looking at. It eases toward the target over a few
 * ticks and eases back to your real view afterwards (no instant snap).
 * Visual rotation: the same eased value, drawn on YOUR model only. Your real yaw/pitch are never touched.
 */
public final class Rotations {
	private static float tgtYaw, tgtPitch, curYaw, curPitch;
	private static int hold;
	private static boolean engaged, first, showVisual;

	private static float sYaw, sPitch;
	private static boolean smoothing;
	private static long lastNs;

	public static void set(float y, float p, int ticks) { set(y, p, ticks, true); }

	public static void set(float y, float p, int ticks, boolean visual) {
		tgtYaw = y; tgtPitch = Mth.clamp(p, -90f, 90f);
		hold = Math.max(1, ticks);
		showVisual = visual;
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

	/** True once the sent rotation is within tol degrees of the target. */
	public static boolean closeTo(float y, float p, float tol) {
		return engaged && !first && Math.abs(Mth.wrapDegrees(y - curYaw)) < tol && Math.abs(p - curPitch) < tol;
	}

	/** Smoothed {headYaw, pitch} for drawing your own model, or null when nothing should be shown. */
	public static float[] visual(float realHeadYaw, float realPitch) {
		long now = System.nanoTime();
		float dt = lastNs == 0 ? 0.016f : Mth.clamp((now - lastNs) / 1.0E9f, 0f, 0.1f);
		lastNs = now;

		boolean want = engaged && showVisual && !first;
		if (!want && !smoothing) return null;
		if (!smoothing) { sYaw = realHeadYaw; sPitch = realPitch; smoothing = true; }

		float ty = want ? curYaw : realHeadYaw, tp = want ? curPitch : realPitch;
		float a = 1f - (float) Math.exp(-dt * 16f);
		sYaw += Mth.wrapDegrees(ty - sYaw) * a;
		sPitch += (tp - sPitch) * a;
		if (!want && Math.abs(Mth.wrapDegrees(realHeadYaw - sYaw)) < 0.6f && Math.abs(realPitch - sPitch) < 0.6f)
			smoothing = false;
		return new float[]{sYaw, sPitch};
	}
}
