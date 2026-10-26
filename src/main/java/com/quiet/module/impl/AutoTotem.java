
package com.quiet.module.impl;

import com.mojang.blaze3d.platform.InputConstants;
import com.quiet.Quiet;
import com.quiet.module.Category;
import com.quiet.module.Module;
import com.quiet.module.Setting;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Items;
import org.lwjgl.glfw.GLFW;

import java.util.concurrent.ThreadLocalRandom;

public class AutoTotem extends Module {
    private enum State {
        IDLE,
        HOTBAR_SEND,
        HOTBAR_RESTORE,
        INVENTORY_OPEN_WAIT,
        INVENTORY_MOVE,
        INVENTORY_VERIFY,
        INVENTORY_CLOSE
    }

    private static AutoTotem instance;
    private static boolean renderHooked;

    private final Setting.Num delay = num("Delay (ticks)", 2, 0, 10, 1);
    private final Setting.Bool fromInv = bool("Use inventory", true);

    private State state = State.IDLE;

    private int waitTicks = -1;
    private int previousSlot = -1;
    private int targetSlot = -1;
    private int timer;

    private long openAtNanos;
    private long animationStartNanos;
    private long animationDurationNanos;
    private long hoverDurationNanos;

    private double startX;
    private double startY;
    private double targetX;
    private double targetY;
    private double controlX;
    private double controlY;

    private boolean cursorInitialized;
    private boolean animationFinished;

    public AutoTotem() {
        super("Auto Totem", Category.COMBAT);
        instance = this;

        if (!renderHooked) {
            renderHooked = true;
            HudElementRegistry.addLast(
                    Quiet.id("auto_totem_cursor"),
                    (graphics, tickCounter) -> {
                        AutoTotem module = instance;
                        if (module != null && module.isEnabled()) {
                            module.animateCursorFrame();
                        }
                    }
            );
        }
    }

    @Override
    protected void onDisable() {
        resetCycle();
    }

    private static int findTotem(Inventory inventory, int start, int end) {
        for (int i = start; i < end; i++) {
            if (inventory.getItem(i).is(Items.TOTEM_OF_UNDYING)) {
                return i;
            }
        }
        return -1;
    }

    private void animateCursorFrame() {
        if (state != State.INVENTORY_MOVE
                || !cursorInitialized
                || animationFinished) {
            return;
        }

        Minecraft mc = mc();

        if (!(mc.screen instanceof InventoryScreen)) {
            return;
        }

        long now = System.nanoTime();
        double progress = Math.min(
                1.0,
                (double) (now - animationStartNanos)
                        / animationDurationNanos
        );

        // Smooth acceleration and deceleration.
        double t = progress * progress * (3.0 - 2.0 * progress);
        double inverse = 1.0 - t;

        // Quadratic Bezier curve.
        double x = inverse * inverse * startX
                + 2.0 * inverse * t * controlX
                + t * t * targetX;

        double y = inverse * inverse * startY
                + 2.0 * inverse * t * controlY
                + t * t * targetY;

        GLFW.glfwSetCursorPos(mc.getWindow().handle(), x, y);

        if (progress >= 1.0) {
            GLFW.glfwSetCursorPos(
                    mc.getWindow().handle(),
                    targetX,
                    targetY
            );
            animationFinished = true;
        }
    }

    private void prepareCursor(
            Minecraft mc,
            InventoryScreen screen,
            Slot slot
    ) {
        double scaleX =
                (double) mc.getWindow().getWidth() / screen.width;
        double scaleY =
                (double) mc.getWindow().getHeight() / screen.height;

        startX = mc.mouseHandler.xpos();
        startY = mc.mouseHandler.ypos();

        // Standard centered inventory layout with the recipe book closed.
        int guiLeft = (screen.width - 176) / 2;
        int guiTop = (screen.height - 166) / 2;

        targetX = (guiLeft + slot.x + 8) * scaleX;
        targetY = (guiTop + slot.y + 8) * scaleY;

        double dx = targetX - startX;
        double dy = targetY - startY;
        double distance = Math.sqrt(dx * dx + dy * dy);

        double curve = Math.min(
                65.0,
                Math.max(18.0, distance * 0.13)
        );
        double direction =
                ThreadLocalRandom.current().nextBoolean() ? 1.0 : -1.0;

        controlX = (startX + targetX) * 0.5
                - (dy / Math.max(1.0, distance)) * curve * direction;

        controlY = (startY + targetY) * 0.5
                + (dx / Math.max(1.0, distance)) * curve * direction;

        int configuredDelay = delay.asInt();
        long totalDelayMs = configuredDelay * 50L;

        long movementMs = Math.max(
                180L,
                Math.round(totalDelayMs * 0.45)
        );
        long hoverMs = Math.max(
                50L,
                Math.round(totalDelayMs * 0.10)
        );

        animationDurationNanos = movementMs * 1_000_000L;
        hoverDurationNanos = hoverMs * 1_000_000L;
        animationStartNanos = System.nanoTime();

        cursorInitialized = true;
        animationFinished = false;
    }

