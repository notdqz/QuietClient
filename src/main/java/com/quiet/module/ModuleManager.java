package com.quiet.module;

import com.google.gson.JsonObject;
import com.quiet.Quiet;
import com.quiet.module.impl.AimAssist;
import com.quiet.module.impl.AutoClicker;
import com.quiet.module.impl.AutoEat;
import com.quiet.module.impl.AutoSprint;
import com.quiet.module.impl.AutoTool;
import com.quiet.module.impl.AutoTotem;
import com.quiet.module.impl.BackTrack;
import com.quiet.module.impl.Bhop;
import com.quiet.module.impl.Fullbright;
import com.quiet.module.impl.HitTracker;
import com.quiet.module.impl.Reach;
import com.quiet.module.impl.STap;
import com.quiet.module.impl.SafeWalk;
import com.quiet.module.impl.Triggerbot;
import com.quiet.module.impl.WTap;
import com.quiet.module.impl.SilentAim;
import com.quiet.module.impl.Velocity;
import com.quiet.module.impl.Scaffold;
import com.quiet.module.impl.NoFall;
import com.quiet.module.impl.NoSlow;
import com.quiet.module.impl.FastPlace;
import com.quiet.module.impl.InventoryMove;
import com.quiet.module.impl.FreeLook;
import com.quiet.module.impl.CameraClip;
import com.quiet.module.impl.QuickLoot;
import com.quiet.module.impl.TestDummy;
import com.quiet.module.impl.KillBot;
import com.quiet.module.impl.KnockbackFlick;
import com.quiet.module.impl.PlayerEsp;
import com.quiet.module.impl.Tracers;
import com.quiet.module.impl.HealthDisplay;
import com.quiet.module.impl.HitboxDisplay;
import com.quiet.module.impl.Chams;
import com.quiet.module.impl.XRay;
import com.quiet.module.impl.DamageIndicators;
import com.quiet.module.impl.TrajectoryPreview;
import com.quiet.module.impl.StorageEsp;
import com.quiet.module.impl.HitParticles;
import com.quiet.module.impl.CustomSky;

import com.quiet.module.setting.Setting;

import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class ModuleManager {
	private static final Map<Category, List<Module>> MODULES = new EnumMap<>(Category.class);

	static {
		for (Category c : Category.values()) MODULES.put(c, new ArrayList<>());
	}

	/** Register every module here: one line each, as many per line as you like. */
	public static void init() {
		register(new AutoClicker(),       new AimAssist(),     new Triggerbot(),   new Reach(),
        		 new WTap(),              new STap(),          new AutoTotem(),    
        		 new AutoSprint(),        new Bhop(),          new SafeWalk(),     
        		 new Fullbright(),        new AutoTool(),      new AutoEat(),     
		  		 new SilentAim(),         new Velocity(),      new Scaffold(),     new NoFall(),           new NoSlow(),
         		 new FastPlace(),         new InventoryMove(), new FreeLook(),     new CameraClip(),      
				 new TestDummy(),         new QuickLoot(),    
				 new KillBot(),           new PlayerEsp(),     new Tracers(),      new HealthDisplay(),    
         		 new HitboxDisplay(),     new Chams(),         new XRay(),         new DamageIndicators(), 
       			 new TrajectoryPreview(), new StorageEsp(),    new HitParticles(), new CustomSky(),
				 new KnockbackFlick(),    new BackTrack());
	}

	public static void register(Module... modules) {
		for (Module m : modules) MODULES.get(m.category()).add(m);
	}

	public static List<Module> byCategory(Category c) { return MODULES.get(c); }

	public static List<Module> all() {
		List<Module> out = new ArrayList<>();
		for (Category c : Category.values()) out.addAll(MODULES.get(c));
		return out;
	}

	public static Module get(String name) {
		for (Module m : all()) if (m.name().equalsIgnoreCase(name)) return m;
		return null;
	}

	/** Keybind polling + per-tick module updates. */
	public static void tick(Minecraft mc) {
		HitTracker.tick(mc);
		boolean canFire = mc.screen == null && mc.player != null;
		for (Module m : all()) {
			if (m.key() >= 0) {
				boolean down = Keys.isDown(m.key());
				if (down && !m.keyHeld && canFire) m.toggle();
				m.keyHeld = down;
			}
			if (m.isEnabled() && mc.player != null) {
				try {
					m.tick(mc);
				} catch (Throwable t) {
					Quiet.LOGGER.error("Module {} crashed and was disabled", m.name(), t);
					m.setEnabled(false);
				}
			}
		}
	}

	// ---- persistence (keys + settings; modules always start disabled) ----------

	public static JsonObject save() {
		JsonObject o = new JsonObject();
		for (Module m : all()) {
			JsonObject e = new JsonObject();
			e.addProperty("key", m.key());
			JsonObject s = new JsonObject();
			for (Setting<?> st : m.settings()) s.add(st.name(), st.toJson());
			e.add("settings", s);
			o.add(m.name(), e);
		}
		return o;
	}

	public static void load(JsonObject o) {
		for (Module m : all()) {
			try {
				if (!o.has(m.name())) continue;
				JsonObject e = o.getAsJsonObject(m.name());
				if (e.has("key")) m.setKey(e.get("key").getAsInt());
				if (e.has("settings")) {
					JsonObject s = e.getAsJsonObject("settings");
					for (Setting<?> st : m.settings())
						if (s.has(st.name())) st.fromJson(s.get(st.name()));
				}
			} catch (Exception ignored) {}
		}
	}
}