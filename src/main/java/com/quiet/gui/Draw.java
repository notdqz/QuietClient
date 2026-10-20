package com.quiet.gui;

import com.quiet.Quiet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/** Sprite-based smooth shapes: one cheap draw call per shape. */
public final class Draw {
	private static final int[] RADII = {1, 2, 3, 4, 6, 8, 10, 12, 13};
	private static final String[] FAM = {"fill", "ring", "gloss"};
	private static final Identifier[][][] IDS = new Identifier[3][RADII.length][5];
	private static final Identifier[] SHADOW = new Identifier[5], SHADOW_SM = new Identifier[5];

	/** Global alpha multiplier (open animation). */
	public static float alpha = 1f;

	private static float guiScale() { return (float) Minecraft.getInstance().getWindow().getGuiScale(); }
	private static int sIdx() { return Mth.clamp(Math.round(guiScale()), 2, 4); }

	public static int a(int c) {
		if (alpha >= 1f) return c;
		return (((int) ((c >>> 24) * alpha)) << 24) | (c & 0xFFFFFF);
	}

	private static Identifier id(int fam, float r, float maxR) {
		int best = 0;
		float bd = Float.MAX_VALUE;
		for (int i = 0; i < RADII.length; i++) {
			if (i > 0 && RADII[i] > maxR + 0.01f) break;
			float d = Math.abs(RADII[i] - r);
			if (d < bd) { bd = d; best = i; }
		}
		int s = sIdx();
		Identifier id = IDS[fam][best][s];
		if (id == null) id = IDS[fam][best][s] = Quiet.id(FAM[fam] + "_r" + RADII[best] + "_s" + s);
		return id;
	}

	private static void blit(GuiGraphics g, Identifier id, float x, float y, int w, int h, int color) {
		if (w <= 0 || h <= 0) return;
		int ix = (int) Math.floor(x), iy = (int) Math.floor(y);
		float dx = x - ix, dy = y - iy;
		boolean shift = dx != 0 || dy != 0;
		if (shift) { g.pose().pushMatrix(); g.pose().translate(dx, dy); }
		g.blitSprite(RenderPipelines.GUI_TEXTURED, id, ix, iy, w, h, a(color));
		if (shift) g.pose().popMatrix();
	}

	private static void shape(GuiGraphics g, int fam, float x, float y, float w, float h, float r, int c) {
		blit(g, id(fam, r, Math.min(w, h) / 2f), x, y, Math.round(w), Math.round(h), c);
	}

	// ------------------------------------------------------------ public API

	public static void round(GuiGraphics g, float x, float y, float w, float h, float r, int c) { shape(g, 0, x, y, w, h, r, c); }

	public static void ring(GuiGraphics g, float x, float y, float w, float h, float r, int c) { shape(g, 1, x, y, w, h, r, c); }

	/** Soft light at the top edge, soft shade at the bottom edge. */
	public static void gloss(GuiGraphics g, float x, float y, float w, float h, float r) {
		shape(g, 2, x, y, w, h, r, (Theme.current().glossA << 24) | 0xFFFFFF);
	}

	/** Fill, then a hairline border on top (border colour may be translucent). */
	public static void roundBorder(GuiGraphics g, float x, float y, float w, float h, float r, int border, int fill) {
		round(g, x, y, w, h, r, fill);
		ring(g, x, y, w, h, r, border);
	}

	public static void circle(GuiGraphics g, float cx, float cy, float r, int c) {
		round(g, cx - r, cy - r, r * 2, r * 2, r, c);
	}

	public static void roundBottom(GuiGraphics g, int x, int y, int w, int h, float r, int c) {
		g.enableScissor(x, y, x + w, y + h);
		round(g, x, y - r - 1, w, h + r + 1, r, c);
		g.disableScissor();
	}

	public static void roundLeft(GuiGraphics g, int x, int y, int w, int h, float r, int c) {
		g.enableScissor(x, y, x + w, y + h);
		round(g, x, y, w + r + 1, h, r, c);
		g.disableScissor();
	}

	public static void shadow(GuiGraphics g, float x, float y, float w, float h, float dy, boolean big) {
		shadow(g, x, y, w, h, dy, big, 1f);
	}

	public static void shadow(GuiGraphics g, float x, float y, float w, float h, float dy, boolean big, float strength) {
		int pad = big ? 12 : 6, s = sIdx();
		Identifier[] arr = big ? SHADOW : SHADOW_SM;
		if (arr[s] == null) arr[s] = Quiet.id((big ? "shadow" : "shadowsm") + "_s" + s);
		int al = (int) (Theme.current().shadowA * strength);
		blit(g, arr[s], x - pad, y - pad + dy, Math.round(w + 2 * pad), Math.round(h + 2 * pad), al << 24);
	}

	/** Hairline-friendly rectangle (never thinner than one real pixel). */
	public static void rect(GuiGraphics g, float x, float y, float w, float h, int c) {
		float s = guiScale();
		int x1 = Math.round(x * s), y1 = Math.round(y * s);
		int x2 = Math.max(x1 + 1, Math.round((x + w) * s)), y2 = Math.max(y1 + 1, Math.round((y + h) * s));
		g.pose().pushMatrix();
		g.pose().scale(1f / s, 1f / s);
		g.fill(x1, y1, x2, y2, a(c));
		g.pose().popMatrix();
	}

	public static void outline(GuiGraphics g, float x, float y, float w, float h, int c) {
		rect(g, x, y, w, 0.5f, c);
		rect(g, x, y + h - 0.5f, w, 0.5f, c);
		rect(g, x, y, 0.5f, h, c);
		rect(g, x + w - 0.5f, y, 0.5f, h, c);
	}

	public static int lerp(int a, int b, float t) {
		t = Math.max(0f, Math.min(1f, t));
		int al = (int) (((a >>> 24) & 255) + ((((b >>> 24) & 255) - ((a >>> 24) & 255)) * t));
		int r = (int) (((a >> 16) & 255) + ((((b >> 16) & 255) - ((a >> 16) & 255)) * t));
		int gr = (int) (((a >> 8) & 255) + ((((b >> 8) & 255) - ((a >> 8) & 255)) * t));
		int bl = (int) ((a & 255) + (((b & 255) - (a & 255)) * t));
		return (al << 24) | (r << 16) | (gr << 8) | bl;
	}
}