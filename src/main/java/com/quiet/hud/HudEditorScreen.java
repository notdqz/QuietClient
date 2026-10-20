package com.quiet.hud;

import com.quiet.config.QuietConfig;
import com.quiet.gui.Draw;
import com.quiet.gui.Fonts;
import com.quiet.gui.Icons;
import com.quiet.gui.Theme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

public class HudEditorScreen extends Screen {
	private record Chip(int x, int y, int w, int h, HudWidget widget) {} // widget == null -> "Done"

	private final Screen parent;
	private HudWidget dragging;
	private int offX, offY;

	public HudEditorScreen(Screen parent) {
		super(Component.literal("HUD"));
		this.parent = parent;
	}

	@Override public boolean isPauseScreen() { return false; }
	@Override public void onClose() { minecraft.setScreen(parent); }
	@Override public void removed() { QuietConfig.save(); }

	@Override
	public void renderBackground(GuiGraphics g, int mx, int my, float pt) {
		if (QuietConfig.blur) super.renderBackground(g, mx, my, pt);
	}

	private static boolean in(double mx, double my, int x, int y, int w, int h) {
		return mx >= x && mx < x + w && my >= y && my < y + h;
	}

	private List<Chip> chips() {
		int n = HudManager.WIDGETS.size() + 1, gap = 6, maxW = width - 40;
		int[] ws = new int[n];
		for (int i = 0; i < n; i++)
			ws[i] = 8 + 9 + 5 + Fonts.width(i < n - 1 ? HudManager.WIDGETS.get(i).name : "Done") + 9;

		List<List<Integer>> lines = new ArrayList<>();
		List<Integer> cur = new ArrayList<>();
		int curW = 0;
		for (int i = 0; i < n; i++) {
			if (!cur.isEmpty() && curW + gap + ws[i] > maxW) { lines.add(cur); cur = new ArrayList<>(); curW = 0; }
			if (!cur.isEmpty()) curW += gap;
			cur.add(i);
			curW += ws[i];
		}
		lines.add(cur);

		List<Chip> out = new ArrayList<>();
		for (int li = 0; li < lines.size(); li++) {
			List<Integer> line = lines.get(li);
			int total = gap * (line.size() - 1);
			for (int i : line) total += ws[i];
			int x = (width - total) / 2, y = height - 14 - (lines.size() - li) * 24;
			for (int i : line) {
				out.add(new Chip(x, y, ws[i], 18, i < n - 1 ? HudManager.WIDGETS.get(i) : null));
				x += ws[i] + gap;
			}
		}
		return out;
	}

	private HudWidget hoveredWidget(double mx, double my) {
		for (int i = HudManager.WIDGETS.size() - 1; i >= 0; i--) {
			HudWidget wd = HudManager.WIDGETS.get(i);
			if (!wd.enabled || !wd.isVisible()) continue;
			if (in(mx, my, wd.x(width), wd.y(height), wd.w, wd.h)) return wd;
		}
		return null;
	}

	@Override
	public void render(GuiGraphics g, int mx, int my, float pt) {
		Theme t = Theme.current();
		g.fillGradient(0, 0, width, height, t.bgTop, t.bgBottom);

		Fonts.drawCentered(g, "Drag to move  -  Right-click to remove  -  Use the chips below to add or remove",
				width / 2, 16, t.headerText, false);

		for (HudWidget wd : HudManager.WIDGETS) if (wd.enabled) wd.update(true);
		HudWidget hov = dragging != null ? dragging : hoveredWidget(mx, my);
		for (HudWidget wd : HudManager.WIDGETS) {
			if (!wd.enabled || !wd.isVisible()) continue;
			int x = wd.x(width), y = wd.y(height);
			wd.draw(g, t, x, y);
			int a = wd == hov ? 0xB0 : 0x40;
			Draw.outline(g, x - 2, y - 2, wd.w + 4, wd.h + 4, (a << 24) | (t.icon & 0xFFFFFF));
		}

		for (Chip c : chips()) {
			boolean done = c.widget() == null;
			boolean on = done || c.widget().enabled;
			Draw.shadow(g, c.x(), c.y(), c.w(), c.h(), 2, false, 0.7f);
			Draw.round(g, c.x(), c.y(), c.w(), c.h(), 9, on ? t.rowActive : t.searchBg);
			Draw.gloss(g, c.x(), c.y(), c.w(), c.h(), 9);
			Draw.ring(g, c.x(), c.y(), c.w(), c.h(), 9, on ? t.rowActiveBorder : t.searchBorder);
			int col = on ? t.textActive : t.text;
			Icons.draw(g, done ? Icons.CHECK : c.widget().icon, c.x() + 8, c.y() + 4, col);
			Fonts.draw(g, done ? "Done" : c.widget().name, c.x() + 8 + 9 + 5, c.y() + c.h() / 2, col);
		}
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent ev, boolean dbl) {
		double mx = ev.x(), my = ev.y();
		int btn = ev.button();

		for (Chip c : chips()) {
			if (!in(mx, my, c.x(), c.y(), c.w(), c.h())) continue;
			if (btn == 0) {
				if (c.widget() == null) onClose();
				else { c.widget().enabled = !c.widget().enabled; QuietConfig.save(); }
			}
			return true;
		}

		HudWidget wd = hoveredWidget(mx, my);
		if (wd != null) {
			if (btn == 0) {
				dragging = wd;
				offX = (int) mx - wd.x(width);
				offY = (int) my - wd.y(height);
			} else if (btn == 1) {
				wd.enabled = false;
				QuietConfig.save();
			}
			return true;
		}
		return super.mouseClicked(ev, dbl);
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent ev, double dx, double dy) {
		if (dragging == null) return super.mouseDragged(ev, dx, dy);
		int nx = Mth.clamp((int) ev.x() - offX, 0, Math.max(0, width - dragging.w));
		int ny = Mth.clamp((int) ev.y() - offY, 0, Math.max(0, height - dragging.h));

		if (Math.abs(nx + dragging.w / 2 - width / 2) <= 4) nx = width / 2 - dragging.w / 2;
		if (Math.abs(ny + dragging.h / 2 - height / 2) <= 4) ny = height / 2 - dragging.h / 2;
		if (nx <= 4) nx = 0;
		if (ny <= 4) ny = 0;
		if (nx + dragging.w >= width - 4) nx = width - dragging.w;
		if (ny + dragging.h >= height - 4) ny = height - dragging.h;

		dragging.moveTo(nx, ny, width, height);
		return true;
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent ev) {
		if (dragging != null) { dragging = null; QuietConfig.save(); }
		return super.mouseReleased(ev);
	}
}