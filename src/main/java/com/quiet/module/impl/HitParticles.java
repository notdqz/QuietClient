package com.quiet.module.impl;

import com.quiet.module.Category;
import com.quiet.module.Module;
import com.quiet.module.setting.ModeSetting;
import com.quiet.module.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.concurrent.ThreadLocalRandom;

/** Extra particles on every hit you land (hooked from MultiPlayerGameModeMixin, so clicks and modules both count). */
public class HitParticles extends Module {
    private static HitParticles inst;

    private final ModeSetting style = mode("Style", "Cross", "Crit", "Sharp", "Sparkle", "Cross", "Ring", "Hearts");
    private final NumberSetting amount = num("Amount", 12, 4, 40, 2);

    public HitParticles() { super("Hit Particles", Category.RENDER); inst = this; }

    public static void onAttack(Player attacker, Entity target) {
        if (inst == null || !inst.isEnabled() || !(target instanceof LivingEntity)) return;
        inst.spawn(Minecraft.getInstance(), attacker, target);
    }

    private void add(Minecraft mc, ParticleOptions t, Vec3 p, double dx, double dy, double dz) {
        mc.level.addParticle(t, p.x, p.y, p.z, dx, dy, dz);
    }

    private void spawn(Minecraft mc, Player attacker, Entity target) {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        Vec3 c = new Vec3(target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ());
        int n = amount.asInt();
        
        switch (style.mode()) {
            case "Crit", "Sharp" -> {
                ParticleOptions t = style.is("Crit") ? ParticleTypes.CRIT : ParticleTypes.ENCHANTED_HIT;
                for (int i = 0; i < n; i++)
                    add(mc, t, c, r.nextDouble(-0.6, 0.6), r.nextDouble(-0.2, 0.6), r.nextDouble(-0.6, 0.6));
            }
            case "Sparkle" -> {
                for (int i = 0; i < n; i++)
                    add(mc, ParticleTypes.END_ROD, c, r.nextDouble(-0.12, 0.12), r.nextDouble(-0.05, 0.15), r.nextDouble(-0.12, 0.12));
            }
            case "Hearts" -> {
                for (int i = 0; i < Math.max(2, n / 4); i++)
                    add(mc, ParticleTypes.HEART, c.add(r.nextDouble(-0.4, 0.4), r.nextDouble(0, 0.4), r.nextDouble(-0.4, 0.4)), 0, 0.05, 0);
            }
            case "Ring" -> {
                double radius = 0.6;
                for (int i = 0; i < n; i++) {
                    double angle = i * Math.PI * 2 / n;
                    double offsetX = Math.cos(angle) * radius;
                    double offsetZ = Math.sin(angle) * radius;
                    
                    Vec3 spawnPos = new Vec3(target.getX() + offsetX, target.getY() + (target.getBbHeight() * 0.4), target.getZ() + offsetZ);
                    add(mc, ParticleTypes.END_ROD, spawnPos, offsetX * 0.1, 0.02, offsetZ * 0.1);
                }
            }
            default -> {   // Cross: Forms a literal Christian cross facing you
                Vec3 toMe = attacker.position().subtract(target.position());
                Vec3 right = new Vec3(-toMe.z, 0, toMe.x);
                right = right.lengthSqr() < 1e-4 ? new Vec3(1, 0, 0) : right.normalize();
                
                int steps = Math.max(4, n / 2);
                
                // 1. Vertical post (spanning from bottom to top)
                for (int i = -steps; i <= steps; i++) {
                    double f = i * 0.6 / steps;
                    add(mc, ParticleTypes.END_ROD, c.add(0, f, 0), 0, 0, 0);
                }
                
                // 2. Horizontal crossbeam (placed higher up on the vertical post)
                int armSteps = Math.max(2, steps / 2);
                for (int i = -armSteps; i <= armSteps; i++) {
                    double f = i * 0.35 / armSteps;
                    add(mc, ParticleTypes.END_ROD, c.add(right.scale(f)).add(0, 0.3, 0), 0, 0, 0);
                }
            }
        }
    }
}