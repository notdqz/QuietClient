package com.quiet.module.impl;

import com.quiet.module.Category;
import com.quiet.module.Module;
import com.quiet.module.setting.BooleanSetting;
import com.quiet.module.setting.ModeSetting;
import com.quiet.render.Palette;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/**
 * Players get a coloured silhouette that shows through walls, using the game's own outline renderer
 * (EntityGlowMixin). That's an outline rather than a custom shader, but it works with every pack.
 */
public class Chams extends Module {
	private static Chams inst;
	private final ModeSetting color = mode("Color", "Purple", Palette.NAMES);
	private final BooleanSetting self = bool("Self (3rd person)", false);

	public Chams() { super("Chams", Category.RENDER); inst = this; }

	public static boolean glows(Entity e) {
		if (inst == null || !inst.isEnabled() || !(e instanceof Player p)) return false;
		Minecraft mc = Minecraft.getInstance();
		if (p == mc.player) return inst.self.get() && !mc.options.getCameraType().isFirstPerson();
		return !p.isSpectator();
	}

	public static int rgb() { return inst == null ? 0xFFFFFF : Palette.color(inst.color.mode()) & 0xFFFFFF; }
}
