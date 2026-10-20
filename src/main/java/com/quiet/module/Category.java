package com.quiet.module;

import com.quiet.gui.Icons;

public enum Category {
	COMBAT("Combat", Icons.COMBAT),
	MOVEMENT("Movement", Icons.MOVEMENT),
	RENDER("Render", Icons.RENDER),
	PLAYER("Player", Icons.PLAYER),
	MISC("Misc", Icons.MISC);

	private final String title;
	private final String icon;

	Category(String title, String icon) { this.title = title; this.icon = icon; }

	public String title() { return title; }
	public String icon() { return icon; }
}