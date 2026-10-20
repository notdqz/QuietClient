package com.quiet.hud;

import com.quiet.config.QuietConfig;
import com.quiet.gui.Icons;
import com.quiet.hud.HudDraw.Row;
import com.quiet.module.Category;
import com.quiet.module.Module;
import com.quiet.module.ModuleManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.*;

/** Row data for the panel-style HUD elements. */
public final class HudData {
	private static final Map<String, Long> SEEN = new HashMap<>();

	private static String mmss(long s) { return String.format("%d:%02d", s / 60, s % 60); }

	public static List<Row> potions(boolean preview) {
		List<Row> out = new ArrayList<>();
		Player p = Minecraft.getInstance().player;
		if (p != null) {
			for (MobEffectInstance e : p.getActiveEffects()) {
				String name = e.getEffect().value().getDisplayName().getString();
				String lvl = e.getAmplifier() > 0 ? String.valueOf(e.getAmplifier() + 1) : "";
				out.add(new Row(Icons.POTION, name, lvl, e.isInfiniteDuration() ? "inf" : mmss(e.getDuration() / 20)));
			}
		}
		if (out.isEmpty() && preview) {
			out.add(new Row(Icons.POTION, "Regeneration", "2", "0:24"));
			out.add(new Row(Icons.POTION, "Strength", "3", "1:46"));
			out.add(new Row(Icons.POTION, "Poison", "", "0:19"));
		}
		return out;
	}

	public static List<Row> staffs(boolean preview) {
		List<Row> out = new ArrayList<>();
		ClientPacketListener conn = Minecraft.getInstance().getConnection();
		if (conn != null && !QuietConfig.staff.isEmpty()) {
			long now = System.currentTimeMillis();
			Set<String> online = new HashSet<>();
			for (PlayerInfo info : conn.getOnlinePlayers()) {
				String n = info.getProfile().name();
				String role = QuietConfig.staff.get(n.toLowerCase(Locale.ROOT));
				if (role == null) continue;
				online.add(n);
				long since = SEEN.computeIfAbsent(n, k -> now);
				out.add(new Row(Icons.SHIELD, n, role, mmss((now - since) / 1000)));
			}
			SEEN.keySet().retainAll(online);
		}
		if (out.isEmpty() && preview) {
			out.add(new Row(Icons.SHIELD, "Player1", "Helper", "14:25"));
			out.add(new Row(Icons.SHIELD, "Player2", "Moder", "0:38"));
			out.add(new Row(Icons.SHIELD, "Player3", "Tester", "4:19"));
		}
		return out;
	}

	public static List<Row> keybinds(boolean preview) {
		List<Row> out = new ArrayList<>();
		for (Category c : Category.values())
			for (Module m : ModuleManager.byCategory(c))
				if (m.key() >= 0) out.add(new Row(c.icon(), m.name(), "", m.keyName()));
		if (out.isEmpty() && preview) {
			out.add(new Row(Icons.RENDER, "NameTags", "", "N"));
			out.add(new Row(Icons.COMBAT, "KillAura", "", "R"));
			out.add(new Row(Icons.FEATHER, "Speed", "", "V"));
		}
		return out;
	}

	public static List<Row> coords(boolean preview) {
		Player p = Minecraft.getInstance().player;
		if (p == null) return List.of();
		String dir = p.getDirection().getName();
		dir = Character.toUpperCase(dir.charAt(0)) + dir.substring(1);
		return List.of(
				new Row(Icons.PIN, "X", "", String.format("%.1f", p.getX())),
				new Row(Icons.PIN, "Y", "", String.format("%.1f", p.getY())),
				new Row(Icons.PIN, "Z", "", String.format("%.1f", p.getZ())),
				new Row(Icons.COMPASS, "Facing", "", dir));
	}

	private static int count(Inventory inv, Item... items) {
		int n = 0;
		for (int i = 0; i < inv.getContainerSize(); i++) {
			ItemStack s = inv.getItem(i);
			for (Item it : items) if (s.is(it)) { n += s.getCount(); break; }
		}
		return n;
	}

	public static List<Row> counters(boolean preview) {
		Player p = Minecraft.getInstance().player;
		if (p == null) return List.of();
		Inventory inv = p.getInventory();
		return List.of(
				new Row(Icons.SHIELD, "Totems", "", String.valueOf(count(inv, Items.TOTEM_OF_UNDYING))),
				new Row(Icons.HEART, "Gapples", "", String.valueOf(count(inv, Items.GOLDEN_APPLE, Items.ENCHANTED_GOLDEN_APPLE))),
				new Row(Icons.COMPASS, "Pearls", "", String.valueOf(count(inv, Items.ENDER_PEARL))),
				new Row(Icons.ZAP, "Crystals", "", String.valueOf(count(inv, Items.END_CRYSTAL))),
				new Row(Icons.ACTIVITY, "XP Bottles", "", String.valueOf(count(inv, Items.EXPERIENCE_BOTTLE))));
	}
}