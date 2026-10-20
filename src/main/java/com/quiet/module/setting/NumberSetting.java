package com.quiet.module.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public class NumberSetting extends Setting<Double> {
	private final double min, max, step;

	public NumberSetting(String name, double def, double min, double max, double step) {
		super(name, def);
		this.min = min; this.max = max; this.step = step;
		set(def);
	}

	@Override
	public void set(Double v) {
		double snapped = Math.round((v - min) / step) * step + min;
		value = Math.max(min, Math.min(max, snapped));
	}

	public double min() { return min; }
	public double max() { return max; }
	public double step() { return step; }
	public int asInt() { return (int) Math.round(value); }
	public float asFloat() { return value.floatValue(); }
	public double fraction() { return (value - min) / (max - min); }

	public String display() {
		if (step >= 1) return String.valueOf((int) Math.round(value));
		return String.format(step >= 0.1 ? "%.1f" : "%.2f", value);
	}

	@Override public int height() { return 24; }
	@Override public JsonElement toJson() { return new JsonPrimitive(value); }
	@Override public void fromJson(JsonElement e) { set(e.getAsDouble()); }
}