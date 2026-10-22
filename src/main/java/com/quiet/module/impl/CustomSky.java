package com.quiet.module.impl;

import com.quiet.module.Category;
import com.quiet.module.Module;
import com.quiet.module.setting.BooleanSetting;
import com.quiet.module.setting.ModeSetting;
import com.quiet.module.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.state.SkyRenderState;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.dimension.DimensionType;
import org.joml.Vector4f;

import java.util.concurrent.ThreadLocalRandom;

public class CustomSky extends Module {
    private static CustomSky inst;

    private final ModeSetting preset = mode("Preset", "Nebula", "Nebula", "Cyberpunk", "Astral Void", "Midnight", "Blood Moon", "Gothic", "Overcast", "Sunny");
    private final NumberSetting fogDensity = num("Fog Density", 1.2, 0.4, 3.0, 0.1);
    private final BooleanSetting stars = bool("Stars & Galaxies", true);
    private final BooleanSetting hideCelestials = bool("Hide Sun/Moon", true);
    private final BooleanSetting hideClouds = bool("Hide Clouds", true);
    private final BooleanSetting endSky = bool("End Skybox Override", false);
    private final ModeSetting particles = mode("Particles", "Sparkle", "None", "Ash", "White Ash", "Snow", "Sparkle", "Portal");
    private final NumberSetting density = num("Particle Density", 4, 1, 10, 1);

    public CustomSky() {
        super("Custom Sky", Category.RENDER);
        inst = this;
    }

    public static boolean active() {
        return inst != null && inst.isEnabled();
    }

    public static boolean shouldHideClouds() {
        return active() && inst.hideClouds.get();
    }

    public static boolean shouldHideCelestials() {
        return active() && inst.hideCelestials.get();
    }

    private int skyRgb() {
        return switch (preset.mode()) {
            case "Cyberpunk" -> 0x0D001A;
            case "Astral Void" -> 0x010103;
            case "Midnight" -> 0x020512;
            case "Blood Moon" -> 0x1A0303;
            case "Overcast" -> 0x4F555C;
            case "Sunny" -> 0x4A85E6;
            case "Gothic" -> 0x0F081A;
            default -> 0x050814;
        };
    }

    private int horizonRgb() {
        return switch (preset.mode()) {
            case "Cyberpunk" -> 0xD9006C;
            case "Astral Void" -> 0x240042;
            case "Midnight" -> 0x09173D;
            case "Blood Moon" -> 0x540909;
            case "Overcast" -> 0x6E757D;
            case "Sunny" -> 0x94BFFF;
            case "Gothic" -> 0x33144A;
            default -> 0x351261;
        };
    }

    private int fogRgb() {
        return horizonRgb();
    }

    public static void applySky(SkyRenderState s) {
        if (!active()) return;
        
        int mainColor = 0xFF000000 | inst.skyRgb();
        int horizonColor = 0xFF000000 | inst.horizonRgb();

        s.skyColor = mainColor;
        s.sunriseAndSunsetColor = horizonColor; 
        
        if (inst.stars.get()) {
            s.starBrightness = 1.4f; 
        } else {
            s.starBrightness = 0.0f;
        }
        
        if (inst.endSky.get()) {
            s.skybox = DimensionType.Skybox.END;
        }
    }

    private static boolean inAir() {
        LocalPlayer p = Minecraft.getInstance().player;
        return p != null && !p.isEyeInFluid(FluidTags.WATER) && !p.isEyeInFluid(FluidTags.LAVA);
    }

    public static Vector4f fogColor(Vector4f original) {
        if (!active() || !inAir()) return original;
        int c = inst.fogRgb();
        return new Vector4f((c >> 16 & 0xFF) / 255f, (c >> 8 & 0xFF) / 255f, (c & 0xFF) / 255f, 1f);
    }

    public static float fogDistance(float v) {
        if (!active() || !inAir()) return v;
        // DIVISION brings fog closer, perfectly masking unrendered chunk boundaries
        return (float) (v / inst.fogDensity.get());
    }

    @Override
    protected void onTick() {
        if (particles.is("None")) return;
        Minecraft mc = mc();
        LocalPlayer p = mc.player;
        if (p == null || mc.level == null) return;

        ParticleOptions particleType = switch (particles.mode()) {
            case "White Ash" -> ParticleTypes.WHITE_ASH;
            case "Snow" -> ParticleTypes.SNOWFLAKE;
            case "Sparkle" -> ParticleTypes.END_ROD;
            case "Portal" -> ParticleTypes.PORTAL;
            default -> ParticleTypes.ASH;
        };

        ThreadLocalRandom rand = ThreadLocalRandom.current();
        for (int i = 0; i < density.asInt(); i++) {
            double x = p.getX() + rand.nextDouble(-20, 20);
            double y = p.getY() + rand.nextDouble(-4, 12);
            double z = p.getZ() + rand.nextDouble(-20, 20);
            
            mc.level.addParticle(
                particleType, 
                x, y, z, 
                rand.nextDouble(-0.005, 0.005), 
                rand.nextDouble(-0.01, 0.002), 
                rand.nextDouble(-0.005, 0.005)
            );
        }
    }
}