package com.quiet.gui;

import com.quiet.Config;
import com.quiet.module.Category;
import com.quiet.module.Module;
import com.quiet.module.ModuleManager;
import com.quiet.module.Setting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/**
 * One small window: tabs, a module list, a footer. Left-click a module to toggle it, right-click for its settings,
 * middle-click to bind a key (Backspace clears). Drag the title bar to move the window. The gear in the footer opens the
 * theme picker (colour wheel, brightness, contrast, reset) beside the window; colours change live while you drag.
 */
public class MenuScreen extends Screen {
	private static final Category[] CATS = Category.values();
	private static final int W = 164, PAD = 5, TITLE_H = 20, TAB_H = 16, TAB_GAP = 2, FOOT_H = 20;
	private static final int ROW = 16, ROW_GAP = 2, BIND_H = 14, LIST_PAD = 3;

	// theme picker panel
	private static final int PW = 120, WHEEL_R = 40, BAR_W = 100;
	private static final int DRAG_NONE = 0, DRAG_WHEEL = 1, DRAG_BRIGHT = 2, DRAG_CONTRAST = 3;

	private static final float[] scroll = new float[CATS.length];   // static: the list keeps its scroll position between openings
	private final long openedAt = System.currentTimeMillis();
	private boolean openKeyReleased;
	private int tab;
	private Module binding;
	private Setting.Num slider;
	private int sliderX, sliderW;
	private boolean dragging;
	private int dragDX, dragDY;
	private boolean pickerOpen;
	private int pickDrag = DRAG_NONE;

	private int left, top, tabsY, listY, listH, panelH, footY;
	private int px, py, ph, wheelCx, wheelCy, barX, brightY, contrastY, resetY;

	public MenuScreen() {
		super(Component.literal("Quiet"));
		tab = Mth.clamp(Config.menuTab, 0, CATS.length - 1);   // reopen on the tab you left
	}

	@Override public boolean isPauseScreen() { return false; }

	@Override
	public void removed() {
		Config.save();
		MenuTheme.save();
	}

	// ---------------------------------------------------------------- layout

	private void layout() {
		listH = Mth.clamp(height - 120, 80, 200);
		panelH = TITLE_H + TAB_H + 6 + listH + FOOT_H;
		left = Config.menuX >= 0 ? Mth.clamp(Config.menuX, 0, Math.max(0, width - W)) : (width - W) / 2;
		top = Config.menuY >= 0 ? Mth.clamp(Config.menuY, 0, Math.max(0, height - panelH)) : Math.max(6, (height - panelH) / 2);
		tabsY = top + TITLE_H;
		listY = tabsY + TAB_H + 6;
		footY = listY + listH;

		// theme picker: to the right of the window, or to the left when there is no room
		ph = 182;
		px = left + W + 6;
		if (px + PW > width - 2) px = left - 6 - PW;
		py = Mth.clamp(top, 2, Math.max(2, height - ph - 2));
		wheelCx = px + PW / 2;
		wheelCy = py + 20 + 2 + WHEEL_R;
		barX = px + (PW - BAR_W) / 2;
		brightY = wheelCy + WHEEL_R + 10;
		contrastY = brightY + 22;
		resetY = contrastY + 26;
	}

	private List<Module> mods() { return ModuleManager.byCategory(CATS[tab]); }

	// tabs share the row margin on both sides, so the strip is exactly as wide as the rows below and sits centred
	private int tabW() { return (W - PAD * 2 - TAB_GAP * (CATS.length - 1)) / CATS.length; }
	private int tabX(int i) { return left + PAD + i * (tabW() + TAB_GAP); }

	private static int settingsHeight(Module m) {
		int h = 0;
		for (Setting<?> s : m.settings) if (s.isVisible()) h += s.height();
		return h;
	}

	private static int rowTotal(Module m) { return m.expanded ? ROW + 2 + settingsHeight(m) + BIND_H + 3 : ROW; }

	private void clampScroll() {
		int content = 0;
		for (Module m : mods()) content += rowTotal(m) + ROW_GAP;
		content = Math.max(0, content - ROW_GAP);
		scroll[tab] = Mth.clamp(scroll[tab], 0, Math.max(0, content - (listH - LIST_PAD * 2)));
	}

	private int gearX() { return left + W - PAD - 16; }
	private int footBtnY() { return footY + 3; }

	// ---------------------------------------------------------------- render

