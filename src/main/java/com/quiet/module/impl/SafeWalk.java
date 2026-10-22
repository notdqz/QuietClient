package com.quiet.module.impl;

import com.quiet.module.Category;
import com.quiet.module.Module;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Holds sneak only while you're about to step off an edge. */
public class SafeWalk extends Module {
	private boolean forced;

	public SafeWalk() { super("Safe Walk", Category.MOVEMENT); }

	@Override
	protected void onDisable() { if (forced) { forced = false; KeyMapping.setAll(); } }

	@Override
	protected void onTick() {
		Minecraft mc = mc();
		LocalPlayer p = mc.player;
		boolean edge = false;
		if (mc.screen == null && p.onGround() && !p.getAbilities().flying) {
			Vec3 v = p.getDeltaMovement();
			AABB ahead = p.getBoundingBox().move(v.x * 3, -0.6, v.z * 3);
			edge = mc.level.noCollision(p, ahead);
		}
		if (edge) { mc.options.keyShift.setDown(true); forced = true; }
		else if (forced) { forced = false; KeyMapping.setAll(); }
	}
}
