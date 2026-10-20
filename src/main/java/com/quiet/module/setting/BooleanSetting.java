package com.quiet.module.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public class BooleanSetting extends Setting<Boolean> {
	public BooleanSetting(String name, boolean def) { super(name, def); }
	public void toggle() { value = !value; }
	@Override public int height() { return 16; }
	@Override public JsonElement toJson() { return new JsonPrimitive(value); }
	@Override public void fromJson(JsonElement e) { value = e.getAsBoolean(); }
}