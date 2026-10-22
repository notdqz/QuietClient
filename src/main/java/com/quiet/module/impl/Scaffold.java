package com.quiet.module.impl;

import com.quiet.module.Category;
import com.quiet.module.Module;
import com.quiet.module.setting.ModeSetting;
import com.quiet.module.setting.NumberSetting;
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

public class Scaffold extends Module {
	private final NumberSetting delay = num("Delay (ticks)", 0, 0, 5, 1);
	private final ModeSetting rotation = mode("Rotation", "Server + Visual", "Off", "Server", "Server + Visual");
	private int cd, prevSlot = -1;

	private static final double[] SAMPLES = {0, 0.3, -0.3};

	public Scaffold() { super("Scaffold", Category.MOVEMENT); }

	@Override
	protected void onDisable() {
		Minecraft mc = mc();
		if (prevSlot >= 0 && mc.player != null) mc.player.getInventory().setSelectedSlot(prevSlot);
		prevSlot = -1;
		cd = 0;
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

	/**
	 * Casts real rays from the eye at several points on one face of `nb`. Only a ray that actually
	 * hits that block, on that face, within block reach counts, so it never places on a hidden face
	 * or from out of reach.
	 */
	private BlockHitResult findFace(Minecraft mc, LocalPlayer p, Vec3 eye, BlockPos nb, Direction face, double reach) {
		Vec3 c = Vec3.atCenterOf(nb).add(face.getStepX() * 0.5, face.getStepY() * 0.5, face.getStepZ() * 0.5);
		Direction.Axis ax = face.getAxis();
		Vec3 a1 = ax == Direction.Axis.X ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0);
		Vec3 a2 = ax == Direction.Axis.Z ? new Vec3(0, 1, 0) : new Vec3(0, 0, 1);
		for (double u : SAMPLES) {
			for (double w : SAMPLES) {
				Vec3 pt = c.add(a1.scale(u)).add(a2.scale(w));
				Vec3 dir = pt.subtract(eye);
				if (dir.lengthSqr() < 1.0E-6) continue;
				Vec3 end = eye.add(dir.normalize().scale(reach));
				BlockHitResult hr = mc.level.clip(new ClipContext(eye, end,
						ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, p));
				if (hr.getType() == HitResult.Type.BLOCK && hr.getBlockPos().equals(nb) && hr.getDirection() == face)
					return hr;
			}
		}
		return null;
	}

	@Override
	protected void onTick() {
		Minecraft mc = mc();
		LocalPlayer p = mc.player;
		if (!ModUtil.canAct(mc) || p.getAbilities().flying) return;
		if (cd > 0) { cd--; return; }

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
				if (!rotation.is("Off")) {
					float[] r = ModUtil.rotTo(eye, hr.getLocation());
					Rotations.set(r[0], r[1], 1, rotation.is("Server + Visual"));
				}
				mc.gameMode.useItemOn(p, InteractionHand.MAIN_HAND, hr);
				p.swing(InteractionHand.MAIN_HAND);
				cd = delay.asInt();
				return;
			}
		}
	}
}