    @Override
    protected void onTick() {
        Minecraft mc = mc();
        LocalPlayer player = mc.player;

        if (player == null || mc.level == null
                || mc.getConnection() == null) {
            resetCycle();
            return;
        }

        Inventory inventory = player.getInventory();
        boolean offhandHasTotem =
                player.getOffhandItem().is(Items.TOTEM_OF_UNDYING);

        switch (state) {
            case HOTBAR_SEND -> {
                mc.getConnection().send(new ServerboundPlayerActionPacket(
                        ServerboundPlayerActionPacket.Action.SWAP_ITEM_WITH_OFFHAND,
                        BlockPos.ZERO,
                        Direction.DOWN
                ));

                state = State.HOTBAR_RESTORE;
                return;
            }

            case HOTBAR_RESTORE -> {
                if (previousSlot >= 0) {
                    inventory.setSelectedSlot(previousSlot);
                }

                previousSlot = -1;
                state = State.IDLE;
                waitTicks = -1;
                return;
            }

            case INVENTORY_OPEN_WAIT -> {
                if (System.nanoTime() < openAtNanos) {
                    return;
                }

                if (findTotem(inventory, 9, 36) < 0) {
                    resetCycle();
                    return;
                }

                mc.setScreen(new InventoryScreen(player));
                cursorInitialized = false;
                animationFinished = false;
                state = State.INVENTORY_MOVE;
                return;
            }

            case INVENTORY_MOVE -> {
                if (!(mc.screen instanceof InventoryScreen screen)) {
                    resetCycle();
                    return;
                }

                if (player.getOffhandItem().is(Items.TOTEM_OF_UNDYING)) {
                    beginClose(1);
                    return;
                }

                if (targetSlot < 0
                        || targetSlot >= player.inventoryMenu.slots.size()) {
                    beginClose(1);
                    return;
                }

                Slot slot = player.inventoryMenu.slots.get(targetSlot);

                if (!slot.hasItem()
                        || !slot.getItem().is(Items.TOTEM_OF_UNDYING)) {
                    beginClose(1);
                    return;
                }

                if (!cursorInitialized) {
                    prepareCursor(mc, screen, slot);
                    return;
                }

                // Cursor movement runs in animateCursorFrame(), independently
                // of the tick loop.
                if (!animationFinished) {
                    return;
                }

                long hoverEndsAt = animationStartNanos
                        + animationDurationNanos
                        + hoverDurationNanos;

                if (System.nanoTime() < hoverEndsAt) {
                    return;
                }

                int keyCode = InputConstants.getKey(
                        mc.options.keySwapOffhand.saveString()
                ).getValue();

                screen.keyPressed(new KeyEvent(keyCode, 0, 0));

                timer = 2;
                state = State.INVENTORY_VERIFY;
                return;
            }

            case INVENTORY_VERIFY -> {
                if (!(mc.screen instanceof InventoryScreen)) {
                    resetCycle();
                    return;
                }

                if (player.getOffhandItem().is(Items.TOTEM_OF_UNDYING)) {
                    beginClose(1);
                    return;
                }

                if (timer > 0) {
                    timer--;
                    return;
                }

                // Fallback if the screen did not process the swap key.
                if (targetSlot >= 0
                        && targetSlot < player.inventoryMenu.slots.size()) {
                    Slot slot = player.inventoryMenu.slots.get(targetSlot);

                    if (slot.hasItem()
                            && slot.getItem().is(Items.TOTEM_OF_UNDYING)) {
                        mc.gameMode.handleInventoryMouseClick(
                                player.inventoryMenu.containerId,
                                targetSlot,
                                40,
                                ClickType.SWAP,
                                player
                        );
                    }
                }

                beginClose(2);
                return;
            }

            case INVENTORY_CLOSE -> {
                if (timer > 0) {
                    timer--;
                    return;
                }

                if (mc.screen instanceof InventoryScreen) {
                    mc.setScreen(null);
                }

                resetCycle();
                return;
            }

            case IDLE -> {
                // Continue below.
            }
        }

        if (mc.screen != null || offhandHasTotem) {
            waitTicks = -1;
            return;
        }

        // Start the delay once; do not reset it when it reaches zero.
        if (waitTicks < 0) {
            waitTicks = delay.asInt();
        }

        if (waitTicks > 0) {
            waitTicks--;
            return;
        }

        int hotbarSlot = findTotem(inventory, 0, 9);

        if (hotbarSlot >= 0) {
            previousSlot = inventory.getSelectedSlot();
            inventory.setSelectedSlot(hotbarSlot);
            state = State.HOTBAR_SEND;
            waitTicks = -1;
            return;
        }

        int inventorySlot = findTotem(inventory, 9, 36);

        if (fromInv.get() && inventorySlot >= 0) {
            targetSlot = inventorySlot;

            long openDelayMs = Math.round(delay.asInt() * 50.0 * 0.45);
            openAtNanos = System.nanoTime()
                    + openDelayMs * 1_000_000L;

            state = State.INVENTORY_OPEN_WAIT;
            waitTicks = -1;
        }
    }

    private void beginClose(int ticks) {
        timer = ticks;
        state = State.INVENTORY_CLOSE;
    }

    private void resetCycle() {
        state = State.IDLE;
        waitTicks = -1;
        previousSlot = -1;
        targetSlot = -1;
        timer = 0;
        cursorInitialized = false;
        animationFinished = false;
        hoverDurationNanos = 0;
    }
}
