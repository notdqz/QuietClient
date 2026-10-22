package com.quiet.module.impl;

import com.quiet.module.Category;
import com.quiet.module.Keys;
import com.quiet.module.Module;
import com.quiet.module.setting.ModeSetting;
import com.quiet.module.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

/**
 * 3rd-person camera that ignores blocks, with a zoom you control: hold the zoom key and scroll.
 * The block check / distance is in CameraMixin, the scroll hook in MouseHandlerMixin.
 */
public class CameraClip extends Module {
	public static boolean active;
	private static CameraClip inst;
	private static float distance = 4f;   // vanilla 3rd-person distance

	private final ModeSetting zoomKey = mode("Zoom key", "Left Alt", "Off", "Left Alt", "Left Ctrl", "Caps Lock", "Z", "C", "V");
	private final NumberSetting step = num("Scroll step", 1.0, 0.25, 5.0, 0.25);
	private final NumberSetting maxDist = num("Max distance", 30, 5, 100, 1);

	public CameraClip() { super("Camera Clip", Category.RENDER); inst = this; }

	public static float distance() {
		return inst == null ? distance : Mth.clamp(distance, 1f, (float) (double) inst.maxDist.get());
	}

	private static int keyCode(String m) {
		return switch (m) {
			case "Left Alt" -> GLFW.GLFW_KEY_LEFT_ALT;
			case "Left Ctrl" -> GLFW.GLFW_KEY_LEFT_CONTROL;
			case "Caps Lock" -> GLFW.GLFW_KEY_CAPS_LOCK;
			case "Z" -> GLFW.GLFW_KEY_Z;
			case "C" -> GLFW.GLFW_KEY_C;
			case "V" -> GLFW.GLFW_KEY_V;
			default -> -1;
		};
	}

	/** Called from MouseHandlerMixin. Returns true when the scroll was used for zooming. */
	public static boolean handleScroll(double dy) {
		if (!active || inst == null) return false;
		Minecraft mc = Minecraft.getInstance();
		if (mc.screen != null || mc.player == null || mc.options.getCameraType().isFirstPerson()) return false;
		int key = keyCode(inst.zoomKey.mode());
		if (key < 0 || !Keys.isDown(key)) return false;
		distance = Mth.clamp(distance - (float) dy * (float) (double) inst.step.get(), 1f, (float) (double) inst.maxDist.get());
		return true;
	}

	@Override protected void onEnable() { active = true; distance = 4f; }
	@Override protected void onDisable() { active = false; }
}
