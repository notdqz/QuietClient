package com.quiet.module.impl;

import com.quiet.module.Category;
import com.quiet.module.Module;
import com.quiet.module.setting.BooleanSetting;
import com.quiet.module.setting.ModeSetting;
import com.quiet.module.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import java.util.concurrent.ThreadLocalRandom;

/**
 * The extra knockback from a sprint hit (or the Knockback enchant) is pushed in the direction the attacker is
 * LOOKING, as the server last heard it. So right before the attack we send a rotation packet turned to the side /
 * back, then send your real rotation again right after. Your own screen never moves. Hooked in
 * MultiPlayerGameModeMixin, so manual clicks and every module go through it.
 */
public class KnockbackFlick extends Module {
	private static KnockbackFlick inst;
	private static boolean flicked;

	private final ModeSetting direction = mode("Push target", "Left", "Left", "Right", "Back", "Random side");
	private final NumberSetting chance = num("Chance %", 100, 0, 100, 5);
	private final BooleanSetting playersOnly = bool("Players only", true);

	public KnockbackFlick() { super("Knockback Flick", Category.COMBAT); inst = this; }

	/** Yaw offset (degrees) to flick by for this attack, or NaN if this hit shouldn't be flicked. */
	public static float plan(Player attacker, Entity target) {
		if (inst == null || !inst.isEnabled() || !(target instanceof LivingEntity)) return Float.NaN;
		if (inst.playersOnly.get() && !(target instanceof Player)) return Float.NaN;
		if (attacker.getAttackStrengthScale(0.5f) <= 0.9f) return Float.NaN;
		// only the extra sprint / enchant knockback follows your yaw; plain knockback always points away from you
		if (!attacker.isSprinting() && enchantLevel(attacker, false) <= 0) return Float.NaN;
		ThreadLocalRandom r = ThreadLocalRandom.current();
		if (r.nextDouble(100) >= inst.chance.get()) return Float.NaN;

		float off = switch (inst.direction.mode()) {
			case "Right" -> 90f;
			case "Back" -> 180f;
			case "Random side" -> r.nextBoolean() ? 90f : -90f;
			default -> -90f;      // Left
		};
		return off + (float) r.nextDouble(-4, 4);
	}

	/** Knockback enchant level on the main hand (or Sharpness when sharp = true). */
	public static int enchantLevel(Player p, boolean sharp) {
		ItemEnchantments ench = p.getMainHandItem().getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
		for (var e : ench.entrySet())
			if (e.getKey().is(sharp ? Enchantments.SHARPNESS : Enchantments.KNOCKBACK)) return e.getIntValue();
		return 0;
	}

	/** Right before the attack packet goes out. */
	public static void before(Player p, float offset) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.getConnection() == null) return;
		mc.getConnection().send(new ServerboundMovePlayerPacket.Rot(p.getYRot() + offset, p.getXRot(), p.onGround(), p.horizontalCollision));
		flicked = true;
	}

	/** Right after it: tell the server where you're really looking. */
	public static void after(Player p) {
		if (!flicked) return;
		flicked = false;
		Minecraft mc = Minecraft.getInstance();
		if (mc.getConnection() == null) return;
		mc.getConnection().send(new ServerboundMovePlayerPacket.Rot(p.getYRot(), p.getXRot(), p.onGround(), p.horizontalCollision));
	}
}
