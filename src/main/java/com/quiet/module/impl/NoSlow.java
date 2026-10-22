package com.quiet.module.impl;

import com.quiet.module.Category;
import com.quiet.module.Module;
import com.quiet.module.setting.BooleanSetting;
import com.quiet.module.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

public class NoSlow extends Module {
	private static NoSlow inst;
	private final BooleanSetting webs = bool("Webs & bushes", true);
	private final BooleanSetting water = bool("Water", true);
	private final BooleanSetting lava = bool("Lava", false);
	private final NumberSetting liquidSpeed = num("Liquid speed (b/s)", 4.3, 1.5, 6.0, 0.1);

	public NoSlow() { super("No Slow", Category.MOVEMENT); inst = this; }

	/** Read by EntityMixin. */
	public static boolean blocksStuck() { return inst != null && inst.isEnabled() && inst.webs.get(); }

	@Override
	protected void onTick() {
		Minecraft mc = mc();
		LocalPlayer p = mc.player;
		boolean w = water.get() && p.isInWater(), l = lava.get() && p.isInLava();
		if (!(w || l) || !ModUtil.isMoving(mc) || p.isShiftKeyDown()) return;

		// The velocity we leave here still gets ~0.02 of acceleration added next tick,
		// so aim a little under the target speed. 4.3 b/s = normal walking speed.
		double cap = liquidSpeed.get() / 20.0 - 0.02;
		Vec3 v = p.getDeltaMovement();
		double h = Math.hypot(v.x, v.z);
		if (h < 0.001 || h >= cap) return;
		double nh = Math.min(cap, h / (l ? 0.5 : 0.8)), k = nh / h;   // undo the liquid drag, up to the cap
		p.setDeltaMovement(v.x * k, v.y, v.z * k);
	}
}
