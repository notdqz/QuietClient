package com.quiet.module.impl;

import com.quiet.module.Category;
import com.quiet.module.Module;
import com.quiet.module.setting.NumberSetting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public class AutoEat extends Module {
	private final NumberSetting hunger = num("Eat at hunger", 14, 1, 19, 1);
	private final NumberSetting health = num("Or health below", 10, 0, 20, 1);
	private boolean eating;
	private int prev = -1, grace;

	public AutoEat() { super("Auto Eat", Category.PLAYER); }

	@Override
	protected void onDisable() { if (eating) finish(); }

	private void finish() {
		Minecraft mc = mc();
		eating = false;
		KeyMapping.setAll();
		if (prev >= 0 && mc.player != null) mc.player.getInventory().setSelectedSlot(prev);
		prev = -1;
	}

	@Override
	protected void onTick() {
		Minecraft mc = mc();
		LocalPlayer p = mc.player;

		if (eating) {
			if (mc.screen != null) { finish(); return; }
			if (grace > 0) grace--;
			if (grace == 0 && !p.isUsingItem()) { finish(); return; }
			mc.options.keyUse.setDown(true);
			return;
		}

		if (!ModUtil.canAct(mc) || p.isUsingItem() || !p.getFoodData().needsFood()) return;
		boolean hungry = p.getFoodData().getFoodLevel() <= hunger.asInt();
		boolean hurt = health.asInt() > 0 && p.getHealth() <= health.asInt();
		if (!hungry && !hurt) return;

		Inventory inv = p.getInventory();
		int slot = -1, bestNutrition = -1;
		for (int i = 0; i < 9; i++) {
			ItemStack s = inv.getItem(i);
			FoodProperties f = s.get(DataComponents.FOOD);
			if (f == null || f.canAlwaysEat()) continue;     // skip golden apples etc.
			if (f.nutrition() > bestNutrition) { bestNutrition = f.nutrition(); slot = i; }
		}
		if (slot < 0) return;

		prev = inv.getSelectedSlot();
		inv.setSelectedSlot(slot);
		eating = true;
		grace = 4;
		mc.options.keyUse.setDown(true);
	}
}
