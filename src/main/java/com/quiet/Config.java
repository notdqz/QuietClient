package com.quiet;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.quiet.module.ModuleManager;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** config/quiet.json: the module list overlay switch, plus every module's key and settings (modules always start off). */
public final class Config {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	public static boolean arraylist = false;
	/** Menu window position in GUI units (-1 = centred) and the last open tab. */
	public static int menuX = -1, menuY = -1, menuTab = 0;

	private static Path file() { return FabricLoader.getInstance().getConfigDir().resolve("quiet.json"); }

	public static void load() {
		Path f = file();
		if (!Files.exists(f)) return;
		try (Reader r = Files.newBufferedReader(f)) {
			JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
			if (o.has("arraylist")) arraylist = o.get("arraylist").getAsBoolean();
			if (o.has("menu")) {
				JsonObject m = o.getAsJsonObject("menu");
				if (m.has("x")) menuX = m.get("x").getAsInt();
				if (m.has("y")) menuY = m.get("y").getAsInt();
				if (m.has("tab")) menuTab = m.get("tab").getAsInt();
			}
			if (o.has("modules")) ModuleManager.load(o.getAsJsonObject("modules"));
		} catch (Exception e) {
			Quiet.LOGGER.error("quiet.json is unreadable; backing it up and using defaults", e);
			try { Files.copy(f, f.resolveSibling("quiet.json.bak"), StandardCopyOption.REPLACE_EXISTING); } catch (Exception ignored) {}
		}
	}

	public static void save() {
		try {
			JsonObject o = new JsonObject();
			o.addProperty("arraylist", arraylist);
			JsonObject menu = new JsonObject();
			menu.addProperty("x", menuX);
			menu.addProperty("y", menuY);
			menu.addProperty("tab", menuTab);
			o.add("menu", menu);
			o.add("modules", ModuleManager.save());
			Path f = file(), tmp = f.resolveSibling("quiet.json.tmp");
			Files.writeString(tmp, GSON.toJson(o));
			try { Files.move(tmp, f, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
			catch (AtomicMoveNotSupportedException e) { Files.move(tmp, f, StandardCopyOption.REPLACE_EXISTING); }
		} catch (Exception e) {
			Quiet.LOGGER.error("Could not save quiet.json", e);
		}
	}
}
