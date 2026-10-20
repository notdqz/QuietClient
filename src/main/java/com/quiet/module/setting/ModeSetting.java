package com.quiet.module.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import java.util.List;

public class ModeSetting extends Setting<Integer> {
	private final List<String> modes;

	public ModeSetting(String name, String def, String... modes) {
		super(name, 0);
		this.modes = List.of(modes);
		value = Math.max(this.modes.indexOf(def), 0);
	}

	public List<String> modes() { return modes; }
	public String mode() { return modes.get(value); }
	public boolean is(String m) { return mode().equals(m); }

	@Override public int height() { return 14 + modes.size() * 11 + 2; }
	@Override public JsonElement toJson() { return new JsonPrimitive(mode()); }

	@Override
	public void fromJson(JsonElement e) {
		int i = modes.indexOf(e.getAsString());
		if (i >= 0) value = i;
	}
}