package com.quiet.module.impl;

import com.quiet.module.Category;
import com.quiet.module.Module;
import com.quiet.module.setting.BooleanSetting;
import com.quiet.module.setting.NumberSetting;
import com.quiet.render.Palette;
import com.quiet.render.WorldBatch;
import com.quiet.render.WorldRender;
import com.quiet.render.WorldScan;
import net.minecraft.client.Minecraft;

/** Highlights the actual block (a box exactly around it) through walls. */
public class StorageEsp extends Module implements WorldRender.Layer {
	private final BooleanSetting chests = bool("Chests", true);
	private final BooleanSetting ender = bool("Ender chests", true);
	private final BooleanSetting shulkers = bool("Shulker boxes", true);
	private final BooleanSetting barrels = bool("Barrels", true);
	private final BooleanSetting hoppers = bool("Hoppers", false);
	private final NumberSetting range = num("Range", 64, 16, 128, 8);

	public StorageEsp() { super("Storage ESP", Category.RENDER); WorldRender.add(this, this); }

	@Override
	public void extract(WorldBatch b, Minecraft mc, float pt) {
		int flags = (chests.get() ? WorldScan.CHEST : 0) | (ender.get() ? WorldScan.ENDER : 0)
				| (shulkers.get() ? WorldScan.SHULKER : 0) | (barrels.get() ? WorldScan.BARREL : 0)
				| (hoppers.get() ? WorldScan.HOPPER : 0);
		if (flags == 0) return;
		for (WorldScan.Box x : WorldScan.storage(mc, range.asInt(), flags))
			b.highlight(x.box().inflate(0.002), x.color(), Palette.alpha(x.color(), 0x38), 1.5f);
	}
}
