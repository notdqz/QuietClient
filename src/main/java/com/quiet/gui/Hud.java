package com.quiet.gui;

import com.quiet.Config;
import com.quiet.module.Module;
import com.quiet.module.ModuleManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/** The only HUD: an optional module list in the top-right corner. Off by default (it's a ghost client). */
public final class Hud {
	public static void render(GuiGraphics g) {
		if (!Config.arraylist) return;
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null) return;

		List<String> names = new ArrayList<>();
		for (Module m : ModuleManager.all()) if (m.isEnabled()) names.add(m.name);
		names.sort((a, b) -> Ui.width(b) - Ui.width(a));

		int y = 3;
		for (String n : names) {
			g.drawString(mc.font, Component.literal(n), g.guiWidth() - Ui.width(n) - 3, y, 0xFFFFFFFF, true);
			y += 10;
		}
	}
}
