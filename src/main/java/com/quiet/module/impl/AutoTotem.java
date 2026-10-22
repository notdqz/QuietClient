package com.quiet.module.impl;

import com.quiet.module.Category;
import com.quiet.module.Module;
import com.quiet.module.setting.BooleanSetting;
import com.quiet.module.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Items;

import java.util.concurrent.ThreadLocalRandom;

public class AutoTotem extends Module {
	private enum St { IDLE, HB_SEND, HB_RESTORE, INV_OPEN, INV_CLOSE }

	private final NumberSetting delay = num("Delay (ticks)", 2, 0, 10, 1);
	private final BooleanSetting fromInv = bool("Use inventory", true);
	private St st = St.IDLE;
	private int wait = -1, timer, prevSlot = -1;

	public AutoTotem() { super("Auto Totem", Category.COMBAT); }

	@Override
	protected void onDisable() { st = St.IDLE; wait = -1; prevSlot = -1; }

	private static int findInv(Inventory inv, int from, int to) {
		for (int i = from; i < to; i++) if (inv.getItem(i).is(Items.TOTEM_OF_UNDYING)) return i;
		return -1;
	}

	@Override
	protected void onTick() {
		Minecraft mc = mc();
		LocalPlayer p = mc.player;
		Inventory inv = p.getInventory();
		boolean hasOffhand = p.getOffhandItem().is(Items.TOTEM_OF_UNDYING);

		switch (st) {
			case HB_SEND -> {   // slot change reached the server last tick, now swap
				mc.getConnection().send(new ServerboundPlayerActionPacket(
						ServerboundPlayerActionPacket.Action.SWAP_ITEM_WITH_OFFHAND, BlockPos.ZERO, Direction.DOWN));
				st = St.HB_RESTORE;
				return;
			}
			case HB_RESTORE -> {
				if (prevSlot >= 0) inv.setSelectedSlot(prevSlot);
				prevSlot = -1; st = St.IDLE;
				return;
			}
			case INV_OPEN -> {   // inventory is on screen: pause like a person would, then swap
				if (!(mc.screen instanceof InventoryScreen)) { st = St.IDLE; return; }
				if (--timer > 0) return;
				int slot = findInv(inv, 9, 36);
				if (slot >= 0 && !hasOffhand)
					mc.gameMode.handleInventoryMouseClick(p.inventoryMenu.containerId, slot, 40, ClickType.SWAP, p);
				timer = 2 + ThreadLocalRandom.current().nextInt(3);
				st = St.INV_CLOSE;
				return;
			}
			case INV_CLOSE -> {
				if (--timer > 0) return;
				if (mc.screen instanceof InventoryScreen) mc.screen.onClose();   // also tells the server it closed
				st = St.IDLE;
				return;
			}
			default -> {}
		}

		if (mc.screen != null || hasOffhand) { wait = -1; return; }
		if (wait < 0) wait = delay.asInt();
		if (wait > 0) { wait--; return; }

		int hot = findInv(inv, 0, 9);
		if (hot >= 0) {
			prevSlot = inv.getSelectedSlot();
			inv.setSelectedSlot(hot);
			st = St.HB_SEND;
			wait = -1;
			return;
		}
		if (fromInv.get() && findInv(inv, 9, 36) >= 0) {
			mc.setScreen(new InventoryScreen(p));          // really opens the inventory
			timer = 2 + ThreadLocalRandom.current().nextInt(3);
			st = St.INV_OPEN;
			wait = -1;
		}
	}
}
