package com.quiet.module.impl;

import com.quiet.module.Category;
import com.quiet.module.Module;
import com.quiet.module.setting.BooleanSetting;
import com.quiet.module.setting.ModeSetting;
import com.quiet.module.setting.NumberSetting;
import com.quiet.render.Lines;
import com.quiet.render.Overlay;
import com.quiet.render.Palette;
import com.quiet.render.RenderCapture;
import com.quiet.render.WorldScan;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public class Tracers extends Module implements Overlay.Layer {
	private final ModeSetting anchor = mode("Anchor", "Crosshair", "Crosshair", "Top", "Bottom");
	private final ModeSetting color = mode("Color", "Cyan", Palette.NAMES);
	private final BooleanSetting players = bool("Players", true);
	private final BooleanSetting chests = bool("Containers", false);
	private final NumberSetting range = num("Range", 96, 16, 256, 8);

	public Tracers() { super("Tracers", Category.RENDER); Overlay.add(this, this); }

	@Override
	public void render(GuiGraphics g, Minecraft mc, float pt, int w, int h) {
		LocalPlayer me = mc.player;
		float ax = w / 2f, ay = anchor.is("Top") ? 0 : anchor.is("Bottom") ? h : h / 2f;
		int c = Palette.color(color.mode());

		if (players.get()) {
			for (Player p : mc.level.players()) {
				if (p == me || !p.isAlive() || p.isSpectator() || me.distanceTo(p) > range.get()) continue;
				Vec3 mid = Lines.interp(p, pt).getCenter();
				float[] s = RenderCapture.projectClamped(mid.x, mid.y, mid.z, w, h);
				if (s != null) Lines.line(g, ax, ay, s[0], s[1], c, 1);
			}
		}
		if (chests.get()) {
			for (WorldScan.Box b : WorldScan.storage(mc, range.asInt(),
					WorldScan.CHEST | WorldScan.ENDER | WorldScan.SHULKER | WorldScan.BARREL)) {
				Vec3 mid = b.box().getCenter();
				float[] s = RenderCapture.projectClamped(mid.x, mid.y, mid.z, w, h);
				if (s != null) Lines.line(g, ax, ay, s[0], s[1], b.color(), 1);
			}
		}
	}
}
