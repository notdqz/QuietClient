package com.quiet.module.impl;

import com.quiet.module.Category;
import com.quiet.module.Module;
import com.quiet.module.setting.BooleanSetting;
import com.quiet.module.setting.ModeSetting;
import com.quiet.module.setting.NumberSetting;
import com.quiet.render.Lines;
import com.quiet.render.Palette;
import com.quiet.render.WorldBatch;
import com.quiet.render.WorldRender;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Hitboxes as real 3D wireframes: the smoothed client box, and/or the last position the server sent. */
public class HitboxDisplay extends Module implements WorldRender.Layer {
	private final ModeSetting show = mode("Show", "Client", "Client", "Server", "Both");
	private final ModeSetting color = mode("Client color", "Cyan", Palette.NAMES);
	private final ModeSetting serverColor = mode("Server color", "Red", Palette.NAMES);
	private final BooleanSetting all = bool("All living entities", false);
	private final BooleanSetting eye = bool("Eye line", false);
	private final NumberSetting range = num("Range", 48, 8, 128, 4);

	public HitboxDisplay() { super("Hitbox Display", Category.RENDER); WorldRender.add(this, this); }

	@Override
	public void extract(WorldBatch b, Minecraft mc, float pt) {
		LocalPlayer me = mc.player;
		for (Entity e : mc.level.entitiesForRendering()) {
			if (e == me || !(e instanceof LivingEntity) || !e.isAlive()) continue;
			if (!all.get() && !(e instanceof Player)) continue;
			if (me.distanceTo(e) > range.get()) continue;

			if (!show.is("Server")) {
				AABB box = Lines.interp(e, pt);
				b.edgeBox(box, Palette.color(color.mode()), 1.5f);
				if (eye.get()) {
					Vec3 from = new Vec3(box.getCenter().x, box.minY + e.getEyeHeight(), box.getCenter().z);
					b.line(from, from.add(e.getViewVector(pt).scale(2.0)), 0xFFFFE14D, 1.5f);
				}
			}
			if (!show.is("Client")) {
				Vec3 base = e.getPositionCodec().getBase();   // newest position the server has sent for this entity
				AABB sb = e.getBoundingBox().move(base.x - e.getX(), base.y - e.getY(), base.z - e.getZ());
				b.edgeBox(sb, Palette.color(serverColor.mode()), 1.5f);
			}
		}
	}
}
