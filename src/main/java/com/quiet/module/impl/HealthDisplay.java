package com.quiet.module.impl;

import com.quiet.gui.Draw;
import com.quiet.gui.Fonts;
import com.quiet.module.Category;
import com.quiet.module.Module;
import com.quiet.module.setting.BooleanSetting;
import com.quiet.module.setting.ModeSetting;
import com.quiet.module.setting.NumberSetting;
import com.quiet.render.Lines;
import com.quiet.render.Overlay;
import com.quiet.render.RenderCapture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

import java.util.Locale;

/** Exact health (with decimals) above every nearby player. */
public class HealthDisplay extends Module implements Overlay.Layer {
	private final ModeSetting mode = mode("Mode", "Text + Bar", "Text", "Bar", "Text + Bar");
	private final BooleanSetting max = bool("Show max health", false);
	private final BooleanSetting self = bool("Self", false);
	private final NumberSetting range = num("Range", 64, 8, 128, 4);

	public HealthDisplay() { super("Health Display", Category.RENDER); Overlay.add(this, this); }

	static String fmt(float hp) {
		return Math.abs(hp - Math.round(hp)) < 0.05f ? String.valueOf(Math.round(hp)) : String.format(Locale.ROOT, "%.1f", hp);
	}

	@Override
	public void render(GuiGraphics g, Minecraft mc, float pt, int w, int h) {
		LocalPlayer me = mc.player;
		boolean third = !mc.options.getCameraType().isFirstPerson();
		for (Player p : mc.level.players()) {
			if (p == me && !(self.get() && third)) continue;
			if (!p.isAlive() || p.isSpectator() || me.distanceTo(p) > range.get()) continue;
			float[] r = RenderCapture.projectBox(Lines.interp(p, pt), w, h);
			if (r == null) continue;

			float hp = p.getHealth(), mx = Math.max(1f, p.getMaxHealth());
			float f = Mth.clamp(hp / mx, 0f, 1f);
			int col = Draw.lerp(0xFFE5484D, 0xFF46C37B, f);
			int cx = Math.round((r[0] + r[2]) / 2f);
			int ty = Math.round(r[1]) - 6;

			if (mode.mode().contains("Bar")) {
				int bw = 26, bx = cx - bw / 2, by = ty + 5;
				g.fill(bx - 1, by - 1, bx + bw + 1, by + 3, 0xA0000000);
				g.fill(bx, by, bx + Math.max(1, Math.round(bw * f)), by + 2, col);
			}
			if (mode.mode().contains("Text")) {
				String s = fmt(hp) + (max.get() ? "/" + fmt(mx) : "");
				if (p.getAbsorptionAmount() > 0) s += " +" + fmt(p.getAbsorptionAmount());
				Fonts.drawCentered(g, s, cx, ty - 2, col, false);
			}
		}
	}
}
