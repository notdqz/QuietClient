package com.quiet.module;

public enum Category {
	COMBAT("Combat"), MOVEMENT("Move"), PLAYER("Player"), VISUAL("Visual");

	public final String title;
	Category(String title) { this.title = title; }
}
