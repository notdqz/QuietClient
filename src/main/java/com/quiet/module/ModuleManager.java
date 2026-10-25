package com.quiet.module;

import com.google.gson.JsonObject;
import com.quiet.Quiet;
import com.quiet.module.impl.*;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class ModuleManager {
	private static final List<Module> MODULES = new ArrayList<>();
	private static final Map<Category, List<Module>> BY_CATEGORY = new EnumMap<>(Category.class);

	private static void add(Module m) {
		MODULES.add(m);
		BY_CATEGORY.computeIfAbsent(m.category, k -> new ArrayList<>()).add(m);
	}

	/** The whole feature list. A new module = one class + one line here. */
	public static void init() {
		add(new AutoClicker());
		add(new AimAssist());
		add(new Reach());
		add(new Triggerbot());
		add(new Velocity());
		add(new KeyTap());
		add(new BackTrack());
		add(new AutoTotem());

		add(new Sprint());
		add(new SafeWalk());
		add(new Scaffold());
		add(new AutoTool());
		add(new QuickTransfer());

		add(new PlayerEsp());
		add(new HitEffect());
		add(new Fullbright());
		add(new MotionBlur());
		add(new FreeLook());
	}

	public static List<Module> all() { return MODULES; }

	public static List<Module> byCategory(Category c) { return BY_CATEGORY.getOrDefault(c, List.of()); }

	/** Keybind polling + per-tick updates. */
	public static void tick(Minecraft mc) {
		boolean canFire = mc.screen == null && mc.player != null;
		for (Module m : MODULES) {
			if (m.key() >= 0) {
				boolean down = Keys.isDown(m.key());
				if (down && !m.keyHeld && canFire) m.toggle();
				m.keyHeld = down;
			}
			if (m.isEnabled() && mc.player != null && mc.level != null) {
				try {
					m.tick();
				} catch (Throwable t) {
					Quiet.LOGGER.error("Module {} crashed and was disabled", m.name, t);
					m.setEnabled(false);
				}
			}
		}
	}

	public static JsonObject save() {
		JsonObject o = new JsonObject();
		for (Module m : MODULES) {
			JsonObject e = new JsonObject();
			e.addProperty("key", m.key());
			JsonObject s = new JsonObject();
			for (Setting<?> st : m.settings) s.add(st.name, st.toJson());
			e.add("settings", s);
			o.add(m.name, e);
		}
		return o;
	}

	public static void load(JsonObject o) {
		for (Module m : MODULES) {
			try {
				if (!o.has(m.name)) continue;
				JsonObject e = o.getAsJsonObject(m.name);
				if (e.has("key")) m.setKey(e.get("key").getAsInt());
				if (e.has("settings")) {
					JsonObject s = e.getAsJsonObject("settings");
					for (Setting<?> st : m.settings) if (s.has(st.name)) st.fromJson(s.get(st.name));
				}
			} catch (Exception ignored) {}
		}
	}
}
