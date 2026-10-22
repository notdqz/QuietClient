package com.quiet.module.impl;

import com.quiet.module.Category;
import com.quiet.module.Module;
import net.minecraft.client.CameraType;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;

/** Mouse moves the camera only (EntityMixin + CameraMixin); your body keeps facing the same way. */
public class FreeLook extends Module {
	public static boolean active;
	public static float yaw, pitch;
	private CameraType prevCam;

	public FreeLook() { super("Free Look", Category.MISC); }

	public static void turn(double dYaw, double dPitch) {
		yaw += (float) dYaw * 0.15f;
		pitch = Mth.clamp(pitch + (float) dPitch * 0.15f, -90f, 90f);
	}

	@Override
	protected void onEnable() {
		LocalPlayer p = mc().player;
		if (p == null) return;
		yaw = p.getYRot();
		pitch = p.getXRot();
		prevCam = mc().options.getCameraType();
		if (prevCam.isFirstPerson()) mc().options.setCameraType(CameraType.THIRD_PERSON_BACK);
		active = true;
	}

	@Override
	protected void onDisable() {
		active = false;
		if (prevCam != null) mc().options.setCameraType(prevCam);
		prevCam = null;
	}
}
