package com.quiet.module.impl;

import com.quiet.Quiet;
import com.quiet.module.Category;
import com.quiet.module.Module;
import com.quiet.module.Setting;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** Extra entity reach through the vanilla interaction-range attribute (no mixin needed). */
public class Reach extends Module {
	private static final Identifier ID = Quiet.id("reach");
	private final Setting.Num reach = num("Reach", 3.5, 3.0, 6.0, 0.1);

	public Reach() { super("Reach", Category.COMBAT); }

	@Override protected void onTick() { apply(mc().player, reach.get() - 3.0); }

	@Override
	protected void onDisable() {
		LocalPlayer p = mc().player;
		if (p != null) apply(p, 0);
	}

	private static void apply(LocalPlayer p, double extra) {
		AttributeInstance inst = p.getAttribute(Attributes.ENTITY_INTERACTION_RANGE);
		if (inst == null) return;
		AttributeModifier cur = inst.getModifier(ID);
		if (extra <= 0.001) { if (cur != null) inst.removeModifier(ID); return; }
		if (cur != null && Math.abs(cur.amount() - extra) < 0.001) return;
		inst.removeModifier(ID);
		inst.addTransientModifier(new AttributeModifier(ID, extra, AttributeModifier.Operation.ADD_VALUE));
	}
}
