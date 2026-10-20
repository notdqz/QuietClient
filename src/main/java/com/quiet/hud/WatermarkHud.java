package com.quiet.hud;

import com.quiet.gui.Draw;
import com.quiet.gui.Fonts;
import com.quiet.gui.Icons;
import com.quiet.gui.Theme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ServerData;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class WatermarkHud extends HudWidget {
	private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("h:mm:ssa");
	private static final int LOGO_W = 19;

	private String user = "", server = "", time = "";
	private int timeW;

	public WatermarkHud() { super("watermark", "Watermark", Icons.LOGO, 0.012f, 0.02f); }

	private static int seg(int textW) { return 6 + 9 + 4 + textW + 6; }

	@Override
	public void update(boolean preview) {
		Minecraft mc = Minecraft.getInstance();
		user = mc.getUser().getName();
		ServerData sd = mc.getCurrentServer();
		server = sd != null ? sd.ip : (mc.getSingleplayerServer() != null ? "Singleplayer" : "Offline");
		time = LocalTime.now().format(FMT);
		timeW = Math.max(Fonts.width(time), Fonts.width("12:00:00PM"));
		w = LOGO_W + 1 + seg(Fonts.width(user)) + 1 + seg(Fonts.width(server)) + 1 + seg(timeW);
		h = 15;
	}

	@Override
	public void draw(GuiGraphics g, Theme t, int x, int y) {
		HudDraw.card(g, t, x, y, w, h, 6);
		int cy = y + h / 2;

		Draw.roundLeft(g, x, y, LOGO_W, h, 6, t.panelRowA);
		Icons.draw(g, Icons.LOGO, x + (LOGO_W - 9) / 2, cy - 4, t.panelText);

		int cx = x + LOGO_W;
		cx = segment(g, t, cx, y, cy, Icons.PLAYER, user, Fonts.width(user));
		cx = segment(g, t, cx, y, cy, Icons.CLOUD, server, Fonts.width(server));
		segment(g, t, cx, y, cy, Icons.CLOCK, time, timeW);
		HudDraw.cardTop(g, t, x, y, w, h, 6);
	}

	private int segment(GuiGraphics g, Theme t, int cx, int y, int cy, String icon, String text, int textW) {
		Draw.rect(g, cx, y + 2, 0.5f, h - 4, t.panelBorder);
		cx++;
		Icons.draw(g, icon, cx + 6, cy - 4, t.panelText);
		Fonts.draw(g, text, cx + 6 + 9 + 4, cy, t.panelText);
		return cx + seg(textW);
	}
}