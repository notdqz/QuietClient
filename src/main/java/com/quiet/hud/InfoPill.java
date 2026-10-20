package com.quiet.hud;

import com.quiet.gui.Draw;
import com.quiet.gui.Fonts;
import com.quiet.gui.Icons;
import com.quiet.gui.Theme;
import net.minecraft.client.gui.GuiGraphics;

import java.util.function.Supplier;

/** Small [icon | text] pill. A new one is a single line in HudManager. */
public class InfoPill extends HudWidget {
	private final Supplier<String> text;
	private String value = "";

	public InfoPill(String id, String name, String icon, float fx, float fy, Supplier<String> text) {
		super(id, name, icon, fx, fy);
		this.text = text;
	}

	@Override
	public void update(boolean preview) {
		value = text.get();
		visible = value != null;
		if (!visible) return;
		int raw = 17 + 6 + Fonts.width(value) + 7;
		w = (raw + 5) / 6 * 6;          // rounded up so the pill doesn't jitter as digits change
		h = 15;
	}

	@Override
	public void draw(GuiGraphics g, Theme t, int x, int y) {
		HudDraw.card(g, t, x, y, w, h, 6);
		Draw.roundLeft(g, x, y, 17, h, 6, t.panelRowA);
		Icons.draw(g, icon, x + 4, y + h / 2 - 4, t.panelText);
		Fonts.draw(g, value, x + 17 + 6, y + h / 2, t.panelText);
		HudDraw.cardTop(g, t, x, y, w, h, 6);
	}
}