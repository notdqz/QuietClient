package com.quiet.module.impl;

import com.quiet.module.Category;
import com.quiet.module.Module;
import com.quiet.module.Setting;
import com.quiet.module.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Places blocks under you as you walk off an edge. Your camera never moves.
 * To look natural to the server it turns your SERVER-side view toward the block face with eased steps (never a snap),
 * places only once that view has actually arrived, waits a random time between blocks, and aims at a slightly
 * different spot on each block. Every placement uses a real ray that really hits that face within reach.
 */
public class Scaffold extends Module {
	private static final double[][] SAMPLES = {
		{0, 0}, {0.3, 0}, {-0.3, 0}, {0, 0.3}, {0, -0.3}, {0.3, 0.3}, {0.3, -0.3}, {-0.3, 0.3}, {-0.3, -0.3}
	};
	private static final int MAX_WAIT_TICKS = 4;   // if the eased turn is slow, place anyway after this many ticks

	private final Setting.Num minDelay = num("Min delay (ms)", 60, 0, 300, 10);
	private final Setting.Num maxDelay = num("Max delay (ms)", 140, 0, 300, 10);
	private final Setting.Mode rotation = mode("Rotation", "Server", "Off", "Server");
	private final Setting.Bool swapBack = bool("Swap back", true);

	private int prevSlot = -1, sampleStart, waitTicks;
	private long nextPlace;

	public Scaffold() { super("Scaffold", Category.MOVEMENT); }

	@Override
	protected void onDisable() {
		Minecraft mc = mc();
		if (swapBack.get() && prevSlot >= 0 && mc.player != null) mc.player.getInventory().setSelectedSlot(prevSlot);
		prevSlot = -1;
		waitTicks = 0;
	}

	private boolean usable(Minecraft mc, ItemStack s) {
		if (!(s.getItem() instanceof BlockItem bi)) return false;
		if (bi.getBlock() instanceof FallingBlock || bi.getBlock() instanceof BaseEntityBlock) return false;
		return bi.getBlock().defaultBlockState().isCollisionShapeFullBlock(mc.level, BlockPos.ZERO);
	}

	private int findSlot(Minecraft mc, Inventory inv) {
		if (usable(mc, inv.getItem(inv.getSelectedSlot()))) return inv.getSelectedSlot();
		for (int i = 0; i < 9; i++) if (usable(mc, inv.getItem(i))) return i;
		return -1;
	}

	private boolean replaceable(Minecraft mc, BlockPos pos) {
		BlockState s = mc.level.getBlockState(pos);
		return s.isAir() || s.canBeReplaced();
	}

	/** Real rays from the eye at points on one face of nb; returns one that really hits that block on that face, within reach. */
	private BlockHitResult findFace(Minecraft mc, LocalPlayer p, Vec3 eye, BlockPos nb, Direction face, double reach) {
		Vec3 c = Vec3.atCenterOf(nb).add(face.getStepX() * 0.5, face.getStepY() * 0.5, face.getStepZ() * 0.5);
		Direction.Axis ax = face.getAxis();
		Vec3 a1 = ax == Direction.Axis.X ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0);
		Vec3 a2 = ax == Direction.Axis.Z ? new Vec3(0, 1, 0) : new Vec3(0, 0, 1);
		for (int n = 0; n < SAMPLES.length; n++) {
			double[] s = SAMPLES[(sampleStart + n) % SAMPLES.length];
			Vec3 pt = c.add(a1.scale(s[0])).add(a2.scale(s[1]));
			Vec3 dir = pt.subtract(eye);
			if (dir.lengthSqr() < 1.0E-6) continue;
			Vec3 end = eye.add(dir.normalize().scale(reach));
			BlockHitResult hr = mc.level.clip(new ClipContext(eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, p));
			if (hr.getType() == HitResult.Type.BLOCK && hr.getBlockPos().equals(nb) && hr.getDirection() == face) return hr;
		}
		return null;
	}

	@Override
	protected void onTick() {
		Minecraft mc = mc();
		LocalPlayer p = mc.player;
		if (!Util.canAct(mc) || p.getAbilities().flying) { waitTicks = 0; return; }
		if (System.currentTimeMillis() < nextPlace) return;

		Vec3 v = p.getDeltaMovement();
		BlockPos[] targets = {
				BlockPos.containing(p.getX() + v.x * 1.5, p.getY() - 0.5, p.getZ() + v.z * 1.5),
				BlockPos.containing(p.getX(), p.getY() - 0.5, p.getZ())
		};
		Vec3 eye = p.getEyePosition();
		double reach = p.blockInteractionRange();

		for (BlockPos t : targets) {
			if (!replaceable(mc, t)) continue;
			for (Direction d : Direction.values()) {
				if (d == Direction.UP) continue;
				BlockPos nb = t.relative(d);
				if (replaceable(mc, nb)) continue;
				BlockHitResult hr = findFace(mc, p, eye, nb, d.getOpposite(), reach);
				if (hr == null) continue;

				Inventory inv = p.getInventory();
				int slot = findSlot(mc, inv);
				if (slot < 0) return;
				if (slot != inv.getSelectedSlot()) {
					if (prevSlot < 0) prevSlot = inv.getSelectedSlot();
					inv.setSelectedSlot(slot);
				}

				// ease the server-side view toward the face first; place once it has arrived
				if (rotation.is("Server")) {
					float[] r = Util.rotTo(eye, hr.getLocation());
					Rotations.set(r[0], r[1], 2);
					if (!Rotations.closeTo(r[0], r[1], 10f) && ++waitTicks <= MAX_WAIT_TICKS) return;
				}
				waitTicks = 0;

				mc.gameMode.useItemOn(p, InteractionHand.MAIN_HAND, hr);
				p.swing(InteractionHand.MAIN_HAND);

				ThreadLocalRandom rnd = ThreadLocalRandom.current();
				long lo = (long) Math.min(minDelay.get(), maxDelay.get()), hi = (long) Math.max(minDelay.get(), maxDelay.get());
				nextPlace = System.currentTimeMillis() + lo + (long) (rnd.nextDouble() * (hi - lo));
				sampleStart = rnd.nextInt(SAMPLES.length);   // aim a slightly different spot on the next block
				return;
			}
		}
		waitTicks = 0;
	}
}
