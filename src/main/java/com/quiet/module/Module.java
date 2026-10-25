package com.quiet.module;

import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;

public abstract class Module {
	public final String name;
	public final Category category;
	public final List<Setting<?>> settings = new ArrayList<>();

	private boolean enabled;
	private int key = -1;

	// menu state
	public boolean expanded;
	public float anim;
	boolean keyHeld;

	protected Module(String name, Category category) { this.name = name; this.category = category; }

	protected Setting.Bool bool(String n, boolean def) { Setting.Bool s = new Setting.Bool(n, def); settings.add(s); return s; }
	protected Setting.Num num(String n, double def, double min, double max, double step) {
		Setting.Num s = new Setting.Num(n, def, min, max, step); settings.add(s); return s;
	}
	protected Setting.Mode mode(String n, String def, String... modes) {
		Setting.Mode s = new Setting.Mode(n, def, modes); settings.add(s); return s;
	}

	protected void onEnable() {}
	protected void onDisable() {}
	protected void onTick() {}

	protected static Minecraft mc() { return Minecraft.getInstance(); }

	public boolean isEnabled() { return enabled; }
	public int key() { return key; }
	public void setKey(int glfwKey) { key = glfwKey; }
	public String keyName() { return Keys.name(key); }
	public void toggle() { setEnabled(!enabled); }

	public void setEnabled(boolean value) {
		if (enabled == value) return;
		enabled = value;
		if (value) onEnable(); else onDisable();
	}

	/** Called every client tick while enabled and in a world. */
	public final void tick() { onTick(); }

	public void tickAnim() { anim += ((enabled ? 1f : 0f) - anim) * 0.3f; }
}
