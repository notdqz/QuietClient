package com.quiet.module.impl;

import com.quiet.module.Category;
import com.quiet.module.Module;
import com.quiet.module.setting.BooleanSetting;
import com.quiet.module.setting.ModeSetting;
import com.quiet.render.Palette;
import com.quiet.render.WorldBatch;
import com.quiet.render.WorldRender;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/** Predicted path of whatever you're about to throw or shoot, drawn in the world, with where it lands. */
public class TrajectoryPreview extends Module implements WorldRender.Layer {
	private record Prof(double speed, double gravity) {}

	private final ModeSetting color = mode("Color", "Cyan", Palette.NAMES);
	private final BooleanSetting idleBow = bool("Bow at full draw when idle", false);

	public TrajectoryPreview() { super("Trajectory", Category.RENDER); WorldRender.add(this, this); }

	private Prof profile(LocalPlayer p, ItemStack st) {
		if (st.is(Items.BOW)) {
			if (!p.isUsingItem() && !idleBow.get()) return null;
			float power = BowItem.getPowerForTime(p.isUsingItem() ? p.getTicksUsingItem() : 20);
			return power < 0.1f ? null : new Prof(power * 3.0, 0.05);
		}
		if (st.is(Items.CROSSBOW)) return CrossbowItem.isCharged(st) ? new Prof(3.15, 0.05) : null;
		if (st.is(Items.TRIDENT)) return new Prof(2.5, 0.05);
		if (st.is(Items.ENDER_PEARL) || st.is(Items.SNOWBALL) || st.is(Items.EGG)) return new Prof(1.5, 0.03);
		if (st.is(Items.SPLASH_POTION) || st.is(Items.LINGERING_POTION)) return new Prof(0.5, 0.05);
		if (st.is(Items.EXPERIENCE_BOTTLE)) return new Prof(0.7, 0.07);
		return null;
	}

	@Override
	public void extract(WorldBatch b, Minecraft mc, float pt) {
		LocalPlayer p = mc.player;
		Prof pr = profile(p, p.getMainHandItem());
		if (pr == null) pr = profile(p, p.getOffhandItem());
		if (pr == null) return;

		Vec3 pos = p.getEyePosition(pt).subtract(0, 0.1, 0);
		Vec3 pv = p.getDeltaMovement();
		Vec3 vel = p.getViewVector(pt).scale(pr.speed()).add(pv.x, p.onGround() ? 0 : pv.y, pv.z);

		List<Vec3> pts = new ArrayList<>();
		pts.add(pos);
		boolean hitPlayer = false;
		for (int i = 0; i < 200; i++) {
			Vec3 next = pos.add(vel);
			BlockHitResult hr = mc.level.clip(new ClipContext(pos, next, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
			Vec3 end = hr.getType() == HitResult.Type.MISS ? next : hr.getLocation();
			for (Player o : mc.level.players()) {
				if (o == p || !o.isAlive()) continue;
				if (o.getBoundingBox().inflate(0.3).clip(pos, end).isPresent()) { hitPlayer = true; break; }
			}
			pts.add(end);
			if (hr.getType() != HitResult.Type.MISS || hitPlayer || end.y < -80) break;
			pos = next;
			double drag = mc.level.getFluidState(BlockPos.containing(pos)).is(FluidTags.WATER) ? 0.8 : 0.99;
			vel = vel.scale(drag).subtract(0, pr.gravity(), 0);
		}

		int c = hitPlayer ? 0xFFFF4D4D : Palette.color(color.mode());
		for (int i = 1; i < pts.size(); i++) b.line(pts.get(i - 1), pts.get(i), c, 1.5f);
		Vec3 last = pts.get(pts.size() - 1);
		b.highlight(new AABB(last, last).inflate(0.18), c, Palette.alpha(c, 0x40), 1.5f);
	}
}
