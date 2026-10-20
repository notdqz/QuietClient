package com.quiet.module;

import com.quiet.hud.NotificationsHud;
import com.quiet.module.setting.*;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class Module {
	private final String name;
	private final Category category;
	private final List<Setting<?>> settings = new ArrayList<>();
	private boolean enabled;
	private int key = -1;

	private Consumer<Minecraft> tickFn;
	private Runnable enableFn, disableFn;

	// UI / input state
	public boolean expanded;
	public float anim;
	boolean keyHeld;

	public Module(String name, Category category) {
		this.name = name;
		this.category = category;
	}

	// ---- quick setting creation -------------------------------------------------

	public <S extends Setting<?>> S add(S setting) { settings.add(setting); return setting; }

	public BooleanSetting bool(String name, boolean def) { return add(new BooleanSetting(name, def)); }

	public NumberSetting num(String name, double def, double min, double max, double step) {
		return add(new NumberSetting(name, def, min, max, step));
	}

	public ModeSetting mode(String name, String def, String... modes) { return add(new ModeSetting(name, def, modes)); }

	// ---- inline hooks for tiny modules ------------------------------------------

	public Module onTick(Consumer<Minecraft> f) { tickFn = f; return this; }
	public Module onEnable(Runnable r) { enableFn = r; return this; }
	public Module onDisable(Runnable r) { disableFn = r; return this; }

	// ---- overridable hooks for class-based modules ------------------------------

	protected void onEnable() {}
	protected void onDisable() {}
	protected void onTick() {}

	protected static Minecraft mc() { return Minecraft.getInstance(); }

	// ---- state ------------------------------------------------------------------

	public String name() { return name; }
	public Category category() { return category; }
	public List<Setting<?>> settings() { return settings; }
	public boolean isEnabled() { return enabled; }

	public int key() { return key; }
	public void setKey(int glfwKey) { key = glfwKey; }
	public String keyName() { return Keys.name(key); }

	public void toggle() { setEnabled(!enabled); }

	public void setEnabled(boolean value) {
		if (enabled == value) return;
		enabled = value;
		if (value) { onEnable(); if (enableFn != null) enableFn.run(); }
		else { onDisable(); if (disableFn != null) disableFn.run(); }
		NotificationsHud.push(name, value);
	}

	/** Called every client tick while the module is enabled. */
	public final void tick(Minecraft mc) {
		if (tickFn != null) tickFn.accept(mc);
		onTick();
	}

	public void tickAnim() { anim += ((enabled ? 1f : 0f) - anim) * 0.3f; }
}