package com.quiet.module;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import java.util.List;
import java.util.function.BooleanSupplier;

/** A module option. Three kinds only: on/off, number slider, pick-one list. */
public abstract class Setting<T> {
	public final String name;
	protected T value;
	private BooleanSupplier visible = () -> true;

	protected Setting(String name, T value) { this.name = name; this.value = value; }

	public T get() { return value; }
	public boolean isVisible() { return visible.getAsBoolean(); }
	public Setting<T> visibleWhen(BooleanSupplier s) { visible = s; return this; }

	/** Row height in the menu. */
	public abstract int height();
	public abstract JsonElement toJson();
	public abstract void fromJson(JsonElement e);

	public static final class Bool extends Setting<Boolean> {
		public Bool(String name, boolean def) { super(name, def); }
		public void toggle() { value = !value; }
		@Override public int height() { return 14; }
		@Override public JsonElement toJson() { return new JsonPrimitive(value); }
		@Override public void fromJson(JsonElement e) { value = e.getAsBoolean(); }
	}

	public static final class Num extends Setting<Double> {
		public final double min, max, step;

		public Num(String name, double def, double min, double max, double step) {
			super(name, def);
			this.min = min; this.max = max; this.step = step;
			set(def);
		}

		public void set(double v) {
			double snapped = Math.round((v - min) / step) * step + min;
			value = Math.max(min, Math.min(max, snapped));
		}

		public int asInt() { return (int) Math.round(value); }
		public float asFloat() { return value.floatValue(); }
		public double fraction() { return (value - min) / (max - min); }

		public String display() {
			if (step >= 1) return String.valueOf((int) Math.round(value));
			return String.format(step >= 0.1 ? "%.1f" : "%.2f", value);
		}

		@Override public int height() { return 20; }
		@Override public JsonElement toJson() { return new JsonPrimitive(value); }
		@Override public void fromJson(JsonElement e) { set(e.getAsDouble()); }
	}

	public static final class Mode extends Setting<Integer> {
		public final List<String> modes;

		public Mode(String name, String def, String... modes) {
			super(name, 0);
			this.modes = List.of(modes);
			value = Math.max(this.modes.indexOf(def), 0);
		}

		public String mode() { return modes.get(value); }
		public boolean is(String m) { return mode().equals(m); }
		public void cycle(int dir) { value = Math.floorMod(value + dir, modes.size()); }

		@Override public int height() { return 14; }
		@Override public JsonElement toJson() { return new JsonPrimitive(mode()); }
		@Override public void fromJson(JsonElement e) {
			int i = modes.indexOf(e.getAsString());
			if (i >= 0) value = i;
		}
	}
}
