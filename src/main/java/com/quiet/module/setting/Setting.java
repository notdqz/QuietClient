package com.quiet.module.setting;

import com.google.gson.JsonElement;

import java.util.function.BooleanSupplier;

public abstract class Setting<T> {
	private final String name;
	protected T value;
	private BooleanSupplier visible = () -> true;

	protected Setting(String name, T value) { this.name = name; this.value = value; }

	public String name() { return name; }
	public T get() { return value; }
	public void set(T v) { value = v; }

	public boolean isVisible() { return visible.getAsBoolean(); }
	public Setting<T> visibleWhen(BooleanSupplier s) { visible = s; return this; }

	/** Height in GUI pixels inside the expanded module card. */
	public abstract int height();

	public abstract JsonElement toJson();
	public abstract void fromJson(JsonElement e);
}