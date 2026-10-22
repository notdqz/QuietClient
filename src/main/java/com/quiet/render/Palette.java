package com.quiet.render;

public final class Palette {
	public static final String[] NAMES = {"White", "Cyan", "Red", "Green", "Blue", "Purple", "Orange", "Yellow", "Pink"};

	public static int color(String n) {
		return switch (n) {
			case "Cyan" -> 0xFF4DE8FF;
			case "Red" -> 0xFFFF4D4D;
			case "Green" -> 0xFF4DFF88;
			case "Blue" -> 0xFF4D8DFF;
			case "Purple" -> 0xFFB066FF;
			case "Orange" -> 0xFFFFA64D;
			case "Yellow" -> 0xFFFFE14D;
			case "Pink" -> 0xFFFF7ACB;
			default -> 0xFFFFFFFF;
		};
	}

	public static int alpha(int c, int a) { return (Math.max(0, Math.min(255, a)) << 24) | (c & 0xFFFFFF); }
}
