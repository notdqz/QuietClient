package com.quiet.module.impl;

import com.quiet.module.Category;
import com.quiet.module.Module;
import com.quiet.module.setting.NumberSetting;
import com.quiet.render.WorldBatch;
import com.quiet.render.WorldRender;
import net.minecraft.client.Minecraft;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Clean, traditional X-Ray module. Hides/fades non-ore blocks 
 * and relies on mixins for true chunk-level rendering transparency.
 */
public class XRay extends Module implements WorldRender.Layer {

    public static final ThreadLocal<Integer> ALPHA = ThreadLocal.withInitial(() -> -1);

    private static XRay instance;
    private static volatile int cachedOpacity255 = 0; // Default to 0 (fully hidden non-ores)

    // Only keep the opacity setting—no cluttered toggles or scan ranges
    private final NumberSetting opacity = num("Opacity", 0, 0, 100, 5);

    private int previousOpacity = -1;

    public XRay() {
        super("X-ray", Category.RENDER);
        instance = this;
        // No WorldRender layer needed since we removed outlines and scanning!
    }

    public static boolean isActive() {
        return instance != null && instance.isEnabled();
    }

    // Mixin compatibility aliases
    public static boolean active() {
        return isActive();
    }

    public static int alpha(BlockState state) {
        return getAlpha(state);
    }

    public static int getAlpha(BlockState state) {
        if (!isActive() || isOre(state)) return -1;
        return cachedOpacity255;
    }

    public static boolean isOre(BlockState state) {
        if (instance == null || state == null) return false;

        // Automatically catch all standard ores and valuable blocks via tags and direct checks
        return state.is(BlockTags.COAL_ORES)
                || state.is(BlockTags.IRON_ORES)
                || state.is(BlockTags.COPPER_ORES)
                || state.is(BlockTags.GOLD_ORES)
                || state.is(BlockTags.REDSTONE_ORES)
                || state.is(BlockTags.LAPIS_ORES)
                || state.is(BlockTags.DIAMOND_ORES)
                || state.is(BlockTags.EMERALD_ORES)
                || state.is(Blocks.NETHER_GOLD_ORE)
                || state.is(Blocks.NETHER_QUARTZ_ORE)
                || state.is(Blocks.ANCIENT_DEBRIS)
                // Optional utility blocks commonly wanted on X-Ray:
                || state.is(Blocks.SPAWNER)
                || state.is(Blocks.CHEST)
                || state.is(Blocks.TRAPPED_CHEST)
                || state.is(Blocks.ENDER_CHEST);
    }

    private void scheduleChunkReload() {
        Minecraft mc = mc();
        if (mc.levelRenderer != null) {
            mc.levelRenderer.allChanged();
        }
    }

    @Override
    protected void onEnable() {
        updateOpacityCache();
        previousOpacity = opacity.asInt();
        scheduleChunkReload();
    }

    @Override
    protected void onDisable() {
        cachedOpacity255 = 255;
        scheduleChunkReload();
    }

    private void updateOpacityCache() {
        // Converts 0-100% to 0-255 alpha value
        cachedOpacity255 = Math.round(opacity.asInt() * 2.55f);
    }

    @Override
    protected void onTick() {
        updateOpacityCache();
        int currentOpacity = opacity.asInt();
        if (currentOpacity != previousOpacity) {
            previousOpacity = currentOpacity;
            scheduleChunkReload();
        }
    }

    // Empty implementation since WorldRender.Layer isn't needed anymore without outlines
    @Override
    public void extract(WorldBatch batch, Minecraft mc, float partialTicks) {
        // Unused
    }
}