	@Override
	public void render(GuiGraphics g, int mx, int my, float pt) {
		layout();
		float p = Mth.clamp((System.currentTimeMillis() - openedAt) / 140f, 0f, 1f);
		p = 1f - (1f - p) * (1f - p);

		g.fillGradient(0, 0, width, height, (int) (0x30 * p) << 24, (int) (0x90 * p) << 24);
		g.pose().pushMatrix();
		g.pose().translate(0, (1f - p) * 6f);

		Ui.round(g, left - 1, top - 1, W + 2, panelH + 2, 5, MenuTheme.edge);
		Ui.round(g, left, top, W, panelH, 4, MenuTheme.panel);
		FlowSurface.draw(g, left, top, W, panelH, MenuTheme.ribbonBase, MenuTheme.ribbonTint, MenuTheme.ribbonStrength);
		Ui.text(g, "Quiet", left + PAD + 3, top + TITLE_H / 2 + 1, MenuTheme.textOn);
		Ui.textRight(g, "@notdqz", left + W - PAD - 3, top + TITLE_H / 2 + 1, MenuTheme.dim);

		for (int i = 0; i < CATS.length; i++) {
			int x = tabX(i), w = tabW();
			boolean sel = i == tab, hover = Ui.in(mx, my, x, tabsY, w, TAB_H);
			Ui.round(g, x, tabsY, w, TAB_H, 3, sel ? MenuTheme.rowOn : hover ? MenuTheme.rowHover : MenuTheme.row);
			if (sel) Ui.rect(g, x + 4, tabsY + TAB_H - 2, w - 8, 1, MenuTheme.accent);
			Ui.textCentered(g, Ui.fit(CATS[i].title, w - 2), x + w / 2, tabsY + TAB_H / 2, sel ? MenuTheme.textOn : MenuTheme.text);
		}

		clampScroll();
		g.enableScissor(left + 1, listY, left + W - 1, listY + listH);
		int ry = listY + LIST_PAD - (int) scroll[tab];
		for (Module m : mods()) {
			int h = rowTotal(m);
			if (ry + h > listY && ry < listY + listH) drawModule(g, m, left + PAD, ry, W - PAD * 2, mx, my);
			ry += h + ROW_GAP;
		}
		g.disableScissor();

		// footer: module-list switch on the left, theme gear on the right
		String hud = "Module list: " + (Config.arraylist ? "On" : "Off");
		int bw = Ui.width(hud) + 12, bx = left + PAD, by = footBtnY();
		boolean hh = Ui.in(mx, my, bx, by, bw, 14);
		Ui.round(g, bx, by, bw, 14, 3, hh ? MenuTheme.rowHover : MenuTheme.row);
		Ui.text(g, hud, bx + 6, by + 7, Config.arraylist ? MenuTheme.textOn : MenuTheme.text);

		int gx = gearX();
		boolean gh = Ui.in(mx, my, gx, by, 16, 14);
		Ui.round(g, gx, by, 16, 14, 3, pickerOpen ? MenuTheme.rowOn : gh ? MenuTheme.rowHover : MenuTheme.row);
		drawGear(g, gx + 8, by + 7, pickerOpen || gh ? MenuTheme.textOn : MenuTheme.text, pickerOpen ? MenuTheme.rowOn : gh ? MenuTheme.rowHover : MenuTheme.row);

		if (pickerOpen) drawPicker(g, mx, my);

		g.pose().popMatrix();
	}

	/** A tiny gear: four bars crossing at the centre make the teeth, a hub on top makes the body. */
	private void drawGear(GuiGraphics g, int cx, int cy, int color, int hubBg) {
		for (int i = 0; i < 4; i++) {
			g.pose().pushMatrix();
			g.pose().translate(cx, cy);
			g.pose().rotate((float) (i * Math.PI / 4));
			g.fill(-5, -1, 5, 1, color);
			g.pose().popMatrix();
		}
		Ui.round(g, cx - 3, cy - 3, 7, 7, 3, color);
		Ui.round(g, cx - 1, cy - 1, 3, 3, 1, hubBg);
	}

