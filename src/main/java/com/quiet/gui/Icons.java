package com.quiet.gui;

import com.quiet.Quiet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;

import java.util.HashMap;
import java.util.Map;

public final class Icons {
	private static final Style STYLE = Style.EMPTY.withFont(new FontDescription.Resource(Quiet.id("icons")));
	private static final int ICON_CENTER = 2;
	private static final Map<String, Component> CACHE = new HashMap<>();

	public static final String LOGO = "\ue54e", COMBAT = "\ue2b4", MOVEMENT = "\ue073", RENDER = "\ue0ff",
			PLAYER = "\ue19f", MISC = "\ue20c", SEARCH = "\ue151", GEAR = "\ue154", HUD = "\ue1c1",
			CHECK = "\ue06c", CHECK_CIRCLE = "\ue226", HEX_X = "\ue128", EYE = "\ue0ba", FEATHER = "\ue0be",
			CLOUD = "\ue088", CLOCK = "\ue087", POTION = "\ue0d5", KEYBOARD = "\ue284", SHIELD = "\ue158",
			TARGET = "\ue0ac", BELL = "\ue059", GRIP = "\ue0eb", ELLIPSIS = "\ue0b6", GAUGE = "\ue1bf",
			WIFI = "\ue1ae", PIN = "\ue111", ZAP = "\ue1b4", LIST = "\ue106", PACKAGE = "\ue129",
			TIMER = "\ue1e0", CPU = "\ue0a9", USERS = "\ue1a4", COMPASS = "\ue09b", SHIRT = "\ue1ca",
			HEART = "\ue0f2", ACTIVITY = "\ue038";

	/** Draws the icon centred in the 9x9 box whose top-left corner is (x, y). */
	public static void draw(GuiGraphics g, String glyph, int x, int y, int color) {
		Font f = Minecraft.getInstance().font;
		Component c = CACHE.computeIfAbsent(glyph, k -> Component.literal(k).withStyle(STYLE));
		int w = f.width(c);
		g.drawString(f, c, x + (9 - w) / 2, y + 4 - ICON_CENTER, Draw.a(color), false);
	}
}