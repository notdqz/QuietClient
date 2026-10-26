
package com.quiet.module.impl;

import com.quiet.module.Category;
import com.quiet.module.Module;
import com.quiet.module.Util;
import com.quiet.module.Setting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class SafeWalk extends Module {
    private static final double STEP = 0.3;

    private final Setting.Bool moonwalkOnly = bool("Moonwalk Only", false);
    private boolean forced;

    public SafeWalk() {
        super("Safe Walk", Category.MOVEMENT);
    }

    @Override
    protected void onDisable() {
        if (forced) {
            forced = false;
            Util.releaseKey(mc().options.keyShift);
        }
    }

    /**
     * True if shifting the hitbox by (dx, dz) would leave nothing to stand on.
     */
    private static boolean dropsAway(
            Minecraft mc, LocalPlayer p, AABB box, double dx, double dz) {
        return mc.level.noCollision(p, box.move(dx, -0.6, dz));
    }


@Override
protected void onTick() {
    Minecraft mc = mc();
    LocalPlayer p = mc.player;

    boolean movingBackward = mc.options.keyDown.isDown();
    boolean edge = false;

    if (mc.screen == null && p != null
            && p.onGround() && !p.getAbilities().flying) {
        AABB box = p.getBoundingBox();
        Vec3 v = p.getDeltaMovement();

        edge = dropsAway(mc, p, box, STEP, 0)
                || dropsAway(mc, p, box, -STEP, 0)
                || dropsAway(mc, p, box, 0, STEP)
                || dropsAway(mc, p, box, 0, -STEP)
                || (Math.hypot(v.x, v.z) > 0.02
                && dropsAway(mc, p, box, v.x * 1.3, v.z * 1.3));
    }

    // Moonwalk Only restricts starting SafeWalk, not maintaining it.
    boolean shouldCrouch = moonwalkOnly.get()
            ? (forced ? edge : movingBackward && edge)
            : edge;

    if (shouldCrouch) {
        mc.options.keyShift.setDown(true);
        forced = true;
    } else if (forced) {
        forced = false;
        Util.releaseKey(mc.options.keyShift);
    }
}

}
