package com.quiet.hud;

import com.quiet.gui.Draw;
import com.quiet.gui.Fonts;
import com.quiet.gui.Icons;
import com.quiet.gui.Theme;
import net.minecraft.client.gui.GuiGraphics;

import java.util.List;

public final class HudDraw {
	public record Row(String icon, String primary, String secondary, String value) {}

	public static final int HEADER = 15, ROW = 14;

	public static int panelHeight(List<Row> rows) { return HEADER + rows.size() * ROW + 1; }

	public static int panelWidth(String title, List<Row> rows) {
		int max = Fonts.widthMed(title) + 40;
		for (Row r : rows) {
			int rw = 5 + 9 + 4 + Fonts.width(r.primary())
					+ (r.secondary().isEmpty() ? 0 : 3 + Fonts.width(r.secondary()))
					+ 14 + Fonts.width(r.value()) + 6;
			max = Math.max(max, rw);
		}
		return Math.max(90, max);
	}

	/** Shared card look for every HUD element. */
	public static void card(GuiGraphics g, Theme t, int x, int y, int w, int h, float r) {
		Draw.shadow(g, x, y, w, h, 2, false);
		Draw.round(g, x, y, w, h, r, t.panel);
	}

	public static void cardTop(GuiGraphics g, Theme t, int x, int y, int w, int h, float r) {
		Draw.gloss(g, x, y, w, h, r);
		Draw.ring(g, x, y, w, h, r, t.panelBorder);
	}

	public static void panel(GuiGraphics g, Theme t, int x, int y, int w, String title, List<Row> rows) {
		int h = panelHeight(rows);
		card(g, t, x, y, w, h, 8);

		for (int i = 0; i < rows.size(); i++) {
			Row r = rows.get(i);
			int ry = y + HEADER + i * ROW, cy = ry + ROW / 2;
			int col = i % 2 == 0 ? t.panelRowA : t.panelRowB;
			if (i == rows.size() - 1) Draw.roundBottom(g, x, ry, w, ROW, 8, col);
			else g.fill(x, ry, x + w, ry + ROW, Draw.a(col));

			Icons.draw(g, r.icon(), x + 5, cy - 4, t.panelText);
			int tx = x + 18;
			Fonts.draw(g, r.primary(), tx, cy, t.panelText);
			if (!r.secondary().isEmpty())
				Fonts.draw(g, r.secondary(), tx + Fonts.width(r.primary()) + 3, cy, t.panelSub);
			Fonts.drawRight(g, r.value(), x + w - 5, cy, t.panelSub);
		}

		int hy = y + HEADER / 2;
		Icons.draw(g, Icons.ELLIPSIS, x + 4, hy - 4, t.dim);
		Fonts.drawCentered(g, title, x + w / 2, hy, t.panelText, true);
		Draw.rect(g, x + 1, y + HEADER, w - 2, 0.5f, t.panelBorder);
		cardTop(g, t, x, y, w, h, 8);
	}
}