package com.quiet.render;

import com.quiet.Quiet;
import com.quiet.module.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.List;

/** Draws every enabled ESP-style module on top of the world. Call Overlay.render(graphics) from the HUD hook. */
public final class Overlay {
	public interface Layer { void render(GuiGraphics g, Minecraft mc, float pt, int w, int h); }

	private record Entry(Module m, Layer l) {}

	private static final List<Entry> LAYERS = new ArrayList<>();

	public static void add(Module m, Layer l) { LAYERS.add(new Entry(m, l)); }

	public static void render(GuiGraphics g) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.level == null) return;
		float pt = mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);
		int w = g.guiWidth(), h = g.guiHeight();
		for (Entry e : LAYERS) {
			if (!e.m().isEnabled()) continue;
			try {
				e.l().render(g, mc, pt, w, h);
			} catch (Throwable t) {
				Quiet.LOGGER.error("Overlay for {} crashed and was disabled", e.m().name(), t);
				e.m().setEnabled(false);
			}
		}
	}
}
