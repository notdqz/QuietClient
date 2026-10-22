package com.quiet.module.impl;

import com.quiet.module.Category;
import com.quiet.module.Module;
import com.quiet.module.setting.BooleanSetting;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/** Client-side night vision (never sent to the server) + optional removal of water/lava fog (CameraMixin). */
public class Fullbright extends Module {
	private static Fullbright inst;
	private final BooleanSetting clearFog = bool("Clear water/lava fog", true);
	private boolean added;

	public Fullbright() { super("Fullbright", Category.RENDER); inst = this; }

	public static boolean clearFog() { return inst != null && inst.isEnabled() && inst.clearFog.get(); }

	@Override
	protected void onDisable() {
		LocalPlayer p = mc().player;
		if (added && p != null) p.removeEffect(MobEffects.NIGHT_VISION);
		added = false;
	}

	@Override
	protected void onTick() {
		LocalPlayer p = mc().player;
		if (!p.hasEffect(MobEffects.NIGHT_VISION)) {
			p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, -1, 0, false, false, false));
			added = true;
		}
	}
}
