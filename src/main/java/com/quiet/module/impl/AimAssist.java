package com.quiet.module.impl;

import com.quiet.module.Category;
import com.quiet.module.Module;
import com.quiet.module.setting.BooleanSetting;
import com.quiet.module.setting.ModeSetting;
import com.quiet.module.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public class AimAssist extends Module {
    private final NumberSetting range = num("Range", 4.5, 2, 8, 0.1);
    private final NumberSetting fov = num("FOV", 60, 10, 180, 5);
    private final NumberSetting speed = num("Smoothing", 12, 1, 50, 1);
    private final ModeSetting aim = mode("Aim at", "Head", "Head", "Body", "Legs");
    private final BooleanSetting clickOnly = bool("Click only", true);

    public AimAssist() { super("Aim Assist", Category.COMBAT); }

    @Override
    protected void onTick() {
        Minecraft mc = mc();
        if (!ModUtil.canAct(mc)) return;
        if (clickOnly.get() && !mc.options.keyAttack.isDown()) return;
        LocalPlayer me = mc.player;

        Player best = null;
        float bestYaw = 0, bestPitch = 0;
        double bestAng = fov.get();
        Vec3 eye = me.getEyePosition();
        for (Player p : mc.level.players()) {
            if (!ModUtil.validTarget(mc, p) || me.distanceTo(p) > range.get()) continue;
            float[] r = rotTo(eye, aimPoint(p));
            float dy = Mth.wrapDegrees(r[0] - me.getYRot());
            float dp = r[1] - me.getXRot();
            double ang = Math.hypot(dy, dp);
            if (ang < bestAng) { bestAng = ang; best = p; bestYaw = dy; bestPitch = dp; }
        }
        if (best == null) return;

        // Use a smoother interpolation step with safety clamping
        float smoothFactor = (float) Math.max(1.0, speed.get());
        
        float yawStep = Mth.clamp(bestYaw / smoothFactor, -30f, 30f);
        float pitchStep = Mth.clamp(bestPitch / smoothFactor, -30f, 30f);

        me.setYRot(me.getYRot() + yawStep);
        me.setXRot(Mth.clamp(me.getXRot() + pitchStep, -90f, 90f));
    }

    private Vec3 aimPoint(Player p) {
        double f = aim.is("Head") ? 0.92 : aim.is("Body") ? 0.55 : 0.15;
        return new Vec3(p.getX(), p.getY() + p.getBbHeight() * f, p.getZ());
    }

    private static float[] rotTo(Vec3 from, Vec3 to) {
        double dx = to.x - from.x, dy = to.y - from.y, dz = to.z - from.z;
        float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.hypot(dx, dz)));
        return new float[]{Mth.wrapDegrees(yaw), pitch};
    }
}