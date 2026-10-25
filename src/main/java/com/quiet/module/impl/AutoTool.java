package com.quiet.module.impl;

import com.quiet.module.Category;
import com.quiet.module.Module;
import com.quiet.module.Setting;
import com.quiet.module.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/** Picks the fastest hotbar tool for the block you're mining. */
public class AutoTool extends Module {
	private final Setting.Bool swapBack = bool("Swap back", true);
	private int prev = -1;

	public AutoTool() { super("Auto Tool", Category.PLAYER); }

	@Override protected void onDisable() { restore(); }

	private void restore() {
		Minecraft mc = mc();
		if (prev >= 0 && mc.player != null) mc.player.getInventory().setSelectedSlot(prev);
		prev = -1;
	}

	@Override
	protected void onTick() {
		Minecraft mc = mc();
		boolean mining = Util.canAct(mc) && mc.options.keyAttack.isDown()
				&& mc.hitResult instanceof BlockHitResult bhr && bhr.getType() == HitResult.Type.BLOCK;
		if (!mining) { if (swapBack.get()) restore(); else prev = -1; return; }

		BlockState st = mc.level.getBlockState(((BlockHitResult) mc.hitResult).getBlockPos());
		Inventory inv = mc.player.getInventory();
		int cur = inv.getSelectedSlot(), best = cur;
		float bestSpeed = inv.getItem(cur).getDestroySpeed(st);
		for (int i = 0; i < 9; i++) {
			float s = inv.getItem(i).getDestroySpeed(st);
			if (s > bestSpeed + 0.01f) { bestSpeed = s; best = i; }
		}
		if (best != cur) {
			if (prev < 0) prev = cur;
			inv.setSelectedSlot(best);
		}
	}
}
