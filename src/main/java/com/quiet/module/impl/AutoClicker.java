package com.quiet.module.impl;

import com.quiet.module.Category;
import com.quiet.module.Module;
import com.quiet.module.Setting;
import com.quiet.module.Util;

import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.HitResult;

import java.util.concurrent.ThreadLocalRandom;

public class AutoClicker extends Module {

    private final Setting.Mode mode =
            mode("Mode", "CPS", "CPS", "Cooldown");

    private final Setting.Num cps =
            num("CPS", 20, 1, 20, 1);

    private final Setting.Num cooldown =
            num("Cooldown %", 87, 50, 100, 1);

    private final Setting.Num deviation =
            num("Deviation %", 20, 0, 100, 5);

    private double acc;
    private double mood = 1.0;
    private double cdTarget = -1.0;

    private int moodTicks;
    private long lastClickMs;
    private long nextCooldownClick;

    public AutoClicker() {
        super("Auto Clicker", Category.COMBAT);

        cps.visibleWhen(() -> mode.is("CPS"));
        cooldown.visibleWhen(() -> mode.is("Cooldown"));
    }

    @Override
    protected void onTick() {
        Minecraft mc = mc();

        if (!Util.canAct(mc)) {
            resetTiming();
            return;
        }

        if (!mc.options.keyAttack.isDown()) {
            resetTiming();
            return;
        }

        HitResult hr = mc.hitResult;

        // Let Minecraft handle block mining normally.
        if (hr == null || hr.getType() == HitResult.Type.BLOCK) {
            if (mode.is("Cooldown")) {
                cdTarget = -1.0;
            }
            return;
        }

        if (mc.player.isUsingItem()) {
            return;
        }

        ThreadLocalRandom rnd = ThreadLocalRandom.current();

        double dev = deviation.get() / 100.0;
        boolean click;

        Entity target = mc.crosshairPickEntity;

        if (mode.is("Cooldown")) {

            // Never repeatedly swing at empty space in Cooldown mode.
            if (target == null) {
                cdTarget = -1.0;
                return;
            }

            // Randomize the cooldown threshold for each attack.
            if (cdTarget < 0.0) {
                cdTarget = nextCooldown(rnd, dev);
            }

            click = Util.cooldown(mc) >= cdTarget;

            if (click) {
                cdTarget = nextCooldown(rnd, dev);
            }

        } else {

            // Occasionally choose a new speed, never above configured CPS.
            if (--moodTicks <= 0) {
                mood = 1.0 - rnd.nextDouble() * dev;
                moodTicks = 20 + rnd.nextInt(40);
            }

            double rate = Math.max(
                    1.0,
                    Math.min(cps.get(), cps.get() * mood)
            );

            acc += rate / 20.0;

            click = acc >= 1.0;

            if (click) {
                acc -= 1.0;
            }
        }

        // Prevent multiple attacks from bunching together after lag.
        long nowMs = System.currentTimeMillis();

        if (click && nowMs - lastClickMs < 40) {
            click = false;
        }

        // Respect the configured CPS cap in Cooldown mode too.
        if (click && mode.is("Cooldown")
                && nowMs < nextCooldownClick) {
            click = false;
        }

        if (!click) {
            return;
        }

        lastClickMs = nowMs;

        if (mode.is("Cooldown")) {
            nextCooldownClick = nowMs
                    + (long) (1000.0 / Math.max(1.0, cps.get()));
        }

        if (target != null) {
            Util.attack(mc, target);
        } else if (mode.is("CPS")) {
            mc.player.swing(InteractionHand.MAIN_HAND);
        }
    }

    private double nextCooldown(
            ThreadLocalRandom rnd,
            double dev
    ) {
        double base = cooldown.get() / 100.0;

        // Deviation changes the threshold by up to 10 percentage
        // points at 100% deviation, while keeping it in range.
        double variation = (rnd.nextDouble() * 2.0 - 1.0)
                * dev * 0.10;

        return Math.max(
                0.50,
                Math.min(1.0, base + variation)
        );
    }

    private void resetTiming() {
        acc = 0.0;
        mood = 1.0;
        moodTicks = 0;
        cdTarget = -1.0;
        lastClickMs = 0L;
        nextCooldownClick = 0L;
    }
}