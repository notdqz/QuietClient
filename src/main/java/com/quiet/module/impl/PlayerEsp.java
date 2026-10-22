package com.quiet.module.impl;

import com.quiet.module.Category;
import com.quiet.module.Module;
import com.quiet.module.setting.BooleanSetting;
import com.quiet.module.setting.ModeSetting;
import com.quiet.module.setting.NumberSetting;
import com.quiet.render.Lines;
import com.quiet.render.Palette;
import com.quiet.render.WorldBatch;
import com.quiet.render.WorldRender;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

/** A real 3D box around each player's hitbox, drawn through walls and stuck to the player. */
public class PlayerEsp extends Module implements WorldRender.Layer {
	private final ModeSetting style = mode("Style", "Both", "Outline", "Filled", "Both");
	private final ModeSetting color = mode("Color", "Cyan", Palette.NAMES);
	private final BooleanSetting self = bool("Self ESP", false);
	private final BooleanSetting team = bool("Team ESP", true);
	private final NumberSetting width = num("Line width", 1.5, 1.0, 4.0, 0.5);
	private final NumberSetting range = num("Range", 96, 16, 256, 8);

	public PlayerEsp() { super("Player ESP", Category.RENDER); WorldRender.add(this, this); }

	@Override
	public void extract(WorldBatch b, Minecraft mc, float pt) {
		LocalPlayer me = mc.player;
		boolean third = !mc.options.getCameraType().isFirstPerson();
		for (Player p : mc.level.players()) {
			if (p == me && !(self.get() && third)) continue;
			if (!p.isAlive() || p.isSpectator() || me.distanceTo(p) > range.get()) continue;
			AABB box = Lines.interp(p, pt).inflate(0.04);
			int c = (team.get() && p != me && p.isAlliedTo(me)) ? 0xFF4DFF88 : Palette.color(color.mode());
			if (!style.is("Outline")) b.fillBox(box, Palette.alpha(c, 0x30));
			if (!style.is("Filled")) b.edgeBox(box, c, width.asFloat());
		}
	}
}
