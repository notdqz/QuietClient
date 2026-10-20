package com.quiet.hud;

import com.google.gson.JsonObject;
import com.quiet.gui.Icons;
import com.quiet.gui.Theme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.LocalPlayer;

import java.util.ArrayList;
import java.util.List;

public final class HudManager {
	public static final List<HudWidget> WIDGETS = new ArrayList<>();
	private static final long START = System.currentTimeMillis();

	private static void add(HudWidget w, boolean on) { w.enabled = on; WIDGETS.add(w); }

	public static void init() {
		// big ones
		add(new WatermarkHud(), true);
		add(new ArraylistHud(), true);
		add(new TargetHud(), true);
		add(new NotificationsHud(), true);
		add(new ArmorHud(), false);

		// rows panels: (id, name, icon, x, y, data source)
		add(new PanelHud("potions", "Potions", Icons.POTION, 0.22f, 0.27f, HudData::potions), true);
		add(new PanelHud("staffs", "Staffs", Icons.SHIELD, 0.63f, 0.28f, HudData::staffs), false);
		add(new PanelHud("keybinds", "Keybinds", Icons.KEYBOARD, 0.70f, 0.62f, HudData::keybinds), true);
		add(new PanelHud("coords", "Coordinates", Icons.PIN, 0.012f, 0.22f, HudData::coords), true);
		add(new PanelHud("counters", "Counters", Icons.PACKAGE, 0.012f, 0.42f, HudData::counters), false);

		// small pills: (id, name, icon, x, y, text)
		add(new InfoPill("fps", "FPS", Icons.GAUGE, 0.012f, 0.085f, () -> Minecraft.getInstance().getFps() + " FPS"), true);
		add(new InfoPill("ping", "Ping", Icons.WIFI, 0.075f, 0.085f, HudManager::ping), true);
		add(new InfoPill("speed", "Speed", Icons.ZAP, 0.14f, 0.085f, HudManager::speed), false);
		add(new InfoPill("session", "Session", Icons.TIMER, 0.012f, 0.14f, HudManager::session), false);
		add(new InfoPill("memory", "Memory", Icons.CPU, 0.085f, 0.14f, HudManager::memory), false);
		add(new InfoPill("players", "Players", Icons.USERS, 0.17f, 0.14f, HudManager::players), false);
	}

	private static String ping() {
		Minecraft mc = Minecraft.getInstance();
		ClientPacketListener conn = mc.getConnection();
		if (mc.player == null || conn == null) return "0 ms";
		PlayerInfo pi = conn.getPlayerInfo(mc.player.getUUID());
		return (pi == null ? 0 : pi.getLatency()) + " ms";
	}

	private static String speed() {
		LocalPlayer p = Minecraft.getInstance().player;
		if (p == null) return "0.0 b/s";
		return String.format("%.1f b/s", Math.hypot(p.getX() - p.xo, p.getZ() - p.zo) * 20);
	}

	private static String session() {
		long s = (System.currentTimeMillis() - START) / 1000;
		return s >= 3600 ? String.format("%d:%02d:%02d", s / 3600, (s / 60) % 60, s % 60)
				: String.format("%d:%02d", s / 60, s % 60);
	}

	private static String memory() {
		Runtime r = Runtime.getRuntime();
		return ((r.totalMemory() - r.freeMemory()) >> 20) + " MB";
	}

	private static String players() {
		ClientPacketListener conn = Minecraft.getInstance().getConnection();
		return (conn == null ? 1 : conn.getOnlinePlayers().size()) + " online";
	}

	public static void render(GuiGraphics g) {
		if (Minecraft.getInstance().screen instanceof HudEditorScreen) return; // the editor draws them itself
		Theme t = Theme.current();
		int sw = g.guiWidth(), sh = g.guiHeight();
		for (HudWidget wd : WIDGETS) {
			if (!wd.enabled) continue;
			wd.update(false);
			if (!wd.isVisible()) continue;
			wd.draw(g, t, wd.x(sw), wd.y(sh));
		}
	}

	public static void load(JsonObject o) {
		for (HudWidget wd : WIDGETS) {
			if (!o.has(wd.id)) continue;
			JsonObject e = o.getAsJsonObject(wd.id);
			if (e.has("enabled")) wd.enabled = e.get("enabled").getAsBoolean();
			if (e.has("x")) wd.fx = e.get("x").getAsFloat();
			if (e.has("y")) wd.fy = e.get("y").getAsFloat();
		}
	}

	public static JsonObject save() {
		JsonObject o = new JsonObject();
		for (HudWidget wd : WIDGETS) {
			JsonObject e = new JsonObject();
			e.addProperty("enabled", wd.enabled);
			e.addProperty("x", wd.fx);
			e.addProperty("y", wd.fy);
			o.add(wd.id, e);
		}
		return o;
	}
}