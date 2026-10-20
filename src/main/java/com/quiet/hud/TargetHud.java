package com.quiet.hud;

import com.quiet.gui.Draw;
import com.quiet.gui.Fonts;
import com.quiet.gui.Icons;
import com.quiet.gui.Theme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

public class TargetHud extends HudWidget {
	private LivingEntity target, shown, lastShown;
	private long seen;
	private float animHealth;

	public TargetHud() { super("targethud", "TargetHUD", Icons.TARGET, 0.47f, 0.52f); }

	@Override
	public void update(boolean preview) {
		Minecraft mc = Minecraft.getInstance();
		long now = System.currentTimeMillis();
		if (mc.crosshairPickEntity instanceof LivingEntity le) { target = le; seen = now; }
		else if (target != null && (now - seen > 2000 || target.isRemoved())) target = null;

		shown = target;
		if (shown == null && preview) shown = mc.player;
		visible = shown != null;
		w = 78; h = 22;
		if (!visible) return;

		if (shown != lastShown) { animHealth = shown.getHealth(); lastShown = shown; }
		animHealth += (shown.getHealth() - animHealth) * 0.15f;
	}

	@Override
	public void draw(GuiGraphics g, Theme t, int x, int y) {
		HudDraw.card(g, t, x, y, w, h, 8);
		Draw.roundLeft(g, x, y, 22, h, 8, t.panelRowA);

		if (shown instanceof AbstractClientPlayer p) {
			PlayerFaceRenderer.draw(g, p.getSkin(), x + 3, y + 3, 16);
		} else {
			Icons.draw(g, Icons.PLAYER, x + 7, y + 7, t.panelSub);
		}
		Draw.rect(g, x + 22, y + 2, 0.5f, h - 4, t.panelBorder);

		String hp = String.valueOf(Math.round(shown.getHealth()));
		Fonts.drawRight(g, hp, x + w - 6, y + 8, t.panelSub);
		Fonts.draw(g, Fonts.fit(shown.getName().getString(), w - 28 - 6 - Fonts.width(hp) - 4), x + 28, y + 8, t.panelText);

		int bw = w - 34;
		float f = Mth.clamp(animHealth / Math.max(1f, shown.getMaxHealth()), 0f, 1f);
		Draw.round(g, x + 28, y + 14, bw, 3, 1, t.sliderTrack);
		Draw.round(g, x + 28, y + 14, Math.max(2, bw * f), 3, 1, t.panelText);
		HudDraw.cardTop(g, t, x, y, w, h, 8);
	}
}