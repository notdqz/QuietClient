package com.quiet.hud;

import com.quiet.gui.Draw;
import com.quiet.gui.Fonts;
import com.quiet.gui.Theme;
import com.quiet.module.Category;
import com.quiet.module.Module;
import com.quiet.module.ModuleManager;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.List;

/** Right-aligned list of enabled modules, longest first. */
public class ArraylistHud extends HudWidget {
	private final List<String> names = new ArrayList<>();

	public ArraylistHud() { super("arraylist", "Arraylist", com.quiet.gui.Icons.LIST, 0.86f, 0.02f); }

	@Override
	public void update(boolean preview) {
		names.clear();
		for (Category c : Category.values())
			for (Module m : ModuleManager.byCategory(c))
				if (m.isEnabled()) names.add(m.name());
		if (names.isEmpty() && preview) { names.add("KillAura"); names.add("Speed"); names.add("NameTags"); }
		names.sort((a, b) -> Fonts.width(b) - Fonts.width(a));
		visible = !names.isEmpty();
		int max = 60;
		for (String n : names) max = Math.max(max, Fonts.width(n) + 14);
		w = max;
		h = Math.max(1, names.size() * 15 - 2);
	}

	@Override
	public void draw(GuiGraphics g, Theme t, int x, int y) {
		for (int i = 0; i < names.size(); i++) {
			String n = names.get(i);
			int pw = Fonts.width(n) + 14, px = x + w - pw, py = y + i * 15;
			HudDraw.card(g, t, px, py, pw, 13, 6);
			Draw.round(g, px + pw - 3, py + 3, 1, 7, 1, t.panelText);   // accent tick on the right edge
			Fonts.draw(g, n, px + 6, py + 6, t.panelText);
			HudDraw.cardTop(g, t, px, py, pw, 13, 6);
		}
	}
}