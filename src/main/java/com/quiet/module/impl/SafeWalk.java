package com.quiet.module.impl;

import com.quiet.module.Category;
import com.quiet.module.Module;
import com.quiet.module.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Crouches only at the very edge, and stands up again as soon as you are no longer there. It holds the real sneak key,
 * so the game's own edge protection does the saving. "At the edge" means: one tick of walking in any direction (about
 * 0.15 blocks), or one tick of your current speed, would leave your hitbox with nothing under it. That is the last
 * moment you can still be saved, so you only crouch when it matters, even from a standstill on the edge.
 */
public class SafeWalk extends Module {
	private static final double STEP = 0.15;   // about one tick of walking from a standstill
	private boolean forced;

	public SafeWalk() { super("Safe Walk", Category.MOVEMENT); }

	@Override
	protected void onDisable() {
		if (forced) { forced = false; Util.releaseKey(mc().options.keyShift); }
	}

	/** True if shifting the hitbox by (dx, dz) would leave nothing to stand on (same test vanilla uses for edge protection). */
	private static boolean dropsAway(Minecraft mc, LocalPlayer p, AABB box, double dx, double dz) {
		return mc.level.noCollision(p, box.move(dx, -0.6, dz));
	}

	@Override
	protected void onTick() {
		Minecraft mc = mc();
		LocalPlayer p = mc.player;
		boolean edge = false;
		if (mc.screen == null && p.onGround() && !p.getAbilities().flying) {
			AABB box = p.getBoundingBox();
			Vec3 v = p.getDeltaMovement();
			edge = dropsAway(mc, p, box, STEP, 0) || dropsAway(mc, p, box, -STEP, 0)
					|| dropsAway(mc, p, box, 0, STEP) || dropsAway(mc, p, box, 0, -STEP)
					|| (Math.hypot(v.x, v.z) > 0.02 && dropsAway(mc, p, box, v.x * 1.3, v.z * 1.3));
		}
		if (edge) {
			mc.options.keyShift.setDown(true);
			forced = true;
		} else if (forced) {
			forced = false;
			Util.releaseKey(mc.options.keyShift);   // stand up again
		}
	}
}
