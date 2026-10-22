package com.quiet.module.impl;

import com.quiet.module.Category;
import com.quiet.module.Module;
import com.quiet.module.setting.ModeSetting;
import com.quiet.module.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;

/** Knockback control. Packet hooks live in ClientPacketListenerMixin. */
public class Velocity extends Module {
	private record Held(ClientboundSetEntityMotionPacket pkt, long at) {}

	private static Velocity inst;
	private static boolean releasing;

	private final ModeSetting mode = mode("Mode", "Reduce", "Reduce", "Cancel", "Delay");
	private final NumberSetting horizontal = num("Horizontal %", 60, 0, 100, 5);
	private final NumberSetting vertical = num("Vertical %", 100, 0, 100, 5);
	private final NumberSetting delayMs = num("Delay (ms)", 120, 30, 500, 10);
	private final ArrayDeque<Held> held = new ArrayDeque<>();

	public Velocity() {
		super("Velocity", Category.COMBAT);
		inst = this;
		horizontal.visibleWhen(() -> mode.is("Reduce"));
		vertical.visibleWhen(() -> mode.is("Reduce"));
		delayMs.visibleWhen(() -> mode.is("Delay"));
	}

	private static boolean live() { return inst != null && inst.isEnabled() && !releasing; }

	/** HEAD of handleSetEntityMotion for our own player. Return true to swallow the packet. */
	public static boolean onPacket(ClientboundSetEntityMotionPacket pkt) {
		if (!live()) return false;
		if (inst.mode.is("Cancel")) return true;
		if (inst.mode.is("Delay")) { inst.held.add(new Held(pkt, System.currentTimeMillis())); return true; }
		return false;
	}

	/** After the packet was applied (Reduce mode): scale the new motion. */
	public static void afterPacket() {
		if (!live() || !inst.mode.is("Reduce")) return;
		LocalPlayer p = Minecraft.getInstance().player;
		if (p == null) return;
		Vec3 v = p.getDeltaMovement();
		double h = inst.horizontal.get() / 100.0, y = inst.vertical.get() / 100.0;
		p.setDeltaMovement(v.x * h, v.y * y, v.z * h);
	}

	@Override
	protected void onTick() {
		long now = System.currentTimeMillis();
		while (!held.isEmpty() && now - held.peek().at() >= delayMs.get()) apply(held.poll().pkt());
	}

	@Override
	protected void onDisable() { while (!held.isEmpty()) apply(held.poll().pkt()); }

	private static void apply(ClientboundSetEntityMotionPacket pkt) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.getConnection() == null) return;
		releasing = true;
		try { mc.getConnection().handleSetEntityMotion(pkt); } finally { releasing = false; }
	}
}
