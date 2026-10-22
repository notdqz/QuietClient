package com.quiet.module.impl;

import com.quiet.Quiet;
import com.quiet.module.Category;
import com.quiet.module.Module;
import com.quiet.module.setting.BooleanSetting;
import com.quiet.module.setting.NumberSetting;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** Uses the vanilla interaction-range attributes, so no mixin is needed. */
public class Reach extends Module {
	private static final Identifier ID = Quiet.id("reach");
	private final NumberSetting entities = num("Entity reach", 3.5, 3.0, 6.0, 0.1);
	private final BooleanSetting blocks = bool("Also blocks", false);
	private final NumberSetting blockRange = num("Block reach", 5.0, 4.5, 8.0, 0.1);

	public Reach() {
		super("Reach", Category.COMBAT);
		blockRange.visibleWhen(blocks::get);
	}

	@Override
	protected void onTick() {
		LocalPlayer p = mc().player;
		apply(p, Attributes.ENTITY_INTERACTION_RANGE, entities.get() - 3.0);
		apply(p, Attributes.BLOCK_INTERACTION_RANGE, blocks.get() ? blockRange.get() - 4.5 : 0);
	}

	@Override
	protected void onDisable() {
		LocalPlayer p = mc().player;
		if (p == null) return;
		apply(p, Attributes.ENTITY_INTERACTION_RANGE, 0);
		apply(p, Attributes.BLOCK_INTERACTION_RANGE, 0);
	}

	private static void apply(LocalPlayer p, Holder<Attribute> attr, double extra) {
		AttributeInstance inst = p.getAttribute(attr);
		if (inst == null) return;
		AttributeModifier cur = inst.getModifier(ID);
		if (extra <= 0.001) { if (cur != null) inst.removeModifier(ID); return; }
		if (cur != null && Math.abs(cur.amount() - extra) < 0.001) return;
		inst.removeModifier(ID);
		inst.addTransientModifier(new AttributeModifier(ID, extra, AttributeModifier.Operation.ADD_VALUE));
	}
}
