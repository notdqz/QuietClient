package com.quiet.module.impl;

import com.quiet.module.Category;
import com.quiet.module.Module;
import com.quiet.module.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;

/** Tells the server you're on the ground once the fall gets long enough, so fall distance resets. */
public class NoFall extends Module {
	private final NumberSetting dist = num("Fall distance", 2.5, 1.5, 3.0, 0.1);

	public NoFall() { super("NoFall", Category.MOVEMENT); }

	@Override
	protected void onTick() {
		Minecraft mc = mc();
		LocalPlayer p = mc.player;
		if (p.getAbilities().mayfly || p.isFallFlying() || p.onGround() || p.isInWater()) return;
		if (p.getDeltaMovement().y >= 0 || p.fallDistance <= dist.get()) return;
		mc.getConnection().send(new ServerboundMovePlayerPacket.StatusOnly(true, p.horizontalCollision));
	}
}