	private void drawModule(GuiGraphics g, Module m, int x, int y, int w, int mx, int my) {
		m.tickAnim();
		float a = m.anim;
		boolean hover = Ui.in(mx, my, x, y, w, ROW);

		Ui.round(g, x, y, w, ROW, 3, Ui.mix(hover ? MenuTheme.rowHover : MenuTheme.row, MenuTheme.rowOn, a));
		if (a > 0.05f) Ui.rect(g, x, y + 3, 2, ROW - 6, Ui.mix(MenuTheme.accent & 0xFFFFFF, MenuTheme.accent, a));

		boolean isBinding = binding == m;
		String right = isBinding ? "press a key" : (m.key() >= 0 ? m.keyName() : "");
		int rw = right.isEmpty() ? 0 : Ui.width(right) + 4;
		Ui.text(g, Ui.fit(m.name, w - 16 - rw), x + 8, y + ROW / 2, Ui.mix(MenuTheme.text, MenuTheme.textOn, a));
		if (!right.isEmpty()) Ui.textRight(g, right, x + w - 5, y + ROW / 2, isBinding ? MenuTheme.accent : MenuTheme.dim);

		if (!m.expanded) return;
		Ui.round(g, x, y + ROW, w, rowTotal(m) - ROW, 3, MenuTheme.inner);
		int sy = y + ROW + 2;
		for (Setting<?> s : m.settings) {
			if (!s.isVisible()) continue;
			drawSetting(g, s, x + 6, sy, w - 12);
			sy += s.height();
		}
		Ui.text(g, "Bind", x + 6, sy + BIND_H / 2, MenuTheme.text);
		Ui.textRight(g, isBinding ? "press a key" : m.keyName(), x + w - 6, sy + BIND_H / 2, isBinding ? MenuTheme.accent : MenuTheme.dim);
	}

	private void drawSetting(GuiGraphics g, Setting<?> s, int x, int y, int w) {
		if (s instanceof Setting.Bool b) {
			Ui.text(g, s.name, x, y + 7, MenuTheme.text);
			Ui.border(g, x + w - 9, y + 3, 9, 9, 2, MenuTheme.edge, b.get() ? MenuTheme.accent : MenuTheme.checkOff);
		} else if (s instanceof Setting.Num n) {
			Ui.text(g, s.name, x, y + 6, MenuTheme.text);
			Ui.textRight(g, n.display(), x + w, y + 6, MenuTheme.accent);
			int fw = (int) Math.round(n.fraction() * w);
			Ui.rect(g, x, y + 14, w, 2, MenuTheme.track);
			Ui.rect(g, x, y + 14, Math.max(1, fw), 2, MenuTheme.accent);
			Ui.rect(g, Mth.clamp(x + fw - 2, x, x + w - 4), y + 12, 4, 6, MenuTheme.textOn);
		} else if (s instanceof Setting.Mode md) {
			Ui.text(g, s.name, x, y + 7, MenuTheme.text);
			Ui.textRight(g, md.mode(), x + w, y + 7, MenuTheme.accent);
		}
	}

	// ---------------------------------------------------------------- theme picker

	private void drawPicker(GuiGraphics g, int mx, int my) {
		Ui.round(g, px - 1, py - 1, PW + 2, ph + 2, 5, MenuTheme.edge);
		Ui.round(g, px, py, PW, ph, 4, MenuTheme.panel);
		Ui.text(g, "Theme", px + 9, py + 11, MenuTheme.textOn);
		drawWheel(g);

		// the dot: where the picked colour sits on the wheel
		double a = MenuTheme.h * Math.PI * 2, rr = MenuTheme.s * WHEEL_R;
		int dx = (int) Math.round(wheelCx + Math.cos(a) * rr), dy = (int) Math.round(wheelCy + Math.sin(a) * rr);
		Ui.round(g, dx - 5, dy - 5, 11, 11, 5, 0xFF000000);
		Ui.round(g, dx - 4, dy - 4, 9, 9, 4, 0xFFFFFFFF);
		Ui.round(g, dx - 3, dy - 3, 7, 7, 3, 0xFF000000 | MenuTheme.hsv(MenuTheme.h, MenuTheme.s, 1f));

		drawBar(g, "Brightness", brightY, MenuTheme.v, MenuTheme.hsv(MenuTheme.h, MenuTheme.s, 0f), MenuTheme.hsv(MenuTheme.h, MenuTheme.s, 1f));
		drawBar(g, "Contrast", contrastY, MenuTheme.c, MenuTheme.hsv(MenuTheme.h, MenuTheme.s, MenuTheme.v), MenuTheme.textOn & 0xFFFFFF);

		int bw = 60, bx = px + (PW - bw) / 2;
		boolean hover = Ui.in(mx, my, bx, resetY, bw, 14);
		Ui.round(g, bx, resetY, bw, 14, 3, hover ? MenuTheme.rowOn : MenuTheme.row);
		Ui.textCentered(g, "Reset", bx + bw / 2, resetY + 7, hover ? MenuTheme.textOn : MenuTheme.text);
	}

