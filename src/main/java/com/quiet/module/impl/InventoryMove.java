package com.quiet.module.impl;

import com.mojang.blaze3d.platform.InputConstants;
import com.quiet.module.Category;
import com.quiet.module.Keys;
import com.quiet.module.Module;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;

public class InventoryMove extends Module {
	private boolean forced;

	public InventoryMove() { super("Inventory Move", Category.PLAYER); }

	@Override
	protected void onDisable() { release(); }

	private void release() {
		if (!forced) return;
		forced = false;
		if (mc().screen == null) KeyMapping.setAll();
	}

	private static void sync(KeyMapping km) {
		InputConstants.Key k = InputConstants.getKey(km.saveString());
		if (k.getType() != InputConstants.Type.KEYSYM) return;
		km.setDown(Keys.isDown(k.getValue()));
	}

	@Override
	protected void onTick() {
		Minecraft mc = mc();
		Screen s = mc.screen;
		boolean ok = s instanceof AbstractContainerScreen<?> && !(s instanceof AnvilScreen)
				&& !(s instanceof CreativeModeInventoryScreen);
		if (!ok) { release(); return; }
		sync(mc.options.keyUp); sync(mc.options.keyDown);
		sync(mc.options.keyLeft); sync(mc.options.keyRight);
		sync(mc.options.keyJump); sync(mc.options.keySprint); sync(mc.options.keyShift);
		forced = true;
	}
}
