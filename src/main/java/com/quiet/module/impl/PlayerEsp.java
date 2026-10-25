package com.quiet.module.impl;

import com.quiet.module.Category;
import com.quiet.module.Module;
import com.quiet.module.Setting;
import com.quiet.render.Palette;
import com.quiet.render.WorldBatch;
import com.quiet.render.WorldRender;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

/** A real 3D box around each player, drawn through walls and stuck to the player. Teammates show green. */
public class PlayerEsp extends Module implements WorldRender.Layer {
	private final Setting.Mode style = mode("Style", "Both", "Outline", "Filled", "Both");
	private final Setting.Mode color = mode("Color", "Cyan", Palette.NAMES);
	private final Setting.Bool team = bool("Teammates green", true);
	private final Setting.Num range = num("Range", 96, 16, 256, 8);

	public PlayerEsp() { super("Player ESP", Category.VISUAL); WorldRender.add(this, this); }

	private static AABB interp(Entity e, float pt) {
		double ix = Mth.lerp((double) pt, e.xo, e.getX());
		double iy = Mth.lerp((double) pt, e.yo, e.getY());
		double iz = Mth.lerp((double) pt, e.zo, e.getZ());
		return e.getBoundingBox().move(ix - e.getX(), iy - e.getY(), iz - e.getZ());
	}

	@Override
	public void extract(WorldBatch b, Minecraft mc, float pt) {
		LocalPlayer me = mc.player;
		for (Player p : mc.level.players()) {
			if (p == me || !p.isAlive() || p.isSpectator() || me.distanceTo(p) > range.get()) continue;
			AABB box = interp(p, pt).inflate(0.04);
			int c = (team.get() && p.isAlliedTo(me)) ? 0xFF4DFF88 : Palette.color(color.mode());
			if (!style.is("Outline")) b.fillBox(box, Palette.alpha(c, 0x30));
			if (!style.is("Filled")) b.edgeBox(box, c, 1.5f);
		}
	}
}