	/**
	 * The colour wheel: 240 thin wedges, each a vertical gradient from white at the centre to the pure hue at the rim
	 * (which is exactly how saturation works), turned to point outward. Drawn in real pixels so it stays crisp.
	 */
	private void drawWheel(GuiGraphics g) {
		float s = (float) Minecraft.getInstance().getWindow().getGuiScale();
		int rr = Math.round(WHEEL_R * s), cx = Math.round(wheelCx * s), cy = Math.round(wheelCy * s);
		int n = 240, wd = (int) Math.ceil(2 * Math.PI * rr / n) + 1;
		g.pose().pushMatrix();
		g.pose().scale(1f / s, 1f / s);
		for (int i = 0; i < n; i++) {
			double a = i * Math.PI * 2 / n;
			g.pose().pushMatrix();
			g.pose().translate(cx, cy);
			g.pose().rotate((float) (a - Math.PI / 2));
			g.fillGradient(-wd / 2, 0, wd - wd / 2, rr, 0xFFFFFFFF, 0xFF000000 | MenuTheme.hsv(i / (float) n, 1f, 1f));
			g.pose().popMatrix();
		}
		g.pose().popMatrix();
	}

	private void drawBar(GuiGraphics g, String label, int y, float value, int leftRgb, int rightRgb) {
		Ui.text(g, label, barX, y + 3, MenuTheme.text);
		int by = y + 9;
		Ui.rect(g, barX - 1, by - 1, BAR_W + 2, 8, MenuTheme.edge);
		int segs = 25;
		for (int i = 0; i < segs; i++) {
			int x0 = barX + i * BAR_W / segs, x1 = barX + (i + 1) * BAR_W / segs;
			g.fill(x0, by, x1, by + 6, 0xFF000000 | MenuTheme.mixRgb(leftRgb, rightRgb, (i + 0.5) / segs));
		}
		int kx = barX + Math.round(value * BAR_W);
		Ui.rect(g, kx - 2, by - 3, 5, 12, 0xFF000000);
		Ui.rect(g, kx - 1, by - 2, 3, 10, 0xFFFFFFFF);
	}

	private void updateWheel(double mx, double my) {
		double dx = mx - wheelCx, dy = my - wheelCy;
		MenuTheme.s = (float) Math.min(1.0, Math.hypot(dx, dy) / WHEEL_R);
		double hue = Math.atan2(dy, dx) / (Math.PI * 2);
		MenuTheme.h = (float) (hue - Math.floor(hue));
		MenuTheme.recompute();
	}

	private void updateBar(double mx, int which) {
		float f = (float) Mth.clamp((mx - barX) / BAR_W, 0.0, 1.0);
		if (which == DRAG_BRIGHT) MenuTheme.v = f; else MenuTheme.c = f;
		MenuTheme.recompute();
	}

	// ---------------------------------------------------------------- input

	@Override
	public boolean mouseClicked(MouseButtonEvent ev, boolean doubleClick) {
		layout();
		Module prevBinding = binding;   // any click cancels binding unless it re-targets below
		binding = null;
		double mx = ev.x(), my = ev.y();
		int btn = ev.button();

		if (pickerOpen && Ui.in(mx, my, px, py, PW, ph)) {
			if (btn == 0) {
				if (Math.hypot(mx - wheelCx, my - wheelCy) <= WHEEL_R + 3) { pickDrag = DRAG_WHEEL; updateWheel(mx, my); }
				else if (Ui.in(mx, my, barX - 3, brightY + 4, BAR_W + 6, 16)) { pickDrag = DRAG_BRIGHT; updateBar(mx, DRAG_BRIGHT); }
				else if (Ui.in(mx, my, barX - 3, contrastY + 4, BAR_W + 6, 16)) { pickDrag = DRAG_CONTRAST; updateBar(mx, DRAG_CONTRAST); }
				else if (Ui.in(mx, my, px + (PW - 60) / 2, resetY, 60, 14)) MenuTheme.reset();
			}
			return true;
		}

		if (btn == 0 && Ui.in(mx, my, left, top, W, TITLE_H)) {   // title bar: drag the window
			dragging = true;
			dragDX = (int) mx - left;
			dragDY = (int) my - top;
			return true;
		}

		for (int i = 0; i < CATS.length; i++)
			if (btn == 0 && Ui.in(mx, my, tabX(i), tabsY, tabW(), TAB_H)) {
				tab = i;
				Config.menuTab = i;
				Config.save();
				return true;
			}

		String hud = "Module list: " + (Config.arraylist ? "On" : "Off");
		if (btn == 0 && Ui.in(mx, my, left + PAD, footBtnY(), Ui.width(hud) + 12, 14)) {
			Config.arraylist = !Config.arraylist;
			Config.save();
			return true;
		}
		if (btn == 0 && Ui.in(mx, my, gearX(), footBtnY(), 16, 14)) {
			pickerOpen = !pickerOpen;
			return true;
		}

		if (Ui.in(mx, my, left + 1, listY, W - 2, listH)) {
			int ry = listY + LIST_PAD - (int) scroll[tab];
			for (Module m : mods()) {
				int h = rowTotal(m);
				if (my >= ry && my < ry + h) { clickModule(m, left + PAD, W - PAD * 2, ry, mx, my, btn, prevBinding); return true; }
				ry += h + ROW_GAP;
			}
			return true;
		}
		return super.mouseClicked(ev, doubleClick);
	}

