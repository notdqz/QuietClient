package com.quiet.module.impl;

import com.quiet.mixin.MinecraftAccessor;
import com.quiet.module.Category;
import com.quiet.module.Module;
import com.quiet.module.setting.BooleanSetting;
import com.quiet.module.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.BlockItem;

public class FastPlace extends Module {
	private final NumberSetting delay = num("Delay (ticks)", 0, 0, 3, 1);
	private final BooleanSetting blocksOnly = bool("Blocks only", true);

	public FastPlace() { super("Fast Place", Category.PLAYER); }

	@Override
	protected void onTick() {
		Minecraft mc = mc();
		if (!ModUtil.canAct(mc) || !mc.options.keyUse.isDown()) return;
		if (blocksOnly.get() && !(mc.player.getMainHandItem().getItem() instanceof BlockItem)) return;
		MinecraftAccessor acc = (MinecraftAccessor) (Object) mc;
		if (acc.getRightClickDelay() > delay.asInt()) acc.setRightClickDelay(delay.asInt());
	}
}
