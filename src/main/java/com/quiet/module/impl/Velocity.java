package com.quiet.module.impl;

import com.quiet.module.Category;
import com.quiet.module.Module;
import com.quiet.module.Setting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

/** Takes less knockback: scales your motion right after the server's knockback packet is applied (ClientPacketListenerMixin). */
public class Velocity extends Module {
	private static Velocity inst;
	private final Setting.Num horizontal = num("Horizontal %", 60, 0, 100, 5);
	private final Setting.Num vertical = num("Vertical %", 100, 0, 100, 5);

	public Velocity() { super("Velocity", Category.COMBAT); inst = this; }

	public static void afterPacket() {
		if (inst == null || !inst.isEnabled()) return;
		LocalPlayer p = Minecraft.getInstance().player;
		if (p == null) return;
		Vec3 v = p.getDeltaMovement();
		double h = inst.horizontal.get() / 100.0, y = inst.vertical.get() / 100.0;
		p.setDeltaMovement(v.x * h, v.y * y, v.z * h);
	}
}
