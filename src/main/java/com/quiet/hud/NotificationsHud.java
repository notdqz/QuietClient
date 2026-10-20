package com.quiet.hud;

import com.quiet.gui.Draw;
import com.quiet.gui.Fonts;
import com.quiet.gui.Icons;
import com.quiet.gui.Theme;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.List;

public class NotificationsHud extends HudWidget {
	private record Toast(String text, boolean on, long time) {}

	private static final List<Toast> TOASTS = new ArrayList<>();
	private static final long LIFE = 2600;

	private List<Toast> shown = List.of();
	private boolean previewing;

	public NotificationsHud() { super("notifications", "Notifications", Icons.BELL, 0.86f, 0.90f); }

	public static void push(String module, boolean on) {
		TOASTS.add(new Toast(module + " was " + (on ? "enabled" : "disabled"), on, System.currentTimeMillis()));
	}

	@Override
	public void update(boolean preview) {
		long now = System.currentTimeMillis();
		TOASTS.removeIf(t -> now - t.time() > LIFE);
		previewing = preview && TOASTS.isEmpty();
		if (previewing) shown = List.of(new Toast("ESP was disabled", false, 0), new Toast("KillAura was enabled", true, 0));
		else shown = new ArrayList<>(TOASTS.subList(Math.max(0, TOASTS.size() - 3), TOASTS.size()));
		visible = !shown.isEmpty();
		w = 120; h = 3 * 19;
	}

	@Override
	public void draw(GuiGraphics g, Theme t, int x, int y) {
		long now = System.currentTimeMillis();
		for (int k = 0; k < shown.size(); k++) {
			Toast ts = shown.get(shown.size() - 1 - k);        // newest sits at the bottom
			int ty = y + h - 16 - k * 19;
			long age = previewing ? 600 : now - ts.time();

			float off = 0;
			if (age < 220) { float p = age / 220f; off = (1 - p) * (1 - p) * w; }
			else if (age > LIFE - 250) { float q = (age - (LIFE - 250)) / 250f; off = q * q * w; }

			int bw = 16 + 6 + Fonts.width(ts.text()) + 9;
			int bx = x + w - bw + Math.round(off);
			int cell = ts.on() ? t.toastOn : t.toastOff;

			HudDraw.card(g, t, bx, ty, bw, 16, 8);
			Draw.roundLeft(g, bx, ty, 16, 16, 8, cell);
			Icons.draw(g, ts.on() ? Icons.CHECK_CIRCLE : Icons.HEX_X, bx + 3, ty + 4, 0xFFFFFFFF);
			Fonts.draw(g, ts.text(), bx + 22, ty + 8, t.panelText);
			HudDraw.cardTop(g, t, bx, ty, bw, 16, 8);
		}
	}
}