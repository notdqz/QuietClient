package com.quiet.hud;

import com.quiet.gui.Draw;
import com.quiet.gui.Fonts;
import com.quiet.gui.Icons;
import com.quiet.gui.Theme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class ArmorHud extends HudWidget {
	private record Slot(ItemStack stack, int pct) {}

	private static final int ROW = 18;
	private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
	private final List<Slot> slots = new ArrayList<>();

	public ArmorHud() { super("armor", "Armor", Icons.SHIRT, 0.92f, 0.45f); }

	@Override
	public void update(boolean preview) {
		slots.clear();
		Minecraft mc = Minecraft.getInstance();
		if (mc.player != null) {
			for (EquipmentSlot es : SLOTS) {
				ItemStack s = mc.player.getItemBySlot(es);
				if (s.isEmpty()) continue;
				int pct = s.isDamageableItem() ? 100 - s.getDamageValue() * 100 / s.getMaxDamage() : 100;
				slots.add(new Slot(s, pct));
			}
		}
		if (slots.isEmpty() && preview) {
			slots.add(new Slot(ItemStack.EMPTY, 100));
			slots.add(new Slot(ItemStack.EMPTY, 82));
			slots.add(new Slot(ItemStack.EMPTY, 55));
			slots.add(new Slot(ItemStack.EMPTY, 31));
		}
		visible = !slots.isEmpty();
		w = 78;
		h = HudDraw.HEADER + slots.size() * ROW + 1;
	}

	@Override
	public void draw(GuiGraphics g, Theme t, int x, int y) {
		HudDraw.card(g, t, x, y, w, h, 8);
		for (int i = 0; i < slots.size(); i++) {
			Slot s = slots.get(i);
			int ry = y + HudDraw.HEADER + i * ROW, cy = ry + ROW / 2;
			int col = i % 2 == 0 ? t.panelRowA : t.panelRowB;
			if (i == slots.size() - 1) Draw.roundBottom(g, x, ry, w, ROW, 8, col);
			else g.fill(x, ry, x + w, ry + ROW, Draw.a(col));

			if (s.stack().isEmpty()) Icons.draw(g, Icons.SHIRT, x + 7, cy - 4, t.panelText);
			else g.renderItem(s.stack(), x + 4, ry + 1);

			int bx = x + 26, bw = 22;
			int color = Draw.lerp(0xFFE5484D, 0xFF46C37B, s.pct() / 100f);
			Draw.round(g, bx, cy - 1, bw, 3, 1, t.sliderTrack);
			Draw.round(g, bx, cy - 1, Math.max(2, bw * s.pct() / 100f), 3, 1, color);
			Fonts.drawRight(g, s.pct() + "%", x + w - 5, cy, t.panelSub);
		}
		Icons.draw(g, Icons.ELLIPSIS, x + 4, y + HudDraw.HEADER / 2 - 4, t.dim);
		Fonts.drawCentered(g, "Armor", x + w / 2, y + HudDraw.HEADER / 2, t.panelText, true);
		Draw.rect(g, x + 1, y + HudDraw.HEADER, w - 2, 0.5f, t.panelBorder);
		HudDraw.cardTop(g, t, x, y, w, h, 8);
	}
}