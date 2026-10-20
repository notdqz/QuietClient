package com.quiet.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.quiet.hud.HudManager;
import com.quiet.module.ModuleManager;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class QuietConfig {
	private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("quiet.json");
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	public static boolean darkMode = true;
	public static boolean blur = true;
	/** lowercase player name -> role, used by the Staffs HUD. Edit in config/quiet.json */
	public static final Map<String, String> staff = new LinkedHashMap<>();

	public static void load() {
		try {
			if (!Files.exists(FILE)) return;
			try (Reader r = Files.newBufferedReader(FILE)) {
				JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
				if (o.has("darkMode")) darkMode = o.get("darkMode").getAsBoolean();
				if (o.has("blur")) blur = o.get("blur").getAsBoolean();
				if (o.has("staff")) {
					for (Map.Entry<String, JsonElement> e : o.getAsJsonObject("staff").entrySet())
						staff.put(e.getKey().toLowerCase(Locale.ROOT), e.getValue().getAsString());
				}
				if (o.has("hud")) HudManager.load(o.getAsJsonObject("hud"));
				if (o.has("modules")) ModuleManager.load(o.getAsJsonObject("modules"));
			}
		} catch (Exception ignored) {}
	}

	public static void save() {
		try {
			JsonObject o = new JsonObject();
			o.addProperty("darkMode", darkMode);
			o.addProperty("blur", blur);
			JsonObject s = new JsonObject();
			staff.forEach(s::addProperty);
			o.add("staff", s);
			o.add("hud", HudManager.save());
			o.add("modules", ModuleManager.save());
			Files.writeString(FILE, GSON.toJson(o));
		} catch (Exception ignored) {}
	}
}