
package com.quiet.module.impl;

import com.quiet.Quiet;
import com.quiet.module.Category;
import com.quiet.module.Module;
import com.quiet.module.Setting;
import com.quiet.render.MotionBlurRenderer;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;

public class MotionBlur extends Module {
    private static MotionBlur instance;
    private static boolean hooked;

    private final Setting.Num strength =
            num("Strength", 3, 1, 5, 1);

    public MotionBlur() {
        super("Motion Blur", Category.VISUAL);
        instance = this;

        if (!hooked) {
            hooked = true;

            HudElementRegistry.addLast(
                    Quiet.id("motion_blur"),
                    (graphics, tickCounter) -> {
                        MotionBlur module = instance;

                        if (module == null || !module.isEnabled()) {
                            MotionBlurRenderer.render(0);
                            return;
                        }

                        int level = (int) module.strength.get().doubleValue();
                        MotionBlurRenderer.render(level);
                    }
            );
        }
    }

    @Override
    protected void onDisable() {
        MotionBlurRenderer.resetHistory();
    }
}
