package com.quiet.module;

import org.lwjgl.glfw.GLFW;

import java.util.Locale;

public final class Keys {
	/** Raw key state. Uses the current GL window, which is the game window on the main thread. */
	public static boolean isDown(int key) {
		long win = GLFW.glfwGetCurrentContext();
		return win != 0 && key >= 0 && GLFW.glfwGetKey(win, key) == GLFW.GLFW_PRESS;
	}

	public static String name(int key) {
		if (key < 0) return "None";
		if (key >= GLFW.GLFW_KEY_F1 && key <= GLFW.GLFW_KEY_F25) return "F" + (key - GLFW.GLFW_KEY_F1 + 1);
		switch (key) {
			case GLFW.GLFW_KEY_SPACE: return "Space";
			case GLFW.GLFW_KEY_TAB: return "Tab";
			case GLFW.GLFW_KEY_ENTER: return "Enter";
			case GLFW.GLFW_KEY_BACKSPACE: return "Backspace";
			case GLFW.GLFW_KEY_INSERT: return "Ins";
			case GLFW.GLFW_KEY_DELETE: return "Del";
			case GLFW.GLFW_KEY_HOME: return "Home";
			case GLFW.GLFW_KEY_END: return "End";
			case GLFW.GLFW_KEY_PAGE_UP: return "PgUp";
			case GLFW.GLFW_KEY_PAGE_DOWN: return "PgDn";
			case GLFW.GLFW_KEY_UP: return "Up";
			case GLFW.GLFW_KEY_DOWN: return "Down";
			case GLFW.GLFW_KEY_LEFT: return "Left";
			case GLFW.GLFW_KEY_RIGHT: return "Right";
			case GLFW.GLFW_KEY_LEFT_SHIFT: return "LShift";
			case GLFW.GLFW_KEY_RIGHT_SHIFT: return "RShift";
			case GLFW.GLFW_KEY_LEFT_CONTROL: return "LCtrl";
			case GLFW.GLFW_KEY_RIGHT_CONTROL: return "RCtrl";
			case GLFW.GLFW_KEY_LEFT_ALT: return "LAlt";
			case GLFW.GLFW_KEY_RIGHT_ALT: return "RAlt";
			case GLFW.GLFW_KEY_CAPS_LOCK: return "Caps";
			default:
				String n = GLFW.glfwGetKeyName(key, 0);
				return n != null ? n.toUpperCase(Locale.ROOT) : "Key " + key;
		}
	}
}