	private void clickModule(Module m, int x, int w, int ry, double mx, double my, int btn, Module prevBinding) {
		if (my < ry + ROW) {
			if (btn == 0) m.toggle();
			else if (btn == 1) m.expanded = !m.expanded;
			else if (btn == 2) binding = prevBinding == m ? null : m;
			return;
		}
		if (!m.expanded) return;

		int sx = x + 6, sw = w - 12;
		int sy = ry + ROW + 2;
		for (Setting<?> s : m.settings) {
			if (!s.isVisible()) continue;
			int sh = s.height();
			if (my >= sy && my < sy + sh) {
				if (s instanceof Setting.Bool b && btn == 0) b.toggle();
				else if (s instanceof Setting.Num n && btn == 0) { slider = n; sliderX = sx; sliderW = sw; setSlider(mx); }
				else if (s instanceof Setting.Mode md && (btn == 0 || btn == 1)) md.cycle(btn == 0 ? 1 : -1);
				return;
			}
			sy += sh;
		}
		if (my >= sy && my < sy + BIND_H && btn == 0) binding = prevBinding == m ? null : m;
	}

	private void setSlider(double mx) {
		double f = Mth.clamp((mx - sliderX) / sliderW, 0.0, 1.0);
		slider.set(slider.min + f * (slider.max - slider.min));
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent ev, double dx, double dy) {
		if (pickDrag == DRAG_WHEEL) { updateWheel(ev.x(), ev.y()); return true; }
		if (pickDrag != DRAG_NONE) { updateBar(ev.x(), pickDrag); return true; }
		if (dragging) {
			Config.menuX = Mth.clamp((int) ev.x() - dragDX, 0, Math.max(0, width - W));
			Config.menuY = Mth.clamp((int) ev.y() - dragDY, 0, Math.max(0, height - panelH));
			return true;
		}
		if (slider != null) { setSlider(ev.x()); return true; }
		return super.mouseDragged(ev, dx, dy);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent ev) {
		if (pickDrag != DRAG_NONE) MenuTheme.save();
		if (slider != null || dragging) Config.save();
		pickDrag = DRAG_NONE;
		slider = null;
		dragging = false;
		return super.mouseReleased(ev);
	}

	@Override
	public boolean mouseScrolled(double mx, double my, double scrollX, double scrollY) {
		layout();
		if (Ui.in(mx, my, left, listY, W, listH)) {
			scroll[tab] -= (float) (scrollY * 16);
			clampScroll();
			return true;
		}
		return super.mouseScrolled(mx, my, scrollX, scrollY);
	}

	@Override
	public boolean keyPressed(KeyEvent ev) {
		int k = ev.key();
		if (binding != null) {
			if (k == GLFW.GLFW_KEY_ESCAPE || k == GLFW.GLFW_KEY_RIGHT_SHIFT) binding = null;          // cancel
			else {
				binding.setKey(k == GLFW.GLFW_KEY_BACKSPACE || k == GLFW.GLFW_KEY_DELETE ? -1 : k);   // bind / clear
				binding = null;
				Config.save();
			}
			return true;
		}
		// RShift opens the menu, so ignore its key-down repeat until it was released once (or 0.6 s passed)
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
}
