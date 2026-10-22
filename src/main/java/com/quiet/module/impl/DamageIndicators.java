package com.quiet.module.impl;

import com.quiet.gui.Fonts;
import com.quiet.module.Category;
import com.quiet.module.Module;
import com.quiet.module.setting.NumberSetting;
import com.quiet.render.Overlay;
import com.quiet.render.RenderCapture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/** Floating damage numbers whenever a player's health (plus absorption) drops. */
public class DamageIndicators extends Module implements Overlay.Layer {
	private record Pop(Vec3 pos, String text, long born) {}

	private final NumberSetting range = num("Range", 48, 8, 128, 4);
	private final Map<Integer, Float> last = new HashMap<>();
	private final List<Pop> pops = new ArrayList<>();

	public DamageIndicators() { super("Damage Indicators", Category.RENDER); Overlay.add(this, this); }

	@Override protected void onDisable() { last.clear(); pops.clear(); }

	@Override
	protected void onTick() {
		Minecraft mc = mc();
		Set<Integer> ids = new HashSet<>();
		ThreadLocalRandom r = ThreadLocalRandom.current();
		for (Player p : mc.level.players()) {
			ids.add(p.getId());
			float hp = p.getHealth() + p.getAbsorptionAmount();
			Float prev = last.put(p.getId(), hp);
			if (prev != null && hp < prev - 0.04f && p.isAlive() && mc.player.distanceTo(p) <= range.get()) {
				float d = prev - hp;
				String t = Math.abs(d - Math.round(d)) < 0.05f ? String.valueOf(Math.round(d)) : String.format(Locale.ROOT, "%.1f", d);
				pops.add(new Pop(new Vec3(p.getX() + r.nextDouble(-0.4, 0.4), p.getY() + p.getBbHeight() * 0.9, p.getZ() + r.nextDouble(-0.4, 0.4)),
						"-" + t, System.currentTimeMillis()));
			}
		}
		last.keySet().retainAll(ids);
	}

	@Override
	public void render(GuiGraphics g, Minecraft mc, float pt, int w, int h) {
		long now = System.currentTimeMillis();
		pops.removeIf(p -> now - p.born() > 1200);
		for (Pop p : pops) {
			float age = (now - p.born()) / 1200f;
			float[] s = RenderCapture.project(p.pos().x, p.pos().y + age * 0.8, p.pos().z, w, h);
			if (s == null) continue;
			int a = Math.max(0, Math.min(255, (int) ((1f - age * age) * 255)));
			Fonts.drawCentered(g, p.text(), Math.round(s[0]), Math.round(s[1]), (a << 24) | 0xFF5555, true);
		}
	}
}
