package com.quiet.hud;

import com.quiet.gui.Theme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

public abstract class HudWidget {
	public final String id, name;
	public final String icon;
	public boolean enabled = true;
	public float fx, fy;     // position as a fraction of the screen
	public int w = 80, h = 16;
	protected boolean visible = true;

	protected HudWidget(String id, String name, String icon, float fx, float fy) {
		this.id = id; this.name = name; this.icon = icon; this.fx = fx; this.fy = fy;
	}

	/** Gather data and set w/h. preview = true shows sample data (HUD editor). */
	public abstract void update(boolean preview);

	public abstract void draw(GuiGraphics g, Theme t, int x, int y);

	public boolean isVisible() { return visible; }

	public int x(int sw) { return Mth.clamp(Math.round(fx * sw), 0, Math.max(0, sw - w)); }
	public int y(int sh) { return Mth.clamp(Math.round(fy * sh), 0, Math.max(0, sh - h)); }

	public void moveTo(int x, int y, int sw, int sh) {
		fx = x / (float) sw;
		fy = y / (float) sh;
	}
}