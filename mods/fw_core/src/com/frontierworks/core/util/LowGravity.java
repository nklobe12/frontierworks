package com.frontierworks.core.util;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Reduced gravity for whole dimensions: living entities get a small upward push each tick and take less fall damage. */
public final class LowGravity {
    private static final Map<String, Double> DIMS = new ConcurrentHashMap<>();
    private static boolean registered;

    private LowGravity() {}

    /** lift is subtracted from vanilla gravity (0.08 per tick); 0.05 gives roughly 40% gravity. */
    public static synchronized void register(String dimension, double lift) {
        DIMS.put(dimension, lift);
        if (!registered) {
            registered = true;
            MinecraftForge.EVENT_BUS.addListener(LowGravity::onTick);
            MinecraftForge.EVENT_BUS.addListener(LowGravity::onFall);
        }
    }

    private static Double lift(LivingEntity e) {
        return DIMS.get(e.level().dimension().location().toString());
    }

    private static void onTick(LivingEvent.LivingTickEvent event) {
        LivingEntity e = event.getEntity();
        Double lift = lift(e);
        if (lift == null || e.onGround() || e.isNoGravity() || e.isInWater() || e.isFallFlying()) return;
        if (e instanceof Player p && p.getAbilities().flying) return;
        Vec3 v = e.getDeltaMovement();
        e.setDeltaMovement(v.x, v.y + lift, v.z);
    }

    private static void onFall(LivingFallEvent event) {
        Double lift = lift(event.getEntity());
        if (lift != null) event.setDistance(event.getDistance() * 0.35f);
    }
}
