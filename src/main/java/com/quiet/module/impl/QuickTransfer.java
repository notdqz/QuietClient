package com.quiet.module.impl;

import com.quiet.module.Category;
import com.quiet.module.Module;
import com.quiet.module.Setting;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ShulkerBoxMenu;

import java.util.ArrayDeque;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Adds two buttons beside chests, double chests, barrels, ender chests and shulker boxes:
 * "Take All" moves everything from the container into your inventory, "Dump All" moves your inventory into it.
 * Items move one click at a time at the speed you choose (with a little random variation), like shift-clicking fast.
 */
public class QuickTransfer extends Module {
	private static QuickTransfer inst;

	private final Setting.Num speed = num("Speed (items/s)", 12, 2, 40, 1);
	private final Setting.Bool keepHotbar = bool("Keep hotbar", true);
	private final ArrayDeque<int[]> queue = new ArrayDeque<>();   // {containerId, slot}
	private double acc;

	public QuickTransfer() {
		super("Quick Transfer", Category.PLAYER);
		inst = this;
		ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
			if (inst == null || !inst.isEnabled()) return;
			if (!(screen instanceof AbstractContainerScreen<?> acs)) return;
			AbstractContainerMenu menu = acs.getMenu();
			int rows;
			if (menu instanceof ChestMenu cm) rows = cm.getRowCount();   // chests, double chests, barrels, ender chests
			else if (menu instanceof ShulkerBoxMenu) rows = 3;
			else return;

			int imageH = 114 + rows * 18;
			int left = (w - 176) / 2, top = (h - imageH) / 2;
			int n = rows * 9;
			Screens.getButtons(screen).add(Button.builder(Component.literal("Take All"),
					b -> inst.enqueue(menu, 0, n)).bounds(left + 176 + 4, top + 4, 56, 16).build());
			Screens.getButtons(screen).add(Button.builder(Component.literal("Dump All"),
					b -> inst.enqueue(menu, n, inst.keepHotbar.get() ? n + 27 : n + 36)).bounds(left + 176 + 4, top + 24, 56, 16).build());
		});
	}

	private void enqueue(AbstractContainerMenu menu, int from, int to) {
		queue.clear();
		acc = 0;
		for (int i = from; i < to; i++)
			if (menu.getSlot(i).hasItem()) queue.add(new int[]{menu.containerId, i});
	}

	@Override protected void onDisable() { queue.clear(); }

	@Override
	protected void onTick() {
		if (queue.isEmpty()) return;
		Minecraft mc = mc();
		if (!(mc.screen instanceof AbstractContainerScreen<?>)) { queue.clear(); return; }   // container was closed
		AbstractContainerMenu menu = mc.player.containerMenu;

		acc += speed.get() / 20.0 * (0.7 + ThreadLocalRandom.current().nextDouble() * 0.6);
		while (acc >= 1 && !queue.isEmpty()) {
			acc -= 1;
			int[] e = queue.poll();
			if (e[0] != menu.containerId) { queue.clear(); return; }
			if (menu.getSlot(e[1]).hasItem())
				mc.gameMode.handleInventoryMouseClick(menu.containerId, e[1], 0, ClickType.QUICK_MOVE, mc.player);
		}
	}
}
