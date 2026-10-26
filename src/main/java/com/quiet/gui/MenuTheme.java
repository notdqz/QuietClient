package com.quiet.gui;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * The menu's colours, all derived from four numbers the colour picker edits: hue, saturation, brightness and contrast.
 * The picked colour IS the menu colour; text turns black or white by itself depending on how light that colour is.
 * Saved on its own in config/quiet_menu.json so it stays separate from everything else.
 */
public final class MenuTheme {
	public static final float DEF_H = 0.62f, DEF_S = 0.12f, DEF_V = 0.09f, DEF_C = 0.5f;

	/** hue 0..1 (the wheel's angle), saturation 0..1 (distance from the wheel's centre), value = brightness, contrast. */
	public static float h = DEF_H, s = DEF_S, v = DEF_V, c = DEF_C;

	// ARGB colours, rebuilt by recompute()
	public static int panel, edge, row, rowHover, rowOn, inner, text, textOn, dim, accent, track, checkOff;
	public static int ribbonBase, ribbonTint;
	public static float ribbonStrength = 1f;
	public static boolean lightTheme;

	static { load(); recompute(); }

	public static int hsv(float h, float s, float v) {
		float hh = (h - (float) Math.floor(h)) * 6f;
		int i = (int) hh;
		float f = hh - i, p = v * (1 - s), q = v * (1 - s * f), t = v * (1 - s * (1 - f));
		float r, g, b;
		switch (i % 6) {
			case 0 -> { r = v; g = t; b = p; }
			case 1 -> { r = q; g = v; b = p; }
			case 2 -> { r = p; g = v; b = t; }
			case 3 -> { r = p; g = q; b = v; }
			case 4 -> { r = t; g = p; b = v; }
			default -> { r = v; g = p; b = q; }
		}
		return (Math.round(r * 255) << 16) | (Math.round(g * 255) << 8) | Math.round(b * 255);
	}

	/** Mixes two 0xRRGGBB colours. */
	public static int mixRgb(int a, int b, double t) {
		t = Math.max(0, Math.min(1, t));
		int r = (int) (((a >> 16) & 255) + (((b >> 16) & 255) - ((a >> 16) & 255)) * t);
		int g = (int) (((a >> 8) & 255) + (((b >> 8) & 255) - ((a >> 8) & 255)) * t);
		int bl = (int) ((a & 255) + ((b & 255) - (a & 255)) * t);
		return (r << 16) | (g << 8) | bl;
	}

	public static void recompute() {
		int base = hsv(h, s, v);
		double yiq = (((base >> 16) & 255) * 299 + ((base >> 8) & 255) * 587 + (base & 255) * 114) / 1000.0;
		lightTheme = yiq >= 135;                                  // light colour -> black text, dark colour -> white text
		int tc = lightTheme ? 0x101114 : 0xFFFFFF;

		panel = 0xF2000000 | base;
		edge = 0xFF000000 | mixRgb(base, tc, 0.06 + 0.12 * c);
		row = 0xC8000000 | mixRgb(base, tc, 0.02 + 0.06 * c);      // rows stay slightly see-through so the ribbons show
		rowHover = 0xD0000000 | mixRgb(base, tc, 0.05 + 0.07 * c);
		rowOn = 0xE6000000 | mixRgb(base, tc, 0.09 + 0.12 * c);
		inner = 0xC0000000 | mixRgb(base, tc, 0.04);
		text = 0xFF000000 | mixRgb(base, tc, 0.28 + 0.44 * c);
		textOn = 0xFF000000 | tc;
		dim = 0xFF000000 | mixRgb(base, tc, 0.18 + 0.20 * c);
		accent = 0xFF000000 | tc;
		track = 0xFF000000 | mixRgb(base, tc, 0.08 + 0.12 * c);
		checkOff = 0xFF000000 | mixRgb(base, tc, 0.03 + 0.04 * c);

		ribbonBase = tc;
		ribbonTint = lightTheme ? hsv(h, 0.8f, 0.5f) : hsv(h, Math.max(s, 0.5f), 1f);
		ribbonStrength = lightTheme ? 0.6f : 1f;
	}

	public static void reset() {
		h = DEF_H; s = DEF_S; v = DEF_V; c = DEF_C;
		recompute();
		save();
	}

	private static Path file() { return FabricLoader.getInstance().getConfigDir().resolve("quiet_menu.json"); }

	private static void load() {
		Path f = file();
		if (!Files.exists(f)) return;
		try (Reader r = Files.newBufferedReader(f)) {
			JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
			if (o.has("h")) h = o.get("h").getAsFloat();
			if (o.has("s")) s = Math.max(0f, Math.min(1f, o.get("s").getAsFloat()));
			if (o.has("v")) v = Math.max(0f, Math.min(1f, o.get("v").getAsFloat()));
			if (o.has("c")) c = Math.max(0f, Math.min(1f, o.get("c").getAsFloat()));
		} catch (Exception ignored) {}
	}

	public static void save() {
		try {
			JsonObject o = new JsonObject();
			o.addProperty("h", h);
			o.addProperty("s", s);
			o.addProperty("v", v);
			o.addProperty("c", c);
			Files.writeString(file(), o.toString());
		} catch (Exception ignored) {}
	}
}
