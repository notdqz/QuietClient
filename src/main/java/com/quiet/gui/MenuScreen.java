package com.quiet.gui;

import com.quiet.config.QuietConfig;
import com.quiet.hud.HudEditorScreen;
import com.quiet.module.Category;
import com.quiet.module.Module;
import com.quiet.module.ModuleManager;
import com.quiet.module.setting.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class MenuScreen extends Screen {
	private static final Category[] CATS = Category.values();
	private static final int GAP = 10, HEADER_H = 34, ROW_H = 20, ROW_GAP = 4, SEARCH_H = 26, PAD = 4, BIND_H = 18;

	private final float[] scroll = new float[CATS.length];
	private final long openedAt = System.currentTimeMillis();
	private boolean openKeyReleased;
	private String search = "";
	private boolean searchFocused;
	private Module dragging;
	private Module binding, prevBinding;
	private NumberSetting slider;
	private int sliderX, sliderW;

	private int colW, left, top, listY, listH, searchX, searchY, searchW, gearX, hudX;

	public MenuScreen() { super(Component.literal("Quiet")); }

	@Override public boolean isPauseScreen() { return false; }

	@Override public void removed() { QuietConfig.save(); }

	@Override
	public void renderBackground(GuiGraphics g, int mx, int my, float pt) {
		if (QuietConfig.blur) super.renderBackground(g, mx, my, pt);
	}

	// ---------------------------------------------------------------- layout

	private void layout() {
		colW = Mth.clamp((width - 32 - GAP * 4) / 5, 92, 118);
		int totalW = colW * 5 + GAP * 4;
		left = (width - totalW) / 2;
		listH = Mth.clamp(height - 150, 110, 290);
		int totalH = HEADER_H + 8 + listH + 16 + SEARCH_H;
		top = Math.max(8, (height - totalH) / 2);
		listY = top + HEADER_H + 8;
		searchY = listY + listH + 16;
		searchW = Math.min(200, totalW - 100);
		searchX = (width - searchW) / 2;
		gearX = searchX - 6 - SEARCH_H;
		hudX = searchX + searchW + 6;
	}

	private int colX(int c) { return left + c * (colW + GAP); }

	private static boolean in(double mx, double my, int x, int y, int w, int h) {
		return mx >= x && mx < x + w && my >= y && my < y + h;
	}

	private List<Module> visible(int c) {
		List<Module> all = ModuleManager.byCategory(CATS[c]);
		if (search.isEmpty()) return all;
		String q = search.toLowerCase(Locale.ROOT);
		List<Module> out = new ArrayList<>();
		for (Module m : all) if (m.name().toLowerCase(Locale.ROOT).contains(q)) out.add(m);
		return out;
	}

	private static int rowTotal(Module m) {
		if (!m.expanded) return ROW_H;
		int h = 0;
		for (Setting<?> s : m.settings()) if (s.isVisible()) h += s.height();
		return ROW_H + PAD * 2 + h + BIND_H;
	}

	private void clampScroll(int c, List<Module> mods) {
		int content = 0;
		for (Module m : mods) content += rowTotal(m) + ROW_GAP;
		content = Math.max(0, content - ROW_GAP);
		float max = Math.max(0, content - (listH - 12));
		scroll[c] = Mth.clamp(scroll[c], 0, max);
	}

	// ---------------------------------------------------------------- render

	@Override
	public void render(GuiGraphics g, int mx, int my, float pt) {
		layout();
		Theme t = Theme.current();

		float p = Mth.clamp((System.currentTimeMillis() - openedAt) / 180f, 0f, 1f);
		p = 1f - (1f - p) * (1f - p) * (1f - p);

		Draw.alpha = p;
		try {
			g.fillGradient(0, 0, width, height, Draw.a(t.bgTop), Draw.a(t.bgBottom));
			g.pose().pushMatrix();
			g.pose().translate(0, (1f - p) * 8f);
			for (int c = 0; c < CATS.length; c++) drawColumn(g, t, c, mx, my);
			drawSearch(g, t, mx, my);
			g.pose().popMatrix();
		} finally {
			Draw.alpha = 1f;
		}
	}

	private void drawColumn(GuiGraphics g, Theme t, int c, int mx, int my) {
		int x = colX(c);
		Category cat = CATS[c];

		Draw.shadow(g, x, top, colW, HEADER_H, 4, true);
		Draw.round(g, x, top, colW, HEADER_H, 12, t.container);
		Draw.gloss(g, x, top, colW, HEADER_H, 12);
		Draw.ring(g, x, top, colW, HEADER_H, 12, t.rowBorder);

		int bx = x + 6, by = top + 6, bw = colW - 12, bh = 22;
		Draw.shadow(g, bx, by, bw, bh, 1, false, 0.6f);
		Draw.roundBorder(g, bx, by, bw, bh, 8, t.headerBorder, t.header);
		Draw.gloss(g, bx, by, bw, bh, 8);
		int tw = Fonts.widthMed(cat.title());
		int sx = bx + (bw - (9 + 5 + tw)) / 2;
		Icons.draw(g, cat.icon(), sx, by + (bh - 9) / 2, t.headerText);
		Fonts.drawMed(g, cat.title(), sx + 14, by + bh / 2, t.headerText);

		Draw.shadow(g, x, listY, colW, listH, 4, true);
		Draw.round(g, x, listY, colW, listH, 12, t.container);
		Draw.gloss(g, x, listY, colW, listH, 12);
		Draw.ring(g, x, listY, colW, listH, 12, t.rowBorder);

		List<Module> mods = visible(c);
		clampScroll(c, mods);

		g.enableScissor(x + 2, listY + 3, x + colW - 2, listY + listH - 3);
		int ry = listY + 6 - (int) scroll[c];
		for (Module m : mods) {
			int h = rowTotal(m);
			if (ry + h > listY && ry < listY + listH) drawModule(g, t, m, x + 6, ry, colW - 12, h, mx, my);
			ry += h + ROW_GAP;
		}
		g.disableScissor();
	}

	private void drawModule(GuiGraphics g, Theme t, Module m, int x, int y, int w, int h, int mx, int my) {
		m.tickAnim();
		float a = m.anim;
		boolean on = m.isEnabled();

		if (m.expanded) {
			Draw.round(g, x, y, w, h, 8, t.settingsBg);
			Draw.ring(g, x, y, w, h, 8, Draw.lerp(t.rowBorder, t.rowActiveBorder, a));
		}
		if (a > 0.03f) Draw.shadow(g, x, y, w, ROW_H, 2, false, a);

		int bg = Draw.lerp(t.row, t.rowActive, a);
		int border = Draw.lerp(t.rowBorder, t.rowActiveBorder, a);
		if (in(mx, my, x, y, w, ROW_H)) bg = Draw.lerp(bg, t.rowActive, 0.5f);
		Draw.round(g, x, y, w, ROW_H, 8, bg);
		Draw.gloss(g, x, y, w, ROW_H, 8);
		Draw.ring(g, x, y, w, ROW_H, 8, border);

		Icons.draw(g, Icons.GRIP, x + 3, y + ROW_H / 2 - 4, t.dim);

		int tx = x + w - 22, ty = y + 6;
		Draw.round(g, tx, ty, 16, 8, 4, Draw.lerp(t.trackOff, t.trackOn, a));
		Draw.circle(g, tx + 4 + a * 8, ty + 4, 3, Draw.lerp(t.knobOff, t.knobOn, a));

		// key badge
		boolean isBinding = binding == m;
		String kn = isBinding ? "..." : (m.key() >= 0 ? m.keyName() : null);
		int chipW = 0;
		if (kn != null) {
			chipW = Fonts.width(kn) + 8;
			int cx = tx - 5 - chipW;
			Draw.roundBorder(g, cx, y + 5, chipW, 10, 4, isBinding ? t.rowActiveBorder : t.checkBorder, t.settingsBg);
			Fonts.drawCentered(g, kn, cx + chipW / 2, y + ROW_H / 2, isBinding ? t.textActive : t.subtle, false);
		}

		int nameMax = w - 15 - 28 - (chipW > 0 ? chipW + 5 : 0);
		Fonts.draw(g, Fonts.fit(m.name(), nameMax), x + 15, y + ROW_H / 2, Draw.lerp(t.text, t.textActive, a));

		if (m.expanded) {
			int sy = y + ROW_H + PAD;
			for (Setting<?> s : m.settings()) {
				if (!s.isVisible()) continue;
				drawSetting(g, t, s, x + 8, sy, w - 16, on);
				sy += s.height();
			}
			drawBindRow(g, t, m, x + 8, sy, w - 16, on);
		}
	}

	private void drawBindRow(GuiGraphics g, Theme t, Module m, int sx, int sy, int sw, boolean on) {
		int label = on ? t.subtleActive : t.subtle;
		boolean isBinding = binding == m;
		Fonts.draw(g, "Bind", sx, sy + BIND_H / 2, label);
		String txt = isBinding ? "Press a key..." : m.keyName();
		int cw = Fonts.width(txt) + 12, cx = sx + sw - cw, cy = sy + 3;
		Draw.roundBorder(g, cx, cy, cw, 12, 5, isBinding ? t.rowActiveBorder : t.checkBorder, isBinding ? t.rowActive : t.checkOff);
		Fonts.drawCentered(g, txt, cx + cw / 2, cy + 6, isBinding ? t.textActive : label, false);
	}

	private void drawSetting(GuiGraphics g, Theme t, Setting<?> s, int sx, int sy, int sw, boolean on) {
		int label = on ? t.subtleActive : t.subtle;

		if (s instanceof BooleanSetting b) {
			Fonts.draw(g, s.name(), sx, sy + 8, label);
			int bx = sx + sw - 10, by = sy + 3;
			Draw.roundBorder(g, bx, by, 10, 10, 4, t.checkBorder, b.get() ? t.checkOn : t.checkOff);
			if (b.get()) Icons.draw(g, Icons.CHECK, bx + 1, by + 1, t.checkMark);

		} else if (s instanceof NumberSetting n) {
			Fonts.draw(g, s.name(), sx, sy + 6, label);
			Fonts.drawRight(g, n.display(), sx + sw, sy + 6, t.subtle);
			int ty = sy + 16;
			float fw = (float) n.fraction() * sw;
			Draw.round(g, sx, ty, sw, 2, 1, t.sliderTrack);
			Draw.round(g, sx, ty, Math.max(2, fw), 2, 1, t.sliderFill);
			Draw.shadow(g, Mth.clamp(sx + fw, sx + 3, sx + sw - 3) - 3, ty - 2, 6, 6, 1, false, 0.7f);
			Draw.circle(g, Mth.clamp(sx + fw, sx + 3, sx + sw - 3), ty + 1, 3, t.sliderKnob);

		} else if (s instanceof ModeSetting md) {
			Fonts.draw(g, s.name(), sx, sy + 6, label);
			Fonts.drawRight(g, md.mode(), sx + sw, sy + 6, t.subtle);
			for (int i = 0; i < md.modes().size(); i++) {
				int oy = sy + 14 + i * 11;
				boolean sel = i == md.get();
				Fonts.draw(g, md.modes().get(i), sx + 2, oy + 5, sel ? label : t.dim);
				int cx = sx + sw - 4, cy = oy + 5;
				Draw.roundBorder(g, cx - 4, cy - 4, 8, 8, 4, t.checkBorder, t.checkOff);
				if (sel) Draw.circle(g, cx, cy, 2, t.sliderKnob);
			}
		}
	}

	private void iconButton(GuiGraphics g, Theme t, int x, int mx, int my, String icon) {
		boolean hover = in(mx, my, x, searchY, SEARCH_H, SEARCH_H);
		Draw.shadow(g, x, searchY, SEARCH_H, SEARCH_H, 2, false, 0.8f);
		Draw.round(g, x, searchY, SEARCH_H, SEARCH_H, 13, t.searchBg);
		Draw.gloss(g, x, searchY, SEARCH_H, SEARCH_H, 13);
		Draw.ring(g, x, searchY, SEARCH_H, SEARCH_H, 13, hover ? t.rowActiveBorder : t.searchBorder);
		Icons.draw(g, icon, x + 8, searchY + 8, t.icon);
	}

	private void drawSearch(GuiGraphics g, Theme t, int mx, int my) {
		iconButton(g, t, gearX, mx, my, Icons.GEAR);   // dark / light mode
		iconButton(g, t, hudX, mx, my, Icons.HUD);     // HUD editor

		Draw.shadow(g, searchX, searchY, searchW, SEARCH_H, 2, false, 0.8f);
		Draw.round(g, searchX, searchY, searchW, SEARCH_H, 13, t.searchBg);
		Draw.gloss(g, searchX, searchY, searchW, SEARCH_H, 13);
		Draw.ring(g, searchX, searchY, searchW, SEARCH_H, 13, searchFocused ? t.rowActiveBorder : t.searchBorder);

		int cy = searchY + SEARCH_H / 2;
		if (search.isEmpty() && !searchFocused) {
			Fonts.draw(g, "Search for module...", searchX + 14, cy, t.searchHint);
		} else {
			String shown = Fonts.fitTail(search, searchW - 44);
			Fonts.draw(g, shown, searchX + 14, cy, t.searchText);
			if (searchFocused && (System.currentTimeMillis() / 500) % 2 == 0)
				Draw.rect(g, searchX + 15 + Fonts.width(shown), searchY + 8, 0.5f, SEARCH_H - 16, t.searchText);
		}
		Icons.draw(g, Icons.SEARCH, searchX + searchW - 23, searchY + (SEARCH_H - 9) / 2, t.searchHint);
	}

	// ---------------------------------------------------------------- input

	@Override
	public boolean mouseClicked(MouseButtonEvent ev, boolean doubleClick) {
		layout();
		prevBinding = binding;   // any click cancels binding unless it re-targets below
		binding = null;

		double mx = ev.x(), my = ev.y();
		int btn = ev.button();

		boolean inSearch = in(mx, my, searchX, searchY, searchW, SEARCH_H);
		searchFocused = inSearch;
		if (inSearch) return true;

		if (btn == 0 && in(mx, my, gearX, searchY, SEARCH_H, SEARCH_H)) {
			QuietConfig.darkMode = !QuietConfig.darkMode;
			QuietConfig.save();
			return true;
		}
		if (btn == 0 && in(mx, my, hudX, searchY, SEARCH_H, SEARCH_H)) {
			minecraft.setScreen(new HudEditorScreen(this));
			return true;
		}

		for (int c = 0; c < CATS.length; c++) {
			int x = colX(c);
			if (!in(mx, my, x + 3, listY + 3, colW - 6, listH - 6)) continue;
			int ry = listY + 6 - (int) scroll[c];
			for (Module m : visible(c)) {
				int h = rowTotal(m);
				if (my >= ry && my < ry + h) {
					clickModule(m, x + 6, colW - 12, ry, mx, my, btn);
					return true;
				}
				ry += h + ROW_GAP;
			}
		}
		return super.mouseClicked(ev, doubleClick);
	}

	private void clickModule(Module m, int x, int w, int ry, double mx, double my, int btn) {
		if (my < ry + ROW_H) {
			if (btn == 0) {
				if (mx < x + 13 && search.isEmpty()) dragging = m;
				else m.toggle();
			} else if (btn == 1) {
				m.expanded = !m.expanded;
			} else if (btn == 2) {
				binding = prevBinding == m ? null : m;   // middle click = quick bind
			}
			return;
		}
		if (!m.expanded || btn != 0) return;

		int sx = x + 8, sw = w - 16;
		int sy = ry + ROW_H + PAD;
		for (Setting<?> s : m.settings()) {
			if (!s.isVisible()) continue;
			int sh = s.height();
			if (my >= sy && my < sy + sh) {
				if (s instanceof BooleanSetting b) {
					b.toggle();
				} else if (s instanceof NumberSetting n) {
					slider = n; sliderX = sx; sliderW = sw;
					setSlider(mx);
				} else if (s instanceof ModeSetting md) {
					for (int i = 0; i < md.modes().size(); i++) {
						int oy = sy + 14 + i * 11;
						if (my >= oy && my < oy + 11) md.set(i);
					}
				}
				return;
			}
			sy += sh;
		}
		if (my >= sy && my < sy + BIND_H) binding = prevBinding == m ? null : m;
	}

	private void setSlider(double mx) {
		double f = Mth.clamp((mx - sliderX) / sliderW, 0.0, 1.0);
		slider.set(slider.min() + f * (slider.max() - slider.min()));
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent ev, double dx, double dy) {
		if (slider != null) { setSlider(ev.x()); return true; }
		if (dragging != null) {
			List<Module> list = ModuleManager.byCategory(dragging.category());
			int c = dragging.category().ordinal();
			int y = listY + 6 - (int) scroll[c];
			int target = list.size() - 1;
			for (int i = 0; i < list.size(); i++) {
				int h = rowTotal(list.get(i));
				if (ev.y() < y + h / 2.0) { target = i; break; }
				y += h + ROW_GAP;
			}
			if (list.indexOf(dragging) != target) {
				list.remove(dragging);
				list.add(target, dragging);
			}
			return true;
		}
		return super.mouseDragged(ev, dx, dy);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent ev) {
		if (slider != null) QuietConfig.save();
		slider = null;
		dragging = null;
		return super.mouseReleased(ev);
	}

	@Override
	public boolean mouseScrolled(double mx, double my, double scrollX, double scrollY) {
		layout();
		for (int c = 0; c < CATS.length; c++) {
			if (in(mx, my, colX(c), listY, colW, listH)) {
				scroll[c] -= (float) (scrollY * 18);
				clampScroll(c, visible(c));
				return true;
			}
		}
		return super.mouseScrolled(mx, my, scrollX, scrollY);
	}

	@Override
	public boolean keyPressed(KeyEvent ev) {
		int k = ev.key();

		if (binding != null) {
			if (k == GLFW.GLFW_KEY_ESCAPE || k == GLFW.GLFW_KEY_RIGHT_SHIFT) {
				binding = null;                                   // cancel (RShift is reserved for the menu)
			} else if (k == GLFW.GLFW_KEY_BACKSPACE || k == GLFW.GLFW_KEY_DELETE) {
				binding.setKey(-1);                               // clear
				binding = null;
				QuietConfig.save();
			} else {
				binding.setKey(k);
				binding = null;
				QuietConfig.save();
			}
			return true;
		}

		if (searchFocused) {
			if (k == GLFW.GLFW_KEY_ESCAPE || k == GLFW.GLFW_KEY_ENTER) searchFocused = false;
			else if (k == GLFW.GLFW_KEY_BACKSPACE && !search.isEmpty()) {
				search = search.substring(0, search.length() - 1);
				Arrays.fill(scroll, 0);
			}
			return true;
		}
		if (k == GLFW.GLFW_KEY_RIGHT_SHIFT && (openKeyReleased || System.currentTimeMillis() - openedAt > 600)) {
			onClose();
			return true;
		}
		return super.keyPressed(ev);
	}

	@Override
	public boolean keyReleased(KeyEvent ev) {
		if (ev.key() == GLFW.GLFW_KEY_RIGHT_SHIFT) openKeyReleased = true;
		return super.keyReleased(ev);
	}

	@Override
	public boolean charTyped(CharacterEvent ev) {
		if (binding != null) return true;
		if (!searchFocused) return super.charTyped(ev);
		int cp = ev.codepoint();
		if (cp >= 32 && cp != 127 && search.length() < 24) {
			search += Character.toString(cp);
			Arrays.fill(scroll, 0);
		}
		return true;
	}
}