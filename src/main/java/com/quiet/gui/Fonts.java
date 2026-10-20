package com.quiet.gui;

import com.quiet.Quiet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;

public final class Fonts {
	private static final Style REG = Style.EMPTY.withFont(new FontDescription.Resource(Quiet.id("inter")));
	private static final Style MED = Style.EMPTY.withFont(new FontDescription.Resource(Quiet.id("inter_medium")));

	/** Distance from the visual middle of the text to the draw origin. Raise/lower if text sits too low/high. */
	private static final int CENTER = 4;

	private static Font font() { return Minecraft.getInstance().font; }
	private static Component c(String s, boolean med) { return Component.literal(s).withStyle(med ? MED : REG); }

	public static int width(String s) { return font().width(c(s, false)); }
	public static int widthMed(String s) { return font().width(c(s, true)); }

	/** All draw methods take the vertical CENTER (cy) of the text. */
	public static void draw(GuiGraphics g, String s, int x, int cy, int color) {
		g.drawString(font(), c(s, false), x, cy - CENTER, color, false);
	}

	public static void drawMed(GuiGraphics g, String s, int x, int cy, int color) {
		g.drawString(font(), c(s, true), x, cy - CENTER, color, false);
	}

	public static void drawRight(GuiGraphics g, String s, int xRight, int cy, int color) {
		draw(g, s, xRight - width(s), cy, color);
	}

	public static void drawCentered(GuiGraphics g, String s, int cx, int cy, int color, boolean med) {
		if (med) drawMed(g, s, cx - widthMed(s) / 2, cy, color);
		else draw(g, s, cx - width(s) / 2, cy, color);
	}

	public static String fit(String s, int maxW) {
		if (width(s) <= maxW) return s;
		while (!s.isEmpty() && width(s + "..") > maxW) s = s.substring(0, s.length() - 1);
		return s + "..";
	}

	public static String fitTail(String s, int maxW) {
		while (!s.isEmpty() && width(s) > maxW) s = s.substring(1);
		return s;
	}
}