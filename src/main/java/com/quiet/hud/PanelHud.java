package com.quiet.hud;

import com.quiet.gui.Theme;
import net.minecraft.client.gui.GuiGraphics;

import java.util.List;
import java.util.function.Function;

/** Generic rows panel. The row data comes from a function, so a new panel is one line in HudManager. */
public class PanelHud extends HudWidget {
	private final String title;
	private final Function<Boolean, List<HudDraw.Row>> source;
	private List<HudDraw.Row> rows = List.of();

	public PanelHud(String id, String name, String icon, float fx, float fy, Function<Boolean, List<HudDraw.Row>> source) {
		super(id, name, icon, fx, fy);
		this.title = name;
		this.source = source;
	}

	@Override
	public void update(boolean preview) {
		rows = source.apply(preview);
		visible = !rows.isEmpty();
		if (visible) {
			w = HudDraw.panelWidth(title, rows);
			h = HudDraw.panelHeight(rows);
		}
	}

	@Override
	public void draw(GuiGraphics g, Theme t, int x, int y) {
		HudDraw.panel(g, t, x, y, w, title, rows);
	}
}