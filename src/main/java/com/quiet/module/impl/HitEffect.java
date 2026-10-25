package com.quiet.module.impl;

import com.quiet.module.Category;
import com.quiet.module.Module;
import com.quiet.module.Setting;
import com.quiet.render.Palette;
import com.quiet.render.WorldBatch;
import com.quiet.render.WorldRender;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Impact effect drawn in the world where your crosshair ray meets the target: a white flash, a ring that bursts out
 * facing the direction of the hit, a smaller trailing ring, and streaks that shoot outward. Everything fades from white
 * into your colour. Hooked from MultiPlayerGameModeMixin (clicks and modules both count).
 */
public class HitEffect extends Module implements WorldRender.Layer {
	private record Impact(Vec3 pos, Vec3 dir, long born, long seed) {}

	private static final long LIFE_MS = 520;
	private static final int RING_SEGS = 28, STREAKS = 10, MAX_ACTIVE = 12;
	private static HitEffect inst;

	private final Setting.Mode color = mode("Color", "Cyan", Palette.NAMES);
	private final Setting.Num size = num("Size", 1.0, 0.5, 2.0, 0.1);
	private final List<Impact> impacts = new ArrayList<>();

	public HitEffect() { super("Hit Effect", Category.VISUAL); inst = this; WorldRender.add(this, this); }

	@Override protected void onDisable() { impacts.clear(); }

	public static void onAttack(Player attacker, Entity target) {
		if (inst == null || !inst.isEnabled() || !(target instanceof LivingEntity)) return;
		inst.add(attacker, target);
	}

	private void add(Player attacker, Entity target) {
		Vec3 eye = attacker.getEyePosition();
		Vec3 look = attacker.getViewVector(1f);
		Vec3 hit = target.getBoundingBox().clip(eye, eye.add(look.scale(8))).orElse(null);
		if (hit == null) hit = new Vec3(target.getX(), target.getY() + target.getBbHeight() * 0.6, target.getZ());
		Vec3 dir = hit.subtract(eye);
		dir = dir.lengthSqr() < 1.0E-4 ? look : dir.normalize();
		if (impacts.size() >= MAX_ACTIVE) impacts.remove(0);
		impacts.add(new Impact(hit, dir, System.currentTimeMillis(), ThreadLocalRandom.current().nextLong()));
	}

	@Override
	public void extract(WorldBatch b, Minecraft mc, float pt) {
		long now = System.currentTimeMillis();
		impacts.removeIf(i -> now - i.born() > LIFE_MS);
		int accent = Palette.color(color.mode());
		for (Impact im : impacts) draw(b, im, (now - im.born()) / (float) LIFE_MS, accent, size.asFloat());
	}

	private static int mix(int a, int b, float t) {
		t = Mth.clamp(t, 0f, 1f);
		int r = (int) (((a >> 16) & 255) + (((b >> 16) & 255) - ((a >> 16) & 255)) * t);
		int g = (int) (((a >> 8) & 255) + (((b >> 8) & 255) - ((a >> 8) & 255)) * t);
		int bl = (int) ((a & 255) + ((b & 255) - (a & 255)) * t);
		return (r << 16) | (g << 8) | bl;
	}

	private static void draw(WorldBatch b, Impact im, float t, int accent, float s) {
		Vec3 n = im.dir();
		Vec3 ref = Math.abs(n.y) > 0.95 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
		Vec3 u = n.cross(ref).normalize(), v = n.cross(u);          // the ring's plane faces the hit direction

		float ease = 1f - (1f - t) * (1f - t) * (1f - t);           // fast burst, slow settle
		float fade = (float) Math.pow(1f - t, 1.4);
		int tint = mix(0xFFFFFF, accent, t * 2.5f);                 // starts white, settles into the colour

		if (t < 0.16f) {
			float f = 1f - t / 0.16f;
			b.fillBox(new AABB(im.pos(), im.pos()).inflate(0.10 * s * (1 + t * 6)), Palette.alpha(0xFFFFFF, (int) (150 * f)));
		}

		double r1 = (0.12 + 0.75 * ease) * s;
		ring(b, im.pos(), u, v, r1, Palette.alpha(tint, (int) (235 * fade)), 0.6f + 1.8f * (1f - t));

		float t2 = Mth.clamp((t - 0.12f) / 0.88f, 0f, 1f);          // second ring trails the first
		if (t2 > 0f) {
			float e2 = 1f - (1f - t2) * (1f - t2) * (1f - t2);
			ring(b, im.pos(), u, v, (0.08 + 0.5 * e2) * s, Palette.alpha(tint, (int) (150 * (1f - t2))), 0.5f + 1.0f * (1f - t2));
		}

		long seed = im.seed();
		for (int i = 0; i < STREAKS; i++) {
			double jitter = (((seed >>> (i * 5)) & 31) / 31.0 - 0.5) * 0.5;
			double len = 0.6 + 0.6 * (((seed >>> (i * 5 + 2)) & 7) / 7.0);
			double a = i * Math.PI * 2 / STREAKS + jitter;
			Vec3 out = u.scale(Math.cos(a)).add(v.scale(Math.sin(a)));
			double inner = r1 * 0.7, outer = inner + (0.22 + 0.34 * len) * s * (1f - t * 0.6f);
			b.line(im.pos().add(out.scale(inner)), im.pos().add(out.scale(outer)),
					Palette.alpha(tint, (int) (220 * fade)), 0.5f + 1.2f * (1f - t));
		}
	}

	private static void ring(WorldBatch b, Vec3 c, Vec3 u, Vec3 v, double r, int argb, float px) {
		Vec3 prev = null;
		for (int k = 0; k <= RING_SEGS; k++) {
			double a = k * Math.PI * 2 / RING_SEGS;
			Vec3 p = c.add(u.scale(Math.cos(a) * r)).add(v.scale(Math.sin(a) * r));
			if (prev != null) b.line(prev, p, argb, px);
			prev = p;
		}
	}
}
