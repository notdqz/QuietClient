
package com.quiet.render;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.quiet.Quiet;
import com.quiet.mixin.GameRendererAccessorMixin;
import com.quiet.mixin.PostChainAccessorMixin;
import com.quiet.mixin.PostPassAccessorMixin;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class MotionBlurRenderer {
    private static final String UNIFORM = "MotionBlurConfig";

    private static PostChain chain;
    private static GpuBuffer blendBuffer;
    private static boolean historyPrimed;
    private static boolean errorLogged;

    private static int lastWidth = -1;
    private static int lastHeight = -1;

    private MotionBlurRenderer() {}

    public static void render(int level) {
        Minecraft mc = Minecraft.getInstance();

        if (level < 1 || level > 5 || mc.player == null || mc.level == null) {
            resetHistory();
            return;
        }

        RenderTarget target = mc.getMainRenderTarget();

        if (target.width != lastWidth || target.height != lastHeight) {
            lastWidth = target.width;
            lastHeight = target.height;
            historyPrimed = false;
        }

        PostChain effect = getChain(mc);
        if (effect == null || blendBuffer == null) {
            return;
        }

        // Start with a clean history frame to avoid blending stale pixels.
        float blend = historyPrimed ? getBlendStrength(level) : 0.0F;

        writeBlendFactor(blend);

        GraphicsResourceAllocator allocator =
                ((GameRendererAccessorMixin) mc.gameRenderer)
                        .quiet$getResourcePool();

        effect.process(target, allocator);
        historyPrimed = true;
    }

    private static float getBlendStrength(int level) {
        return switch (level) {
            case 1 -> 0.60F;
            case 2 -> 0.68F;
            case 3 -> 0.75F;
            case 4 -> 0.81F;
            case 5 -> 0.86F;
            default -> 0.75F;
        };
    }

    public static void resetHistory() {
        historyPrimed = false;
        lastWidth = -1;
        lastHeight = -1;
    }

    private static PostChain getChain(Minecraft mc) {
        if (blendBuffer != null && blendBuffer.isClosed()) {
            invalidate();
        }

        if (chain != null && blendBuffer != null) {
            return chain;
        }

        try {
            chain = Objects.requireNonNull(
                    mc.getShaderManager().getPostChain(
                            Quiet.id("motion_blur"),
                            Set.of(PostChain.MAIN_TARGET_ID)
                    ),
                    "Motion blur post chain could not be loaded"
            );

            blendBuffer = RenderSystem.getDevice().createBuffer(
                    () -> "Quiet motion blur strength",
                    GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_MAP_WRITE,
                    4
            );

            patchUniforms(chain, blendBuffer);
            errorLogged = false;
            return chain;
        } catch (RuntimeException exception) {
            if (blendBuffer != null) {
                blendBuffer.close();
                blendBuffer = null;
            }

            chain = null;

            if (!errorLogged) {
                Quiet.LOGGER.error(
                        "Failed to initialize motion blur",
                        exception
                );
                errorLogged = true;
            }

            return null;
        }
    }

    private static void patchUniforms(
            PostChain postChain,
            GpuBuffer buffer
    ) {
        boolean patched = false;

        for (PostPass pass :
                ((PostChainAccessorMixin) postChain).quiet$getPasses()) {

            Map<String, GpuBuffer> uniforms =
                    ((PostPassAccessorMixin) pass).quiet$getCustomUniforms();

            if (!uniforms.containsKey(UNIFORM)) {
                continue;
            }

            GpuBuffer old = uniforms.put(UNIFORM, buffer);

            if (old != null && old != buffer) {
                old.close();
            }

            patched = true;
        }

        if (!patched) {
            throw new IllegalStateException(
                    "MotionBlurConfig uniform was not found in the post chain"
            );
        }
    }

    private static void writeBlendFactor(float value) {
        try (GpuBuffer.MappedView mapped =
                     RenderSystem.getDevice()
                             .createCommandEncoder()
                             .mapBuffer(blendBuffer, false, true)) {
            Std140Builder.intoBuffer(mapped.data()).putFloat(value);
        }
    }

    private static void invalidate() {
        if (blendBuffer != null) {
            blendBuffer.close();
        }

        blendBuffer = null;
        chain = null;
        historyPrimed = false;
    }
}
