package com.quiet.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

import java.util.Random;

/**
 * Animated panel surface. Four ribbons of light drift across the dark panel, each with a soft glow behind it, a
 * bright core, and a fainter twin line that twists around it. A pulse of light travels along every ribbon, the ribbons
 * have faint cool tints (white, ice blue, pale violet), a soft light falls from the top edge, and a few specks of dust
 * drift upward and twinkle. Plain procedural drawing, no shader or texture. Time is continuous, so nothing loops.
 */
public final class FlowSurface {
	private record Ribbon(float y, float amp, float f1, float s1, float p1, float f2, float s2, float p2, int rgb) {}

	private static final Ribbon[] RIBBONS = {
		new Ribbon(0.22f, 0.13f, 5.0f,  0.40f, 0.0f, 11.0f, -0.30f, 1.7f, 0xFFFFFF),
		new Ribbon(0.45f, 0.17f, 3.6f, -0.34f, 2.1f,  8.3f,  0.26f, 4.0f, 0xBFD0FF),
		new Ribbon(0.68f, 0.15f, 6.2f,  0.30f, 4.4f, 13.0f, -0.22f, 0.6f, 0xFFFFFF),
		new Ribbon(0.88f, 0.10f, 4.4f, -0.27f, 1.2f,  9.5f,  0.19f, 3.3f, 0xD3C2FF),
	};
	private static final int SEG = 10;   // segment length in real pixels
	private static final long START = System.nanoTime();

	/** Dust specks: {x0, y0, x speed, y speed, twinkle phase, size}, all as fractions of the panel. */
	private static final float[][] DUST = buildDust();

	private static float[][] buildDust() {
		Random r = new Random(7);
		float[][] d = new float[26][];
		for (int i = 0; i < d.length; i++)
			d[i] = new float[]{r.nextFloat(), r.nextFloat(), 0.004f + r.nextFloat() * 0.010f, 0.006f + r.nextFloat() * 0.014f,
					r.nextFloat() * 6.28f, r.nextFloat() < 0.3f ? 2 : 1};
		return d;
	}

	private static double smooth(double v) {
		v = Math.max(0, Math.min(1, v));
		return v * v * (3 - 2 * v);
	}

	private static double yAt(Ribbon r, double nx, double time, double off) {
		double wave = Math.sin(nx * r.f1() + time * r.s1() + r.p1()) * r.amp()
				+ Math.sin(nx * r.f2() + time * r.s2() + r.p2()) * r.amp() * 0.35;
		double spread = (0.012 + 0.022 * (0.5 + 0.5 * Math.sin(nx * 3.1 + time * 0.2 + r.p1()))) * off;
		return r.y() + wave + spread;
	}

	/** Draws inside the panel rectangle (GUI units). */
	public static void draw(GuiGraphics g, int x, int y, int w, int h) {
		float s = (float) Minecraft.getInstance().getWindow().getGuiScale();
		int inset = Math.round(5 * s);
		int left = Math.round(x * s) + inset, right = Math.round((x + w) * s) - inset;
		int top = Math.round(y * s) + inset, bottom = Math.round((y + h) * s) - inset;
		int span = right - left, height = bottom - top;
		if (span < SEG * 4 || height < 20) return;

		double time = (System.nanoTime() - START) / 1.0e9;
		int core = Math.max(1, Math.round(s)), side = Math.max(1, Math.round(s * 0.5f)), glow = core * 4;

		g.pose().pushMatrix();
		g.pose().scale(1f / s, 1f / s);

		g.fillGradient(left, top, right, top + height / 3, 0x16FFFFFF, 0x00FFFFFF);   // soft light from the top edge

		for (Ribbon r : RIBBONS) {
			for (int pass = 0; pass < 3; pass++) {            // 0 = glow, 1 = core, 2 = twin
				int thick = pass == 0 ? glow : pass == 1 ? core : side;
				int baseA = pass == 0 ? 0x0E : pass == 1 ? 0x38 : 0x1A;
				double off = pass == 2 ? 1 : 0;
				float px0 = left;
				float py0 = (float) (top + yAt(r, 0, time, off) * height);
				for (int px = left + SEG; px <= right; px += SEG) {
					double nx = (px - left) / (double) span;
					float py1 = (float) (top + yAt(r, nx, time, off) * height);

					double mid = nx - SEG * 0.5 / span;
					double ny = (py1 - top) / (double) height;
					double fade = smooth(mid / 0.2) * smooth((1 - mid) / 0.2) * smooth(ny / 0.1) * smooth((1 - ny) / 0.1);
					// a pulse of light travelling along the ribbon; the glow stays calmer than the core
					double pulse = Math.pow(0.5 + 0.5 * Math.sin(mid * 5.0 - time * 0.9 + r.p2()), 2.5);
					double bright = pass == 0 ? 0.55 + 0.45 * pulse : 0.28 + 0.72 * pulse;
					int a = (int) (baseA * fade * bright);
					if (a >= 2 && py1 > top && py1 < bottom) {
						float dx = px - px0, dy = py1 - py0;
						float len = (float) Math.hypot(dx, dy) + 1f;
						g.pose().pushMatrix();
						g.pose().translate(px0, py0);
						g.pose().rotate((float) Math.atan2(dy, dx));
						g.fill(0, -(thick / 2), Math.round(len), thick - thick / 2, (Mth.clamp(a, 0, 255) << 24) | r.rgb());
						g.pose().popMatrix();
					}
					px0 = px;
					py0 = py1;
				}
			}
		}

		// dust drifting up and twinkling
		int dustSize = Math.max(1, Math.round(s * 0.6f));
		for (float[] d : DUST) {
			double nx = (d[0] + d[2] * time) % 1.0;
			double ny = 1.0 - ((d[1] + d[3] * time) % 1.0);       // moves upward
			double edge = smooth(nx / 0.12) * smooth((1 - nx) / 0.12) * smooth(ny / 0.1) * smooth((1 - ny) / 0.1);
			double twinkle = 0.5 + 0.5 * Math.sin(time * 1.4 + d[4]);
			int a = (int) (0x58 * edge * twinkle);
			if (a < 3) continue;
			int sz = (int) d[5] * dustSize;
			int px = left + (int) (nx * span), py = top + (int) (ny * height);
			g.fill(px, py, px + sz, py + sz, (a << 24) | 0xFFFFFF);
		}
		g.pose().popMatrix();
	}
}
