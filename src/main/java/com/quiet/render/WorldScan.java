package com.quiet.render;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.EnderChestBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;

/** Throttled scan of loaded block entities (chests etc.) shared by Storage ESP and Tracers. */
public final class WorldScan {
	public record Box(AABB box, int color) {}

	public static final int CHEST = 1, ENDER = 2, SHULKER = 4, BARREL = 8, HOPPER = 16;

	private static List<Box> cache = List.of();
	private static long at;
	private static int cachedFlags = -1, cachedRange;

	public static List<Box> storage(Minecraft mc, int range, int flags) {
		long now = System.currentTimeMillis();
		if (now - at < 400 && flags == cachedFlags && range == cachedRange) return cache;
		at = now; cachedFlags = flags; cachedRange = range;

		List<Box> out = new ArrayList<>();
		int pcx = Mth.floor(mc.player.getX()) >> 4, pcz = Mth.floor(mc.player.getZ()) >> 4;
		int cr = range / 16 + 1;
		double r2 = (double) range * range;
		for (int cx = pcx - cr; cx <= pcx + cr; cx++) {
			for (int cz = pcz - cr; cz <= pcz + cr; cz++) {
				LevelChunk chunk = mc.level.getChunkSource().getChunkNow(cx, cz);
				if (chunk == null) continue;
				for (BlockEntity be : chunk.getBlockEntities().values()) {
					int color = 0;
					if ((flags & ENDER) != 0 && be instanceof EnderChestBlockEntity) color = 0xFFB066FF;
					else if ((flags & CHEST) != 0 && be instanceof ChestBlockEntity) color = 0xFFFFA64D;
					else if ((flags & SHULKER) != 0 && be instanceof ShulkerBoxBlockEntity) color = 0xFFFF7ACB;
					else if ((flags & BARREL) != 0 && be instanceof BarrelBlockEntity) color = 0xFFD8AF93;
					else if ((flags & HOPPER) != 0 && be instanceof HopperBlockEntity) color = 0xFF9AA0A8;
					if (color == 0) continue;
					if (be.getBlockPos().distToCenterSqr(mc.player.position()) > r2) continue;
					out.add(new Box(new AABB(be.getBlockPos()), color));
				}
			}
		}
		cache = out;
		return out;
	}
}
