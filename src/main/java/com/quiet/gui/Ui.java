package com.quiet.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/** Flat drawing helpers: plain rectangles with trimmed corners and the vanilla font. No sprites, no custom assets. */
public final class Ui {
	public static final int PANEL = 0xF2121317, EDGE = 0xFF2A2B31, INNER = 0xC016171B, CHECK_OFF = 0xFF202126;
	public static final int ROW = 0xC81B1C21, ROW_HOVER = 0xD023242A, ROW_ON = 0xE62C2D35;
	public static final int TEXT = 0xFF8E9096, TEXT_ON = 0xFFFFFFFF, DIM = 0xFF5A5C63, ACCENT = 0xFFE6E7EB, TRACK = 0xFF34353B;

	public static void rect(GuiGraphics g, int x, int y, int w, int h, int c) { g.fill(x, y, x + w, y + h, c); }

	/** Rectangle with rounded corners of radius r (kept small: it is built from per-row fills). */
	public static void round(GuiGraphics g, int x, int y, int w, int h, int r, int c) {
		r = Math.min(r, Math.min(w, h) / 2);
		if (r <= 0) { rect(g, x, y, w, h, c); return; }
		for (int i = 0; i < r; i++) {
			double dy = r - i - 0.5;
			int inset = r - (int) Math.round(Math.sqrt(r * r - dy * dy));
			g.fill(x + inset, y + i, x + w - inset, y + i + 1, c);
			g.fill(x + inset, y + h - 1 - i, x + w - inset, y + h - i, c);
		}
		g.fill(x, y + r, x + w, y + h - r, c);
	}

	/** One-pixel border: draws the border colour, then the fill inset by 1. Fill should be opaque. */
	public static void border(GuiGraphics g, int x, int y, int w, int h, int r, int border, int fill) {
		round(g, x, y, w, h, r, border);
		round(g, x + 1, y + 1, w - 2, h - 2, Math.max(0, r - 1), fill);
	}

	public static int mix(int a, int b, float t) {
		t = Math.max(0f, Math.min(1f, t));
		int al = (int) (((a >>> 24) & 255) + ((((b >>> 24) & 255) - ((a >>> 24) & 255)) * t));
		int r = (int) (((a >> 16) & 255) + ((((b >> 16) & 255) - ((a >> 16) & 255)) * t));
		int gr = (int) (((a >> 8) & 255) + ((((b >> 8) & 255) - ((a >> 8) & 255)) * t));
		int bl = (int) ((a & 255) + (((b & 255) - (a & 255)) * t));
		return (al << 24) | (r << 16) | (gr << 8) | bl;
	}

	public static boolean in(double mx, double my, int x, int y, int w, int h) {
		return mx >= x && mx < x + w && my >= y && my < y + h;
	}

	// ---- text (cy = vertical centre of the line)

	public static int width(String s) { return Minecraft.getInstance().font.width(Component.literal(s)); }

	public static void text(GuiGraphics g, String s, int x, int cy, int color) {
		g.drawString(Minecraft.getInstance().font, Component.literal(s), x, cy - 4, color, false);
	}

	public static void textRight(GuiGraphics g, String s, int xRight, int cy, int color) { text(g, s, xRight - width(s), cy, color); }

	public static void textCentered(GuiGraphics g, String s, int cx, int cy, int color) { text(g, s, cx - width(s) / 2, cy, color); }

	public static String fit(String s, int maxW) {
		if (width(s) <= maxW) return s;
		while (!s.isEmpty() && width(s + "..") > maxW) s = s.substring(0, s.length() - 1);
		return s + "..";
	}
}
