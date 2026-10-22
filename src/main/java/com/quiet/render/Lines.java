package com.quiet.render;

import com.quiet.gui.Draw;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;

public final class Lines {
	/** Line between two GUI-space points, drawn in real pixels so it stays crisp at any GUI scale. */
	public static void line(GuiGraphics g, float x1, float y1, float x2, float y2, int color, int thickness) {
		float dx = x2 - x1, dy = y2 - y1;
		float len = (float) Math.hypot(dx, dy);
		if (len < 0.5f) return;
		float s = (float) Minecraft.getInstance().getWindow().getGuiScale();
		g.pose().pushMatrix();
		g.pose().scale(1f / s, 1f / s);
		g.pose().translate(x1 * s, y1 * s);
		g.pose().rotate((float) Math.atan2(dy, dx));
		g.fill(0, 0, Math.max(1, Math.round(len * s)), Math.max(1, thickness), Draw.a(color));
		g.pose().popMatrix();
	}

	/** Entity bounding box at its smoothed (per-frame) position. */
	public static AABB interp(Entity e, float pt) {
		double ix = Mth.lerp((double) pt, e.xo, e.getX());
		double iy = Mth.lerp((double) pt, e.yo, e.getY());
		double iz = Mth.lerp((double) pt, e.zo, e.getZ());
		return e.getBoundingBox().move(ix - e.getX(), iy - e.getY(), iz - e.getZ());
	}

	/** The 12 edges of a box, given its 8 projected corners (null corners are skipped). */
	public static void edges(GuiGraphics g, float[][] c, int color) {
		for (int i = 0; i < 8; i++) {
			for (int j = i + 1; j < 8; j++) {
				if (Integer.bitCount(i ^ j) != 1 || c[i] == null || c[j] == null) continue;
				line(g, c[i][0], c[i][1], c[j][0], c[j][1], color, 1);
			}
		}
	}
}
